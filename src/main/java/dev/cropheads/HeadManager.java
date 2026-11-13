package dev.cropheads;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class HeadManager {
    record Pending(String world, int x, int y, int z, String data, long at) {
        String key() { return world + ";" + x + ";" + y + ";" + z; }
    }

    private final CropHeadsPlugin plugin;
    private final Map<Integer, CropHead> heads = new LinkedHashMap<>();
    private final Map<String, Pending> pending = new HashMap<>();
    private final File file;
    private int nextId = 1;
    private int counter = 0;
    private BukkitTask task, saveTask;

    public HeadManager(CropHeadsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "heads.yml");
    }

    // ---------------------------------------------------------------- persistencia
    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection hs = y.getConfigurationSection("heads");
        if (hs != null) for (String k : hs.getKeys(false)) {
            CropHead h = CropHead.load(Integer.parseInt(k), hs.getConfigurationSection(k));
            heads.put(h.id, h);
            nextId = Math.max(nextId, h.id + 1);
        }
        for (String s : y.getStringList("pending")) {
            try {
                String[] a = s.split(";", 6);
                Pending p = new Pending(a[0], Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                        Integer.parseInt(a[3]), a[5], Long.parseLong(a[4]));
                pending.put(p.key(), p);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (CropHead h : heads.values()) h.save(y.createSection("heads." + h.id));
        List<String> list = new ArrayList<>();
        for (Pending p : pending.values())
            list.add(p.world() + ";" + p.x() + ";" + p.y() + ";" + p.z() + ";" + p.at() + ";" + p.data());
        y.set("pending", list);
        try { y.save(file); } catch (IOException e) { plugin.getLogger().warning("No se pudo guardar heads.yml: " + e.getMessage()); }
    }

    // ---------------------------------------------------------------- ciclo de vida
    public void start() {
        cleanupStrayHolograms();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L);
    }

    public void stop() {
        if (task != null) task.cancel();
        if (saveTask != null) saveTask.cancel();
        for (CropHead h : heads.values()) removeHologram(h);
        save();
    }

    private void cleanupStrayHolograms() {
        for (World w : Bukkit.getWorlds())
            for (TextDisplay d : w.getEntitiesByClass(TextDisplay.class))
                if (d.getPersistentDataContainer().has(plugin.keyHolo, PersistentDataType.BYTE)) d.remove();
    }

    // ---------------------------------------------------------------- consultas
    public Collection<CropHead> all() { return heads.values(); }
    public CropHead get(int id) { return heads.get(id); }

    public CropHead headAt(Block b) {
        for (CropHead h : heads.values())
            if (h.x == b.getX() && h.y == b.getY() && h.z == b.getZ() && h.world.equals(b.getWorld().getName())) return h;
        return null;
    }

    public CropHead findRegion(Block b) {
        for (CropHead h : heads.values()) if (h.contains(b)) return h;
        return null;
    }

    public boolean noWater(Block b) {
        for (CropHead h : heads.values()) if (h.noWater && h.contains(b)) return true;
        return false;
    }

    public boolean isProtected(Block b) {
        if (headAt(b) != null) return true;
        CropHead h = findRegion(b);
        return h != null && h.protectOthers;
    }

    public CropHead create(Location l) {
        CropHead h = CropHead.create(nextId++, l, plugin.getConfig());
        heads.put(h.id, h);
        save();
        spawnHologram(h);
        return h;
    }

    public void removeHead(CropHead h, boolean removeBlock) {
        removeHologram(h);
        heads.remove(h.id);
        if (removeBlock) { Block b = h.block(); if (b != null) b.setType(Material.AIR); }
        save();
    }

    // ---------------------------------------------------------------- cosecha / regeneracion
    public void harvest(Player p, CropHead h, Block b) {
        BlockData data = b.getBlockData();
        BlockData fx = data.clone();
        ItemStack tool = p.getInventory().getItemInMainHand();
        String target = data.getAsString();
        Collection<ItemStack> vanilla;

        if (data instanceof Ageable a) {
            if (a.getAge() < a.getMaximumAge()) return;   // solo se cosecha maduro
            vanilla = b.getDrops(tool, p);
            a.setAge(0);
            b.setBlockData(a, false);
        } else {
            vanilla = b.getDrops(tool, p);
            b.setType(Material.AIR, false);
        }

        List<ItemStack> out = new ArrayList<>();
        if (h.dropMode != CropHead.DropMode.CUSTOM) out.addAll(vanilla);
        if (h.dropMode != CropHead.DropMode.DEFAULT && ThreadLocalRandom.current().nextDouble() * 100 < h.dropChance)
            for (ItemStack s : h.customDrops) out.add(s.clone());

        World w = b.getWorld();
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack it : out) w.dropItemNaturally(c, it);
        w.playSound(c, Sound.BLOCK_CROP_BREAK, 1f, 1f);
        w.spawnParticle(Particle.BLOCK, c, 12, 0.25, 0.25, 0.25, fx);

        Pending pd = new Pending(h.world, b.getX(), b.getY(), b.getZ(), target,
                System.currentTimeMillis() + h.regenSeconds * 1000L);
        pending.put(pd.key(), pd);
    }

    private void restore(World w, Pending p) {
        Block b = w.getBlockAt(p.x(), p.y(), p.z());
        BlockData d;
        try { d = Bukkit.createBlockData(p.data()); } catch (Exception e) { return; }
        if (!b.getType().isAir() && b.getType() != d.getMaterial()) return;   // alguien lo cambio
        b.setBlockData(d, false);
        hydrate(b.getRelative(0, -1, 0));
        Location c = b.getLocation().add(0.5, 0.5, 0.5);
        w.spawnParticle(Particle.HAPPY_VILLAGER, c, 5, 0.3, 0.3, 0.3);
    }

    public void hydrate(Block b) {
        if (b.getBlockData() instanceof Farmland f && f.getMoisture() < f.getMaximumMoisture()) {
            f.setMoisture(f.getMaximumMoisture());
            b.setBlockData(f, false);
        }
    }

    /** Regenera ya todos los cultivos pendientes de una cabeza. */
    public int regenNow(CropHead h) {
        int n = 0;
        Iterator<Pending> it = pending.values().iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            World w = Bukkit.getWorld(p.world());
            if (w == null || !p.world().equals(h.world)) continue;
            if (Math.abs(p.x() - h.x) > h.radius || Math.abs(p.y() - h.y) > h.radius || Math.abs(p.z() - h.z) > h.radius) continue;
            if (!w.isChunkLoaded(p.x() >> 4, p.z() >> 4)) continue;
            it.remove();
            restore(w, p);
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- tick principal (cada 0.5s)
    private void tick() {
        long now = System.currentTimeMillis();
        Iterator<Pending> it = pending.values().iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.at() > now) continue;
            World w = Bukkit.getWorld(p.world());
            if (w == null || !w.isChunkLoaded(p.x() >> 4, p.z() >> 4)) continue;
            it.remove();
            restore(w, p);
        }
        counter++;
        for (CropHead h : heads.values()) {
            Location base = h.location();
            if (base == null || !base.getWorld().isChunkLoaded(h.x >> 4, h.z >> 4)) continue;
            if (h.partEnabled) spawnParticles(h, base);
            if (counter % 10 == 0) ensureHologram(h);
        }
    }

    private void spawnParticles(CropHead h, Location base) {
        Location l = base.clone().add(0.5, h.partYOffset, 0.5);
        if (l.getWorld().getNearbyPlayers(l, 40).isEmpty()) return;
        try {
            l.getWorld().spawnParticle(Particle.valueOf(h.partType), l, h.partCount, 0.3, 0.2, 0.3, 0.01);
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- hologramas
    public void refreshVisuals(CropHead h) { spawnHologram(h); }
    public void refreshAllVisuals() { for (CropHead h : heads.values()) spawnHologram(h); }

    public void ensureHologram(CropHead h) {
        if (!h.holoEnabled) return;
        if (h.holoUuid != null && Bukkit.getEntity(h.holoUuid) != null) return;
        spawnHologram(h);
    }

    public void removeHologram(CropHead h) {
        if (h.holoUuid != null) {
            Entity e = Bukkit.getEntity(h.holoUuid);
            if (e != null) e.remove();
            h.holoUuid = null;
        }
    }

    public void spawnHologram(CropHead h) {
        removeHologram(h);
        if (!h.holoEnabled) return;
        Location base = h.location();
        if (base == null || !base.getWorld().isChunkLoaded(h.x >> 4, h.z >> 4)) return;
        Location l = base.add(0.5, h.holoYOffset, 0.5);
        Component text = buildText(h);
        TextDisplay td = l.getWorld().spawn(l, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.text(text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(h.holoShadow);
            d.setSeeThrough(h.holoSeeThrough);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setLineWidth(400);
            String bg = h.holoBackground == null ? "transparent" : h.holoBackground;
            if (bg.equalsIgnoreCase("default")) {
                d.setDefaultBackground(true);
            } else {
                d.setDefaultBackground(false);
                d.setBackgroundColor(parseColor(bg));
            }
            float s = h.holoScale;
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(s, s, s), new AxisAngle4f()));
            d.getPersistentDataContainer().set(plugin.keyHolo, PersistentDataType.BYTE, (byte) 1);
        });
        h.holoUuid = td.getUniqueId();
    }

    private Component buildText(CropHead h) {
        List<Component> lines = new ArrayList<>();
        for (String s : h.holoLines) {
            lines.add(CropHeadsPlugin.MM.deserialize(s
                    .replace("%id%", String.valueOf(h.id))
                    .replace("%radius%", String.valueOf(h.radius))
                    .replace("%regen%", String.valueOf(h.regenSeconds))
                    .replace("%permission%", h.permission.isEmpty() ? "-" : h.permission)));
        }
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    static Color parseColor(String s) {
        try {
            s = s.replace("#", "");
            if (s.length() == 8) {
                long v = Long.parseLong(s, 16);
                return Color.fromARGB((int) (v >> 24 & 255), (int) (v >> 16 & 255), (int) (v >> 8 & 255), (int) (v & 255));
            }
            if (s.length() == 6) {
                int v = Integer.parseInt(s, 16);
                return Color.fromARGB(180, (v >> 16) & 255, (v >> 8) & 255, v & 255);
            }
        } catch (Exception ignored) {}
        return Color.fromARGB(0, 0, 0, 0); // transparente
    }
}
