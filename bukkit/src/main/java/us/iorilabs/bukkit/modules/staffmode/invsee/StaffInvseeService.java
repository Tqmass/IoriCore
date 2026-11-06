package us.iorilabs.bukkit.modules.staffmode.invsee;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.util.LegacyMaterials;

public final class StaffInvseeService implements Listener {

    private final CoreLanguage language;
    private final Set<UUID> viewing = ConcurrentHashMap.newKeySet();

    public StaffInvseeService(IoriCorePlugin plugin, CoreLanguage language) {
        this.language = language;
    }

    public void open(Player staff, Player target) {
        if (staff == null || target == null) return;

        String title = language.rawFor(staff, "STAFF-MODE.INVSEE-TITLE", Map.of("player", target.getName()));
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory view = Bukkit.createInventory(null, 54, title);
        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null) view.setItem(i, contents[i].clone());
        }

        ItemStack[] armor = target.getInventory().getArmorContents();
        Material empty = LegacyMaterials.grayPane();
        for (int i = 0; i < 4; i++) {
            int slot = 45 + i;
            ItemStack piece = i < armor.length ? armor[i] : null;
            view.setItem(slot, piece == null ? spacer(empty) : piece.clone());
        }

        viewing.add(staff.getUniqueId());
        staff.openInventory(view);
        staff.sendMessage(language.rawFor(staff, "STAFF-MODE.INSPECT", Map.of("player", target.getName())));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!viewing.contains(event.getWhoClicked().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!viewing.contains(event.getWhoClicked().getUniqueId())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) viewing.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        viewing.remove(event.getPlayer().getUniqueId());
    }

    private static ItemStack spacer(Material material) {
        ItemStack stack = new ItemStack(material == null ? Material.STONE : material);
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
