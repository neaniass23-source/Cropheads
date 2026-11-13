package dev.cropheads;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

/** Inventario libre: lo que dejes dentro al cerrar se guarda como drops custom. */
public class DropsMenu extends Menu {
    private final CropHead h;

    public DropsMenu(CropHeadsPlugin pl, CropHead h) {
        super(pl, 6, "<dark_green>Drops custom <gray>(cierra para guardar)");
        this.h = h;
    }

    @Override public boolean locked() { return false; }

    @Override
    public void render() {
        inv.clear();
        int i = 0;
        for (ItemStack it : h.customDrops) if (i < inv.getSize()) inv.setItem(i++, it.clone());
    }

    @Override public void onClick(Player p, InventoryClickEvent e) {}

    @Override
    public void onClose(Player p) {
        h.customDrops = new ArrayList<>();
        for (ItemStack it : inv.getContents()) if (it != null && !it.getType().isAir()) h.customDrops.add(it.clone());
        plugin.manager.save();
        if (plugin.isEnabled())
            Bukkit.getScheduler().runTask(plugin, () -> new EditMenu(plugin, h).open(p));
    }
}
