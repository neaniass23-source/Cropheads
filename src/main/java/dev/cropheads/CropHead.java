package dev.cropheads;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Datos de una cabeza regeneradora (una "region"). */
public class CropHead {
    public enum DropMode { DEFAULT, CUSTOM, BOTH }

    public final int id;
    public String world;
    public int x, y, z;

    public int radius = 30;
    public int regenSeconds = 10;
    public String permission = "";
    public boolean protectOthers = true;
    public boolean noWater = true;
    public DropMode dropMode = DropMode.DEFAULT;
    public double dropChance = 100;
    public List<ItemStack> customDrops = new ArrayList<>();

    public boolean holoEnabled = true, holoShadow = true, holoSeeThrough = false;
    public List<String> holoLines = new ArrayList<>();
    public String holoBackground = "#80000000";
    public float holoScale = 1f;
    public double holoYOffset = 1.6;

    public boolean partEnabled = true;
    public String partType = "HAPPY_VILLAGER";
    public int partCount = 4;
    public double partYOffset = 1.2;

    public UUID holoUuid; // runtime

    public CropHead(int id) { this.id = id; }

    public static CropHead create(int id, Location l, FileConfiguration c) {
        CropHead h = new CropHead(id);
        h.world = l.getWorld().getName();
        h.x = l.getBlockX(); h.y = l.getBlockY(); h.z = l.getBlockZ();
        String d = "defaults.";
        h.radius = c.getInt(d + "radius", 30);
        h.regenSeconds = c.getInt(d + "regen-seconds", 10);
        h.permission = c.getString(d + "permission", "");
        h.protectOthers = c.getBoolean(d + "protect-other-blocks", true);
        h.noWater = c.getBoolean(d + "no-water", true);
        try { h.dropMode = DropMode.valueOf(c.getString(d + "drop-mode", "DEFAULT").toUpperCase()); } catch (Exception ignored) {}
        h.dropChance = c.getDouble(d + "drop-chance", 100);
        h.holoEnabled = c.getBoolean(d + "hologram.enabled", true);
        h.holoLines = new ArrayList<>(c.getStringList(d + "hologram.lines"));
        h.holoShadow = c.getBoolean(d + "hologram.shadow", true);
        h.holoSeeThrough = c.getBoolean(d + "hologram.see-through", false);
        h.holoBackground = c.getString(d + "hologram.background", "#80000000");
        h.holoScale = (float) c.getDouble(d + "hologram.scale", 1.0);
        h.holoYOffset = c.getDouble(d + "hologram.y-offset", 1.6);
        h.partEnabled = c.getBoolean(d + "particles.enabled", true);
        h.partType = c.getString(d + "particles.type", "HAPPY_VILLAGER");
        h.partCount = c.getInt(d + "particles.count", 4);
        h.partYOffset = c.getDouble(d + "particles.y-offset", 1.2);
        return h;
    }

    public void save(ConfigurationSection s) {
        s.set("world", world); s.set("x", x); s.set("y", y); s.set("z", z);
        s.set("radius", radius); s.set("regen-seconds", regenSeconds);
        s.set("permission", permission);
        s.set("protect-others", protectOthers); s.set("no-water", noWater);
        s.set("drop-mode", dropMode.name()); s.set("drop-chance", dropChance);
        s.set("custom-drops", customDrops);
        s.set("hologram.enabled", holoEnabled); s.set("hologram.lines", holoLines);
        s.set("hologram.shadow", holoShadow); s.set("hologram.see-through", holoSeeThrough);
        s.set("hologram.background", holoBackground); s.set("hologram.scale", (double) holoScale);
        s.set("hologram.y-offset", holoYOffset);
        s.set("particles.enabled", partEnabled); s.set("particles.type", partType);
        s.set("particles.count", partCount); s.set("particles.y-offset", partYOffset);
    }

    public static CropHead load(int id, ConfigurationSection s) {
        CropHead h = new CropHead(id);
        h.world = s.getString("world"); h.x = s.getInt("x"); h.y = s.getInt("y"); h.z = s.getInt("z");
        h.radius = s.getInt("radius", 30); h.regenSeconds = s.getInt("regen-seconds", 10);
        h.permission = s.getString("permission", "");
        h.protectOthers = s.getBoolean("protect-others", true); h.noWater = s.getBoolean("no-water", true);
        try { h.dropMode = DropMode.valueOf(s.getString("drop-mode", "DEFAULT")); } catch (Exception ignored) {}
        h.dropChance = s.getDouble("drop-chance", 100);
        for (Object o : s.getList("custom-drops", List.of())) if (o instanceof ItemStack i) h.customDrops.add(i);
        h.holoEnabled = s.getBoolean("hologram.enabled", true);
        h.holoLines = new ArrayList<>(s.getStringList("hologram.lines"));
        h.holoShadow = s.getBoolean("hologram.shadow", true);
        h.holoSeeThrough = s.getBoolean("hologram.see-through", false);
        h.holoBackground = s.getString("hologram.background", "#80000000");
        h.holoScale = (float) s.getDouble("hologram.scale", 1.0);
        h.holoYOffset = s.getDouble("hologram.y-offset", 1.6);
        h.partEnabled = s.getBoolean("particles.enabled", true);
        h.partType = s.getString("particles.type", "HAPPY_VILLAGER");
        h.partCount = s.getInt("particles.count", 4);
        h.partYOffset = s.getDouble("particles.y-offset", 1.2);
        return h;
    }

    public Location location() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z);
    }

    public Block block() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : w.getBlockAt(x, y, z);
    }

    public boolean contains(Block b) {
        return b.getWorld().getName().equals(world)
                && Math.abs(b.getX() - x) <= radius
                && Math.abs(b.getY() - y) <= radius
                && Math.abs(b.getZ() - z) <= radius;
    }
}
