package us.iorilabs.bukkit.modules.chatgames;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.chatgames.enums.ChatGameType;
import us.iorilabs.bukkit.modules.economy.EconomyModule;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.bukkit.storage.StoragePaths;

@Getter
@Accessors(fluent = true)
public final class ChatGamesService {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final File configFile;
    private final List<ChatGameQuestion> questions = new ArrayList<>();
    private final List<String> words = new ArrayList<>();
    private final Set<ChatGameType> enabledTypes = EnumSet.noneOf(ChatGameType.class);
    private final Map<UUID, Long> winnerCooldownUntil = new ConcurrentHashMap<>();
    private final AtomicBoolean active = new AtomicBoolean(false);

    private int intervalSeconds = 300;
    private int timeoutSeconds = 45;
    private int minPlayers = 1;
    private int winnerCooldownSeconds = 60;
    private double rewardMoney = 50.0D;
    private List<String> rewardCommands = Collections.emptyList();
    private int mathMin = 1;
    private int mathMax = 20;
    private boolean mathHard;
    private int algebraMin = 1;
    private int algebraMax = 20;

    private ChatGameRound current;
    private BukkitTask intervalTask;
    private BukkitTask timeoutTask;

    public ChatGamesService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.language = plugin.language();
        File legacy = new File(plugin.getDataFolder(), "data/chatgames.yml");
        this.configFile = StoragePaths.storage(plugin, "chatgames.yml");
        StoragePaths.migrateIfPresent(legacy, configFile);
    }

    public void load() {
        if (!configFile.exists()) {
            us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "data/chatgames.yml");
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        intervalSeconds = Math.max(30, yaml.getInt("INTERVAL-SECONDS", 300));
        timeoutSeconds = Math.max(10, yaml.getInt("TIMEOUT-SECONDS", 45));
        minPlayers = Math.max(1, yaml.getInt("MIN-PLAYERS", 1));
        winnerCooldownSeconds = Math.max(0, yaml.getInt("WINNER-COOLDOWN-SECONDS", 60));
        rewardMoney = Math.max(0.0D, yaml.getDouble("REWARD.MONEY", 50.0D));
        rewardCommands = yaml.getStringList("REWARD.COMMANDS");
        if (rewardCommands == null) rewardCommands = Collections.emptyList();

        mathMin = yaml.getInt("MATH.MIN", 1);
        mathMax = yaml.getInt("MATH.MAX", 20);
        mathHard = yaml.getBoolean("MATH.HARD", false);
        algebraMin = yaml.getInt("ALGEBRA.MIN", 1);
        algebraMax = yaml.getInt("ALGEBRA.MAX", 20);

        enabledTypes.clear();
        List<String> rawTypes = yaml.getStringList("TYPES");
        if (rawTypes == null || rawTypes.isEmpty()) {
            enabledTypes.add(ChatGameType.TRIVIA);
        } else {
            for (String raw : rawTypes) {
                ChatGameType type = ChatGameType.byId(raw);
                if (type != null) enabledTypes.add(type);
            }
            if (enabledTypes.isEmpty()) enabledTypes.add(ChatGameType.TRIVIA);
        }

        words.clear();
        for (String word : yaml.getStringList("WORDS")) {
            if (word == null || word.trim().isEmpty()) continue;
            words.add(word.trim().toLowerCase(Locale.ROOT));
        }

        questions.clear();
        List<?> rawQuestions = yaml.getList("QUESTIONS");
        if (rawQuestions != null) {
            for (Object entry : rawQuestions) {
                ChatGameQuestion question = parseQuestion(entry);
                if (question != null && !question.question().isEmpty() && !question.answers().isEmpty()) {
                    questions.add(question);
                }
            }
        }

        ConfigurationSection section = yaml.getConfigurationSection("QUESTIONS");
        if (section != null && questions.isEmpty()) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) continue;
                ChatGameQuestion question = new ChatGameQuestion(
                    child.getString("QUESTION", ""),
                    child.getStringList("ANSWERS")
                );
                if (!question.question().isEmpty() && !question.answers().isEmpty()) {
                    questions.add(question);
                }
            }
        }
    }

    public void startScheduler() {
        stopScheduler();
        long ticks = intervalSeconds * 20L;
        intervalTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tryStartGame(null), ticks, ticks);
    }

    public void stopScheduler() {
        if (intervalTask != null) {
            intervalTask.cancel();
            intervalTask = null;
        }
        endRound(false);
    }

    public boolean tryStartGame() {
        return tryStartGame(null);
    }

    public boolean tryStartGame(ChatGameType forced) {
        if (active.get()) return false;
        if (Bukkit.getOnlinePlayers().size() < minPlayers) return false;

        ChatGameType type = forced == null ? pickType() : forced;
        if (type == null) return false;
        ChatGameRound round = generate(type);
        if (round == null) return false;

        current = round;
        active.set(true);
        broadcastStart(round);

        if (timeoutTask != null) timeoutTask.cancel();
        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> endRound(true), timeoutSeconds * 20L);
        return true;
    }

    public boolean tryAnswer(Player player, String message) {
        if (player == null || message == null || !active.get()) return false;
        ChatGameRound round = current;
        if (round == null || !round.matches(message)) return false;

        long now = System.currentTimeMillis();
        Long until = winnerCooldownUntil.get(player.getUniqueId());
        if (until != null && until > now) {
            long left = Math.max(1L, (until - now + 999L) / 1000L);
            player.sendMessage(language.raw(player, "CHAT-GAMES.COOLDOWN", Map.of("seconds", String.valueOf(left))));
            return true;
        }

        if (!active.compareAndSet(true, false)) return false;
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }

        if (winnerCooldownSeconds > 0) {
            winnerCooldownUntil.put(player.getUniqueId(), now + winnerCooldownSeconds * 1000L);
        }

        boolean showMoneyReward = reward(player);
        String winKey = showMoneyReward ? "CHAT-GAMES.WIN" : "CHAT-GAMES.WIN-NO-REWARD";
        Map<String, String> winPlaceholders = new HashMap<>();
        winPlaceholders.put("player", player.getName());
        winPlaceholders.put("answer", round.primaryAnswer());
        winPlaceholders.put("question", round.question());
        winPlaceholders.put("type", round.type().id());
        if (showMoneyReward) winPlaceholders.put("money", formatMoney(rewardMoney));
        broadcast(winKey, winPlaceholders);
        if (plugin.sounds() != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                plugin.sounds().play(online, "SUCCESS");
            }
        }
        current = null;
        return true;
    }

    public void endRound(boolean timedOut) {
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        ChatGameRound round = current;
        boolean wasActive = active.getAndSet(false);
        current = null;
        if (!wasActive || !timedOut || round == null) return;

        broadcast(
            "CHAT-GAMES.TIMEOUT",
            Map.of("answer", round.primaryAnswer(), "question", round.question(), "type", round.type().id())
        );
    }

    private ChatGameType pickType() {
        List<ChatGameType> available = new ArrayList<>();
        for (ChatGameType type : enabledTypes) {
            if (canGenerate(type)) available.add(type);
        }
        if (available.isEmpty()) return null;
        return available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }

    private boolean canGenerate(ChatGameType type) {
        switch (type) {
            case TRIVIA:
                return !questions.isEmpty();
            case REACTION:
            case UNSCRAMBLE:
                return !words.isEmpty();
            case MATH:
            case ALGEBRA:
                return true;
            default:
                return false;
        }
    }

    private ChatGameRound generate(ChatGameType type) {
        switch (type) {
            case TRIVIA:
                return ChatGameGenerators.trivia(questions);
            case REACTION:
                return ChatGameGenerators.reaction(words);
            case UNSCRAMBLE:
                return ChatGameGenerators.unscramble(words);
            case MATH:
                return ChatGameGenerators.math(mathMin, mathMax, mathHard);
            case ALGEBRA:
                return ChatGameGenerators.algebra(algebraMin, algebraMax);
            default:
                return null;
        }
    }

    private void broadcastStart(ChatGameRound round) {
        String typed = "CHAT-GAMES.START-" + round.type().messageKey();
        Map<String, String> placeholders = Map.of(
            "question",
            round.question(),
            "type",
            round.type().id(),
            "answer",
            round.primaryAnswer()
        );
        for (Player online : Bukkit.getOnlinePlayers()) {
            List<String> lines = language.list(online, typed, placeholders);
            if (lines.isEmpty()) lines = language.list(online, "CHAT-GAMES.START", placeholders);
            for (String line : lines) online.sendMessage(line);
            if (plugin.sounds() != null) plugin.sounds().play(online, "NOTIFICATION");
        }
        List<String> console = language.list(typed, placeholders);
        if (console.isEmpty()) console = language.list("CHAT-GAMES.START", placeholders);
        for (String line : console) Bukkit.getConsoleSender().sendMessage(line);
    }

    private void broadcast(String key, Map<String, String> placeholders) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            for (String line : language.list(online, key, placeholders)) {
                online.sendMessage(line);
            }
        }
        for (String line : language.list(key, placeholders)) {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    private boolean reward(Player player) {
        boolean showMoneyReward = false;
        if (rewardMoney > 0.0D) {
            EconomyModule module = plugin.modules().get(ModuleId.ECONOMY);
            if (module == null) {
                plugin.coreLogger().warn("ChatGames · REWARD.MONEY>0 but EconomyModule missing");
            } else if (!module.enabled()) {
                plugin
                    .coreLogger()
                    .warn("ChatGames · REWARD.MONEY>0 but MODULES.ECONOMY is false — enable economy or set MONEY to 0");
            } else {
                EconomyService economy = module.service();
                if (economy == null) {
                    plugin.coreLogger().warn("ChatGames · EconomyModule enabled but service unavailable");
                } else {
                    boolean deposited = economy.deposit(player.getUniqueId(), rewardMoney);
                    showMoneyReward = deposited || module.enabled();
                }
            }
        }

        for (String command : rewardCommands) {
            if (command == null || command.trim().isEmpty()) continue;
            String parsed = command.replace("%player%", player.getName()).replace("<player>", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
        return showMoneyReward;
    }

    private String formatMoney(double amount) {
        EconomyModule module = plugin.modules().get(ModuleId.ECONOMY);
        EconomyService economy = module == null ? null : module.service();
        return economy == null ? String.valueOf((long) amount) : economy.format(amount);
    }

    @SuppressWarnings("unchecked")
    private ChatGameQuestion parseQuestion(Object entry) {
        if (!(entry instanceof Map)) return null;
        Map<?, ?> map = (Map<?, ?>) entry;
        Object question = map.get("QUESTION");
        if (question == null) question = map.get("question");
        Object answersObj = map.get("ANSWERS");
        if (answersObj == null) answersObj = map.get("answers");

        List<String> answers = new ArrayList<>();
        if (answersObj instanceof List) {
            for (Object value : (List<?>) answersObj) {
                if (value != null) answers.add(String.valueOf(value));
            }
        } else if (answersObj != null) {
            answers.add(String.valueOf(answersObj));
        } else {
            Object answer = map.get("ANSWER");
            if (answer == null) answer = map.get("answer");
            if (answer != null) answers.add(String.valueOf(answer));
        }
        return new ChatGameQuestion(question == null ? "" : String.valueOf(question), answers);
    }
}
