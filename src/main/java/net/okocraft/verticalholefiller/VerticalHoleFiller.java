package net.okocraft.verticalholefiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VerticalHoleFiller extends JavaPlugin implements Listener {

	private FileConfiguration config;

    private final Map<Player, List<BlockState>> brokenBlocks = new HashMap<>();
    private final Map<Player, Integer> timesNotPass = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		config = getConfig();

		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Listener) this);
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (!isEnabledWorld(event.getBlock().getWorld())) {
			return;
		}

		Player player = event.getPlayer();

		List<BlockState> playersBrokenBlocks = brokenBlocks.get(player);
		if (playersBrokenBlocks == null) {
			playersBrokenBlocks = new ArrayList<>();
			brokenBlocks.put(player, playersBrokenBlocks);
		}

		if (playersBrokenBlocks.size() > 0) {
			Location prevLocation = playersBrokenBlocks.get(0).getLocation();
			Location blockLocation = event.getBlock().getLocation();
			if (!prevLocation.getWorld().equals(blockLocation.getWorld())
					|| prevLocation.distance(blockLocation) > 10) {
				clearHistory(player);
			}
		}

		if (event.getBlock().getY() > player.getLocation().getY()) {
			notPass(player);
			return;
		}

		// ブロックの端っこを見たときのピッチ
		double pitchCondition = 90 - (180 / Math.PI) * Math.atan(0.7 * Math.sqrt(2) / player.getEyeHeight());
		if (player.getLocation().getPitch() < pitchCondition) {
			notPass(player);
			return;
		}

		playersBrokenBlocks.add(mapStateType(event.getBlock().getState()));
		if (playersBrokenBlocks.size() > 8) {
			playersBrokenBlocks.get(0).update(true, true);
			playersBrokenBlocks.remove(0);
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		brokenBlocks.remove(event.getPlayer());
		timesNotPass.remove(event.getPlayer());
	}

	private BlockState mapStateType(BlockState state) {
		switch (state.getType()) {
		case GRASS_BLOCK:
		case DIRT:
		case ANDESITE:
		case DIORITE:
		case GRANITE:
		case SANDSTONE:
			break;
		case SAND:
			state.setType(Material.SANDSTONE);
			break;
		default:
			state.setType(Material.STONE);
			break;
		}

		return state;
	}

	private void notPass(Player player) {
		timesNotPass.put(player, timesNotPass.getOrDefault(player, 0) + 1);
		if (timesNotPass.get(player) >= 2) {
			timesNotPass.remove(player);
			clearHistory(player);
		}
	}

	private void clearHistory(Player player) {
		List<BlockState> playersBrokenBlocks = brokenBlocks.get(player);
		if (playersBrokenBlocks != null) {
			playersBrokenBlocks.clear();
		}
	}

	private boolean isEnabledWorld(World world) {
		for (String pattern : config.getStringList("enabled-worlds")) {
			if (world.getName().matches(pattern)) {
				return true;
			}
		}

		return false;
	}
}
