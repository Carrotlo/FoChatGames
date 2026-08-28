package me.foesio.foChatGames.message;

import me.foesio.core.update.UpdateNoticeService;
import me.foesio.foChatGames.FoChatGames;
import me.foesio.foChatGames.util.TextUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MessageService {
    private final FoChatGames plugin;

    public MessageService(FoChatGames plugin) {
        this.plugin = plugin;
    }

    public String prefix() {
        return TextUtil.color(TextUtil.tokens(token("prefix"), colorTokens()));
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, Map.of());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        send(sender, path, path, placeholders);
    }

    public void send(CommandSender sender, String path, String fallback, Map<String, String> placeholders) {
        String body = plugin.configs().messages().getString(path, fallback);
        sender.sendMessage(formatWithPrefix(body, placeholders));
    }

    public void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        List<String> lines = plugin.configs().messages().getStringList(path);
        for (String line : lines) {
            sender.sendMessage(formatWithPrefix(line, placeholders));
        }
    }

    public void sendFixed(CommandSender sender, String template, Map<String, String> placeholders) {
        sender.sendMessage(formatWithPrefix(template, placeholders));
    }

    public void sendClickableFixed(CommandSender sender, String template, String url, Map<String, String> placeholders) {
        String body = formatWithPrefix(template, placeholders);
        if (!(sender instanceof Player player) || url == null || url.isBlank()) {
            sender.sendMessage(body);
            return;
        }
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(body));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(TextUtil.color(token("muted") + "Open Modrinth page"))));
        player.spigot().sendMessage(component);
    }

    public UpdateNoticeService.UpdateMessenger updateMessenger() {
        return new UpdateNoticeService.UpdateMessenger() {
            @Override
            public void send(CommandSender sender, String template, Map<String, String> placeholders) {
                MessageService.this.sendFixed(sender, template, placeholders);
            }

            @Override
            public void sendClickable(CommandSender sender, String template, String url, Map<String, String> placeholders) {
                MessageService.this.sendClickableFixed(sender, template, url, placeholders);
            }
        };
    }

    public void broadcastGame(String chatPath, String hoverPath, Map<String, String> placeholders) {
        String chat = formatWithPrefix(messagePathText(chatPath), placeholders);
        String hover = formatNoPrefix(messagePathText(hoverPath), placeholders);
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(chat));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.games().canSeeGames(player)) {
                continue;
            }
            player.spigot().sendMessage(component);
        }
    }

    public void title(Player player, String title, String subtitle) {
        player.sendTitle(TextUtil.color(title), TextUtil.color(subtitle), 10, 45, 10);
    }

    public void actionbar(Player player, String text) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(TextUtil.color(text)));
    }

    public void playConfiguredSound(Player player, String sectionPath) {
        if (!plugin.configs().settings().soundBroadcasts) {
            return;
        }
        plugin.sounds().play(player, sectionPath);
    }

    private String formatWithPrefix(String input, Map<String, String> placeholders) {
        String body = formatNoPrefix(input, placeholders);
        return input != null && input.contains("{prefix}") ? body : prefix() + body;
    }

    public String formatNoPrefix(String input, Map<String, String> placeholders) {
        Map<String, String> merged = new HashMap<>(colorTokens());
        merged.putAll(placeholders);
        merged.put("prefix", prefix());
        return TextUtil.color(TextUtil.tokens(input, merged));
    }

    public String formatPathNoPrefix(String path, Map<String, String> placeholders) {
        return formatNoPrefix(messagePathText(path), placeholders);
    }

    public String messagePathText(String path) {
        return plugin.configs().messages().getString(path, path);
    }

    private Map<String, String> colorTokens() {
        Map<String, String> tokens = new HashMap<>();
        tokens.put("theme", token("theme"));
        tokens.put("muted", token("muted"));
        tokens.put("white", token("white"));
        tokens.put("good", token("good"));
        tokens.put("bad", token("bad"));
        return tokens;
    }

    private String token(String key) {
        return plugin.configs().messages().getString("tokens." + key, switch (key) {
            case "theme" -> "#03fc88";
            case "muted" -> "#a7b8b0";
            case "white" -> "#ffffff";
            case "good" -> "#3ecf8e";
            case "bad" -> "#ff5d73";
            case "prefix" -> "{theme}FoChatGames &8» {muted}";
            default -> "#ffffff";
        });
    }
}
