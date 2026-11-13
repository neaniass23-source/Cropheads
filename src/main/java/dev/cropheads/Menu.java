package dev.cropheads;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public abstract class Menu implements InventoryHolder {
    protected final CropHeadsPlugin plugin;
    protected final Inventory inv;

    protected Menu(CropHeadsPlugin plugin, int rows, String title) {
        this.plugin = plugin;
        this.inv = Bukkit.createInventory(this, rows * 9, CropHeadsPlugin.MM.deserialize(title));
    }

    @Override public Inventory getInventory() { return inv; }
    public abstract void render();
    public abstract void onClick(Player p, InventoryClickEvent e);
    public boolean locked() { return true; }
    public void onClose(Player p) {}

    public void open(Player p) { render(); p.openInventory(inv); }

    protected void fill() {
        ItemStack g = of(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, g);
    }

    public static ItemStack of(Material m, String name, String... lore) {
        ItemStack i = new ItemStack(m);
        ItemMeta meta = i.getItemMeta();
        meta.displayName(CropHeadsPlugin.MM.deserialize("<!italic>" + name));
        if (lore.length > 0)
            meta.lore(Arrays.stream(lore).map(l -> (Component) CropHeadsPlugin.MM.deserialize("<!italic><gray>" + l)).toList());
        i.setItemMeta(meta);
        return i;
    }

    public static String onOff(boolean b) { return b ? "<green>ACTIVADO" : "<red>DESACTIVADO"; }

    /** izq = +small, der = -small, shift = big. */
    public static int delta(ClickType c, int small, int big) {
        int s = c.isShiftClick() ? big : small;
        if (c.isLeftClick()) return s;
        if (c.isRightClick()) return -s;
        return 0;
    }

    public static int parseInt(CropHeadsPlugin pl, Player p, String s, int def) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception e) { pl.msg(p, "invalid-number"); return def; }
    }
}
