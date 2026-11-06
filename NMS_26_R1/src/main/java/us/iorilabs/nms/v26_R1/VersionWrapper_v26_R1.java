package us.iorilabs.nms.v26_R1;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
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

public final class VersionWrapper_v26_R1 implements VersionWrapper {

    private final Map<UUID, AnvilSession> sessions = new ConcurrentHashMap<>();

    @Override
    public String revision() {
        return "v26_R1";
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
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("IoriCore");
        if (plugin == null || !plugin.isEnabled()) return;

        closeAnvilInput(playerId);

        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        Component titleComponent = Component.literal(title == null || title.isEmpty() ? "Repair & Name" : title);
        int containerId = handle.nextContainerCounter();
        AnvilContainer container = new AnvilContainer(player, containerId, titleComponent);
        Inventory inventory = ((AbstractContainerMenu) container).getBukkitView().getTopInventory();

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
        handle.doCloseContainer();
        handle.connection.send(new ClientboundOpenScreenPacket(containerId, MenuType.ANVIL, titleComponent));
        handle.containerMenu = container;
        handle.initMenu(container);

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

        org.bukkit.entity.Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        CraftEventFactory.handleInventoryCloseEvent(handle);
        handle.connection.send(new ClientboundContainerClosePacket(session.containerId));
        handle.containerMenu = handle.inventoryMenu;
        handle.doCloseContainer();
    }

    @Override
    public void sendLegacyTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        org.bukkit.entity.Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;
        player.sendTitle(title == null ? "" : title, subtitle == null ? "" : subtitle, fadeIn, stay, fadeOut);
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
            if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
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
            if (!(event.getPlayer() instanceof org.bukkit.entity.Player player)) return;
            if (!player.getUniqueId().equals(playerId)) return;
            if (closing || completed) return;

            sessions.remove(playerId);
            HandlerList.unregisterAll(this);
            ServerPlayer handle = ((CraftPlayer) player).getHandle();
            handle.containerMenu = handle.inventoryMenu;
            if (onCancel != null) onCancel.run();
        }
    }

    private static final class AnvilContainer extends AnvilMenu {

        private AnvilContainer(org.bukkit.entity.Player player, int containerId, Component title) {
            super(
                containerId,
                ((CraftPlayer) player).getHandle().getInventory(),
                ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(), new BlockPos(0, 0, 0))
            );
            this.checkReachable = false;
            setTitle(title);
        }

        @Override
        public void createResult() {
            Slot output = getSlot(RESULT_SLOT);
            if (!output.hasItem()) {
                Slot input = getSlot(INPUT_SLOT);
                if (input.hasItem()) {
                    output.set(input.getItem().copy());
                }
            }
            this.cost.set(0);
            broadcastChanges();
        }

        @Override
        public void removed(Player player) {}

        @Override
        protected void clearContainer(Player player, Container container) {}

        private String getRenameText() {
            return this.itemName;
        }

        private void setRenameText(String text) {
            Slot inputLeft = getSlot(INPUT_SLOT);
            if (inputLeft.hasItem()) {
                inputLeft.getItem().set(DataComponents.CUSTOM_NAME, Component.literal(text));
            }
            this.itemName = text;
        }
    }
}
