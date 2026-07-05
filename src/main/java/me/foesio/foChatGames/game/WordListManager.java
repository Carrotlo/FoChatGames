package me.foesio.foChatGames.game;

import me.foesio.foChatGames.FoChatGames;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class WordListManager {
    private final FoChatGames plugin;
    private final Map<String, List<String>> lists = new HashMap<>();

    public WordListManager(FoChatGames plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ensureBuiltIns();
        lists.clear();
        File folder = new File(plugin.getDataFolder(), "word-lists");
        File[] files = folder.listFiles((directory, name) -> name.toLowerCase(Locale.ROOT).endsWith(".txt"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                List<String> words = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).stream()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .toList();
                String name = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                lists.put(name, words);
            } catch (IOException exception) {
                plugin.log().error("Failed to read word list " + file.getName(), exception);
            }
        }
    }

    public List<String> words(List<String> listNames) {
        Set<String> merged = new LinkedHashSet<>();
        for (String listName : listNames) {
            List<String> words = lists.get(listName.toLowerCase(Locale.ROOT));
            if (words != null) {
                merged.addAll(words);
            }
        }
        if (merged.isEmpty()) {
            merged.addAll(lists.getOrDefault("example", List.of("diamond", "emerald", "creeper")));
        }
        return new ArrayList<>(merged);
    }

    private void ensureBuiltIns() {
        File folder = new File(plugin.getDataFolder(), "word-lists");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.log().warning("Could not create word-lists folder.");
            return;
        }
        writeIfMissing(new File(folder, "minecraft-blocks.txt"), this::blockWords);
        writeIfMissing(new File(folder, "minecraft-items.txt"), this::itemWords);
        writeIfMissing(new File(folder, "minecraft-entities.txt"), this::entityWords);
    }

    private void writeIfMissing(File file, Supplier<List<String>> wordSupplier) {
        if (file.exists()) {
            return;
        }
        try {
            Files.write(file.toPath(), wordSupplier.get(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.log().error("Failed to write built-in word list " + file.getName(), exception);
        }
    }

    private List<String> blockWords() {
        List<String> words = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isBlock() && !material.isAir()) {
                words.add(readable(material.name()));
            }
        }
        return words;
    }

    private List<String> itemWords() {
        List<String> words = new ArrayList<>();
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                words.add(readable(material.name()));
            }
        }
        return words;
    }

    private List<String> entityWords() {
        List<String> words = new ArrayList<>();
        for (EntityType entityType : EntityType.values()) {
            if (entityType.isAlive()) {
                words.add(readable(entityType.name()));
            }
        }
        return words;
    }

    private String readable(String key) {
        return key.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
