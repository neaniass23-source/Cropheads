package dev.cropheads;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeadUtil {
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private HeadUtil() {}

    public static boolean isHead(Material m) {
        return m == Material.PLAYER_HEAD || m == Material.PLAYER_WALL_HEAD;
    }

    /** value = nombre de jugador, URL de textures.minecraft.net o base64. */
    public static CompletableFuture<PlayerProfile> resolve(String value) {
        try {
            if (value == null || value.isBlank()) return CompletableFuture.completedFuture(null);
            value = value.trim();
            if (value.startsWith("http")) return CompletableFuture.completedFuture(fromUrl(value));
            if (value.length() > 16) {
                String json = new String(Base64.getDecoder().decode(value));
                Matcher m = URL_PATTERN.matcher(json);
                if (m.find()) return CompletableFuture.completedFuture(fromUrl(m.group(1)));
                return CompletableFuture.failedFuture(new IllegalArgumentException("base64 invalido"));
            }
            return Bukkit.createPlayerProfile(value).update();
        } catch (Exception ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    private static PlayerProfile fromUrl(String url) throws Exception {
        PlayerProfile p = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(url.getBytes()), "CropHead");
        PlayerTextures t = p.getTextures();
        t.setSkin(new URL(url));
        p.setTextures(t);
        return p;
    }

    public static ItemStack item(CropHeadsPlugin pl, PlayerProfile profile) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        if (profile != null) meta.setOwnerProfile(profile);
        meta.displayName(CropHeadsPlugin.MM.deserialize("<!italic><green><bold>Cabeza de Cultivos"));
        meta.lore(java.util.List.of(
                CropHeadsPlugin.MM.deserialize("<!italic><gray>Colocala para crear una region"),
                CropHeadsPlugin.MM.deserialize("<!italic><gray>que regenera cultivos.")));
        meta.getPersistentDataContainer().set(pl.keyHead, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    /** Item a partir del bloque colocado (conserva la textura actual). */
    public static ItemStack itemFromBlock(CropHeadsPlugin pl, Block b) {
        PlayerProfile profile = null;
        if (b.getState() instanceof Skull s) profile = s.getOwnerProfile();
        return item(pl, profile);
    }

    public static void applyToBlock(Block b, PlayerProfile profile) {
        if (b.getState() instanceof Skull s) {
            s.setOwnerProfile(profile);
            s.update(true, false);
        }
    }

    public static void give(CropHeadsPlugin pl, Player target, String texture) {
        resolve(texture).whenComplete((profile, ex) ->
                Bukkit.getScheduler().runTask(pl, () -> {
                    ItemStack it = item(pl, ex == null ? profile : null);
                    target.getInventory().addItem(it).values()
                            .forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
                }));
    }
}
