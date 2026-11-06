package us.iorilabs.bukkit.modules.staffmode.inventory;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

@Getter
@Setter
@Accessors(fluent = true)
public final class StaffSnapshot {

    private ItemStack[] contents = new ItemStack[0];
    private ItemStack[] armor = new ItemStack[0];
    private Location location;
    private double health = 20.0D;
    private int food = 20;
    private float saturation = 5.0F;
    private float exp;
    private int level;
    private List<PotionEffect> effects = new ArrayList<>();
    private GameMode gameMode;
    private boolean allowFlight;
    private boolean flying;
    private boolean vanished;

    public static StaffSnapshot capture(Player player, boolean vanished) {
        StaffSnapshot snapshot = new StaffSnapshot();
        snapshot.contents(cloneItems(player.getInventory().getContents()));
        snapshot.armor(cloneItems(player.getInventory().getArmorContents()));
        snapshot.location(player.getLocation().clone());
        snapshot.health(player.getHealth());
        snapshot.food(player.getFoodLevel());
        snapshot.saturation(player.getSaturation());
        snapshot.exp(player.getExp());
        snapshot.level(player.getLevel());
        snapshot.effects(new ArrayList<>(player.getActivePotionEffects()));
        snapshot.gameMode(player.getGameMode());
        snapshot.allowFlight(player.getAllowFlight());
        snapshot.flying(player.isFlying());
        snapshot.vanished(vanished);
        return snapshot;
    }

    @SuppressWarnings("deprecation")
    public void restore(Player player, boolean restoreLocation) {
        player.getInventory().clear();
        player.getInventory().setContents(contents);
        if (armor.length > 0) player.getInventory().setArmorContents(armor);

        for (PotionEffect active : player.getActivePotionEffects()) {
            player.removePotionEffect(active.getType());
        }
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect, true);
        }

        if (gameMode != null) player.setGameMode(gameMode);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying && player.getAllowFlight());
        player.setHealth(Math.max(0.5D, Math.min(health, player.getMaxHealth())));
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setExp(exp);
        player.setLevel(level);
        if (restoreLocation && location != null && location.getWorld() != null) player.teleport(location);
        player.updateInventory();
    }

    private static ItemStack[] cloneItems(ItemStack[] source) {
        if (source == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }
}
