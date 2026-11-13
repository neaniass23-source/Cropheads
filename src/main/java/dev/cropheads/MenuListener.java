package dev.cropheads;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {
    public MenuListener(CropHeadsPlugin plugin) {}

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu m)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (m.locked()) {
            e.setCancelled(true);
            if (e.getClickedInventory() == e.getView().getTopInventory()) m.onClick(p, e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu m && m.locked()) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Menu m && e.getPlayer() instanceof Player p) m.onClose(p);
    }
}
