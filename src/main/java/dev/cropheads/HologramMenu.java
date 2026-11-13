package dev.cropheads;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class HologramMenu extends Menu {
    private static final List<String> BACKGROUNDS =
            List.of("transparent", "default", "#80000000", "#C0000000", "#8000AA00", "#80AA0000", "#800055FF");
    private final CropHead h;

    public HologramMenu(CropHeadsPlugin pl, CropHead h) {
        super(pl, 4, "<dark_green>Holograma #" + h.id);
        this.h = h;
    }

    @Override
    public void render() {
        fill();
        inv.setItem(10, of(Material.LEVER, "<green>Holograma: " + onOff(h.holoEnabled), "<yellow>Click: <white>alternar"));
        inv.setItem(11, of(Material.WRITABLE_BOOK, "<green>Editar lineas",
                "Separa lineas con <white>|", "Admite MiniMessage (<gradient>, <bold>...)",
                "Variables: %radius% %regen% %id% %permission%", "", "<yellow>Click: <white>escribir"));
        inv.setItem(12, of(Material.BLACK_DYE, "<green>Sombra del texto: " + onOff(h.holoShadow), "<yellow>Click: <white>alternar"));
        inv.setItem(13, of(Material.GRAY_STAINED_GLASS, "<green>Fondo: <white>" + h.holoBackground,
                "transparent | default | #AARRGGBB", "", "<yellow>Izq: <white>siguiente preset", "<yellow>Der: <white>escribir color"));
        inv.setItem(14, of(Material.SPYGLASS, "<green>Escala: <white>" + h.holoScale, "<yellow>Izq/Der: <white>+0.25 / -0.25"));
        inv.setItem(15, of(Material.FEATHER, "<green>Altura: <white>" + h.holoYOffset, "<yellow>Izq/Der: <white>+0.1 / -0.1"));
        inv.setItem(16, of(Material.GLASS, "<green>Ver a traves de bloques: " + onOff(h.holoSeeThrough), "<yellow>Click: <white>alternar"));
        inv.setItem(31, of(Material.ARROW, "<yellow>Volver"));
    }

    private void changed() { plugin.manager.save(); plugin.manager.refreshVisuals(h); render(); }

    @Override
    public void onClick(Player p, InventoryClickEvent e) {
        Runnable back = () -> new HologramMenu(plugin, h).open(p);
        switch (e.getRawSlot()) {
            case 10 -> { h.holoEnabled = !h.holoEnabled; changed(); }
            case 11 -> plugin.prompt(p, "Escribe las lineas separadas por |", back, in -> {
                List<String> l = new ArrayList<>();
                for (String s : in.split("\\|")) l.add(s.trim());
                h.holoLines = l; plugin.manager.save(); plugin.manager.refreshVisuals(h);
            });
            case 12 -> { h.holoShadow = !h.holoShadow; changed(); }
            case 13 -> {
                if (e.getClick().isRightClick())
                    plugin.prompt(p, "Escribe el color: transparent, default, #AARRGGBB o #RRGGBB", back, in -> {
                        h.holoBackground = in.trim(); plugin.manager.save(); plugin.manager.refreshVisuals(h); });
                else {
                    int i = BACKGROUNDS.indexOf(h.holoBackground);
                    h.holoBackground = BACKGROUNDS.get((i + 1) % BACKGROUNDS.size());
                    changed();
                }
            }
            case 14 -> {
                float d = delta(e.getClick(), 1, 1) * 0.25f;
                h.holoScale = Math.max(0.25f, Math.min(5f, h.holoScale + d)); changed();
            }
            case 15 -> {
                h.holoYOffset = Math.round((h.holoYOffset + delta(e.getClick(), 1, 1) * 0.1) * 10) / 10.0; changed();
            }
            case 16 -> { h.holoSeeThrough = !h.holoSeeThrough; changed(); }
            case 31 -> new EditMenu(plugin, h).open(p);
            default -> {}
        }
    }
}
