package dev.cropheads;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class CropListener implements Listener {
    private final CropHeadsPlugin plugin;
    private final HeadManager mgr;

    public CropListener(CropHeadsPlugin plugin) { this.plugin = plugin; this.mgr = plugin.manager; }

    // ------------------------------------------------ colocar cabeza
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack it = e.getItemInHand();
        if (!it.hasItemMeta() || !it.getItemMeta().getPersistentDataContainer()
                .has(plugin.keyHead, PersistentDataType.BYTE)) return;
        Player p = e.getPlayer();
        if (!p.hasPermission("cropheads.admin")) { e.setCancelled(true); return; }
        e.setCancelled(false);
        CropHead h = mgr.create(e.getBlockPlaced().getLocation());
        plugin.msg(p, "head-created", "id", String.valueOf(h.id));
    }

    // ------------------------------------------------ romper (cultivo / cabeza / proteccion)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        CropHead own = mgr.headAt(b);
        if (own != null) {
            e.setCancelled(true);
            if (!p.hasPermission("cropheads.admin")) { plugin.msg(p, "no-permission"); return; }
            ItemStack drop = HeadUtil.itemFromBlock(plugin, b);
            mgr.removeHead(own, false);
            b.setType(Material.AIR);
            if (p.getGameMode() != GameMode.CREATIVE) b.getWorld().dropItemNaturally(b.getLocation().add(.5, .5, .5), drop);
            plugin.msg(p, "head-removed", "id", String.valueOf(own.id));
            return;
        }

        CropHead h = mgr.findRegion(b);
        if (h == null || p.hasPermission("cropheads.bypass")) return;

        if (plugin.isCrop(b.getType())) {
            e.setCancelled(true);
            if (!h.permission.isEmpty() && !p.hasPermission(h.permission)) { plugin.msg(p, "no-permission"); return; }
            mgr.harvest(p, h, b);
        } else if (h.protectOthers) {
            e.setCancelled(true);
        }
    }

    // ------------------------------------------------ menu al hacer click derecho en la cabeza
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (b == null) return;
        // evitar pisoteo de tierra de cultivo
        if (e.getAction() == org.bukkit.event.block.Action.PHYSICAL && b.getType() == Material.FARMLAND && mgr.noWater(b)) {
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        CropHead h = mgr.headAt(b);
        if (h == null || e.getPlayer().isSneaking() || !e.getPlayer().hasPermission("cropheads.admin")) return;
        e.setCancelled(true);
        new EditMenu(plugin, h).open(e.getPlayer());
    }

    // ------------------------------------------------ sin agua
    @EventHandler(ignoreCancelled = true)
    public void onMoisture(MoistureChangeEvent e) {
        Block b = e.getBlock();
        if (!mgr.noWater(b)) return;
        e.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> mgr.hydrate(b));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) {
        if (e.getBlock().getType() == Material.FARMLAND && mgr.noWater(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrample(EntityChangeBlockEvent e) {
        if (e.getBlock().getType() == Material.FARMLAND && mgr.noWater(e.getBlock())) e.setCancelled(true);
    }

    // ------------------------------------------------ explosiones
    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) { e.blockList().removeIf(mgr::isProtected); }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) { e.blockList().removeIf(mgr::isProtected); }
}
