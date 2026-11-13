package dev.cropheads;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EditMenu extends Menu {
    private final CropHead h;

    public EditMenu(CropHeadsPlugin pl, CropHead h) {
        super(pl, 6, "<dark_green><bold>Cabeza #" + h.id);
        this.h = h;
    }

    @Override
    public void render() {
        fill();
        inv.setItem(10, of(Material.COMPASS, "<green>Radio: <white>" + h.radius,
                "Alcance del cubo alrededor de la cabeza.", "",
                "<yellow>Izq/Der: <white>+1 / -1", "<yellow>Shift: <white>+5 / -5", "<yellow>Q: <white>escribir valor"));
        inv.setItem(11, of(Material.CLOCK, "<green>Regeneracion: <white>" + h.regenSeconds + "s",
                "Tiempo hasta que el cultivo vuelve.", "",
                "<yellow>Izq/Der: <white>+1 / -1", "<yellow>Shift: <white>+10 / -10", "<yellow>Q: <white>escribir valor"));
        inv.setItem(12, of(Material.NAME_TAG, "<green>Permiso de cosecha",
                "Actual: <white>" + (h.permission.isEmpty() ? "ninguno (todos)" : h.permission), "",
                "<yellow>Click: <white>escribir permiso", "<yellow>Click der: <white>quitar permiso"));
        inv.setItem(13, of(Material.HOPPER, "<green>Modo de drops: <white>" + h.dropMode,
                "DEFAULT: drops normales", "CUSTOM: solo los tuyos", "BOTH: ambos", "", "<yellow>Click: <white>cambiar"));
        inv.setItem(14, of(Material.ENDER_CHEST, "<green>Drops personalizados <gray>(" + h.customDrops.size() + ")",
                "Coloca los items que soltaran.", "", "<yellow>Click: <white>editar"));
        inv.setItem(15, of(Material.GOLD_NUGGET, "<green>Prob. drops custom: <white>" + h.dropChance + "%", "",
                "<yellow>Izq/Der: <white>+5 / -5", "<yellow>Shift: <white>+1 / -1"));
        inv.setItem(16, of(Material.SHIELD, "<green>Proteger otros bloques: " + onOff(h.protectOthers),
                "Si esta activo, solo se pueden romper", "cultivos dentro del radio.", "", "<yellow>Click: <white>alternar"));
        inv.setItem(19, of(Material.WATER_BUCKET, "<green>Cultivos sin agua: " + onOff(h.noWater), "", "<yellow>Click: <white>alternar"));
        inv.setItem(20, of(Material.PLAYER_HEAD, "<green>Cambiar cabeza", "Cambia la textura de la cabeza.", "", "<yellow>Click: <white>abrir"));
        inv.setItem(21, of(Material.OAK_SIGN, "<green>Holograma: " + onOff(h.holoEnabled), "", "<yellow>Click: <white>configurar"));
        inv.setItem(22, of(Material.BLAZE_POWDER, "<green>Particulas: " + onOff(h.partEnabled), "", "<yellow>Click: <white>configurar"));
        inv.setItem(23, of(Material.ENDER_PEARL, "<green>Teletransportarme"));
        inv.setItem(24, of(Material.BONE_MEAL, "<green>Regenerar todo ahora", "Cultivos pendientes de esta region."));
        inv.setItem(25, of(Material.TNT, "<red>Eliminar cabeza", "<red>Shift + click para confirmar."));
        inv.setItem(49, of(Material.ARROW, "<yellow>Volver"));
    }

    private void changed() {
        plugin.manager.save();
        plugin.manager.refreshVisuals(h);
        render();
    }

    @Override
    public void onClick(Player p, InventoryClickEvent e) {
        ClickType c = e.getClick();
        Runnable back = () -> new EditMenu(plugin, h).open(p);
        int max = plugin.getConfig().getInt("max-radius", 128);
        switch (e.getRawSlot()) {
            case 10 -> {
                if (c == ClickType.DROP) plugin.prompt(p, "Escribe el radio (1-" + max + ")", back, in -> {
                    h.radius = Math.max(1, Math.min(max, parseInt(plugin, p, in, h.radius))); plugin.manager.save(); plugin.manager.refreshVisuals(h); });
                else { h.radius = Math.max(1, Math.min(max, h.radius + delta(c, 1, 5))); changed(); }
            }
            case 11 -> {
                if (c == ClickType.DROP) plugin.prompt(p, "Escribe los segundos de regeneracion", back, in -> {
                    h.regenSeconds = Math.max(1, parseInt(plugin, p, in, h.regenSeconds)); plugin.manager.save(); plugin.manager.refreshVisuals(h); });
                else { h.regenSeconds = Math.max(1, Math.min(86400, h.regenSeconds + delta(c, 1, 10))); changed(); }
            }
            case 12 -> {
                if (c.isRightClick()) { h.permission = ""; changed(); }
                else plugin.prompt(p, "Escribe el permiso (ej: spawn.cosechar)", back, in -> { h.permission = in.trim(); plugin.manager.save(); });
            }
            case 13 -> {
                CropHead.DropMode[] v = CropHead.DropMode.values();
                h.dropMode = v[(h.dropMode.ordinal() + 1) % v.length]; changed();
            }
            case 14 -> new DropsMenu(plugin, h).open(p);
            case 15 -> { int st = c.isShiftClick() ? 1 : 5; int d = c.isLeftClick() ? st : (c.isRightClick() ? -st : 0); h.dropChance = Math.max(0, Math.min(100, h.dropChance + d)); changed(); }
            case 16 -> { h.protectOthers = !h.protectOthers; changed(); }
            case 19 -> { h.noWater = !h.noWater; changed(); }
            case 20 -> new TextureMenu(plugin, h).open(p);
            case 21 -> new HologramMenu(plugin, h).open(p);
            case 22 -> new ParticleMenu(plugin, h).open(p);
            case 23 -> { if (h.location() != null) p.teleport(h.location().add(0.5, 1, 0.5)); }
            case 24 -> { int n = plugin.manager.regenNow(h); p.sendMessage(CropHeadsPlugin.MM.deserialize("<green>Regenerados: " + n)); }
            case 25 -> {
                if (c.isShiftClick()) {
                    plugin.manager.removeHead(h, true);
                    plugin.msg(p, "head-removed", "id", String.valueOf(h.id));
                    new MainMenu(plugin, 0).open(p);
                }
            }
            case 49 -> new MainMenu(plugin, 0).open(p);
            default -> {}
        }
    }
}
