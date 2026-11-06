package us.iorilabs.bukkit.menu.api.yaml.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.api.yaml.YamlMenuService;
import us.iorilabs.bukkit.modules.coinshop.CoinshopModule;
import us.iorilabs.bukkit.modules.coinshop.CoinshopOffer;
import us.iorilabs.bukkit.modules.coinshop.CoinshopService;
import us.iorilabs.bukkit.modules.coinshop.enums.CoinshopOfferType;
import us.iorilabs.bukkit.modules.coinshop.enums.PurchaseResult;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.bukkit.modules.tags.Tag;
import us.iorilabs.bukkit.modules.tags.TagService;

public final class CoinshopMenuInternals {

    private CoinshopMenuInternals() {}

    public static void register(IoriCorePlugin plugin, YamlMenuService menus) {
        menus.dynamics().register("coinshop.offers", (player, ctx) -> {
            CoinshopService shop = shop(plugin);
            if (shop == null) return List.of();
            CoinshopOfferType type = typeOf(ctx == null ? null : ctx.get("type"));
            List<Map<String, String>> out = new ArrayList<>();
            for (CoinshopOffer offer : shop.offers(type)) {
                out.add(placeholders(plugin, shop, player, offer));
            }
            return out;
        });

        menus.internals().registerEnricher("coinshop", (player, menuId, ctx) -> {
            CoinshopService shop = shop(plugin);
            if (shop == null) return;
            EconomyService economy = shop.economy();
            double bal = economy == null ? 0.0D : economy.balance(player.getUniqueId());
            ctx.put("balance", economy == null ? "0" : economy.format(bal));
            CoinshopOffer offer = resolveOffer(shop, ctx);
            if (offer != null) ctx.putAll(placeholders(plugin, shop, player, offer));
        });

        menus.internals().register("coinshop.open-tags", (player, argument, ctx, service) -> {
            Map<String, String> next = new HashMap<>(ctx);
            next.put("type", CoinshopOfferType.TAG.name());
            service.open(player, "coinshop-tags", next);
        });

        menus.internals().register("coinshop.open-confirm", (player, argument, ctx, service) -> {
            CoinshopService shop = shop(plugin);
            Map<String, String> next = new HashMap<>(ctx == null ? Map.of() : ctx);
            if (argument != null && !argument.trim().isEmpty()) next.put("offer", argument.trim());
            CoinshopOffer offer = resolveOffer(shop, next);
            if (offer == null && argument != null && !argument.isEmpty() && shop != null) {
                offer = shop.offer(argument.trim());
            }
            if (shop != null && offer != null && shop.owns(player, offer)) {
                player.sendMessage(
                    plugin
                        .language()
                        .rawFor(player, "COINSHOP.ALREADY-OWNED", placeholders(plugin, shop, player, offer))
                );
                service.refresh(player);
                return;
            }
            service.open(player, "coinshop-confirm", next);
        });

        menus.internals().register("coinshop.purchase", (player, argument, ctx, service) -> {
            CoinshopService shop = shop(plugin);
            if (shop == null) {
                player.sendMessage(plugin.language().rawFor(player, "COINSHOP.UNAVAILABLE"));
                return;
            }
            CoinshopOffer offer = resolveOffer(shop, ctx);
            if (offer == null && argument != null && !argument.isEmpty()) offer = shop.offer(argument.trim());
            if (offer == null) {
                player.sendMessage(plugin.language().rawFor(player, "COINSHOP.UNKNOWN-OFFER"));
                return;
            }
            Map<String, String> placeholders = placeholders(plugin, shop, player, offer);
            PurchaseResult result = shop.purchase(player, offer);
            switch (result) {
                case SUCCESS:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.PURCHASED", placeholders));
                    if (offer.type() == CoinshopOfferType.TAG) {
                        player.sendMessage(plugin.language().rawFor(player, "COINSHOP.TAG-HINT", placeholders));
                    }
                    player.closeInventory();
                    return;
                case ALREADY_OWNED:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.ALREADY-OWNED", placeholders));
                    break;
                case INSUFFICIENT:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.INSUFFICIENT", placeholders));
                    break;
                case UNKNOWN:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.UNKNOWN-OFFER", placeholders));
                    break;
                case COSMETIC_UNAVAILABLE:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.COSMETIC-UNAVAILABLE", placeholders));
                    break;
                case UNAVAILABLE:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.UNAVAILABLE"));
                    break;
                default:
                    player.sendMessage(plugin.language().rawFor(player, "COINSHOP.FAILED"));
                    break;
            }
            service.refresh(player);
        });
    }

    private static CoinshopOffer resolveOffer(CoinshopService shop, Map<String, String> ctx) {
        if (shop == null || ctx == null) return null;
        String id = ctx.get("offer");
        if (id == null || id.isEmpty()) id = ctx.get("id");
        return id == null || id.isEmpty() ? null : shop.offer(id);
    }

    private static Map<String, String> placeholders(
        IoriCorePlugin plugin,
        CoinshopService shop,
        org.bukkit.entity.Player player,
        CoinshopOffer offer
    ) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", offer.id());
        placeholders.put("offer", offer.id());
        placeholders.put("target", offer.target());
        placeholders.put("price", formatPrice(shop, offer.price()));
        placeholders.put("display", offer.target());
        placeholders.put("preview", offer.target());
        placeholders.put("type", offer.type().name());
        placeholders.put("material", offer.material());
        placeholders.put("data", String.valueOf(offer.data()));
        EconomyService economy = shop.economy();
        double bal = economy == null ? 0.0D : economy.balance(player.getUniqueId());
        placeholders.put("balance", economy == null ? "0" : economy.format(bal));
        boolean owned = shop.owns(player, offer);
        placeholders.put("owned", owned ? "true" : "false");
        placeholders.put("stub", offer.stub() ? "true" : "false");
        placeholders.put("glow", owned ? "true" : "false");
        String loreRoot = offer.type() == CoinshopOfferType.TAG ? "COINSHOP.TAGS" : "COINSHOP.COSMETICS";
        placeholders.put(
            "status",
            plugin.language().rawFor(player, owned ? loreRoot + ".OWNED-LINE" : loreRoot + ".BUY-LINE")
        );
        placeholders.put("stub_line", offer.stub() ? firstListLine(plugin, player, loreRoot + ".STUB-LORE") : "");
        if (offer.type() == CoinshopOfferType.TAG) {
            TagService tags = shop.tags();
            Tag tag = tags == null ? null : tags.get(offer.target());
            if (tag != null) {
                placeholders.put("display", tag.display());
                placeholders.put("preview", tag.fragment());
            }
        }
        return placeholders;
    }

    private static String firstListLine(IoriCorePlugin plugin, org.bukkit.entity.Player player, String key) {
        List<String> lines = plugin.language().listFor(player, key);
        return lines == null || lines.isEmpty() ? "" : lines.get(0);
    }

    private static String formatPrice(CoinshopService shop, double price) {
        EconomyService economy = shop.economy();
        return economy == null ? String.valueOf((long) price) : economy.format(price);
    }

    private static CoinshopOfferType typeOf(String raw) {
        if (raw == null || raw.trim().isEmpty()) return CoinshopOfferType.TAG;
        try {
            return CoinshopOfferType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return CoinshopOfferType.TAG;
        }
    }

    private static CoinshopService shop(IoriCorePlugin plugin) {
        if (plugin.modules() == null) return null;
        CoinshopModule module = plugin.modules().get(ModuleId.COINSHOP);
        return module == null ? null : module.service();
    }
}
