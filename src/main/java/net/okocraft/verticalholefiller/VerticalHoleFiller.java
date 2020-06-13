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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class VerticalHoleFiller extends JavaPlugin implements Listener {

    private final Map<Player, List<BlockState>> brokenBlocks = new HashMap<>();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		Bukkit.getPluginManager().registerEvents(this, this);
	}

	@Override
	public void onDisable() {
		HandlerList.unregisterAll((Plugin) this);
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!isEnabledWorld(event.getBlock().getWorld())) {
			return;
		}

		// 掘ったブロックが
		BlockState mappedState = mapStateType(event.getBlock().getState()); 
		if (mappedState == null) {
			return;
		}

		Player player = event.getPlayer();
		List<BlockState> playersBrokenBlocks = brokenBlocks.get(player);
		if (playersBrokenBlocks == null) {
			playersBrokenBlocks = new ArrayList<>();
			brokenBlocks.put(player, playersBrokenBlocks);
		}

		// 遠かったり、別ワールドなら履歴を削除する。
		if (playersBrokenBlocks.size() > 0) {
			Location prevLocation = playersBrokenBlocks.get(0).getLocation();
			Location blockLocation = event.getBlock().getLocation();
			if (!prevLocation.getWorld().equals(blockLocation.getWorld())
					|| prevLocation.distance(blockLocation) > 10) {
				clearHistory(player);
			}
		}

		// ブロックのほうがプレイヤーより高い場合は穴埋めしない。
		if (event.getBlock().getY() > player.getLocation().getY()) {
			notPass(player);
			return;
		}

		// 下を向いていなかったら穴埋めしない。
		double pitchCondition = 90 - (180 / Math.PI) * Math.atan(0.7 * Math.sqrt(2) / player.getEyeHeight());
		if (player.getLocation().getPitch() < pitchCondition) {
			notPass(player);
			return;
		}

		playersBrokenBlocks.add(mappedState);

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

	/**
	 * 受け取った{@code state}を適切なタイプに変換する。
	 * 
	 * @param state 変換するBlockState
	 * @return 掘ったブロックが地面の構成ブロックだったらそのままの{@code state}を返す。<br>砂だったら砂岩にタイプを置換した{@code state}を返す。<br>それら以外の場合はnullを返す。
	 */
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
			return null;
		}

		return state;
	}

    private final Map<Player, Integer> timesNotPass = new HashMap<>();

	/**
	 * 何回も直下掘り以外の掘り方をしたら履歴を削除する。
	 * 
	 * @param player
	 */
	private void notPass(Player player) {
		timesNotPass.put(player, timesNotPass.getOrDefault(player, 0) + 1);
		if (timesNotPass.get(player) >= 2) {
			timesNotPass.remove(player);
			clearHistory(player);
		}
	}

	/**
	 * 履歴を削除する。
	 * 
	 * @param player
	 */
	private void clearHistory(Player player) {
		List<BlockState> playersBrokenBlocks = brokenBlocks.get(player);
		if (playersBrokenBlocks != null) {
			playersBrokenBlocks.clear();
		}
	}

	/**
	 * ワールド名が設定にあるかどうか調べる。設定のワールド名リストが正規表現なのでこの実装に。
	 * 
	 * @param world
	 * @return
	 */
	private boolean isEnabledWorld(World world) {
		for (String pattern : getConfig().getStringList("enabled-worlds")) {
			if (world.getName().matches(pattern)) {
				return true;
			}
		}

		return false;
	}
}
