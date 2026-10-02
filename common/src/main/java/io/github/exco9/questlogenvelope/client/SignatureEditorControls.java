package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Contextual inspector for the signature placed on the paper. */
final class SignatureEditorControls {
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private boolean enabled;
    private boolean framed;
    private int x;
    private int y;
    private String text;
    private String color;
    private String magicColor;
    private String command;
    private EditBox commandBox;
    private EditBox magicColorBox;
    private String size;
    private EditBox textBox;
    private EditBox colorBox;
    private EditBox sizeBox;
    private Button frameButton;
    private Button resetButton;
    private boolean dragging;
    private boolean resizing;
    private double grabX;
    private double grabY;
    private double resizeStartY;
    private int resizeStartSize;
    private int panelX;
    private int panelY;

    SignatureEditorControls(JsonObject json) {
        LetterSignature initial = LetterSignature.fromJson(json);
        enabled = initial.enabled();
        framed = initial.framed();
        x = initial.x();
        y = initial.y();
        text = initial.text();
        color = String.format("#%06X", initial.color());
        size = Integer.toString(initial.size());
        magicColor = String.format("#%06X", initial.magicColor());
        try { command = json.has("signature_command") ? json.get("signature_command").getAsString() : ""; }
        catch (RuntimeException ignored) { command = ""; }
    }

    void rebuild(Font font, int panelX, int panelY, int panelWidth, Consumer<AbstractWidget> add, Runnable deselect, Runnable changed) {
        // Capture raw drafts, including invalid input, before a GUI-scale/window resize.
        if (textBox != null) {
            text = textBox.getValue();
            color = colorBox.getValue();
            magicColor = magicColorBox.getValue();
            size = sizeBox.getValue();
            command = commandBox.getValue();
        }
        this.panelX = panelX;
        this.panelY = panelY;
        dragging = resizing = false;
        widgets.clear();
        textBox = new EditBox(font, panelX, panelY + 124, panelWidth, 18, tr("text"));
        textBox.setMaxLength(LetterSignature.MAX_TEXT_LENGTH);
        textBox.setValue(text);
        textBox.setHint(tr("text_hint"));
        textBox.setResponder(value -> changed.run());
        add(textBox, add);
        colorBox = new EditBox(font, panelX, panelY + 158, 76, 18, tr("color"));
        colorBox.setMaxLength(8);
        colorBox.setValue(color);
        colorBox.setTooltip(Tooltip.create(tr("color")));
        colorBox.setResponder(value -> changed.run());
        add(colorBox, add);
        magicColorBox = new EditBox(font, panelX + 82, panelY + 158, 76, 18, tr("magic_color"));
        magicColorBox.setMaxLength(8);
        magicColorBox.setValue(magicColor);
        magicColorBox.setTooltip(Tooltip.create(tr("magic_color_hint")));
        magicColorBox.setResponder(value -> changed.run());
        add(magicColorBox, add);
        sizeBox = new EditBox(font, panelX + 164, panelY + 158, panelWidth - 164, 18, tr("size"));
        sizeBox.setMaxLength(3);
        sizeBox.setValue(size);
        sizeBox.setTooltip(Tooltip.create(tr("size")));
        sizeBox.setResponder(value -> changed.run());
        add(sizeBox, add);
        frameButton = Button.builder(label("frame", framed), button -> {
            framed = !framed;
            button.setMessage(label("frame", framed));
            changed.run();
        }).bounds(panelX, panelY + 180, 102, 18).build();
        add(frameButton, add);
        add(Button.builder(tr("player"), button -> textBox.setValue(LetterSignature.PLAYER_TOKEN))
                .bounds(panelX + 108, panelY + 180, 102, 18).build(), add);
        resetButton = Button.builder(tr("reset"), button -> {
            x = LetterSignature.EMPTY.x();
            y = LetterSignature.EMPTY.y();
            sizeBox.setValue(Integer.toString(LetterSignature.DEFAULT_SIZE));
        }).bounds(panelX, panelY + 202, 102, 18).build();
        add(resetButton, add);
        add(Button.builder(tr("remove"), button -> {
            enabled = false;
            dragging = resizing = false;
            changed.run();
            deselect.run();
        }).bounds(panelX + 108, panelY + 202, 102, 18).build(), add);
        commandBox = new EditBox(font, panelX, panelY + 236, panelWidth, 18, tr("command"));
        commandBox.setMaxLength(512);
        commandBox.setValue(command);
        commandBox.setHint(tr("command_hint"));
        commandBox.setTooltip(Tooltip.create(tr("command_help")));
        commandBox.setResponder(value -> changed.run());
        add(commandBox, add);
        updateActive();
    }

    private void add(AbstractWidget widget, Consumer<AbstractWidget> add) {
        widgets.add(widget);
        add.accept(widget);
    }

    void setVisible(boolean visible) {
        for (AbstractWidget widget : widgets) widget.visible = visible;
        if (!visible) dragging = resizing = false;
    }

    private void updateActive() {
        textBox.active = colorBox.active = magicColorBox.active = sizeBox.active = commandBox.active = frameButton.active = resetButton.active = enabled;
    }

    private static Component tr(String suffix) {
        return Component.translatable("questlog_envelope.editor.signature." + suffix);
    }

    private static Component label(String suffix, boolean value) {
        return Component.translatable("questlog_envelope.editor.signature." + suffix,
                Component.translatable(value ? "options.on" : "options.off"));
    }

    @Nullable Component validate() {
        if (!enabled) return null;
        if (textBox.getValue().isBlank()) return tr("invalid_text");
        if (parseColor() == null || parseColor(magicColorBox) == null) return tr("invalid_color");
        int requested = parseSize();
        if (requested < LetterSignature.MIN_SIZE || requested > LetterSignature.MAX_SIZE) {
            return Component.translatable("questlog_envelope.editor.signature.invalid_size",
                    LetterSignature.MIN_SIZE, LetterSignature.MAX_SIZE);
        }
        return null;
    }

    private @Nullable Integer parseColor() {
        return parseColor(colorBox);
    }

    private @Nullable Integer parseColor(EditBox box) {
        String hex = box.getValue().trim().replaceFirst("^(#|0[xX])", "");
        if (!hex.matches("[0-9a-fA-F]{6}")) return null;
        try { return Integer.parseInt(hex, 16); }
        catch (NumberFormatException ignored) { return null; }
    }

    private int parseSize() {
        try { return Integer.parseInt(sizeBox.getValue().trim()); }
        catch (NumberFormatException ignored) { return -1; }
    }

    boolean isEnabled() {
        return enabled;
    }

    void addSignature() {
        enabled = true;
        if (textBox.getValue().isBlank()) textBox.setValue(LetterSignature.PLAYER_TOKEN);
        updateActive();
    }

    private LetterSignature draft() {
        Integer rgb = parseColor();
        return new LetterSignature(enabled ? textBox.getValue() : "", rgb == null ? LetterSignature.DEFAULT_COLOR : rgb,
                parseSize() < 0 ? LetterSignature.DEFAULT_SIZE : parseSize(), x, y, framed, parseColor(magicColorBox) == null ? LetterSignature.DEFAULT_MAGIC_COLOR : parseColor(magicColorBox));
    }

    private LetterSignature preview() {
        var player = Minecraft.getInstance().player;
        return draft().resolvePlayer(player == null ? "Player" : player.getGameProfile().getName());
    }

    void save(JsonObject json, Font font) {
        SignatureRenderer.Bounds box = SignatureRenderer.bounds(font, preview(), 0, 0);
        LetterSignature signature = draft();
        new LetterSignature(signature.text(), signature.color(), signature.size(), box.x(), box.y(), framed, signature.magicColor()).writeJson(json);
        json.remove("signature_command");
        if (enabled && !commandBox.getValue().isBlank()) json.addProperty("signature_command", commandBox.getValue().trim());
    }

    void render(GuiGraphics graphics, Font font, int paperX, int paperY, boolean selected) {
        LetterSignature signature = preview();
        SignatureRenderer.Bounds box = SignatureRenderer.render(graphics, font, signature, paperX, paperY);
        if (!selected) return;
        graphics.drawString(font, tr("text"), panelX, panelY + 111, 0xFFFFFF, false);
        graphics.drawString(font, tr("color_short"), panelX, panelY + 146, 0xFFFFFF, false);
        graphics.drawString(font, tr("magic_color"), panelX + 82, panelY + 146, 0xFFFFFF, false);
        graphics.drawString(font, tr("size_short"), panelX + 164, panelY + 146, 0xFFFFFF, false);
        graphics.drawString(font, tr("command"), panelX, panelY + 224, 0xFFFFFF, false);
        if (signature.enabled()) {
            graphics.renderOutline(box.x() - 1, box.y() - 1, box.width() + 2, box.height() + 2, 0xA0FFFFFF);
            graphics.fill(box.x() + box.width() - 4, box.y() + box.height() - 4,
                    box.x() + box.width() + 4, box.y() + box.height() + 4, 0xFFFFD86A);
        }
    }

    boolean mouseClicked(Font font, int paperX, int paperY, double mouseX, double mouseY, int button) {
        if (button != 0 || !preview().enabled()) return false;
        SignatureRenderer.Bounds box = SignatureRenderer.bounds(font, preview(), paperX, paperY);
        if (Math.abs(mouseX - box.x() - box.width()) <= 5 && Math.abs(mouseY - box.y() - box.height()) <= 5) {
            resizing = true;
            resizeStartY = mouseY;
            resizeStartSize = preview().size();
            return true;
        }
        if (!box.contains(mouseX, mouseY)) return false;
        dragging = true;
        grabX = mouseX - box.x();
        grabY = mouseY - box.y();
        return true;
    }

    boolean mouseDragged(Font font, int paperX, int paperY, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (resizing) {
            int requested = resizeStartSize + (int) Math.round((mouseY - resizeStartY) / 2);
            sizeBox.setValue(Integer.toString(Math.max(LetterSignature.MIN_SIZE, Math.min(LetterSignature.MAX_SIZE, requested))));
            return true;
        }
        if (!dragging) return false;
        int requestedX = (int) Math.round(mouseX - paperX - grabX);
        int requestedY = (int) Math.round(mouseY - paperY - grabY);
        if (Screen.hasShiftDown()) {
            requestedX = Math.round(requestedX / 4.0F) * 4;
            requestedY = Math.round(requestedY / 4.0F) * 4;
        }
        SignatureRenderer.Bounds box = SignatureRenderer.bounds(font, preview(), 0, 0);
        x = Math.max(0, Math.min(QuestMagicCircle.WRITABLE_WIDTH - box.width(), requestedX));
        y = Math.max(0, Math.min(QuestMagicCircle.WRITABLE_HEIGHT - box.height(), requestedY));
        return true;
    }

    boolean mouseReleased(int button) {
        if (button != 0 || (!dragging && !resizing)) return false;
        dragging = resizing = false;
        return true;
    }
}
