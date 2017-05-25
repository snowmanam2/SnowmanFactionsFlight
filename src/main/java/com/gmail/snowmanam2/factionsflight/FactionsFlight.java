package com.gmail.snowmanam2.factionsflight;

import org.bukkit.plugin.java.JavaPlugin;

public class FactionsFlight extends JavaPlugin {
	@Override
	public void onEnable() {
		getConfig().addDefault("taskTickInterval", 10);
		getConfig().addDefault("disableRadius", 32);
		
		Messages.loadMessages(this);
		EngineFlight.init(this);
	}
}
