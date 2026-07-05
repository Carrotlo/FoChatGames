package me.foesio.foChatGames.game;

import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.api.ChatGameEndEvent;
import me.foesio.foChatGames.api.ChatGameStartEvent;
import me.foesio.foChatGames.api.ChatGameWinEvent;
import me.foesio.foChatGames.config.Settings;
import me.foesio.foChatGames.storage.PlayerStats;
import me.foesio.foChatGames.util.NumberUtil;
import me.foesio.foChatGames.util.TextUtil;
import me.foesio.core.inventory.OverflowPolicy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class GameManager {
    private final FoChatGames plugin;
    private final WordListManager wordLists;
    private final Random random = new Random();
    private final Listener chatListener = new Listener() {
    };
    private volatile GameSession active;
    private BukkitTask timerTask;
    private BukkitTask timeoutTask;
    private long nextRunAtMillis;

    public GameManager(FoChatGames plugin, WordListManager wordLists) {
        this.plugin = plugin;
        this.wordLists = wordLists;
    }

    public void start() {
        registerChatListener();
        scheduleNext();
    }

    public void stopScheduler() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        HandlerList.unregisterAll(chatListener);
        plugin.log().info("Game scheduler stopped.");
    }

    public void reload() {
        HandlerList.unregisterAll(chatListener);
        registerChatListener();
        scheduleNext();
    }

    public GameSession active() {
        return active;
    }

    public int secondsUntilNext() {
        if (nextRunAtMillis <= 0L) {
            return 0;
        }
        return Math.max(0, (int) ((nextRunAtMillis - System.currentTimeMillis()) / 1000L));
    }

    public boolean canSeeGames(Player player) {
        return !blacklisted(player) && plugin.database().enabled(player.getUniqueId(), player.getName());
    }

    public boolean canWinGames(Player player) {
        return !blacklisted(player);
    }

    public boolean startRandom(GameType requested, boolean manual) {
        if (active != null) {
            return false;
        }
        if (!manual && !plugin.configs().settings().enabled) {
            scheduleNext();
            return false;
        }
        if (!manual && eligiblePlayerCount() < plugin.configs().settings().minimumPlayers) {
            plugin.log().debug("Skipped auto game: not enough players.");
            scheduleNext();
            return false;
        }

        GameType type = requested == null ? pickWeightedGame() : requested;
        if (type == null) {
            scheduleNext();
            return false;
        }
        GameSession session = generate(type, false, List.of());
        if (session == null) {
            scheduleNext();
            return false;
        }
        return startSession(session);
    }

    public boolean startCustom(GameType type, List<String> settings) {
        if (active != null || type == null) {
            return false;
        }
        GameSession session = generate(type, true, settings);
        return session != null && startSession(session);
    }

    public boolean stopActive(boolean broadcastEnd) {
        GameSession session = active;
        if (session == null) {
            return false;
        }
        active = null;
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        if (broadcastEnd) {
            broadcastEnd(session);
            plugin.log().info("Game ended without winner: " + session.type().key() + " answer=" + session.answer());
        } else {
            plugin.log().info("Game stopped silently: " + session.type().key());
        }
        Bukkit.getPluginManager().callEvent(new ChatGameEndEvent(session));
        scheduleNext();
        return true;
    }

    public boolean handleChat(AsyncPlayerChatEvent event) {
        GameSession session = active;
        if (session == null || event.isCancelled()) {
            return false;
        }
        Player player = event.getPlayer();
        if (plugin.editor().hasPrompt(player.getUniqueId())) {
            return false;
        }
        if (!canWinGames(player)) {
            return false;
        }

        String message = event.getMessage().trim();
        Settings settings = plugin.configs().settings();
        boolean exact = message.equals(session.answer());
        boolean ignoreCase = message.equalsIgnoreCase(session.answer());
        if (!exact && ignoreCase && settings.caseSensitive && settings.notifyWrongCapitalization) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.messages().send(player, "game.wrong-capitalization"));
            return false;
        }
        if (!exact && (settings.caseSensitive || !ignoreCase)) {
            return false;
        }

        Bukkit.getScheduler().runTask(plugin, () -> win(player, session));
        return settings.cancelWinningChat;
    }

    private void registerChatListener() {
        EventExecutor executor = (listener, event) -> {
            if (event instanceof AsyncPlayerChatEvent chatEvent && handleChat(chatEvent)) {
                chatEvent.setCancelled(true);
            }
        };
        plugin.getServer().getPluginManager().registerEvent(
                AsyncPlayerChatEvent.class,
                chatListener,
                plugin.configs().settings().chatListenerPriority,
                executor,
                plugin,
                false
        );
    }

    private void scheduleNext() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        Settings settings = plugin.configs().settings();
        if (!settings.autoStart) {
            nextRunAtMillis = 0L;
            plugin.log().debug("Auto game scheduler disabled.");
            return;
        }
        int delaySeconds = settings.nextDelaySeconds();
        nextRunAtMillis = System.currentTimeMillis() + delaySeconds * 1000L;
        timerTask = Bukkit.getScheduler().runTaskLater(plugin, () -> startRandom(null, false), delaySeconds * 20L);
        plugin.log().debug("Next automatic game scheduled in " + delaySeconds + "s.");
    }

    private boolean startSession(GameSession session) {
        ChatGameStartEvent event = new ChatGameStartEvent(session);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.log().warning("Game start cancelled by API: " + session.type().key());
            scheduleNext();
            return false;
        }

        active = session;
        plugin.database().recordStart(session.type());
        plugin.log().info("Game started: " + session.type().key() + " prompt=" + session.prompt() + " custom=" + session.custom());
        broadcastStart(session);
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
        timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (active == session) {
                stopActive(true);
            }
        }, plugin.configs().settings().gameDurationSeconds * 20L);
        return true;
    }

    private void win(Player player, GameSession session) {
        if (active != session) {
            return;
        }
        long timeMs = Math.max(1L, System.currentTimeMillis() - session.startedAtMillis());
        ChatGameWinEvent event = new ChatGameWinEvent(session, player, timeMs);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            plugin.log().warning("Game win cancelled by API: " + session.type().key() + " winner=" + player.getName());
            return;
        }
        active = null;
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }

        plugin.database().recordWin(player.getUniqueId(), player.getName(), session.type(), timeMs);
        giveRewards(player, session);
        broadcastWin(player, session, timeMs);
        plugin.log().info("Game won: " + session.type().key() + " winner=" + player.getName() + " timeMs=" + timeMs);
        scheduleNext();
    }

    private void giveRewards(Player player, GameSession session) {
        GameDefinition definition = plugin.configs().game(session.type());
        if (definition == null || definition.rewards().isEmpty()) {
            return;
        }
        if ("weighted_one".equalsIgnoreCase(plugin.configs().settings().rewardChanceMode)) {
            giveWeightedReward(player, session, definition.rewards());
            return;
        }
        for (Reward reward : definition.rewards()) {
            if (random.nextDouble() * 100.0D <= reward.chance()) {
                giveReward(player, session, reward);
            }
        }
    }

    private void giveWeightedReward(Player player, GameSession session, List<Reward> rewards) {
        double total = rewards.stream().mapToDouble(Reward::chance).sum();
        if (total <= 0.0D) {
            return;
        }
        double roll = random.nextDouble() * total;
        double current = 0.0D;
        for (Reward reward : rewards) {
            current += reward.chance();
            if (roll <= current) {
                giveReward(player, session, reward);
                return;
            }
        }
    }

    private void giveReward(Player player, GameSession session, Reward reward) {
        Map<String, String> placeholders = Map.of(
                "player", player.getName(),
                "game", session.type().key(),
                "answer", session.answer()
        );
        switch (reward.type()) {
            case CONSOLE_COMMAND -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacePercent(reward.command(), placeholders));
            case PLAYER_COMMAND -> player.performCommand(replacePercent(reward.command(), placeholders));
            case ITEM -> {
                ItemStack itemStack = reward.itemStack();
                plugin.core().inventoryDeposits().deposit(player, itemStack, OverflowPolicy.DROP_OVERFLOW);
            }
        }
    }

    private String replacePercent(String input, Map<String, String> placeholders) {
        String output = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            output = output.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return output;
    }

    private void broadcastStart(GameSession session) {
        broadcastGameState("game.start", "sounds.start", gameTokens(session, 0L, null));
    }

    private void broadcastWin(Player winner, GameSession session, long timeMs) {
        broadcastGameState("game.win", "sounds.win", gameTokens(session, timeMs, winner));
    }

    private void broadcastEnd(GameSession session) {
        broadcastGameState("game.end", "sounds.end", gameTokens(session, 0L, null));
    }

    private void broadcastGameState(String messageBasePath, String soundPath, Map<String, String> tokens) {
        Settings settings = plugin.configs().settings();
        if (settings.chatBroadcasts) {
            plugin.messages().broadcastGame(messageBasePath + ".chat", messageBasePath + ".hover", tokens);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canSeeGames(player)) {
                continue;
            }
            if (settings.titleBroadcasts || settings.subtitleBroadcasts) {
                plugin.messages().title(player,
                        settings.titleBroadcasts ? plugin.messages().formatPathNoPrefix(messageBasePath + ".title", tokens) : "",
                        settings.subtitleBroadcasts ? plugin.messages().formatPathNoPrefix(messageBasePath + ".subtitle", tokens) : "");
            }
            if (settings.actionbarBroadcasts) {
                plugin.messages().actionbar(player, plugin.messages().formatPathNoPrefix(messageBasePath + ".actionbar", tokens));
            }
            plugin.messages().playConfiguredSound(player, soundPath);
        }
    }

    private Map<String, String> gameTokens(GameSession session, long timeMs, Player winner) {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("game", session.displayName());
        tokens.put("prompt", session.prompt());
        tokens.put("answer", session.answer());
        tokens.put("duration", String.valueOf(plugin.configs().settings().gameDurationSeconds));
        tokens.put("time", NumberUtil.formatSeconds(timeMs / 1000.0D));
        tokens.put("player", winner == null ? "" : winner.getName());
        if (winner != null) {
            PlayerStats stats = plugin.database().stats(winner.getUniqueId());
            tokens.put("wins", NumberUtil.format(stats.totalWins()));
            tokens.put("record", NumberUtil.formatSeconds(stats.bestRecordMs() / 1000.0D));
        } else {
            tokens.put("wins", "0");
            tokens.put("record", "-");
        }
        return tokens;
    }

    private GameType pickWeightedGame() {
        List<GameDefinition> weighted = plugin.configs().games().values().stream()
                .filter(GameDefinition::enabled)
                .filter(definition -> definition.chance() > 0)
                .toList();
        int total = weighted.stream().mapToInt(GameDefinition::chance).sum();
        if (total <= 0) {
            return null;
        }
        int roll = random.nextInt(total) + 1;
        int current = 0;
        for (GameDefinition definition : weighted) {
            current += definition.chance();
            if (roll <= current) {
                return definition.type();
            }
        }
        return weighted.getFirst().type();
    }

    private int eligiblePlayerCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (canWinGames(player)) {
                count++;
            }
        }
        return count;
    }

    private boolean blacklisted(Player player) {
        return plugin.configs().settings().worldBlacklist.contains(player.getWorld().getName().toLowerCase(Locale.ROOT));
    }

    private GameSession generate(GameType type, boolean custom, List<String> settings) {
        GameDefinition definition = plugin.configs().game(type);
        if (definition == null) {
            return null;
        }
        return switch (type) {
            case COMPLETION -> completion(definition, custom, settings);
            case GUESS_THE_NUMBER -> guessNumber(definition, custom, settings);
            case MATH -> math(definition, custom, settings);
            case TRIVIA -> trivia(definition, custom, settings);
            case TYPE -> typeGame(definition, custom, settings);
            case UNREVERSE -> unreverse(definition, custom, settings);
            case UNSCRAMBLE -> unscramble(definition, custom, settings);
        };
    }

    private GameSession completion(GameDefinition definition, boolean custom, List<String> settings) {
        String answer = custom && !settings.isEmpty() ? settings.getFirst() : randomWord(definition);
        int percent = custom && settings.size() > 1 ? parseInt(settings.get(1), definition.missingPercent()) : definition.missingPercent();
        String prompt = hideLetters(answer, percent);
        return new GameSession(definition.type(), definition.displayName(), prompt, answer, custom);
    }

    private GameSession guessNumber(GameDefinition definition, boolean custom, List<String> settings) {
        int min = definition.minimum();
        int max = definition.maximum();
        Integer answer = null;
        if (custom && settings.size() >= 2) {
            min = parseInt(settings.get(0), min);
            max = parseInt(settings.get(1), max);
            if (settings.size() >= 3) {
                answer = parseInt(settings.get(2), min);
            }
        }
        if (max < min) {
            max = min;
        }
        int value = answer == null ? min + random.nextInt(max - min + 1) : answer;
        String prompt = "Guess a number between " + min + " and " + max;
        return new GameSession(definition.type(), definition.displayName(), prompt, String.valueOf(value), custom);
    }

    private GameSession math(GameDefinition definition, boolean custom, List<String> settings) {
        if (custom && settings.size() >= 2) {
            return new GameSession(definition.type(), definition.displayName(), settings.get(0), settings.get(1), true);
        }
        int min = Math.min(definition.minimum(), definition.maximum());
        int max = Math.max(definition.minimum(), definition.maximum());
        int left = min + random.nextInt(max - min + 1);
        int right = min + random.nextInt(max - min + 1);
        List<String> operators = definition.operators().isEmpty() ? List.of("add", "subtract", "multiply") : definition.operators();
        String operator = operators.get(random.nextInt(operators.size())).toLowerCase(Locale.ROOT);
        return switch (operator) {
            case "subtract", "-" -> new GameSession(definition.type(), definition.displayName(), left + " - " + right, String.valueOf(left - right), false);
            case "multiply", "*" -> new GameSession(definition.type(), definition.displayName(), left + " x " + right, String.valueOf(left * right), false);
            default -> new GameSession(definition.type(), definition.displayName(), left + " + " + right, String.valueOf(left + right), false);
        };
    }

    private GameSession trivia(GameDefinition definition, boolean custom, List<String> settings) {
        if (custom && settings.size() >= 2) {
            return new GameSession(definition.type(), definition.displayName(), settings.get(0), settings.get(1), true);
        }
        List<TriviaQuestion> questions = definition.triviaQuestions();
        TriviaQuestion question = questions.isEmpty()
                ? new TriviaQuestion("What mob explodes near players?", "creeper")
                : questions.get(random.nextInt(questions.size()));
        return new GameSession(definition.type(), definition.displayName(), question.question(), question.answer(), false);
    }

    private GameSession typeGame(GameDefinition definition, boolean custom, List<String> settings) {
        String answer = custom && !settings.isEmpty() ? settings.getFirst() : randomWord(definition);
        String prompt = definition.randomizedText() ? randomCase(answer) : answer;
        String expected = definition.randomizedText() ? prompt : answer;
        return new GameSession(definition.type(), definition.displayName(), prompt, expected, custom);
    }

    private GameSession unreverse(GameDefinition definition, boolean custom, List<String> settings) {
        String answer = custom && !settings.isEmpty() ? settings.getFirst() : randomWord(definition);
        String prompt = new StringBuilder(answer).reverse().toString();
        return new GameSession(definition.type(), definition.displayName(), prompt, answer, custom);
    }

    private GameSession unscramble(GameDefinition definition, boolean custom, List<String> settings) {
        String answer = custom && !settings.isEmpty() ? settings.getFirst() : randomWord(definition);
        String prompt = scramble(answer, definition.smartScramble());
        return new GameSession(definition.type(), definition.displayName(), prompt, answer, custom);
    }

    private String randomWord(GameDefinition definition) {
        List<String> words = wordLists.words(definition.wordLists());
        return words.get(random.nextInt(words.size()));
    }

    private String hideLetters(String answer, int percent) {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < answer.length(); index++) {
            if (!Character.isWhitespace(answer.charAt(index))) {
                indexes.add(index);
            }
        }
        Collections.shuffle(indexes, random);
        int hide = Math.max(1, Math.round(indexes.size() * (percent / 100.0F)));
        char[] chars = answer.toCharArray();
        for (int index = 0; index < Math.min(hide, indexes.size()); index++) {
            chars[indexes.get(index)] = '_';
        }
        return new String(chars);
    }

    private String scramble(String answer, boolean smart) {
        if (!smart) {
            return shuffle(answer);
        }
        String[] parts = answer.split(" ");
        for (int index = 0; index < parts.length; index++) {
            parts[index] = shuffle(parts[index]);
        }
        return String.join(" ", parts);
    }

    private String shuffle(String input) {
        if (input.length() < 2 || input.chars().distinct().count() < 2L) {
            return input;
        }
        String output = input;
        for (int attempt = 0; attempt < 8 && output.equalsIgnoreCase(input); attempt++) {
            output = shuffledOnce(input);
        }
        return output;
    }

    private String shuffledOnce(String input) {
        List<Character> chars = new ArrayList<>();
        for (char character : input.toCharArray()) {
            chars.add(character);
        }
        for (int index = chars.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            char temp = chars.get(index);
            chars.set(index, chars.get(swap));
            chars.set(swap, temp);
        }
        StringBuilder builder = new StringBuilder();
        chars.forEach(builder::append);
        return builder.toString();
    }

    private String randomCase(String input) {
        StringBuilder builder = new StringBuilder();
        for (char character : input.toCharArray()) {
            builder.append(random.nextBoolean() ? Character.toUpperCase(character) : Character.toLowerCase(character));
        }
        return builder.toString();
    }

    private int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public String formatLast(long millis) {
        if (millis <= 0L) {
            return plugin.messages().messagePathText("status.never");
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        if (seconds < 60L) {
            return seconds + "s ago";
        }
        if (seconds < 3600L) {
            return (seconds / 60L) + "m ago";
        }
        return Instant.ofEpochMilli(millis).toString();
    }
}
