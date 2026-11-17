package com.loohp.interactivechatdiscordsrvaddon.utils;

import java.util.regex.Pattern;
import java.util.function.Function;
import com.loohp.interactivechat.libs.net.kyori.adventure.key.Key;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.minimessage.MiniMessage;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class DiscordItemNamePresentation {
    private static final Pattern TEXT = Pattern.compile("(?s).+");
    private static final Pattern SUPPORTED = Pattern.compile("(?i)(?<!\\\\)</?(?:#[0-9a-f]{6}|black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white|color|colour|gradient|rainbow|bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf|reset)(?::[^<>]*)?>");
    private static final Pattern GLYPH = Pattern.compile("(?<!\\\\)(:[A-Za-z0-9_.-]+:|<(?:glyph|g):[A-Za-z0-9_.-]+>)");
    private static final Pattern LEGACY_FORMAT = Pattern.compile("(?i)\u00a7[0-9a-fk-orx]");
    private static final MiniMessage FORMAT = MiniMessage.builder().tags(TagResolver.resolver(
            StandardTags.color(), StandardTags.decorations(), StandardTags.gradient(), StandardTags.rainbow(), StandardTags.reset())).build();

    private DiscordItemNamePresentation() {}

    public static Component format(Component input) {
        if (input == null) return null;
        return input.replaceText(builder -> builder.match(TEXT).replacement((match, original) -> {
            String text = match.group();
            if (text.length() > 4096 || text.contains("\\<") || !SUPPORTED.matcher(text).find()) return Component.text(text);
            try { return FORMAT.deserialize(text); }
            catch (RuntimeException ex) { return Component.text(text); }
        }));
    }
    public static String plain(Component input) { return PlainTextComponentSerializer.plainText().serialize(format(input)); }
    public static String formatPlainDeathMessage(String input) {
        if (input == null) return null;
        // Discord strips legacy formatting anyway; MiniMessage rejects it if left in its input.
        return plain(Component.text(LEGACY_FORMAT.matcher(input).replaceAll("")));
    }
    public static String removeLegacyFormatting(String input) {
        return input == null ? null : LEGACY_FORMAT.matcher(input).replaceAll("");
    }
    public static Component formatForImage(Component input) {
        return formatGlyphs(format(input), DiscordItemNamePresentation::nexoGlyph);
    }
    public static Component formatGlyphs(Component input, Function<String, Component> resolver) {
        if (input == null) return null;
        return input.replaceText(builder -> builder.match(GLYPH).replacement((match, original) -> {
            Component resolved = resolver.apply(match.group());
            return resolved == null ? Component.text(match.group()) : resolved;
        }));
    }
    private static Component nexoGlyph(String token) {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object manager = bukkit.getMethod("getPluginManager").invoke(null);
            Object plugin = manager.getClass().getMethod("getPlugin", String.class).invoke(manager, "Nexo");
            if (plugin == null) return null;
            Object fonts = plugin.getClass().getMethod("fontManager").invoke(plugin);
            Object glyph;
            if (token.charAt(0) == ':') {
                glyph = fonts.getClass().getMethod("glyphFromPlaceholder", String.class).invoke(fonts, token);
            } else {
                String id = token.substring(token.indexOf(':') + 1, token.length() - 1);
                glyph = fonts.getClass().getMethod("glyphFromID", String.class).invoke(fonts, id);
            }
            if (glyph == null) return null;
            char[] chars = (char[]) glyph.getClass().getMethod("getChars").invoke(glyph);
            Object font = glyph.getClass().getMethod("getFont").invoke(glyph);
            return Component.text(new String(chars)).font(Key.key(font.toString()));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            return null;
        }
    }
    public static String replaceWeaponName(String message, Component name) {
        if (message == null || name == null) return message;
        String original = PlainTextComponentSerializer.plainText().serialize(name);
        String formatted = plain(name);
        if (original.isEmpty() || original.equals(formatted)) return message;
        String bracketed = "[" + original + "]";
        int index = message.lastIndexOf(bracketed);
        return index < 0 ? message : message.substring(0, index) + "[" + formatted + "]" + message.substring(index + bracketed.length());
    }
}
