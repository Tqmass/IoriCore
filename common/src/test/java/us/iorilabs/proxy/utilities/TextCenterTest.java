package us.iorilabs.proxy.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TextCenterTest {

    @Test
    void centerStripsWhitespaceInsideTagsSoTrailingNewlineFromYamlDoesNotBreakWidth() {
        String input = "<center>&eShop &7@ &6https://example.com/path\n</center>";
        int width = 54;
        String out = TextCenter.apply(input, width);
        int pad = (width - "Shop @ https://example.com/path".length()) / 2;
        assertTrue(pad > 0);
        assertTrue(out.startsWith(" ".repeat(pad)));
        assertTrue(out.contains("https://example.com/path"));
    }

    @Test
    void urlOnlyCenteredLineKeepsLeadingSpacesAfterApply() {
        String url = "https://example.com/path";
        String centered = TextCenter.apply("<center>&e" + url + "</center>", 54);
        assertFalse(centered.startsWith(url));
    }
}
