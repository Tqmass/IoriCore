package us.iorilabs.bukkit.modules.coinshop;

import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.modules.coinshop.enums.CoinshopOfferType;

@Getter
@Accessors(fluent = true)
public final class CoinshopOffer {

    private final String id;
    private final CoinshopOfferType type;
    private final String target;
    private final double price;
    private final String material;
    private final short data;
    private final boolean stub;

    public CoinshopOffer(
        String id,
        CoinshopOfferType type,
        String target,
        double price,
        String material,
        short data,
        boolean stub
    ) {
        this.id = id;
        this.type = type;
        this.target = target == null ? id : target;
        this.price = Math.max(0.0D, price);
        this.material = material == null || material.isEmpty() ? "NAME_TAG" : material;
        this.data = data;
        this.stub = stub;
    }
}
