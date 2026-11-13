package dev.cropheads;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class CropHeadsPlugin extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {
    public static final MiniMessage MM = MiniMessage.miniMessage();
    public NamespacedKey keyHead, keyHolo;
    public HeadManager manager;
    private final Set<Material> crops = EnumSet.noneOf(Material.class);
    private final Map<UUID, Consumer<String>> prompts = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keyHead = new NamespacedKey(this, "crophead");
        keyHolo = new NamespacedKey(this, "hologram");
        loadCrops();
        manager = new HeadManager(this);
        manager.load();
        Bukkit.getPluginManager().registerEvents(new CropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand cmd = getCommand("cropheads");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        manager.start();
    }

    @Override
    public void onDisable() { if (manager != null) manager.stop(); }

    void loadCrops() {
        crops.clear();
        for (String s : getConfig().getStringList("crops")) {
            Material m = Material.matchMaterial(s);
            if (m != null) crops.add(m); else getLogger().warning("Material desconocido en crops: " + s);
        }
    }

    public boolean isCrop(Material m) { return crops.contains(m); }

    // ------------------------------------------------------------ mensajes
    public void msg(org.bukkit.command.CommandSender s, String key, String... kv) {
        String t = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < kv.length; i += 2) t = t.replace("{" + kv[i] + "}", kv[i + 1]);
        s.sendMessage(MM.deserialize(getConfig().getString("messages.prefix", "") + t));
    }

    // ------------------------------------------------------------ entrada por chat
    public void prompt(Player p, String text, Runnable back, Consumer<String> handler) {
        Bukkit.getScheduler().runTask(this, p::closeInventory);
        prompts.put(p.getUniqueId(), in -> {
            if (!in.equalsIgnoreCase("cancelar")) handler.accept(in);
            back.run();
        });
        msg(p, "prompt", "text", text);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Consumer<String> c = prompts.remove(e.getPlayer().getUniqueId());
        if (c == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        Bukkit.getScheduler().runTask(this, () -> c.accept(text));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { prompts.remove(e.getPlayer().getUniqueId()); }

    /** Cambia la textura de una cabeza ya colocada. */
    public void changeTexture(Player p, CropHead h, String value, Runnable back) {
        HeadUtil.resolve(value).whenComplete((profile, ex) -> Bukkit.getScheduler().runTask(this, () -> {
            if (ex != null || profile == null || h.block() == null || !HeadUtil.isHead(h.block().getType())) {
                msg(p, "texture-failed");
            } else {
                HeadUtil.applyToBlock(h.block(), profile);
                msg(p, "texture-changed");
            }
            back.run();
        }));
    }

    // ------------------------------------------------------------ comandos
    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("cropheads.admin")) { s.sendMessage(MM.deserialize("<red>Sin permiso.")); return true; }
        String sub = a.length == 0 ? "menu" : a[0].toLowerCase();
        switch (sub) {
            case "menu" -> {
                if (s instanceof Player p) new MainMenu(this, 0).open(p);
                else s.sendMessage("Solo jugadores.");
            }
            case "give" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player pl ? pl : null);
                if (t == null) { s.sendMessage(MM.deserialize("<red>/cropheads give <jugador> [textura]")); return true; }
                String tex = a.length > 2 ? a[2] : getConfig().getString("default-head-texture", "MHF_Melon");
                HeadUtil.give(this, t, tex);
                msg(s, "given", "player", t.getName());
            }
            case "reload" -> {
                reloadConfig(); loadCrops(); manager.refreshAllVisuals();
                msg(s, "reloaded");
            }
            case "list" -> {
                for (CropHead h : manager.all())
                    s.sendMessage(MM.deserialize("<gray>#" + h.id + " <white>" + h.world + " " + h.x + " " + h.y + " " + h.z
                            + " <gray>r=" + h.radius + " t=" + h.regenSeconds + "s"));
            }
            case "remove", "regen" -> {
                CropHead h = null;
                try { h = manager.get(Integer.parseInt(a[1])); } catch (Exception ignored) {}
                if (h == null) { s.sendMessage(MM.deserialize("<red>/cropheads " + sub + " <id>")); return true; }
                if (sub.equals("remove")) { manager.removeHead(h, true); msg(s, "head-removed", "id", String.valueOf(h.id)); }
                else s.sendMessage(MM.deserialize("<green>Regenerados: " + manager.regenNow(h)));
            }
            default -> s.sendMessage(MM.deserialize("<yellow>/cropheads [menu|give|list|remove <id>|regen <id>|reload]"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) return List.of("menu", "give", "list", "remove", "regen", "reload");
        if (a.length == 2 && a[0].equalsIgnoreCase("give")) return null; // jugadores
        return List.of();
    }
}
