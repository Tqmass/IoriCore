package us.iorilabs.nms.v1_20_R3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.inventory.Slot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import us.iorilabs.abstraction.VersionWrapper;

public final class VersionWrapper_v1_20_R3 implements VersionWrapper {

    private final Map<UUID, AnvilSession> sessions = new ConcurrentHashMap<>();

    @Override
    public String revision() {
        return "v1_20_R3";
    }

    @Override
    public boolean supportsAnvilInput() {
        return true;
    }

    @Override
    public void openAnvilInput(
        UUID playerId,
        String title,
        String initial,
        Consumer<String> onComplete,
        Runnable onCancel
    ) {
        if (playerId == null || onComplete == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("IoriCore");
        if (plugin == null || !plugin.isEnabled()) return;

        closeAnvilInput(playerId);

        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        IChatBaseComponent titleComponent = IChatBaseComponent.b(
            title == null || title.isEmpty() ? "Repair & Name" : title
        );
        int containerId = handle.nextContainerCounter();
        AnvilContainer container = new AnvilContainer(player, containerId, titleComponent);
        Inventory inventory = ((Container) container).getBukkitView().getTopInventory();

        ItemStack left = new ItemStack(Material.PAPER);
        ItemMeta meta = left.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(initial == null ? "" : initial);
            left.setItemMeta(meta);
        }
        inventory.setItem(0, left);
        if (initial != null && !initial.isEmpty()) {
            container.setRenameText(initial);
        }

        CraftEventFactory.handleInventoryCloseEvent(handle);
        handle.s();
        handle.c.b(new PacketPlayOutOpenWindow(containerId, Containers.i, titleComponent));
        handle.bS = container;
        handle.a(container);

        AnvilSession session = new AnvilSession(playerId, containerId, inventory, container, onComplete, onCancel);
        sessions.put(playerId, session);
        Bukkit.getPluginManager().registerEvents(session, plugin);
    }

    @Override
    public void closeAnvilInput(UUID playerId) {
        if (playerId == null) return;
        AnvilSession session = sessions.remove(playerId);
        if (session == null) return;
        session.closing = true;
        HandlerList.unregisterAll(session);

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        CraftEventFactory.handleInventoryCloseEvent(handle);
        handle.c.b(new PacketPlayOutCloseWindow(session.containerId));
        handle.bS = handle.bR;
        handle.s();
    }

    @Override
    public void sendLegacyTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        player.sendTitle(title == null ? "" : title, subtitle == null ? "" : subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public void sendActionBar(UUID playerId, String message) {
        if (message == null || message.isEmpty()) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        player
            .spigot()
            .sendMessage(
                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
            );
    }

    @Override
    public void sendTabHeaderFooter(UUID playerId, String header, String footer) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        player.setPlayerListHeaderFooter(header == null ? "" : header, footer == null ? "" : footer);
    }

    private final class AnvilSession implements Listener {

        private final UUID playerId;
        private final int containerId;
        private final Inventory inventory;
        private final AnvilContainer container;
        private final Consumer<String> onComplete;
        private final Runnable onCancel;
        private boolean completed;
        private boolean closing;

        private AnvilSession(
            UUID playerId,
            int containerId,
            Inventory inventory,
            AnvilContainer container,
            Consumer<String> onComplete,
            Runnable onCancel
        ) {
            this.playerId = playerId;
            this.containerId = containerId;
            this.inventory = inventory;
            this.container = container;
            this.onComplete = onComplete;
            this.onCancel = onCancel;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!event.getInventory().equals(inventory)) return;
            event.setCancelled(true);
            if (event.getRawSlot() != 2) return;
            if (!(event.getWhoClicked() instanceof Player)) return;

            Player player = (Player) event.getWhoClicked();
            if (!player.getUniqueId().equals(playerId)) return;

            String text = container.getRenameText();
            if (text == null) text = "";

            completed = true;
            closeAnvilInput(playerId);
            onComplete.accept(text);
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().equals(inventory)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (!event.getInventory().equals(inventory)) return;
            if (!(event.getPlayer() instanceof Player)) return;
            if (!event.getPlayer().getUniqueId().equals(playerId)) return;
            if (closing || completed) return;

            sessions.remove(playerId);
            HandlerList.unregisterAll(this);
            Player player = (Player) event.getPlayer();
            EntityPlayer handle = ((CraftPlayer) player).getHandle();
            handle.bS = handle.bR;
            if (onCancel != null) onCancel.run();
        }
    }

    private static final class AnvilContainer extends ContainerAnvil {

        private AnvilContainer(Player player, int containerId, IChatBaseComponent title) {
            super(
                containerId,
                ((CraftPlayer) player).getHandle().fS(),
                ContainerAccess.a(((CraftWorld) player.getWorld()).getHandle(), new BlockPosition(0, 0, 0))
            );
            this.checkReachable = false;
            setTitle(title);
        }

        @Override
        public void m() {
            Slot output = this.b(2);
            if (!output.h()) {
                Slot input = this.b(0);
                if (input.h()) {
                    output.e(input.g().p());
                }
            }
            this.w.a(0);
            this.b();
            this.d();
        }

        @Override
        public void b(EntityHuman player) {}

        @Override
        protected void a(EntityHuman player, IInventory container) {}

        private String getRenameText() {
            return this.v;
        }

        private void setRenameText(String text) {
            Slot inputLeft = b(0);
            if (inputLeft.h()) {
                inputLeft.g().a(IChatBaseComponent.b(text));
            }
            this.v = text;
        }
    }
}
