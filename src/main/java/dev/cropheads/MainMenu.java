package dev.cropheads;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class MainMenu extends Menu {
    private final int page;
    private List<CropHead> list = new ArrayList<>();

    public MainMenu(CropHeadsPlugin pl, int page) {
        super(pl, 6, "<dark_green><bold>CropHeads <gray>- Regiones");
        this.page = page;
    }

    @Override
    public void render() {
        list = new ArrayList<>(plugin.manager.all());
        fill();
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= list.size()) { inv.setItem(i, null); continue; }
            CropHead h = list.get(idx);
            inv.setItem(i, of(Material.PLAYER_HEAD, "<green>Cabeza #" + h.id,
                    "Mundo: <white>" + h.world,
                    "Pos: <white>" + h.x + ", " + h.y + ", " + h.z,
                    "Radio: <white>" + h.radius + " <gray>| Regen: <white>" + h.regenSeconds + "s",
                    "",
                    "<yellow>Click izq: <white>editar",
                    "<yellow>Click der: <white>teletransportarte"));
        }
        inv.setItem(48, of(Material.ARROW, "<yellow>Pagina anterior"));
        inv.setItem(49, of(Material.EMERALD, "<green>Obtener cabeza", "Recibes una cabeza para colocar."));
        inv.setItem(50, of(Material.ARROW, "<yellow>Pagina siguiente"));
    }

    @Override
    public void onClick(Player p, InventoryClickEvent e) {
        int s = e.getRawSlot();
        if (s == 48 && page > 0) new MainMenu(plugin, page - 1).open(p);
        else if (s == 50 && (page + 1) * 45 < list.size()) new MainMenu(plugin, page + 1).open(p);
        else if (s == 49) HeadUtil.give(plugin, p, plugin.getConfig().getString("default-head-texture", "MHF_Melon"));
        else if (s < 45) {
            int idx = page * 45 + s;
            if (idx >= list.size()) return;
            CropHead h = list.get(idx);
            if (e.getClick().isRightClick()) {
                if (h.location() != null) p.teleport(h.location().add(0.5, 1, 0.5));
            } else new EditMenu(plugin, h).open(p);
        }
    }
}
