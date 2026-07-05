package me.foesio.foChatGames.game;

import me.foesio.core.item.FoItemStacks;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.function.Consumer;

public final class Reward {
    public enum Type {
        CONSOLE_COMMAND,
        PLAYER_COMMAND,
        ITEM
    }

    private final Type type;
    private final double chance;
    private final String command;
    private final ItemStack itemStack;

    public Reward(Type type, double chance, String command, ItemStack itemStack) {
        this.type = type;
        this.chance = Math.max(0.0D, chance);
        this.command = command == null ? "" : command;
        this.itemStack = itemStack == null ? new ItemStack(Material.AIR) : FoItemStacks.cloneItem(itemStack);
    }

    public Type type() {
        return type;
    }

    public double chance() {
        return chance;
    }

    public String command() {
        return command;
    }

    public ItemStack itemStack() {
        return FoItemStacks.cloneItem(itemStack);
    }

    public static Reward from(ConfigurationSection section, Consumer<String> warningSink, String context) {
        String configuredType = section.getString("type", "console_command");
        String rawType = (configuredType == null || configuredType.isBlank() ? "console_command" : configuredType)
                .toUpperCase(Locale.ROOT)
                .replace('-', '_');
        Type type;
        try {
            type = Type.valueOf(rawType);
        } catch (IllegalArgumentException exception) {
            warn(warningSink, "Invalid reward type at " + context + ": " + configuredType + ". Using console_command.");
            type = Type.CONSOLE_COMMAND;
        }

        String configuredMaterial = section.getString("material", "EMERALD");
        String rawMaterial = configuredMaterial == null || configuredMaterial.isBlank() ? "EMERALD" : configuredMaterial;
        Material material = Material.matchMaterial(rawMaterial.trim());
        if (material == null) {
            warn(warningSink, "Invalid reward material at " + context + ": " + configuredMaterial + ". Using EMERALD.");
            material = Material.EMERALD;
        }

        ItemStack itemStack = itemStack(section, warningSink, context);
        if (itemStack == null) {
            itemStack = new ItemStack(material, Math.max(1, section.getInt("amount", 1)));
        }
        return new Reward(
                type,
                section.getDouble("chance", 100.0D),
                section.getString("command", ""),
                itemStack
        );
    }

    private static ItemStack itemStack(ConfigurationSection section, Consumer<String> warningSink, String context) {
        try {
            return section.getItemStack("item");
        } catch (RuntimeException | LinkageError exception) {
            warn(warningSink, "Serialized reward item at " + context + " could not be loaded on this server. Using material fallback.");
            return null;
        }
    }

    private static void warn(Consumer<String> warningSink, String message) {
        if (warningSink != null) {
            warningSink.accept(message);
        }
    }
}
