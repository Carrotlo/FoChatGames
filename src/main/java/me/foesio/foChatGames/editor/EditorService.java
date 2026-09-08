package me.foesio.foChatGames.editor;

import me.foesio.core.dialog.DialogButton;
import me.foesio.core.dialog.DialogService;
import me.foesio.core.dialog.DialogServiceFactory;
import me.foesio.core.dialog.FallbackDialogService;
import me.foesio.core.dialog.NativeDialogSettings;
import me.foesio.core.dialog.NativeDialogSupport;
import me.foesio.core.dialog.TextDialogRequest;
import me.foesio.core.editor.CursorItemEditor;
import me.foesio.core.editor.EditorDialogInputs;
import me.foesio.core.editor.EditorItemFactory;
import me.foesio.core.gui.GuiButtons;
import me.foesio.core.message.FoStyle;
import me.foesio.core.number.LargeNumberParser;
import me.foesio.core.selector.TriStateSelectionActionType;
import me.foesio.core.selector.TriStateSelectionClick;
import me.foesio.core.selector.TriStateSelectionHolder;
import me.foesio.core.selector.TriStateSelectionMenus;
import me.foesio.core.selector.TriStateSelectionRequest;
import me.foesio.core.selector.TriStateSelectionState;
import me.foesio.core.selector.WorldSelectionEntries;
import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.game.GameDefinition;
import me.foesio.foChatGames.game.GameType;
import me.foesio.foChatGames.util.NumberUtil;
import me.foesio.foChatGames.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class EditorService implements Listener {
    private final FoChatGames plugin;
    private final Map<UUID, EditorPrompt> prompts = new HashMap<>();
    private final Set<UUID> warnedFallback = new HashSet<>();
    private NativeDialogSupport nativeDialogSupport;

    public EditorService(FoChatGames plugin) {
        this.plugin = plugin;
        refreshInputService();
    }

    public boolean hasPrompt(UUID uuid) {
        return prompts.containsKey(uuid);
    }

    public void refreshInputService() {
        nativeDialogSupport = NativeDialogSupport.detect(
                plugin,
                NativeDialogSettings.fromConfig(plugin.getConfig().getConfigurationSection("native-dialogs"))
        );
        warnedFallback.clear();
        logNativeDialogStatus();
    }

    public void warnFallbackIfNeeded(Player player) {
        if (!nativeDialogSupport.configEnabled() || nativeDialogSupport.canUseNativeDialogs(player)) {
            return;
        }
        if (!nativeDialogSupport.warnOnFallback() || !warnedFallback.add(player.getUniqueId())) {
            return;
        }
        plugin.messages().send(player, "editor.native-dialog-fallback");
    }

    private boolean nativeDialogsEnabled() {
        return nativeDialogSupport.configEnabled();
    }

    private boolean nativeDialogsSupported() {
        return nativeDialogSupport.serverSupportsNativeDialogs();
    }

    private void logNativeDialogStatus() {
        if (nativeDialogsEnabled() && !nativeDialogsSupported()) {
            plugin.log().warning("Native dialogs enabled but unavailable. Admin editor will use chat input fallback.");
        } else if (nativeDialogsEnabled()) {
            plugin.log().info("Native dialogs enabled for admin editor inputs.");
        } else {
            plugin.log().info("Native dialogs disabled in config. Admin editor will use chat input fallback.");
        }
    }

    public void openMain(Player player) {
        openMain(player, true);
    }

    private void openMain(Player player, boolean playOpenSound) {
        if (playOpenSound) {
            plugin.editorSounds().open(player);
        }
        EditorHolder holder = new EditorHolder(Page.MAIN, null, -1);
        Inventory inventory = Bukkit.createInventory(holder, 27, TextUtil.color("&8ғᴏᴄʜᴀᴛɢᴀᴍᴇs ᴇᴅɪᴛᴏʀ"));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(11, button(player, FoStyle.THEME, Material.COMPARATOR, "General", List.of("&#ffffffEdit timers, toggles, modes."), "edit general settings"));
        inventory.setItem(13, button(player, FoStyle.THEME, Material.BOOK, "Game Types", List.of("&#ffffffEdit enabled games, chances, settings."), "manage game types"));
        inventory.setItem(15, button(player, FoStyle.THEME, Material.GRASS_BLOCK, "World Blacklist", List.of("&#ffffffClick worlds to allow/block."), "configure world blacklist"));
        player.openInventory(inventory);
    }

    private void openGeneral(Player player) {
        openGeneral(player, true);
    }

    private void openGeneral(Player player, boolean playOpenSound) {
        if (playOpenSound) {
            plugin.editorSounds().open(player);
        }
        EditorHolder holder = new EditorHolder(Page.GENERAL, null, -1);
        Inventory inventory = Bukkit.createInventory(holder, 36, TextUtil.color("&8ɢᴇɴᴇʀᴀʟ sᴇᴛᴛɪɴɢs"));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(10, toggle(player, "Plugin Enabled", plugin.configs().settings().enabled));
        inventory.setItem(11, toggle(player, "Auto Start", plugin.configs().settings().autoStart));
        inventory.setItem(12, button(player, FoStyle.THEME, Material.CLOCK, "Start Time", List.of("&#ffffffCurrent: &#03fc88" + plugin.configs().settings().startTimeSeconds, "&#ffffffClick to type seconds or range."), "edit start time"));
        inventory.setItem(13, button(player, FoStyle.THEME, Material.REPEATER, "Game Duration", List.of("&#ffffffCurrent: &#03fc88" + plugin.configs().settings().gameDurationSeconds + "s", "&#ffffffClick to type seconds."), "edit game duration"));
        inventory.setItem(14, button(player, FoStyle.THEME, Material.PLAYER_HEAD, "Minimum Players", List.of("&#ffffffCurrent: &#03fc88" + plugin.configs().settings().minimumPlayers, "&#ffffffClick to type amount."), "edit minimum players"));
        inventory.setItem(15, toggle(player, "Case Sensitive", plugin.configs().settings().caseSensitive));
        inventory.setItem(16, toggle(player, "Wrong Caps Notice", plugin.configs().settings().notifyWrongCapitalization));
        inventory.setItem(19, toggle(player, "Cancel Winning Chat", plugin.configs().settings().cancelWinningChat));
        inventory.setItem(20, button(player, FoStyle.THEME, Material.NAME_TAG, "Chat Priority", List.of("&#ffffffCurrent: &#03fc88" + plugin.configs().settings().chatListenerPriority.name(), "&#ffffffClick to cycle."), "cycle chat priority"));
        inventory.setItem(21, button(player, FoStyle.THEME, Material.EMERALD, "Reward Mode", List.of("&#ffffffCurrent: &#03fc88" + plugin.configs().settings().rewardChanceMode, "&#ffffffClick to cycle."), "cycle reward mode"));
        inventory.setItem(22, toggle(player, "Title Screens", plugin.configs().settings().titleBroadcasts || plugin.configs().settings().subtitleBroadcasts));
        inventory.setItem(31, GuiButtons.back(player));
        player.openInventory(inventory);
    }

    private void openGames(Player player) {
        openGames(player, true);
    }

    private void openGames(Player player, boolean playOpenSound) {
        if (playOpenSound) {
            plugin.editorSounds().open(player);
        }
        EditorHolder holder = new EditorHolder(Page.GAMES, null, -1);
        Inventory inventory = Bukkit.createInventory(holder, 36, TextUtil.color("&8ɢᴀᴍᴇ ᴛʏᴘᴇs"));
        holder.inventory = inventory;
        fill(inventory);
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int index = 0;
        for (GameType type : GameType.values()) {
            GameDefinition game = plugin.configs().game(type);
            Material material = game.enabled() ? Material.LIME_DYE : Material.RED_DYE;
            inventory.setItem(slots[index++], button(player, game.enabled() ? FoStyle.GOOD : FoStyle.BAD, material, game.displayName(), List.of(
                    "&#ffffffEnabled: " + state(game.enabled()),
                    "&#ffffffChance: &#03fc88" + game.chance(),
                    "&#ffffffClick to edit."
            ), "edit game settings"));
        }
        inventory.setItem(31, GuiButtons.back(player));
        player.openInventory(inventory);
    }

    private void openGame(Player player, GameType type) {
        openGame(player, type, true);
    }

    private void openGame(Player player, GameType type, boolean playOpenSound) {
        if (playOpenSound) {
            plugin.editorSounds().open(player);
        }
        GameDefinition game = plugin.configs().game(type);
        EditorHolder holder = new EditorHolder(Page.GAME_DETAIL, type, -1);
        Inventory inventory = Bukkit.createInventory(holder, 36, TextUtil.color("&8" + TextUtil.smallCaps(game.displayName())));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(10, toggle(player, "Enabled", game.enabled()));
        inventory.setItem(11, button(player, FoStyle.THEME, Material.GOLD_NUGGET, "Chance", List.of("&#ffffffCurrent: &#03fc88" + game.chance(), "&#ffffffClick to type weight."), "edit chance"));
        inventory.setItem(12, button(player, FoStyle.THEME, Material.PAPER, "Display Name", List.of("&#ffffffCurrent: &#03fc88" + game.displayName(), "&#ffffffClick to type name."), "edit display name"));
        inventory.setItem(13, button(player, FoStyle.THEME, Material.BOOK, "Word Lists", List.of("&#ffffffCurrent: &#03fc88" + String.join(", ", game.wordLists()), "&#ffffffClick to type comma list."), "edit word lists"));
        if (type == GameType.COMPLETION) {
            inventory.setItem(14, button(player, FoStyle.THEME, Material.SHEARS, "Missing Percent", List.of("&#ffffffCurrent: &#03fc88" + game.missingPercent() + "%", "&#ffffffClick to type percent."), "edit missing percent"));
        }
        if (type == GameType.GUESS_THE_NUMBER || type == GameType.MATH) {
            inventory.setItem(14, button(player, FoStyle.THEME, Material.COMPASS, "Range", List.of("&#ffffffCurrent: &#03fc88" + game.minimum() + "-" + game.maximum(), "&#ffffffClick to type min-max."), "edit range"));
        }
        if (type == GameType.TYPE || type == GameType.UNREVERSE) {
            inventory.setItem(15, toggle(player, "Randomized Text", game.randomizedText()));
        }
        if (type == GameType.UNSCRAMBLE) {
            inventory.setItem(15, toggle(player, "Smart Scramble", game.smartScramble()));
        }
        inventory.setItem(16, button(player, FoStyle.THEME, Material.CHEST, "Rewards", List.of("&#ffffffAdd, delete, and inspect rewards."), "manage rewards"));
        inventory.setItem(31, GuiButtons.back(player));
        player.openInventory(inventory);
    }

    private void openWorlds(Player player) {
        openWorlds(player, 0, "", true);
    }

    private void openWorlds(Player player, int page, String filter) {
        openWorlds(player, page, filter, true);
    }

    private void openWorlds(Player player, int page, String filter, boolean playOpenSound) {
        if (playOpenSound) {
            plugin.editorSounds().open(player);
        }
        TriStateSelectionMenus.open(player, worldSelectionRequest(page, filter));
    }

    private void openRewards(Player player, GameType type) {
        openRewards(player, type, true);
    }

    private void openRewards(Player player, GameType type, boolean playOpenSound) {
        if (playOpenSound) {
            plugin.editorSounds().open(player);
        }
        EditorHolder holder = new EditorHolder(Page.REWARDS, type, -1);
        Inventory inventory = Bukkit.createInventory(holder, 54, TextUtil.color("&8" + TextUtil.smallCaps("rewards " + type.key())));
        holder.inventory = inventory;
        fill(inventory);
        List<Map<String, Object>> rewards = plugin.configs().serializableRewards(type);
        for (int index = 0; index < Math.min(28, rewards.size()); index++) {
            Map<String, Object> reward = rewards.get(index);
            inventory.setItem(10 + index + (index / 7) * 2, button(player, FoStyle.THEME, Material.PAPER, "Reward #" + (index + 1), rewardLore(reward), "edit reward"));
        }
        inventory.setItem(45, button(player, FoStyle.GOOD, Material.COMMAND_BLOCK, "Add Console Command", List.of("&#ffffffClick to type command.", "&#ffffffUse %player% for winner."), "add console command"));
        inventory.setItem(46, button(player, FoStyle.GOOD, Material.PLAYER_HEAD, "Add Player Command", List.of("&#ffffffClick to type command.", "&#ffffffUse %player% for winner."), "add player command"));
        inventory.setItem(47, button(player, FoStyle.GOOD, Material.CHEST, "Add Held Item", List.of("&#ffffffHold item on cursor.", "&#ffffffClick this button."), "add held item reward"));
        inventory.setItem(49, GuiButtons.back(player));
        player.openInventory(inventory);
    }

    private void openDeleteConfirm(Player player, GameType type, int rewardIndex) {
        plugin.editorSounds().open(player);
        EditorHolder holder = new EditorHolder(Page.CONFIRM_REWARD_DELETE, type, rewardIndex);
        Inventory inventory = Bukkit.createInventory(holder, 27, TextUtil.color("&8" + TextUtil.smallCaps("confirm delete")));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(11, button(player, FoStyle.BAD, Material.LAVA_BUCKET, "Confirm Delete", List.of("&#ffffffDelete reward #" + (rewardIndex + 1) + "."), "confirm deletion"));
        inventory.setItem(22, GuiButtons.back(player));
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        InventoryHolder topHolder = event.getView().getTopInventory().getHolder();
        if (topHolder instanceof TriStateSelectionHolder worldHolder) {
            event.setCancelled(true);
            if (!player.hasPermission("fochatgames.admin")) {
                plugin.messages().send(player, "general.no-permission");
                player.closeInventory();
                return;
            }
            clickWorldSelector(player, event.getRawSlot(), worldHolder);
            return;
        }
        if (!(topHolder instanceof EditorHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("fochatgames.admin")) {
            plugin.messages().send(player, "general.no-permission");
            player.closeInventory();
            return;
        }
        int slot = event.getSlot();
        switch (holder.page) {
            case MAIN -> clickMain(player, slot);
            case GENERAL -> clickGeneral(player, slot);
            case GAMES -> clickGames(player, slot);
            case GAME_DETAIL -> clickGame(player, holder.game, slot);
            case REWARDS -> clickRewards(player, holder.game, slot, event.getCursor(), event.getClick());
            case CONFIRM_REWARD_DELETE -> clickConfirm(player, holder.game, holder.index, slot);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && plugin.core().inventoryCloseSuppressor().consumeSuppressedClose(player)) {
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrompt(AsyncPlayerChatEvent event) {
        EditorPrompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        plugin.log().debug("Editor prompt received from " + player.getName() + ": " + prompt.type() + " " + prompt.path());
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(player, prompt, message));
    }

    private void clickMain(Player player, int slot) {
        if (slot == 11) {
            openGeneral(player);
        } else if (slot == 13) {
            openGames(player);
        } else if (slot == 15) {
            openWorlds(player);
        }
    }

    private void clickGeneral(Player player, int slot) {
        switch (slot) {
            case 10 -> saveConfig(player, "general.enabled", !plugin.configs().settings().enabled, () -> openGeneral(player, false));
            case 11 -> saveConfig(player, "general.auto-start", !plugin.configs().settings().autoStart, () -> openGeneral(player, false));
            case 12 -> prompt(player, new EditorPrompt(PromptType.CONFIG_STRING, "general.start-time-seconds", null, "Start Time", plugin.configs().settings().startTimeSeconds, "seconds or range, example 180-300", Page.GENERAL));
            case 13 -> prompt(player, new EditorPrompt(PromptType.CONFIG_INT, "general.game-duration-seconds", null, "Game Duration", String.valueOf(plugin.configs().settings().gameDurationSeconds), "seconds, example 45", Page.GENERAL));
            case 14 -> prompt(player, new EditorPrompt(PromptType.CONFIG_INT, "general.minimum-players", null, "Minimum Players", String.valueOf(plugin.configs().settings().minimumPlayers), "player amount, example 3", Page.GENERAL));
            case 15 -> saveConfig(player, "general.case-sensitive", !plugin.configs().settings().caseSensitive, () -> openGeneral(player, false));
            case 16 -> saveConfig(player, "general.notify-wrong-capitalization", !plugin.configs().settings().notifyWrongCapitalization, () -> openGeneral(player, false));
            case 19 -> saveConfig(player, "general.cancel-winning-chat", !plugin.configs().settings().cancelWinningChat, () -> openGeneral(player, false));
            case 20 -> cyclePriority(player);
            case 21 -> cycleRewardMode(player);
            case 22 -> toggleTitleScreens(player);
            case 31 -> {
                plugin.editorSounds().back(player);
                openMain(player, false);
            }
            default -> {
            }
        }
    }

    private void clickGames(Player player, int slot) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int index = 0; index < slots.length; index++) {
            if (slot == slots[index]) {
                openGame(player, GameType.values()[index]);
                return;
            }
        }
        if (slot == 31) {
            plugin.editorSounds().back(player);
            openMain(player, false);
        }
    }

    private void clickGame(Player player, GameType type, int slot) {
        GameDefinition game = plugin.configs().game(type);
        switch (slot) {
            case 10 -> saveGame(player, type, "enabled", !game.enabled(), () -> openGame(player, type, false));
            case 11 -> prompt(player, new EditorPrompt(PromptType.GAME_INT, "chance", type, "Chance", String.valueOf(game.chance()), "chance weight, example 100", Page.GAME_DETAIL));
            case 12 -> prompt(player, new EditorPrompt(PromptType.GAME_STRING, "display-name", type, "Display Name", game.displayName(), "display name, example Quick Math", Page.GAME_DETAIL));
            case 13 -> prompt(player, new EditorPrompt(PromptType.GAME_LIST, "word-lists", type, "Word Lists", String.join(",", game.wordLists()), "comma list, example example,minecraft-items", Page.GAME_DETAIL));
            case 14 -> {
                if (type == GameType.COMPLETION) {
                    prompt(player, new EditorPrompt(PromptType.GAME_INT, "missing-percent", type, "Missing Percent", String.valueOf(game.missingPercent()), "percent 1-95", Page.GAME_DETAIL));
                } else if (type == GameType.GUESS_THE_NUMBER || type == GameType.MATH) {
                    prompt(player, new EditorPrompt(PromptType.GAME_RANGE, "range", type, "Range", game.minimum() + "-" + game.maximum(), "min-max, example 1-1000", Page.GAME_DETAIL));
                }
            }
            case 15 -> {
                if (type == GameType.TYPE || type == GameType.UNREVERSE) {
                    saveGame(player, type, "randomized-text", !game.randomizedText(), () -> openGame(player, type, false));
                } else if (type == GameType.UNSCRAMBLE) {
                    saveGame(player, type, "smart-scramble", !game.smartScramble(), () -> openGame(player, type, false));
                }
            }
            case 16 -> openRewards(player, type);
            case 31 -> {
                plugin.editorSounds().back(player);
                openGames(player, false);
            }
            default -> {
            }
        }
    }

    private void clickWorldSelector(Player player, int rawSlot, TriStateSelectionHolder holder) {
        TriStateSelectionClick click = TriStateSelectionMenus.handleClick(rawSlot, holder);
        if (click.action() == TriStateSelectionActionType.BACK) {
            plugin.editorSounds().back(player);
            openMain(player, false);
            return;
        }
        if (click.action() == TriStateSelectionActionType.SEARCH) {
            plugin.editorSounds().search(player);
            TextDialogRequest searchRequest = new TextDialogRequest(
                    "Search",
                    List.of("Search loaded and configured worlds."),
                    "Search",
                    click.filter(),
                    "world name",
                    DialogButton.search(),
                    DialogButton.cancel("Back"),
                    320,
                    300,
                    64,
                    true,
                    true,
                    false
            );
            openTextInputFromInventory(player, searchRequest,
                    input -> openWorlds(player, 0, input.trim(), false),
                    () -> openWorlds(player, click.page(), click.filter(), false));
            return;
        }
        if (click.action() == TriStateSelectionActionType.PREVIOUS_PAGE
                || click.action() == TriStateSelectionActionType.NEXT_PAGE
                || click.action() == TriStateSelectionActionType.CLEAR_SEARCH) {
            switch (click.action()) {
                case PREVIOUS_PAGE -> plugin.editorSounds().previousPage(player);
                case NEXT_PAGE -> plugin.editorSounds().nextPage(player);
                case CLEAR_SEARCH -> plugin.editorSounds().clearSearch(player);
                default -> { }
            }
            player.openInventory(TriStateSelectionMenus.createInventory(player, click.nextRequest()));
            return;
        }
        if (click.action() != TriStateSelectionActionType.TOGGLE) {
            return;
        }
        List<String> worlds = disabledWorlds(click.nextRequest());
        if (click.newState() == TriStateSelectionState.DISABLED) {
            plugin.messages().send(player, "editor.world-added", Map.of("world", click.displayName()));
        } else {
            plugin.messages().send(player, "editor.world-removed", Map.of("world", click.displayName()));
        }
        saveConfig(player, "world-blacklist", worlds, () -> openWorlds(player, click.page(), click.filter(), false),
                () -> plugin.editorSounds().toggle(player, click.newState() != TriStateSelectionState.DISABLED));
    }

    private void clickRewards(Player player, GameType type, int slot, ItemStack cursor, ClickType clickType) {
        if (slot == 49) {
            plugin.editorSounds().back(player);
            openGame(player, type, false);
            return;
        }
        if (slot == 45) {
            prompt(player, new EditorPrompt(PromptType.REWARD_CONSOLE, "rewards", type, "Console Command Reward", "", "console command without slash, %player% supported", Page.REWARDS));
            return;
        }
        if (slot == 46) {
            prompt(player, new EditorPrompt(PromptType.REWARD_PLAYER, "rewards", type, "Player Command Reward", "", "player command without slash, %player% supported", Page.REWARDS));
            return;
        }
        if (slot == 47) {
            addHeldReward(player, type, cursor);
            return;
        }
        int index = rewardIndexFromSlot(slot);
        List<Map<String, Object>> rewards = plugin.configs().serializableRewards(type);
        if (index >= 0 && index < rewards.size()) {
            if (clickType.isRightClick()) {
                openDeleteConfirm(player, type, index);
                return;
            }
            Object chance = rewards.get(index).getOrDefault("chance", 100);
            prompt(player, new EditorPrompt(PromptType.REWARD_CHANCE, "rewards", type, "Reward Chance", String.valueOf(chance), "chance percent/weight, example 100", Page.REWARDS, index));
        }
    }

    private void clickConfirm(Player player, GameType type, int index, int slot) {
        if (slot == 11) {
            List<Map<String, Object>> rewards = plugin.configs().serializableRewards(type);
            if (index >= 0 && index < rewards.size()) {
                rewards.remove(index);
                saveGame(player, type, "rewards", rewards, () -> {
                    openRewards(player, type, false);
                    plugin.messages().send(player, "editor.reward-deleted", Map.of("game", type.key()));
                }, () -> plugin.editorSounds().delete(player));
            }
        } else if (slot == 22) {
            plugin.editorSounds().back(player);
            openRewards(player, type, false);
        }
    }

    private int rewardIndexFromSlot(int slot) {
        for (int index = 0; index < 28; index++) {
            if (slot == 10 + index + (index / 7) * 2) {
                return index;
            }
        }
        return -1;
    }

    private void prompt(Player player, EditorPrompt prompt) {
        if (!nativeDialogSupport.canUseNativeDialogs(player)) {
            warnFallbackIfNeeded(player);
            openChatInput(player, prompt);
            return;
        }

        TextDialogRequest request = dialogRequest(prompt);
        plugin.core().inventoryCloseSuppressor().suppressNextClose(player);
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, () -> openNativePrompt(player, prompt, request));
    }

    private void openNativePrompt(Player player, EditorPrompt prompt, TextDialogRequest request) {
        if (!player.isOnline()) {
            clearSuppressedClose(player);
            return;
        }

        DialogService dialogService = DialogServiceFactory.create(plugin, nativeDialogSupport, fallbackService(prompt), plugin.core().scheduler());
        boolean openedNative = dialogService.openTextInput(
                player,
                request,
                message -> handlePrompt(player, prompt, message.trim()),
                () -> handleDialogCancel(player, prompt)
        );
        if (openedNative) {
            plugin.editorSounds().open(player);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> clearSuppressedClose(player), 5L);
            return;
        }

        clearSuppressedClose(player);
        warnFallbackIfNeeded(player);
    }

    private DialogService fallbackService(EditorPrompt prompt) {
        return new FallbackDialogService(
                nativeDialogSupport,
                (player, request, onClose) -> run(onClose),
                (player, request, onConfirm, onCancel) -> run(onCancel),
                (player, request, onSubmit, onCancel) -> openChatInput(player, prompt)
        );
    }

    private TextDialogRequest dialogRequest(EditorPrompt prompt) {
        boolean numberInput = prompt.numberInput();
        String current = safeText(prompt.currentValue());
        List<String> body = List.of(
                "Enter " + safeText(prompt.format()) + ". Current: " + current,
                "Short numbers like 1k, 5M, 2.5b, and 1T are supported."
        );
        if (numberInput) {
            return TextDialogRequest.number(body, current, current.isBlank() ? "Example: 1k" : current);
        }
        return new TextDialogRequest(
                "FoChatGames Editor",
                List.of(body.get(0)),
                "Value",
                current,
                current,
                DialogButton.confirm("Save", "", 150),
                DialogButton.cancel("Cancel", "", 150),
                320,
                300,
                256,
                true,
                true,
                false
        );
    }

    private void openChatInput(Player player, EditorPrompt prompt) {
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        plugin.messages().send(player, "editor.prompt", Map.of("format", prompt.format()));
    }

    private void clearSuppressedClose(Player player) {
        plugin.core().inventoryCloseSuppressor().clear(player);
    }

    private void handleDialogCancel(Player player, EditorPrompt prompt) {
        plugin.messages().send(player, "editor.cancelled");
        plugin.editorSounds().back(player);
        reopen(player, prompt.returnPage(), prompt.game());
    }

    private void run(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private void handlePrompt(Player player, EditorPrompt prompt, String message) {
        if (message.equalsIgnoreCase("cancel")) {
            plugin.messages().send(player, "editor.cancelled");
            plugin.editorSounds().back(player);
            reopen(player, prompt.returnPage(), prompt.game());
            return;
        }
        switch (prompt.type()) {
            case CONFIG_STRING -> saveConfig(player, prompt.path(), message, () -> reopen(player, prompt.returnPage(), prompt.game()));
            case CONFIG_INT -> {
                Long value = NumberUtil.parseLarge(message);
                if (value == null) {
                    invalid(player, "number expected", prompt);
                    return;
                }
                saveConfig(player, prompt.path(), toSafeInt(value), () -> reopen(player, prompt.returnPage(), prompt.game()));
            }
            case GAME_STRING -> saveGame(player, prompt.game(), prompt.path(), message, () -> reopen(player, prompt.returnPage(), prompt.game()));
            case GAME_INT -> {
                Long value = NumberUtil.parseLarge(message);
                if (value == null) {
                    invalid(player, "number expected", prompt);
                    return;
                }
                saveGame(player, prompt.game(), prompt.path(), toSafeInt(value), () -> reopen(player, prompt.returnPage(), prompt.game()));
            }
            case GAME_LIST -> saveGame(player, prompt.game(), prompt.path(), parseList(message), () -> reopen(player, prompt.returnPage(), prompt.game()));
            case GAME_RANGE -> saveRange(player, prompt.game(), message);
            case REWARD_CONSOLE -> addCommandReward(player, prompt.game(), "console_command", message);
            case REWARD_PLAYER -> addCommandReward(player, prompt.game(), "player_command", message);
            case REWARD_CHANCE -> saveRewardChance(player, prompt.game(), prompt.index(), message);
        }
    }

    private void invalid(Player player, String reason, EditorPrompt prompt) {
        plugin.editorSounds().error(player);
        plugin.messages().send(player, "editor.invalid", Map.of("reason", reason));
        reopen(player, prompt.returnPage(), prompt.game());
    }

    private void saveRange(Player player, GameType game, String message) {
        String[] split = message.split("-", 2);
        if (split.length != 2) {
            plugin.editorSounds().error(player);
            plugin.messages().send(player, "editor.invalid", Map.of("reason", "use min-max"));
            openGame(player, game, false);
            return;
        }
        Long min = NumberUtil.parseLarge(split[0]);
        Long max = NumberUtil.parseLarge(split[1]);
        if (min == null || max == null) {
            plugin.editorSounds().error(player);
            plugin.messages().send(player, "editor.invalid", Map.of("reason", "number expected"));
            openGame(player, game, false);
            return;
        }
        try {
            plugin.configs().saveGameValue(game, "minimum", toSafeInt(min));
            plugin.configs().saveGameValue(game, "maximum", toSafeInt(max));
            plugin.log().info("Editor saved range by " + player.getName() + ": " + game.key() + "=" + min + "-" + max);
            plugin.messages().send(player, "general.saved", Map.of("setting", game.key() + ".range"));
            plugin.editorSounds().save(player);
        } catch (IOException exception) {
            plugin.editorSounds().error(player);
            plugin.log().error("Failed to save range", exception);
            plugin.messages().send(player, "general.save-failed", Map.of("setting", game.key() + ".range"));
        }
        openGame(player, game, false);
    }

    private void addCommandReward(Player player, GameType game, String type, String command) {
        List<Map<String, Object>> rewards = plugin.configs().serializableRewards(game);
        Map<String, Object> reward = new HashMap<>();
        reward.put("type", type);
        reward.put("chance", 100);
        reward.put("command", command);
        rewards.add(reward);
        saveGame(player, game, "rewards", rewards, () -> {
            openRewards(player, game, false);
            plugin.messages().send(player, "editor.reward-added", Map.of("game", game.key()));
        }, () -> plugin.editorSounds().add(player));
    }

    private void addHeldReward(Player player, GameType game, ItemStack cursor) {
        var inputItem = CursorItemEditor.cloneItem(cursor);
        if (inputItem.isEmpty()) {
            plugin.editorSounds().addItemError(player);
            plugin.messages().send(player, "editor.invalid", Map.of("reason", "hold item on cursor"));
            openRewards(player, game, false);
            return;
        }
        ItemStack rewardItem = inputItem.get();
        List<Map<String, Object>> rewards = plugin.configs().serializableRewards(game);
        Map<String, Object> reward = new HashMap<>();
        reward.put("type", "item");
        reward.put("chance", 100);
        reward.put("material", rewardItem.getType().name());
        reward.put("amount", rewardItem.getAmount());
        reward.put("item", rewardItem);
        rewards.add(reward);
        saveGame(player, game, "rewards", rewards, () -> {
            openRewards(player, game, false);
            plugin.messages().send(player, "editor.reward-added", Map.of("game", game.key()));
        }, () -> plugin.editorSounds().addItem(player));
    }

    private void saveRewardChance(Player player, GameType game, int index, String message) {
        Double chance = parseChance(message);
        if (chance == null) {
            plugin.editorSounds().error(player);
            plugin.messages().send(player, "editor.invalid", Map.of("reason", "chance number expected"));
            openRewards(player, game, false);
            return;
        }
        List<Map<String, Object>> rewards = plugin.configs().serializableRewards(game);
        if (index < 0 || index >= rewards.size()) {
            plugin.editorSounds().error(player);
            plugin.messages().send(player, "editor.invalid", Map.of("reason", "reward not found"));
            openRewards(player, game, false);
            return;
        }
        rewards.get(index).put("chance", compactChance(chance));
        saveGame(player, game, "rewards", rewards, () -> openRewards(player, game, false));
    }

    private Double parseChance(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        OptionalDouble parsed = LargeNumberParser.parseDouble(input);
        if (parsed.isEmpty() || parsed.getAsDouble() < 0.0D) {
            return null;
        }
        return parsed.getAsDouble();
    }

    private Object compactChance(double chance) {
        long rounded = Math.round(chance);
        return Math.abs(chance - rounded) < 0.000001D ? rounded : chance;
    }

    private void cyclePriority(Player player) {
        List<String> options = List.of("LOWEST", "LOW", "NORMAL", "HIGH", "HIGHEST", "MONITOR");
        String current = plugin.configs().settings().chatListenerPriority.name();
        String next = options.get((options.indexOf(current) + 1) % options.size());
        saveConfig(player, "general.chat-listener-priority", next, () -> openGeneral(player, false), () -> plugin.editorSounds().cycle(player));
    }

    private void cycleRewardMode(Player player) {
        String current = plugin.configs().settings().rewardChanceMode;
        String next = current.equalsIgnoreCase("independent") ? "weighted_one" : "independent";
        saveConfig(player, "rewards.chance-mode", next, () -> openGeneral(player, false), () -> plugin.editorSounds().cycle(player));
    }

    private void toggleTitleScreens(Player player) {
        boolean enabled = plugin.configs().settings().titleBroadcasts || plugin.configs().settings().subtitleBroadcasts;
        boolean next = !enabled;
        try {
            plugin.configs().saveConfigValue("broadcasts.titles", next);
            plugin.configs().saveConfigValue("broadcasts.subtitles", next);
            plugin.games().reload();
            plugin.log().info("Editor saved config by " + player.getName() + ": broadcasts.title-screens=" + next);
            plugin.messages().send(player, "general.saved", Map.of("setting", "broadcasts.title-screens"));
            plugin.editorSounds().toggle(player, next);
            openGeneral(player, false);
        } catch (IOException exception) {
            plugin.editorSounds().error(player);
            plugin.log().error("Failed to save title screen broadcast toggle", exception);
            plugin.messages().send(player, "general.save-failed", Map.of("setting", "broadcasts.title-screens"));
        }
    }

    private List<String> parseList(String input) {
        List<String> list = new ArrayList<>();
        for (String part : input.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private void saveConfig(Player player, String path, Object value, Runnable after) {
        saveConfig(player, path, value, after, () -> playSavedSound(player, value));
    }

    private void saveConfig(Player player, String path, Object value, Runnable after, Runnable feedback) {
        try {
            plugin.configs().saveConfigValue(path, value);
            plugin.games().reload();
            plugin.log().info("Editor saved config by " + player.getName() + ": " + path + "=" + safeValue(value));
            plugin.messages().send(player, "general.saved", Map.of("setting", path));
            feedback.run();
            after.run();
        } catch (IOException exception) {
            plugin.editorSounds().error(player);
            plugin.log().error("Failed to save config path " + path, exception);
            plugin.messages().send(player, "general.save-failed", Map.of("setting", path));
        }
    }

    private void saveGame(Player player, GameType type, String path, Object value, Runnable after) {
        saveGame(player, type, path, value, after, () -> playSavedSound(player, value));
    }

    private void saveGame(Player player, GameType type, String path, Object value, Runnable after, Runnable feedback) {
        try {
            plugin.configs().saveGameValue(type, path, value);
            plugin.log().info("Editor saved game config by " + player.getName() + ": " + type.key() + "." + path + "=" + safeValue(value));
            plugin.messages().send(player, "general.saved", Map.of("setting", type.key() + "." + path));
            feedback.run();
            after.run();
        } catch (IOException exception) {
            plugin.editorSounds().error(player);
            plugin.log().error("Failed to save game path " + type.key() + "." + path, exception);
            plugin.messages().send(player, "general.save-failed", Map.of("setting", type.key() + "." + path));
        }
    }

    private void reopen(Player player, Page page, GameType game) {
        switch (page) {
            case GENERAL -> openGeneral(player, false);
            case GAME_DETAIL -> openGame(player, game, false);
            case REWARDS -> openRewards(player, game, false);
            case GAMES -> openGames(player, false);
            default -> openMain(player, false);
        }
    }

    private void playSavedSound(Player player, Object value) {
        if (value instanceof Boolean enabled) {
            plugin.editorSounds().toggle(player, enabled);
            return;
        }
        plugin.editorSounds().save(player);
    }

    private TriStateSelectionRequest worldSelectionRequest(int page, String filter) {
        Map<String, TriStateSelectionState> states = new HashMap<>();
        Set<String> blocked = new HashSet<>(plugin.configs().settings().worldBlacklist);
        var entries = WorldSelectionEntries.loadedAndConfigured(plugin, blocked);
        for (var entry : entries) {
            String key = entry.key().toLowerCase(Locale.ROOT);
            states.put(entry.key(), blocked.contains(key) ? TriStateSelectionState.DISABLED : TriStateSelectionState.ENABLED);
        }
        return TriStateSelectionRequest.builder()
                .worldSelection()
                .entries(entries)
                .states(states)
                .cycleOrder(List.of(TriStateSelectionState.ENABLED, TriStateSelectionState.DISABLED))
                .page(page)
                .filter(filter)
                .enabledLabel("Allowed")
                .disabledLabel("Blocked")
                .clickHint("Click to toggle blacklist.")
                .emptyTitle("No Worlds")
                .emptyLore(List.of("No worlds are loaded or configured."))
                .build();
    }

    private void openTextInputFromInventory(Player player,
                                            TextDialogRequest request,
                                            Consumer<String> onSubmit,
                                            Runnable onCancel) {
        EditorDialogInputs.openTextFromInventory(
                plugin,
                plugin.core().inventoryCloseSuppressor(),
                plugin.core().dialogService(),
                player,
                request,
                onSubmit,
                onCancel
        );
    }

    private List<String> disabledWorlds(TriStateSelectionRequest request) {
        List<String> worlds = new ArrayList<>();
        for (var entry : request.entries()) {
            if (request.state(entry.key()) == TriStateSelectionState.DISABLED) {
                worlds.add(entry.key().toLowerCase(Locale.ROOT));
            }
        }
        return worlds;
    }

    private void fill(Inventory inventory) {
        ItemStack filler = EditorItemFactory.filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack toggle(Player player, String name, boolean enabled) {
        return EditorItemFactory.toggle(player, name, enabled);
    }

    private ItemStack button(Player player, String color, Material material, String label, List<String> information, String action) {
        return EditorItemFactory.button(player, material, color, label, information, action);
    }

    private String state(boolean enabled) {
        return enabled ? "&#3ecf8eenabled" : "&#ff5d73disabled";
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        return EditorItemFactory.item(material, name, lore);
    }

    private List<String> rewardLore(Map<String, Object> reward) {
        String type = String.valueOf(reward.getOrDefault("type", "console_command")).toLowerCase(Locale.ROOT).replace('-', '_');
        List<String> lore = new ArrayList<>();
        lore.add("&#ffffffType: &#03fc88" + type);
        if (type.equals("console_command") || type.equals("player_command")) {
            lore.add("&#ffffffCommand: &#03fc88" + rewardCommand(reward));
        } else if (type.equals("item")) {
            lore.add("&#ffffffItem: &#03fc88" + rewardItemName(reward) + " x" + rewardAmount(reward));
        }
        lore.add("&#ffffffChance: &#03fc88" + reward.getOrDefault("chance", 100));
        lore.add("&#ffffffLeft click: &#03fc88edit chance");
        lore.add("&#ffffffRight click: &#ff5d73delete");
        return lore;
    }

    private String rewardCommand(Map<String, Object> reward) {
        Object command = reward.get("command");
        if (command == null || String.valueOf(command).isBlank()) {
            return "none";
        }
        return String.valueOf(command);
    }

    private String rewardItemName(Map<String, Object> reward) {
        ItemStack itemStack = rewardItemStack(reward);
        if (itemStack != null) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            return formatMaterialName(itemStack.getType());
        }
        Material material = Material.matchMaterial(String.valueOf(reward.getOrDefault("material", "EMERALD")));
        return formatMaterialName(material == null ? Material.EMERALD : material);
    }

    private int rewardAmount(Map<String, Object> reward) {
        Object amount = reward.get("amount");
        if (amount instanceof Number number) {
            return Math.max(1, number.intValue());
        }
        if (amount != null) {
            try {
                return Math.max(1, Integer.parseInt(String.valueOf(amount)));
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        ItemStack itemStack = rewardItemStack(reward);
        return itemStack == null ? 1 : Math.max(1, itemStack.getAmount());
    }

    private ItemStack rewardItemStack(Map<String, Object> reward) {
        Object item = reward.get("item");
        return item instanceof ItemStack itemStack ? itemStack : null;
    }

    private String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        List<String> formatted = new ArrayList<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
            }
        }
        return String.join(" ", formatted);
    }

    private int toSafeInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private String safeValue(Object value) {
        if (value instanceof ItemStack) {
            return "ItemStack";
        }
        String text = String.valueOf(value);
        return text.length() > 120 ? text.substring(0, 120) + "..." : text;
    }

    public enum Page {
        MAIN,
        GENERAL,
        GAMES,
        GAME_DETAIL,
        REWARDS,
        CONFIRM_REWARD_DELETE
    }

    public enum PromptType {
        CONFIG_STRING,
        CONFIG_INT,
        GAME_STRING,
        GAME_INT,
        GAME_LIST,
        GAME_RANGE,
        REWARD_CONSOLE,
        REWARD_PLAYER,
        REWARD_CHANCE
    }

    private static final class EditorHolder implements InventoryHolder {
        private final Page page;
        private final GameType game;
        private final int index;
        private Inventory inventory;

        private EditorHolder(Page page, GameType game, int index) {
            this.page = page;
            this.game = game;
            this.index = index;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public record EditorPrompt(PromptType type, String path, GameType game, String title, String currentValue, String format, Page returnPage, int index) {
        public EditorPrompt(PromptType type, String path, GameType game, String title, String currentValue, String format, Page returnPage) {
            this(type, path, game, title, currentValue, format, returnPage, -1);
        }

        public boolean numberInput() {
            return type == PromptType.CONFIG_INT || type == PromptType.GAME_INT || type == PromptType.REWARD_CHANCE;
        }
    }
}
