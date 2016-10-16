package com.gmail.snowmanam2.factionsflight;

import org.bukkit.plugin.java.JavaPlugin;

public class FactionsFlight extends JavaPlugin {
	@Override
	public void onEnable() {
		Messages.loadMessages(this);
		EngineFlight.init(this);
	}
}
