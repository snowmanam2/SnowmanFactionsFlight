package com.gmail.snowmanam2.factionsflight;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsChunksChange;
import com.massivecraft.massivecore.ps.PS;

public class EngineFlight {
	public static EngineFlight i = new EngineFlight();
	public static Set<Player> fallingPlayers = new HashSet<Player>();
	public static EngineFlight get() {
		return i;
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void factionsChunksChange(EventFactionsChunksChange e) {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			updatePlayerFlightStatus(player);
		}
	}
	
	@EventHandler
	public void onPlayerMoveEvent(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		Player player = event.getPlayer();
		
		if (((Entity) player).isOnGround() && EngineFlight.fallingPlayers.contains(player)) {
			EngineFlight.fallingPlayers.remove(player);
		}
		
		boolean moved = false;
		moved = moved || from.getWorld() == to.getWorld();
		moved = moved || Math.abs(from.getBlockX()>>4 - to.getBlockX()>>4) > 0;
		moved = moved || Math.abs(from.getBlockZ()>>4 - to.getBlockZ()>>4) > 0;
		
		if (moved) {
			updatePlayerFlightStatus(player);
		}
	}
	
	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		updatePlayerFlightStatus(event.getPlayer());
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		Entity entity = event.getEntity();
		if (entity instanceof Player && event.getCause() == DamageCause.FALL) {
			Player player = (Player) entity;
			if (EngineFlight.fallingPlayers.contains(player)) {
				event.setCancelled(true);
			} else if (player.hasPermission("factionsflight.nodamage")) {
				event.setCancelled(true);
			}
		}
	}
	
	public void updatePlayerFlightStatus (Player player) {
		Location location = player.getLocation();
		PS psChunk = PS.valueOf(location.getWorld().getName(), location.getBlockX()>>4, location.getBlockX()>>4);
		Faction faction = BoardColl.get().getFactionAt(psChunk);
		MPlayer mplayer = MPlayer.get(player);
		
		MPerm flyPerm = MPerm.get("fly");
		if (flyPerm.has(mplayer, faction, false)) {
			player.setAllowFlight(true);
		} else {
			if (player.getAllowFlight() && !player.hasPermission("factionsflight.bypass")) {
				player.setAllowFlight(false);
				EngineFlight.fallingPlayers.add(player);
				player.sendMessage(flyPerm.createDeniedMessage(mplayer, faction));
			}
		}
	}
}