package com.civclassic.blocklimits;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import vg.civcraft.mc.civmodcore.ACivMod;
import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

public class BlockLimits extends ACivMod {

	private BlockManager bm;
	private Logger log;
	
	public void onEnable() {
		log = getLogger();
		reloadConfig();
		try {
			ManagedDatasource db = new ManagedDatasource(getConfig().getConfigurationSection("db").getValues(true));
			Map<Material, Integer> limits = new HashMap<Material, Integer>();
			ConfigurationSection limitsConfig = getConfig().getConfigurationSection("limits");
			for(String key : limitsConfig.getKeys(false)) {
				try {
					Material mat = Material.valueOf(key);
					limits.put(mat, limitsConfig.getInt(key));
				} catch (IllegalArgumentException e) {}
			}
			if(db != null) {
				try {
					db.getConnection().close();
				} catch (SQLException e) {
					log.log(Level.WARNING, "Failed to initalize database, shutting down", e);
					getServer().getPluginManager().disablePlugin(this);
					return;
				}
				bm = new BlockManager(db, log, limits);
				bm.registerMigrations();
				if(!db.updateDatabase()) {
					log.log(Level.WARNING, "Failed to update database, shutting down");
					getServer().getPluginManager().disablePlugin(this);
					return;
				}
				getServer().getPluginManager().registerEvents(new BlockListener(bm), this);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Something went wrong, probs with deserializing the db config, you should check that", e);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
	}
	
	public void onDisable() {
		bm.cull();
	}
	
	public String getPluginName() {
		return "BlockLimits";
	}
	
}