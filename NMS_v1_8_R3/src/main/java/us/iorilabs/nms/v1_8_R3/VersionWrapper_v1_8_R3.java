package us.iorilabs.nms.v1_8_R3;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Blocks;
import net.minecraft.server.v1_8_R3.ChatMessage;
import net.minecraft.server.v1_8_R3.ContainerAnvil;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import net.minecraft.server.v1_8_R3.PacketPlayOutCloseWindow;
import net.minecraft.server.v1_8_R3.PacketPlayOutOpenWindow;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.Slot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.event.CraftEventFactory;
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

public final class VersionWrapper_v1_8_R3 implements VersionWrapper {

    private final Map<UUID, AnvilSession> sessions = new ConcurrentHashMap<UUID, AnvilSession>();

    @Override
    public String revision() {
        return "v1_8_R3";
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
        AnvilContainer container = new AnvilContainer(handle);
        Inventory inventory = container.getBukkitView().getTopInventory();

        ItemStack left = new ItemStack(Material.PAPER);
        ItemMeta meta = left.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(initial == null ? "" : initial);
            left.setItemMeta(meta);
        }
        inventory.setItem(0, left);
        if (initial != null && !initial.isEmpty()) {
            container.a(initial);
        }

        int containerId = handle.nextContainerCounter();
        CraftEventFactory.handleInventoryCloseEvent(handle);
        handle.p();
        handle.playerConnection.sendPacket(
            new PacketPlayOutOpenWindow(containerId, "minecraft:anvil", new ChatMessage(Blocks.ANVIL.a() + ".name"))
        );
        handle.activeContainer = container;
        container.windowId = containerId;
        container.addSlotListener(handle);

        AnvilSession session = new AnvilSession(playerId, containerId, inventory, onComplete, onCancel);
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
        handle.playerConnection.sendPacket(new PacketPlayOutCloseWindow(session.containerId));
        handle.activeContainer = handle.defaultContainer;
        handle.p();
    }

    @Override
    public void sendLegacyTitle(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
        connection.sendPacket(new PacketPlayOutTitle(fadeIn, stay, fadeOut));
        if (title != null && !title.isEmpty()) {
            connection.sendPacket(
                new PacketPlayOutTitle(
                    PacketPlayOutTitle.EnumTitleAction.TITLE,
                    IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + escape(title) + "\"}")
                )
            );
        }
        if (subtitle != null && !subtitle.isEmpty()) {
            connection.sendPacket(
                new PacketPlayOutTitle(
                    PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + escape(subtitle) + "\"}")
                )
            );
        }
    }

    @Override
    public void sendActionBar(UUID playerId, String message) {
        if (message == null || message.isEmpty()) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
        IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + escape(message) + "\"}");
        connection.sendPacket(new PacketPlayOutChat(component, (byte) 2));
    }

    @Override
    public void sendTabHeaderFooter(UUID playerId, String header, String footer) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) return;

        IChatBaseComponent headerComponent = IChatBaseComponent.ChatSerializer.a(
            "{\"text\":\"" + escape(header == null ? "" : header) + "\"}"
        );
        IChatBaseComponent footerComponent = IChatBaseComponent.ChatSerializer.a(
            "{\"text\":\"" + escape(footer == null ? "" : footer) + "\"}"
        );
        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
        try {
            Field headerField = packet.getClass().getDeclaredField("a");
            Field footerField = packet.getClass().getDeclaredField("b");
            headerField.setAccessible(true);
            footerField.setAccessible(true);
            headerField.set(packet, headerComponent);
            footerField.set(packet, footerComponent);
        } catch (ReflectiveOperationException ignored) {
            return;
        }
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    private static String escape(String raw) {
        if (raw == null) return "";
        return raw
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private final class AnvilSession implements Listener {

        private final UUID playerId;
        private final int containerId;
        private final Inventory inventory;
        private final Consumer<String> onComplete;
        private final Runnable onCancel;
        private boolean completed;
        private boolean closing;

        private AnvilSession(
            UUID playerId,
            int containerId,
            Inventory inventory,
            Consumer<String> onComplete,
            Runnable onCancel
        ) {
            this.playerId = playerId;
            this.containerId = containerId;
            this.inventory = inventory;
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

            EntityPlayer handle = ((CraftPlayer) player).getHandle();
            String text = "";
            if (handle.activeContainer instanceof AnvilContainer) {
                text = ((AnvilContainer) handle.activeContainer).renameText();
            }
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
            handle.activeContainer = handle.defaultContainer;
            if (onCancel != null) onCancel.run();
        }
    }

    private static final class AnvilContainer extends ContainerAnvil {

        private String typed = "";

        private AnvilContainer(EntityPlayer player) {
            super(player.inventory, player.world, new BlockPosition(0, 0, 0), player);
        }

        @Override
        public void e() {
            Slot output = this.getSlot(2);
            if (!output.hasItem()) {
                Slot input = this.getSlot(0);
                if (input.hasItem()) {
                    output.set(input.getItem().cloneItemStack());
                }
            }
            this.a = 0;
            this.b();
        }

        @Override
        public boolean a(EntityHuman human) {
            return true;
        }

        @Override
        public void b(EntityHuman entityhuman) {}

        @Override
        public void a(String value) {
            this.typed = value == null ? "" : value;
            super.a(value);
        }

        private String renameText() {
            return typed;
        }
    }
}
