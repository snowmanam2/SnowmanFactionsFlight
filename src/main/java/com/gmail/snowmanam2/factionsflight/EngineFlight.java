package com.gmail.snowmanam2.factionsflight;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import com.massivecraft.factions.Rel;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsChunksChange;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.util.MUtil;

public class EngineFlight implements Listener {
	public static EngineFlight i = new EngineFlight();
	public static Set<Player> fallingPlayers = new HashSet<Player>();
	public static EngineFlight get() {
		return i;
	}
	
	public static void registerPermissions() {
		MPerm.getCreative(25000, "fly", "fly", "fly", MUtil.set(Rel.LEADER, Rel.OFFICER, Rel.MEMBER, Rel.RECRUIT, Rel.ALLY), false, true, true);
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void factionsChunksChange(EventFactionsChunksChange e) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			updatePlayerFlightStatus(player);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerMoveEvent(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		Player player = event.getPlayer();
		
		if (((Entity) player).isOnGround() && fallingPlayers.contains(player)) {
			fallingPlayers.remove(player);
		}
		
		boolean moved = false;
		moved = moved || from.getWorld() == to.getWorld();
		moved = moved || Math.abs(from.getBlockX()>>4 - to.getBlockX()>>4) > 0;
		moved = moved || Math.abs(from.getBlockZ()>>4 - to.getBlockZ()>>4) > 0;
		
		if (moved) {
			updatePlayerFlightStatus(player, to);
		}
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		updatePlayerFlightStatus(event.getPlayer(), event.getTo());
	}
	
	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		updatePlayerFlightStatus(event.getPlayer());
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
			}
		}
	}
	
	public void updatePlayerFlightStatus (Player player) {
		updatePlayerFlightStatus(player, player.getLocation());
	}
	
	public void updatePlayerFlightStatus (Player player, Location location) {
		if (player.hasPermission("factionsflight.bypass")) {
			return;
		}
		
		PS psChunk = PS.valueOf(location.getWorld().getName(), location.getBlockX()>>4, location.getBlockZ()>>4);
		Faction faction = BoardColl.get().getFactionAt(psChunk);
		MPlayer mplayer = MPlayer.get(player);
		
		MPerm flyPerm = MPerm.get("fly");
		if (flyPerm.has(mplayer, faction, false)) {
			if (!player.getAllowFlight()) {
				player.sendMessage(Messages.get("flightEnabled"));
			}
			player.setAllowFlight(true);
			
		} else {
			if (player.getAllowFlight()) {
				player.setAllowFlight(false);
				player.sendMessage(flyPerm.createDeniedMessage(mplayer, faction));

				if (!((Entity) player).isOnGround()) {
					fallingPlayers.add(player);
				}
			}
		}
	}
}
