package dev.cropheads;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class TextureMenu extends Menu {
    private final CropHead h;
    private final List<String> presetValues = new ArrayList<>();

    public TextureMenu(CropHeadsPlugin pl, CropHead h) {
        super(pl, 5, "<dark_green>Cambiar cabeza #" + h.id);
        this.h = h;
    }

    @Override
    public void render() {
        fill();
        presetValues.clear();
        inv.setItem(11, of(Material.WRITABLE_BOOK, "<green>Escribir en el chat",
                "Admite: nombre de jugador,", "URL de textures.minecraft.net", "o textura base64.", "", "<yellow>Click: <white>escribir"));
        inv.setItem(15, of(Material.PLAYER_HEAD, "<green>Usar cabeza de mi mano",
                "Copia la textura de la cabeza", "que llevas en la mano.", "", "<yellow>Click: <white>aplicar"));
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("head-presets");
        int slot = 18;
        if (s != null) for (String k : s.getKeys(false)) {
            if (slot > 35) break;
            presetValues.add(s.getString(k));
            inv.setItem(slot++, of(Material.PLAYER_HEAD, "<yellow>Preset: <white>" + k, "<yellow>Click: <white>aplicar"));
        }
        inv.setItem(40, of(Material.ARROW, "<yellow>Volver"));
    }

    @Override
    public void onClick(Player p, InventoryClickEvent e) {
        Runnable back = () -> new EditMenu(plugin, h).open(p);
        int s = e.getRawSlot();
        if (s == 40) back.run();
        else if (s == 11) plugin.prompt(p, "Escribe nombre / URL / base64 de la cabeza", back,
                in -> plugin.changeTexture(p, h, in.trim(), () -> {}));
        else if (s == 15) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand.getItemMeta() instanceof SkullMeta sm && sm.getOwnerProfile() != null && h.block() != null) {
                HeadUtil.applyToBlock(h.block(), sm.getOwnerProfile());
                plugin.msg(p, "texture-changed");
            } else plugin.msg(p, "texture-failed");
        } else if (s >= 18 && s <= 35 && s - 18 < presetValues.size()) {
            plugin.changeTexture(p, h, presetValues.get(s - 18), () -> {});
        }
    }
}
