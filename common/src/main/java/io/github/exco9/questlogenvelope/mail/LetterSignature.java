package io.github.exco9.questlogenvelope.mail;

import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Decorative signature; carries no quest actions or executable commands. */
public record LetterSignature(String text, int color, int size, int x, int y, boolean framed, int magicColor) {
    public static final int DEFAULT_MAGIC_COLOR = 0xAA55FF;
    public LetterSignature(String text, int color, int size, int x, int y, boolean framed) {
        this(text, color, size, x, y, framed, DEFAULT_MAGIC_COLOR);
    }
    public static final int DEFAULT_COLOR = 0x7B593D;
    public static final int DEFAULT_SIZE = 12;
    public static final int MIN_SIZE = 8;
    public static final int MAX_SIZE = 28;
    public static final int MAX_TEXT_LENGTH = 80;
    public static final LetterSignature EMPTY = new LetterSignature("", DEFAULT_COLOR, DEFAULT_SIZE, 40, 110, false);
    public static final String PLAYER_TOKEN = "@s";
    private static final Pattern PLAYER_PLACEHOLDER = Pattern.compile("(?<![\\p{L}\\p{N}_])@s(?![\\p{L}\\p{N}_])");

    public LetterSignature {
        text = normalizeText(text);
        color &= 0xFFFFFF;
        magicColor &= 0xFFFFFF;
        size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));
        x = Math.max(0, Math.min(QuestMagicCircle.WRITABLE_WIDTH - 1, x));
        y = Math.max(0, Math.min(QuestMagicCircle.WRITABLE_HEIGHT - 1, y));
    }

    public boolean enabled() {
        return !text.isBlank();
    }

    /** Resolve once for the recipient, never for whoever subsequently reads the letter. */
    public LetterSignature resolvePlayer(String playerName) {
        return new LetterSignature(PLAYER_PLACEHOLDER.matcher(text).replaceAll(Matcher.quoteReplacement(playerName)),
                color, size, x, y, framed, magicColor);
    }

    private static String normalizeText(String text) {
        if (text == null) return "";
        // A signature is a single plain-text line, including in hand-authored JSON.
        String line = text.replaceAll("[\\p{Cntrl}\\u2028\\u2029]", " ").trim();
        int end = line.offsetByCodePoints(0, Math.min(MAX_TEXT_LENGTH, line.codePointCount(0, line.length())));
        return line.substring(0, end);
    }

    public static LetterSignature fromJson(JsonObject json) {
        String text = string(json, "signature", "");
        int color = DEFAULT_COLOR;
        try {
            if (json.has("signature_color")) {
                var value = json.get("signature_color").getAsJsonPrimitive();
                if (value.isNumber()) color = value.getAsInt();
                else {
                    String hex = value.getAsString().trim().replaceFirst("^(#|0[xX])", "");
                    if (hex.matches("[0-9a-fA-F]{6}")) color = Integer.parseInt(hex, 16);
                }
            }
        } catch (RuntimeException ignored) { }
        return new LetterSignature(text, color,
                integer(json, "signature_size", DEFAULT_SIZE),
                integer(json, "signature_x", EMPTY.x()),
                integer(json, "signature_y", EMPTY.y()),
                bool(json, "signature_frame"), jsonColor(json, "signature_magic_color", DEFAULT_MAGIC_COLOR));
    }

    public void writeJson(JsonObject json) {
        for (String key : new String[]{"signature", "signature_color", "signature_size",
                "signature_x", "signature_y", "signature_frame", "signature_magic_color"}) json.remove(key);
        if (!enabled()) return;
        json.addProperty("signature", text);
        json.addProperty("signature_color", String.format("#%06X", color));
        json.addProperty("signature_size", size);
        json.addProperty("signature_x", x);
        json.addProperty("signature_y", y);
        json.addProperty("signature_frame", framed);
        json.addProperty("signature_magic_color", String.format("#%06X", magicColor));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("text", text);
        tag.putInt("color", color);
        tag.putInt("magic_color", magicColor);
        tag.putInt("size", size);
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putBoolean("frame", framed);
        return tag;
    }

    public static LetterSignature fromTag(CompoundTag tag) {
        return new LetterSignature(tag.getString("text"),
                tag.contains("color", Tag.TAG_INT) ? tag.getInt("color") : DEFAULT_COLOR,
                tag.contains("size", Tag.TAG_INT) ? tag.getInt("size") : DEFAULT_SIZE,
                tag.contains("x", Tag.TAG_INT) ? tag.getInt("x") : EMPTY.x(),
                tag.contains("y", Tag.TAG_INT) ? tag.getInt("y") : EMPTY.y(),
                tag.getBoolean("frame"), tag.contains("magic_color", Tag.TAG_INT) ? tag.getInt("magic_color") : DEFAULT_MAGIC_COLOR);
    }

    public void attach(ItemStack stack) {
        if (!enabled()) return;
        CompoundTag custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag addon = custom.getCompound("questlog_envelope");
        addon.put("signature", toTag());
        custom.put("questlog_envelope", addon);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
    }

    public static LetterSignature read(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return EMPTY;
        return fromTag(custom.copyTag().getCompound("questlog_envelope").getCompound("signature"));
    }

    public static boolean isSigned(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null && custom.copyTag().getCompound("questlog_envelope")
                .getCompound("signature").getBoolean("signed");
    }

    /** Changes only the signing flag, retaining circle actions and all other item metadata. */
    public static boolean sign(ItemStack stack, LetterSignature expected) {
        if (!expected.enabled() || !read(stack).equals(expected) || isSigned(stack)) return false;
        CompoundTag custom = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        CompoundTag addon = custom.getCompound("questlog_envelope");
        CompoundTag signature = addon.getCompound("signature");
        signature.putBoolean("signed", true);
        addon.put("signature", signature);
        custom.put("questlog_envelope", addon);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return true;
    }

    private static int jsonColor(JsonObject json, String key, int fallback) {
        try {
            if (!json.has(key)) return fallback;
            var value = json.get(key).getAsJsonPrimitive();
            if (value.isNumber()) return value.getAsInt();
            String hex = value.getAsString().trim().replaceFirst("^(#|0[xX])", "");
            return hex.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(hex, 16) : fallback;
        } catch (RuntimeException ignored) { return fallback; }
    }

    private static String string(JsonObject json, String key, String fallback) {
        try { return json.has(key) ? json.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int integer(JsonObject json, String key, int fallback) {
        try { return json.has(key) ? json.get(key).getAsInt() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static boolean bool(JsonObject json, String key) {
        try { return json.has(key) && json.get(key).getAsBoolean(); }
        catch (RuntimeException ignored) { return false; }
    }
}
