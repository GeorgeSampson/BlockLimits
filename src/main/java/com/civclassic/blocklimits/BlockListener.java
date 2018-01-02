package com.civclassic.blocklimits;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class BlockListener implements Listener {

	private BlockManager bm;
	
	public BlockListener(BlockManager bm) {
		this.bm = bm;
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		boolean cancel = !bm.handleBlockAdded(event.getBlock().getChunk(), event.getBlock().getType());
		event.setCancelled(cancel);
		if(cancel) {
			event.getPlayer().sendMessage(ChatColor.RED + "Chunk limit for " + event.getBlock().getType() + " has been reached.");
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		List<BlockMoveStore> todo = new LinkedList<BlockMoveStore>();
		for(Block block : event.getBlocks()) {
			Chunk from = block.getChunk();
			Chunk to = block.getRelative(event.getDirection()).getChunk();
			if(to.getX() != from.getX() || to.getZ() != from.getZ()) {
				if(bm.canHaveMore(to, block.getType())) {
					todo.add(new BlockMoveStore(block.getType(), from, to));
				} else {
					event.setCancelled(true);
					return;
				}
			}
		}
		for(BlockMoveStore store : todo) {
			store.move();
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		List<BlockMoveStore> todo = new LinkedList<BlockMoveStore>();
		for(Block block : event.getBlocks()) {
			Chunk from = block.getChunk();
			Chunk to = block.getRelative(event.getDirection()).getChunk();
			if(to.getX() != from.getX() || to.getZ() != from.getZ()) {
				if(bm.canHaveMore(to, block.getType())) {
					todo.add(new BlockMoveStore(block.getType(), from, to));
				} else {
					event.setCancelled(true);
					return;
				}
			}
		}
		for(BlockMoveStore store : todo) {
			store.move();
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent event) {
		bm.handleBlockRemoved(event.getBlock().getChunk(), event.getBlock().getType());
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockExplode(BlockExplodeEvent event) {
		for(Block block : event.blockList()) {
			bm.handleBlockRemoved(block.getChunk(), block.getType());
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onEntityExplode(EntityExplodeEvent event) {
		for(Block block : event.blockList()) {
			bm.handleBlockRemoved(block.getChunk(), block.getType());
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBurn(BlockBurnEvent event) {
		bm.handleBlockRemoved(event.getBlock().getChunk(), event.getBlock().getType());
	}
	
	private class BlockMoveStore {
		private Material type;
		private Chunk from, to;
		
		public BlockMoveStore(Material type, Chunk from, Chunk to) {
			this.type = type;
			this.from = from;
			this.to = to;
		}
		
		public void move() {
			bm.handleBlockAdded(to, type);
			bm.handleBlockRemoved(from, type);
		}
	}
}
