package us.iorilabs.proxy.hook.luckperms;

import java.lang.reflect.Method;
import java.util.UUID;

public final class LuckPermsHook {

    private Object userManager;
    private Method getUser;
    private Method getCachedData;
    private Method getMetaData;
    private Method getPrefix;

    public LuckPermsHook() {
        try {
            Object luckPerms = Class.forName("net.luckperms.api.LuckPermsProvider").getMethod("get").invoke(null);
            userManager = Class.forName("net.luckperms.api.LuckPerms").getMethod("getUserManager").invoke(luckPerms);
            getUser = Class.forName("net.luckperms.api.model.user.UserManager").getMethod("getUser", UUID.class);
            getCachedData = Class.forName("net.luckperms.api.model.user.User").getMethod("getCachedData");
            getMetaData = Class.forName("net.luckperms.api.cacheddata.CachedDataManager").getMethod("getMetaData");
            getPrefix = Class.forName("net.luckperms.api.cacheddata.CachedMetaData").getMethod("getPrefix");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            userManager = null;
        }
    }

    public boolean isPresent() {
        return userManager != null;
    }

    public String getPrefix(UUID uuid) {
        if (userManager == null || uuid == null) return "";

        try {
            Object user = getUser.invoke(userManager, uuid);
            if (user == null) return "";

            Object prefix = getPrefix.invoke(getMetaData.invoke(getCachedData.invoke(user)));
            return prefix == null ? "" : String.valueOf(prefix);
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return "";
        }
    }
}
