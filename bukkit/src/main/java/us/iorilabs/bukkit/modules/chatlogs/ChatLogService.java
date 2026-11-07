package us.iorilabs.bukkit.modules.chatlogs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.storage.StoragePaths;

public final class ChatLogService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter GENERATED = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern DATE_IN_NAME = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
    private static final String HEADER_RULE = "────────────────────────────────────────";

    private final IoriCorePlugin plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ioricore-chat-logs");
                t.setDaemon(true);
                return t;
            }
        }
    );

    private final Map<String, AtomicInteger> chatTotals = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> commandTotals = new ConcurrentHashMap<>();

    private File logsRoot;
    private String serverId = "server";
    private boolean useUuidFolder;
    private int maxLines = 100_000;
    private int purgeDays = 60;

    public ChatLogService(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration settings) {
        serverId = settings.getString("SETTINGS.SERVER-NAME", "server");
        useUuidFolder = "UUID".equalsIgnoreCase(settings.getString("CHAT-LOGS.PLAYER-FOLDER", "NAME"));
        maxLines = Math.max(100, settings.getInt("CHAT-LOGS.MAX-LINES", 100_000));
        purgeDays = Math.max(0, settings.getInt("CHAT-LOGS.PURGE-DAYS", 60));

        logsRoot = StoragePaths.logs(plugin);
        ensureDirs();

        executor.execute(this::purge);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public void logChat(Player player, String message) {
        if (player == null || message == null) return;
        logChat(player, message, resolveRank(player));
    }

    public void logCommand(Player player, String command, boolean blocked) {
        if (player == null || command == null) return;
        logCommand(player, command, resolveRank(player), blocked);
    }

    public String resolveRank(Player player) {
        CoreLanguage language = plugin.language();
        if (player.hasPermission("ioricore.rank.owner") || player.isOp()) return language.plain("CHAT-LOGS.RANK.OWNER");
        if (player.hasPermission("ioricore.staff") || player.hasPermission("ioricore.rank.staff")) {
            return language.plain("CHAT-LOGS.RANK.STAFF");
        }
        if (player.hasPermission("ioricore.logs.donor") || player.hasPermission("ioricore.rank.donor")) {
            return language.plain("CHAT-LOGS.RANK.DONOR");
        }
        return language.plain("CHAT-LOGS.RANK.PLAYER");
    }

    public void logChat(Player player, String message, String rankPrefix) {
        long now = System.currentTimeMillis();
        String name = player.getName();
        String key = playerKey(player);
        UUID uuid = player.getUniqueId();
        String rank = rankPrefix == null || rankPrefix.trim().isEmpty() ? "-" : rankPrefix;
        String line = formatLine(now, rank, name, message, null);

        executor.execute(() -> appendPlayer(key, name, uuid, rank, "chat", chatTotals, line, now));
    }

    public void logCommand(Player player, String command, String rankPrefix, boolean blocked) {
        long now = System.currentTimeMillis();
        String name = player.getName();
        String key = playerKey(player);
        UUID uuid = player.getUniqueId();
        String rank = rankPrefix == null || rankPrefix.trim().isEmpty() ? "-" : rankPrefix;
        String status = blocked ? "[! BLOCKED]" : "[✓ ALLOWED]";
        String cmd = command.startsWith("/") ? command : "/" + command;
        String line = formatLine(now, rank, name, cmd, status);

        executor.execute(() -> {
            appendPlayer(key, name, uuid, rank, "commands", commandTotals, line, now);
            appendGlobal(new File(logsRoot, "commands.log"), line);
        });
    }

    public void logStaffAction(String actor, String action) {
        long now = System.currentTimeMillis();
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault());
        String line = "[" + TIME.format(dt) + "] (" + serverId + ") " + actor + ": " + action;
        executor.execute(() -> appendGlobal(new File(logsRoot, "staff-actions.log"), line));
    }

    public void purge() {
        if (logsRoot == null || !logsRoot.exists() || purgeDays <= 0) return;

        LocalDate cutoff = LocalDate.now(ZoneId.systemDefault()).minusDays(purgeDays);
        int deleted = purgeDir(logsRoot, cutoff);
        if (deleted > 0) {
            plugin.getLogger().info("Chat-logs purge · removed " + deleted + " file(s) older than " + purgeDays + "d");
        }
    }

    private void flattenLegacyPlayerFolders(File playersDir) {
        if (playersDir == null || !playersDir.isDirectory()) return;

        File[] children = playersDir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (!child.isDirectory()) continue;
            File target = new File(logsRoot, child.getName());
            if (!target.exists()) child.renameTo(target);
            else mergeDirectory(child, target);
        }
        playersDir.delete();
    }

    private void mergeDirectory(File source, File target) {
        File[] children = source.listFiles();
        if (children == null) return;

        if (!target.exists()) target.mkdirs();
        for (File child : children) {
            File dest = new File(target, child.getName());
            if (child.isDirectory()) mergeDirectory(child, dest);
            else if (!dest.exists()) child.renameTo(dest);
        }
        source.delete();
    }

    private int purgeDir(File dir, LocalDate cutoff) {
        File[] children = dir.listFiles();
        if (children == null) return 0;

        int deleted = 0;
        for (File child : children) {
            if (child.isDirectory()) {
                deleted += purgeDir(child, cutoff);
                File[] left = child.listFiles();
                if (left != null && left.length == 0) child.delete();
                continue;
            }
            if (!child.getName().endsWith(".log")) continue;

            LocalDate fileDate = dateFromName(child.getName());
            if (fileDate == null) {
                fileDate = Instant.ofEpochMilli(child.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            if (!fileDate.isBefore(cutoff)) continue;
            if (child.delete()) deleted++;
        }
        return deleted;
    }

    private void capFile(File file) {
        if (file == null || !file.exists() || maxLines <= 0) return;

        List<String> header = new ArrayList<>();
        List<String> body = new ArrayList<>();
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            )
        ) {
            String line;
            boolean inHeader = true;
            int borderCount = 0;
            while ((line = reader.readLine()) != null) {
                if (inHeader) {
                    if (line.startsWith("[") && line.length() > 9 && Character.isDigit(line.charAt(1))) {
                        inHeader = false;
                        body.add(line);
                        continue;
                    }
                    header.add(line);
                    if (line.startsWith("====") || line.startsWith("────")) {
                        borderCount++;
                        if (borderCount >= 2) inHeader = false;
                    }
                    continue;
                }
                body.add(line);
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to read log for cap " + file.getName(), ex);
            return;
        }

        if (body.size() <= maxLines) return;

        List<String> kept = body.subList(body.size() - maxLines, body.size());
        try (
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8)
            )
        ) {
            for (String h : header) {
                writer.write(h);
                writer.newLine();
            }
            for (String b : kept) {
                writer.write(b);
                writer.newLine();
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to cap log " + file.getName(), ex);
        }
    }

    private static LocalDate dateFromName(String name) {
        Matcher matcher = DATE_IN_NAME.matcher(name);
        if (!matcher.find()) return null;
        try {
            return LocalDate.parse(matcher.group(1), DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void ensureDirs() {
        mk(logsRoot);
    }

    private String playerKey(Player player) {
        return useUuidFolder ? player.getUniqueId().toString() : player.getName();
    }

    private String formatLine(long epochMs, String rank, String name, String body, String status) {
        LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        StringBuilder sb = new StringBuilder();
        sb.append('[')
            .append(TIME.format(dt))
            .append("] (")
            .append(serverId)
            .append(") [")
            .append(rank)
            .append("] ")
            .append(name)
            .append(": ")
            .append(body);
        if (status != null) sb.append(' ').append(status);
        return sb.toString();
    }

    private void appendPlayer(
        String folderKey,
        String playerName,
        UUID uuid,
        String rank,
        String kind,
        Map<String, AtomicInteger> totals,
        String line,
        long epochMs
    ) {
        File dir = new File(logsRoot, folderKey);
        mk(dir);

        String date = DATE.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()));
        File file = new File(dir, kind + "-" + date + ".log");
        String totalKey = folderKey + ":" + kind;
        AtomicInteger total = totals.computeIfAbsent(totalKey, k -> new AtomicInteger(0));

        if (!file.exists()) writeHeader(file, kind, playerName, uuid, rank, total.get());

        try (
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)
            )
        ) {
            writer.write(line);
            writer.newLine();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to append player log " + file.getName(), ex);
            return;
        }

        total.incrementAndGet();
        capFile(file);
    }

    private void appendGlobal(File file, String line) {
        mk(file.getParentFile());
        try (
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)
            )
        ) {
            writer.write(line);
            writer.newLine();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to append global log " + file.getName(), ex);
            return;
        }
        capFile(file);
    }

    private void writeHeader(File file, String kind, String playerName, UUID uuid, String rank, int total) {
        String generated = GENERATED.format(LocalDateTime.now());
        String countLabel = "commands".equalsIgnoreCase(kind) ? "Commands count" : "Messages count";
        String rankLabel = rank == null || rank.trim().isEmpty() ? "-" : rank.trim();
        try (
            BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8)
            )
        ) {
            writer.write(HEADER_RULE);
            writer.newLine();
            writer.write("# Player: " + (playerName == null ? "-" : playerName));
            writer.newLine();
            writer.write("# UUID: " + (uuid == null ? "-" : uuid));
            writer.newLine();
            writer.write("# Rank: " + rankLabel);
            writer.newLine();
            writer.write("# " + countLabel + ": " + total);
            writer.newLine();
            writer.write("# Generated: " + generated);
            writer.newLine();
            writer.write(HEADER_RULE);
            writer.newLine();
            writer.newLine();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to write log header " + file.getName(), ex);
        }
    }

    private static void mk(File dir) {
        if (dir != null && !dir.exists()) dir.mkdirs();
    }
}
