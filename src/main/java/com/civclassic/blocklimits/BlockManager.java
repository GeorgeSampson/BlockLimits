package com.civclassic.blocklimits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Chunk;
import org.bukkit.Material;

import vg.civcraft.mc.civmodcore.dao.ManagedDatasource;

public class BlockManager {
	
	private static final String GET_COUNT = "select count from blocklimits where world=? and x=? and z=? and mat=?;";
	private static final String INCREMENT = "insert into blocklimits (world, x, z, mat, count) values (?, ?, ?, ?, 1) on duplicate key update count = count + 1;";
	private static final String DECREMENT = "update blocklimits set count = count - 1 where world=? and x=? and z=? and mat=?;";

	private ManagedDatasource db;
	private Logger log;
	private Map<Material, Integer> limits;
	
	public BlockManager(ManagedDatasource db, Logger log, Map<Material, Integer> limits) {
		this.db = db;
		this.log = log;
		this.limits = limits;
	}
	
	public void registerMigrations() {
		db.registerMigration(1, true, 
				"create table if not exists blocklimits("
				+ "world int not null,"
				+ "x int not null,"
				+ "z int not null,"
				+ "mat varchar(40) not null,"
				+ "count int not null default 1,"
				+ "constraint chunkKey primary key (world, x, z, mat));");
	}
	
	/**
	 * Handles logic for adding a block to a chunk
	 * @param chunk
	 * @param type
	 * @return if the block should be kept in the chunk
	 */
	public boolean handleBlockAdded(Chunk chunk, Material type) {
		if(!limits.containsKey(type) || limits.get(type) == null) return true;
		if(canHaveMore(chunk, type)) {
			incrementChunkCount(chunk, type);
			return true;
		}
		return false;
	}
	
	public boolean canHaveMore(Chunk chunk, Material type) {
		if(!limits.containsKey(type) || limits.get(type) == null) return true;
		return getChunkCount(chunk, type) < limits.get(type);
	}
	
	public void handleBlockRemoved(Chunk chunk, Material type) {
		if(!limits.containsKey(type) || limits.get(type) == null) return;
		decrementChunkCount(chunk, type);
	}
	
	private void incrementChunkCount(Chunk chunk, Material mat) {
		try(Connection conn = db.getConnection();
				PreparedStatement ps = conn.prepareStatement(INCREMENT)) {
			ps.setInt(1, chunk.getWorld().getName().hashCode());
			ps.setInt(2, chunk.getX());
			ps.setInt(3, chunk.getZ());
			ps.setString(4, mat.name());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.log(Level.WARNING, "Failed to increment count for " + mat + " at " + chunk.getWorld() + " [" + chunk.getX() + ", " + chunk.getZ() + "]", e);
		}
	}
	
	private void decrementChunkCount(Chunk chunk, Material mat) {
		try(Connection conn = db.getConnection();
				PreparedStatement ps = conn.prepareStatement(DECREMENT)) {
			ps.setInt(1, chunk.getWorld().getName().hashCode());
			ps.setInt(2, chunk.getX());
			ps.setInt(3, chunk.getZ());
			ps.setString(4, mat.name());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.log(Level.WARNING, "Failed to decrement count for " + mat + " at " + chunk.getWorld() + " [" + chunk.getX() + ", " + chunk.getZ() + "]", e);
		}
	}
	
	private int getChunkCount(Chunk chunk, Material mat) {
		try(Connection conn = db.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_COUNT)) {
			ps.setInt(1, chunk.getWorld().getName().hashCode());
			ps.setInt(2, chunk.getX());
			ps.setInt(3, chunk.getZ());
			ps.setString(4, mat.name());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getInt("count");
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Failed to get count for " + mat + " at " + chunk.getWorld() + " [" + chunk.getX() + ", " + chunk.getZ() + "]", e);
		}
		return 0;
	}

	public void cull() {
		try (Connection conn = db.getConnection()) {
			conn.prepareStatement("delete from blocklimits where count=0;").executeUpdate();
		} catch (SQLException e) {
			log.log(Level.WARNING, "Failed to cull empty rows", e);
		}
	}
}
