package us.iorilabs.proxy.utilities;

import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class PageUtil {

    public static final int DEFAULT_PAGE_SIZE = 10;

    private PageUtil() {}

    public static int totalPages(int size, int pageSize) {
        if (size <= 0 || pageSize <= 0) return 1;
        return (size + pageSize - 1) / pageSize;
    }

    public static int clampPage(int requested, int totalPages) {
        if (totalPages <= 0) return 1;
        return Math.max(1, Math.min(requested, totalPages));
    }

    public static <T> List<T> slice(List<T> items, int page, int pageSize) {
        if (items == null || items.isEmpty() || pageSize <= 0) return List.of();

        int total = totalPages(items.size(), pageSize);
        int safe = clampPage(page, total);
        int from = (safe - 1) * pageSize;
        if (from >= items.size()) return List.of();

        int to = Math.min(from + pageSize, items.size());
        return Collections.unmodifiableList(items.subList(from, to));
    }

    public static int parsePage(String[] args, int index) {
        if (args == null || args.length <= index) return 1;

        try {
            return Math.max(1, Integer.parseInt(args[index]));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    public static Component navBar(
        String commandBase,
        int page,
        int totalPages,
        String labelLegacy,
        String prevHover,
        String nextHover
    ) {
        Component prev;
        if (page > 1) {
            prev = Component.text("[◀]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand(commandBase + " " + (page - 1)))
                .hoverEvent(
                    HoverEvent.showText(
                        Component.text(
                            prevHover == null || prevHover.isBlank() ? "Prev" : prevHover,
                            NamedTextColor.GRAY
                        )
                    )
                );
        } else {
            prev = Component.text("[◀]", NamedTextColor.DARK_GRAY);
        }

        Component next;
        if (page < totalPages) {
            next = Component.text("[▶]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand(commandBase + " " + (page + 1)))
                .hoverEvent(
                    HoverEvent.showText(
                        Component.text(
                            nextHover == null || nextHover.isBlank() ? "Next" : nextHover,
                            NamedTextColor.GRAY
                        )
                    )
                );
        } else {
            next = Component.text("[▶]", NamedTextColor.DARK_GRAY);
        }

        String label =
            labelLegacy == null || labelLegacy.isBlank() ? "&7Page &f" + page + "&7/&f" + totalPages : labelLegacy;

        return Component.empty()
            .append(prev)
            .append(Component.text(" "))
            .append(ColorUtil.parse(label))
            .append(Component.text(" "))
            .append(next);
    }

    public static Component actionButton(String label, NamedTextColor color, ClickEvent click, String hover) {
        Component button = Component.text(label, color).clickEvent(click);
        if (hover != null && !hover.isBlank()) {
            button = button.hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
        }
        return button;
    }
}
