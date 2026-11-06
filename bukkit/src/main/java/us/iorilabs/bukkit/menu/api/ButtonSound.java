package us.iorilabs.bukkit.menu.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.fx.SoundKey;
import us.iorilabs.bukkit.fx.SoundService;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public enum ButtonSound {
    CLICK(SoundKey.UI_CLICK),
    SUCCESS(SoundKey.SUCCESS),
    FAIL(SoundKey.ERROR),
    DENY(SoundKey.DENY),
    CONFIRM(SoundKey.CONFIRM),
    PAGE(SoundKey.PAGE_TURN);

    private final SoundKey key;

    public void play(Player player) {
        SoundService sounds = MenuApi.sounds();
        if (sounds == null || player == null) return;
        sounds.play(player, key);
    }
}
