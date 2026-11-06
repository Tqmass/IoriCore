package us.iorilabs.bukkit.hook;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import us.iorilabs.api.hook.LuckPermsHook;

public final class SoftLuckPermsHook implements LuckPermsHook {

    private final Logger logger;
    private final boolean present;
    private Object luckPermsApi;

    public SoftLuckPermsHook(Logger logger, boolean moduleEnabled) {
        this.logger = logger;
        if (!moduleEnabled) {
            this.present = false;
            return;
        }

        this.present = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        if (present) {
            logger.fine("LuckPerms detected (soft-hook)");
            tryLoadApi();
        }
    }

    private void tryLoadApi() {
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            luckPermsApi = provider.getMethod("get").invoke(null);
        } catch (ReflectiveOperationException ignored) {
            luckPermsApi = null;
        }
    }

    @Override
    public boolean available() {
        return present && luckPermsApi != null;
    }

    @Override
    public void applyPrefix(UUID playerId, String prefix) {
        if (playerId == null || prefix == null || !available()) return;
    }

    @Override
    public String prefix(UUID playerId) {
        return meta(playerId, "prefix");
    }

    @Override
    public String suffix(UUID playerId) {
        return meta(playerId, "suffix");
    }

    @Override
    public void syncGroup(String groupName, Collection<String> permissions) {
        if (!available() || groupName == null || groupName.trim().isEmpty()) return;
        String name = groupName.trim().toLowerCase(Locale.ROOT);
        try {
            Object groupManager = luckPermsApi.getClass().getMethod("getGroupManager").invoke(luckPermsApi);
            Object group = loadOrCreateGroup(groupManager, name);
            if (group == null) return;

            Object data = group.getClass().getMethod("data").invoke(group);
            clearPermissionNodes(data);

            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
            Class<?> permissionNode = Class.forName("net.luckperms.api.node.types.PermissionNode");
            if (permissions != null) {
                for (String raw : permissions) {
                    Object node = buildPermissionNode(permissionNode, raw);
                    if (node == null) continue;
                    data.getClass().getMethod("add", nodeClass).invoke(data, node);
                }
            }

            Class<?> groupType = Class.forName("net.luckperms.api.model.group.Group");
            await(groupManager.getClass().getMethod("saveGroup", groupType).invoke(groupManager, group));
        } catch (ReflectiveOperationException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "LuckPerms syncGroup failed · " + name, ex);
        }
    }

    @Override
    public void setUserInherit(UUID playerId, String groupName, boolean add) {
        if (!available() || playerId == null || groupName == null || groupName.trim().isEmpty()) return;
        String name = groupName.trim().toLowerCase(Locale.ROOT);
        try {
            Object userManager = luckPermsApi.getClass().getMethod("getUserManager").invoke(luckPermsApi);
            Object userFuture = userManager.getClass().getMethod("loadUser", UUID.class).invoke(userManager, playerId);
            Object user = await(userFuture);
            if (user == null) return;

            Object data = user.getClass().getMethod("data").invoke(user);
            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
            Object inheritance = buildInheritanceNode(name);
            if (inheritance == null) return;

            if (add) data.getClass().getMethod("add", nodeClass).invoke(data, inheritance);
            else data.getClass().getMethod("remove", nodeClass).invoke(data, inheritance);

            Class<?> userType = Class.forName("net.luckperms.api.model.user.User");
            await(userManager.getClass().getMethod("saveUser", userType).invoke(userManager, user));
        } catch (ReflectiveOperationException | java.util.concurrent.ExecutionException | InterruptedException ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.log(Level.WARNING, "LuckPerms setUserInherit failed · " + name, ex);
        }
    }

    private Object loadOrCreateGroup(Object groupManager, String name)
        throws ReflectiveOperationException, java.util.concurrent.ExecutionException, InterruptedException {
        Object optional = await(
            groupManager.getClass().getMethod("loadGroup", String.class).invoke(groupManager, name)
        );
        if (optional != null && Boolean.TRUE.equals(optional.getClass().getMethod("isPresent").invoke(optional))) {
            return optional.getClass().getMethod("get").invoke(optional);
        }
        return await(groupManager.getClass().getMethod("createAndLoadGroup", String.class).invoke(groupManager, name));
    }

    private void clearPermissionNodes(Object data) throws ReflectiveOperationException {
        Class<?> nodeType = Class.forName("net.luckperms.api.node.NodeType");
        Object permissionType = nodeType.getField("PERMISSION").get(null);
        Object predicate = permissionType.getClass().getMethod("predicate").invoke(permissionType);
        data.getClass().getMethod("clear", java.util.function.Predicate.class).invoke(data, predicate);
    }

    private Object buildPermissionNode(Class<?> permissionNode, String raw) throws ReflectiveOperationException {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;
        int at = value.indexOf('@');
        if (at >= 0) value = value.substring(0, at).trim();
        if (value.isEmpty()) return null;

        boolean allowed = true;
        if (value.startsWith("-")) {
            allowed = false;
            value = value.substring(1).trim();
        }
        if (value.isEmpty()) return null;

        Object builder = permissionNode.getMethod("builder", String.class).invoke(null, value);
        builder = builder.getClass().getMethod("value", boolean.class).invoke(builder, allowed);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private Object buildInheritanceNode(String groupName) throws ReflectiveOperationException {
        Class<?> inheritanceNode = Class.forName("net.luckperms.api.node.types.InheritanceNode");
        Object builder = inheritanceNode.getMethod("builder", String.class).invoke(null, groupName);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private Object await(Object future)
        throws ReflectiveOperationException, java.util.concurrent.ExecutionException, InterruptedException {
        if (future == null) return null;
        if (future instanceof Future) return ((Future<?>) future).get();
        return future.getClass().getMethod("join").invoke(future);
    }

    private String meta(UUID playerId, String kind) {
        if (playerId == null || !available()) return "";
        try {
            Object userManager = luckPermsApi.getClass().getMethod("getUserManager").invoke(luckPermsApi);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, playerId);
            if (user == null) return "";
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            String method = kind.equals("suffix") ? "getSuffix" : "getPrefix";
            Object value = metaData.getClass().getMethod(method).invoke(metaData);
            return value == null ? "" : String.valueOf(value);
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }
}
