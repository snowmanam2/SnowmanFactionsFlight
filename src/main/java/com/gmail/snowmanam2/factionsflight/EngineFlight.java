package com.gmail.snowmanam2.factionsflight;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;

public class EngineFlight implements Listener {
	public static EngineFlight i = new EngineFlight();
	public static Set<OfflinePlayer> fallingPlayers = new HashSet<OfflinePlayer>();
	private static int radius;
	public static EngineFlight get() {
		return i;
	}
	
	public static void init (JavaPlugin plugin) {
		Bukkit.getPluginManager().registerEvents(EngineFlight.get(), plugin);
		registerPermissions();
		
		EngineFlight.radius = plugin.getConfig().getInt("disableRadius");
		final long taskTickInterval = plugin.getConfig().getInt("taskTickInterval");
		BukkitScheduler scheduler = plugin.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				updatePlayerFlight();
			}
		}, 0L, taskTickInterval);
	}
	
	public static void registerPermissions() {
		MPerm.getCreative(25000, "fly", "fly", "fly", MUtil.set(Rel.LEADER, Rel.OFFICER, Rel.MEMBER, Rel.RECRUIT, Rel.ALLY), false, true, true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDamageEvent(EntityDamageEvent e) {
		if (!(e.getEntity() instanceof Player)) {
			return;
		}
		Player p = (Player) e.getEntity();
		if (e.getCause() == DamageCause.FALL) {
			if (fallingPlayers.contains(p)) {
				e.setCancelled(true);
			} else if (p.hasPermission("factionsflight.nofalldamage")) {
				e.setCancelled(true);
			}
		}
	}
	
	public static void updatePlayerFlight () {
		Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
		for (Player player : players) {
			
			if (player.hasPermission("factionsflight.bypass") || player.getGameMode().equals(GameMode.CREATIVE)) {
				player.setAllowFlight(true);
				return;
			}

			/* Clean up falling players */
			if (((Entity) player).isOnGround() && fallingPlayers.contains(player)) {
				fallingPlayers.remove(player);
			}
			
			/* Territory handling */
			boolean flightAllowedTerritory = getPlayerFlightTerritory(player);
			
			/* Player proximity handling */
			boolean flightAllowedProximity = getPlayerFlightProximity(player);
			
			/* Only take action when there are changes in flight status */
			if (!player.getAllowFlight() && flightAllowedTerritory && flightAllowedProximity) {

				enableFlight(player);
			}
			
			if (player.getAllowFlight() && !flightAllowedTerritory) {
				Faction faction = BoardColl.get().getFactionAt(PS.valueOf(player.getLocation()));
				MPlayer mplayer = MPlayer.get(player);
				MPerm flyPerm = MPerm.get("fly");
				
				disableFlight(player, flyPerm.createDeniedMessage(mplayer, faction));
				
			} else if (player.getAllowFlight() && !flightAllowedProximity) {
				
				disableFlight(player, Messages.get("flightDisabledProximity"));
			}
		}
	}
	
	public static boolean getPlayerFlightProximity (Player player1) {
		Collection<? extends Player> players = Bukkit.getServer().getOnlinePlayers();
		
		boolean flightAllowed = true;
		
		for (Player player2 : players) {
			if (player1 == player2) continue;
			
			Location loc1 = player1.getLocation();
			Location loc2 = player2.getLocation();
			
			if (loc1.getWorld() != loc2.getWorld()) continue;
			
			double dX = loc1.getX()-loc2.getX();
			double dZ = loc1.getZ()-loc2.getZ();
			
			if (radius*radius > dX*dX + dZ*dZ) {
				MPlayer mp1 = MPlayer.get(player1);
				MPlayer mp2 = MPlayer.get(player2);
				
				Rel relation = mp1.getRelationTo(mp2);
				
				if (!relation.isFriend()) {
					flightAllowed = false;
				}
			}
		}
		
		return flightAllowed;
	}
	
	public static boolean getPlayerFlightTerritory (Player player) {
		Location location = player.getLocation();
		
		PS psChunk = PS.valueOf(location);
		Faction faction = BoardColl.get().getFactionAt(psChunk);
		MPlayer mplayer = MPlayer.get(player);
		
		MPerm flyPerm = MPerm.get("fly");
		if (flyPerm.has(mplayer, faction, false)) {
			
			return true;
			
		} else {
			
			return false;
		}
	}
	
	public static void enableFlight (Player p) {
		p.sendMessage(Messages.get("flightEnabled"));
		p.setAllowFlight(true);
	}
	
	public static void disableFlight (Player p, String message) {
		if (p.isFlying()) {
			p.sendMessage(message);
		}
		
		p.setAllowFlight(false);
		
		if (!((Entity) p).isOnGround()) {
			fallingPlayers.add(p);
		}
	}
}
