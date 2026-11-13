package dev.cropheads;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class ParticleMenu extends Menu {
    private static final List<String> TYPES = List.of("HAPPY_VILLAGER", "END_ROD", "FLAME", "HEART", "ENCHANT",
            "CLOUD", "PORTAL", "SOUL_FIRE_FLAME", "CRIT", "NOTE", "SNOWFLAKE", "GLOW", "WAX_ON",
            "TOTEM_OF_UNDYING", "FIREWORK", "COMPOSTER", "ELECTRIC_SPARK");
    private final CropHead h;

    public ParticleMenu(CropHeadsPlugin pl, CropHead h) {
        super(pl, 4, "<dark_green>Particulas #" + h.id);
        this.h = h;
    }

    @Override
    public void render() {
        fill();
        inv.setItem(10, of(Material.LEVER, "<green>Particulas: " + onOff(h.partEnabled), "<yellow>Click: <white>alternar"));
        inv.setItem(12, of(Material.BLAZE_POWDER, "<green>Tipo: <white>" + h.partType,
                "<yellow>Izq: <white>siguiente", "<yellow>Der: <white>anterior", "<yellow>Q: <white>escribir nombre"));
        inv.setItem(14, of(Material.GLOWSTONE_DUST, "<green>Cantidad: <white>" + h.partCount,
                "<yellow>Izq/Der: <white>+1 / -1", "<yellow>Shift: <white>+5 / -5"));
        inv.setItem(16, of(Material.FEATHER, "<green>Altura: <white>" + h.partYOffset, "<yellow>Izq/Der: <white>+0.1 / -0.1"));
        inv.setItem(31, of(Material.ARROW, "<yellow>Volver"));
    }

    private void changed() { plugin.manager.save(); render(); }

    @Override
    public void onClick(Player p, InventoryClickEvent e) {
        ClickType c = e.getClick();
        switch (e.getRawSlot()) {
            case 10 -> { h.partEnabled = !h.partEnabled; changed(); }
            case 12 -> {
                if (c == ClickType.DROP) {
                    plugin.prompt(p, "Escribe el nombre de la particula (ej: FLAME)", () -> new ParticleMenu(plugin, h).open(p), in -> {
                        try { Particle.valueOf(in.trim().toUpperCase()); h.partType = in.trim().toUpperCase(); plugin.manager.save(); }
                        catch (Exception ex) { p.sendMessage(CropHeadsPlugin.MM.deserialize("<red>Particula no valida.")); }
                    });
                } else {
                    int i = TYPES.indexOf(h.partType);
                    int n = c.isRightClick() ? i - 1 : i + 1;
                    h.partType = TYPES.get(((n % TYPES.size()) + TYPES.size()) % TYPES.size());
                    changed();
                }
            }
            case 14 -> { h.partCount = Math.max(1, Math.min(100, h.partCount + delta(c, 1, 5))); changed(); }
            case 16 -> { h.partYOffset = Math.round((h.partYOffset + delta(c, 1, 1) * 0.1) * 10) / 10.0; changed(); }
            case 31 -> new EditMenu(plugin, h).open(p);
            default -> {}
        }
    }
}
