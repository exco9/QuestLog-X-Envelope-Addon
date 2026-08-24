package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.client.gui.screen.LetterEditScreen;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Questlog reward editor that reuses Envelope's own letter paper, text box,
 * formatting toolbar and seal renderer.
 */
public final class LetterRewardEditorScreen extends Screen {
    private final Screen parent;
    private final JsonObject rewardEntry;
    private final SealSelection sealSelection;

    private EditBox senderBox;
    private EditBox letterTitleBox;
    private TextBox textBox;
    private boolean autoClaim;
    private String fontSize;

    private int letterLeft;
    private int letterTop;
    private int panelX;
    private int panelY;
    private int panelWidth;

    @Nullable
    private Component validationError;

    public LetterRewardEditorScreen(Screen parent, JsonObject rewardEntry) {
        super(Component.translatable("questlog_envelope.editor.letter.title"));
        this.parent = parent;
        this.rewardEntry = rewardEntry;
        this.autoClaim = rewardEntry.has("auto_claim") && rewardEntry.get("auto_claim").getAsBoolean();
        this.fontSize = QuestMailMarker.normalizeFontSize(
                rewardEntry.has("font_size") ? rewardEntry.get("font_size").getAsString() : "normal"
        );
        this.sealSelection = new SealSelection(
                rewardEntry.has("seal") ? rewardEntry.get("seal").getAsString() : null
        );
    }

    @Override
    protected void init() {
        int letterWidth = 176;
        int letterHeight = 192;
        int panelHeight = 232;
        this.panelWidth = 180;
        int gap = 12;
        int totalWidth = letterWidth + gap + panelWidth;
        int top = Math.max(4, (this.height - panelHeight) / 2);

        this.letterLeft = Math.max(4, (this.width - totalWidth) / 2);
        this.letterTop = top + (panelHeight - letterHeight) / 2;
        this.panelX = this.letterLeft + letterWidth + gap;
        this.panelY = top;

        if (this.panelX + panelWidth > this.width - 4) {
            this.panelX = Math.max(4, this.width - panelWidth - 4);
            this.letterLeft = 4;
        }

        this.textBox = this.addRenderableWidget(new TextBox(
                this.font,
                this.letterLeft + 17,
                this.letterTop + 21,
                142,
                144
        )
                .setFontColor(0xFF7B593D)
                .setFontUnfocusedColor(0xFF7B593D)
                .setSelectionColor(0xFF664488)
                .setSelectionUnfocusedColor(0xFF696170)
                .setHintColor(0xFFC2A57F)
                .setText(FormattedString.parse(getString("text", ""))));

        this.senderBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 30,
                this.panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.letter.sender")
        );
        this.senderBox.setMaxLength(128);
        this.senderBox.setValue(getString("sender", ""));
        this.senderBox.setHint(Component.translatable("questlog_envelope.editor.letter.sender_hint"));
        this.addRenderableWidget(this.senderBox);

        this.letterTitleBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 68,
                this.panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.letter.letter_title")
        );
        this.letterTitleBox.setMaxLength(128);
        this.letterTitleBox.setValue(getString("title", "Letter"));
        this.addRenderableWidget(this.letterTitleBox);

        this.addRenderableWidget(Button.builder(autoClaimLabel(), button -> {
            this.autoClaim = !this.autoClaim;
            button.setMessage(autoClaimLabel());
        }).bounds(this.panelX, this.panelY + 94, this.panelWidth, 18).build());

        this.addRenderableWidget(Button.builder(fontSizeLabel(), button -> {
            this.fontSize = switch (this.fontSize) {
                case "normal" -> "small";
                case "small" -> "large";
                default -> "normal";
            };
            button.setMessage(fontSizeLabel());
        }).bounds(this.panelX, this.panelY + 116, this.panelWidth, 18).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> this.sealSelection.previous()
        ).bounds(this.panelX, this.panelY + 143, 32, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.seal.none"),
                button -> this.sealSelection.clear()
        ).bounds(this.panelX + 38, this.panelY + 143, this.panelWidth - 76, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> this.sealSelection.next()
        ).bounds(this.panelX + this.panelWidth - 32, this.panelY + 143, 32, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> this.onClose()
        ).bounds(this.panelX, this.panelY + 208, (this.panelWidth - 6) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(
                this.panelX + (this.panelWidth + 6) / 2,
                this.panelY + 208,
                (this.panelWidth - 6) / 2,
                20
        ).build());

        this.setInitialFocus(this.textBox);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        FormattedString message = this.textBox == null
                ? new FormattedString()
                : this.textBox.getEditor().getText();
        int cursor = this.textBox == null ? 0 : this.textBox.getEditor().getCursorPos();

        super.resize(minecraft, width, height);

        this.textBox.getEditor().setText(message);
        this.textBox.getEditor().setCursorPos(cursor, false);
    }

    private Component autoClaimLabel() {
        return Component.translatable(
                "questlog_envelope.editor.letter.auto_claim",
                Component.translatable(this.autoClaim ? "options.on" : "options.off")
        );
    }

    private Component fontSizeLabel() {
        return Component.translatable(
                "questlog_envelope.editor.letter.font_size",
                Component.translatable("questlog_envelope.editor.letter.font_size." + this.fontSize)
        );
    }

    private void saveAndClose() {
        String sender = this.senderBox.getValue().trim();
        if (!sender.isEmpty() && ResourceLocation.tryParse(sender) == null) {
            this.validationError = Component.translatable("questlog_envelope.editor.letter.invalid_sender");
            return;
        }

        if (sender.isEmpty()) {
            this.rewardEntry.remove("sender");
        } else {
            this.rewardEntry.addProperty("sender", sender);
        }

        String title = this.letterTitleBox.getValue().trim();
        if (title.isEmpty()) {
            this.rewardEntry.remove("title");
        } else {
            this.rewardEntry.addProperty("title", title);
        }

        String text = this.textBox.getEditor().getText().toString();
        if (text.isEmpty()) {
            this.rewardEntry.remove("text");
        } else {
            this.rewardEntry.addProperty("text", text);
        }

        if ("normal".equals(this.fontSize)) {
            this.rewardEntry.remove("font_size");
        } else {
            this.rewardEntry.addProperty("font_size", this.fontSize);
        }

        ResourceLocation seal = this.sealSelection.get();
        if (seal == null) {
            this.rewardEntry.remove("seal");
        } else {
            this.rewardEntry.addProperty("seal", seal.toString());
        }

        this.rewardEntry.addProperty("auto_claim", this.autoClaim);
        this.validationError = null;

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private String getString(String key, String fallback) {
        return this.rewardEntry.has(key) ? this.rewardEntry.get(key).getAsString() : fallback;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Same special handling Envelope uses for its formatting toolbar.
        if (this.getFocused() instanceof TextBox box
                && box.formattingToolbarMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        @Nullable GuiEventListener previous = this.getFocused();
        super.setFocused(focused);
        if (previous != null && !previous.equals(this.getFocused()) && previous instanceof TextBox box) {
            box.getEditor().clearSelection();
            box.getDisplayCache().scheduleUpdate();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        graphics.blit(LetterEditScreen.TEXTURE, this.letterLeft, this.letterTop, 0, 0, 176, 192);
        graphics.fill(
                this.panelX - 6,
                this.panelY - 6,
                this.panelX + this.panelWidth + 6,
                this.panelY + 232,
                0xB0101010
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.letter.settings"),
                this.panelX + this.panelWidth / 2,
                this.panelY + 6,
                0xFFFFFF
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.letter.sender"),
                this.panelX,
                this.panelY + 19,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.letter.letter_title"),
                this.panelX,
                this.panelY + 57,
                0xFFFFFF,
                false
        );

        this.sealSelection.renderPreview(
                graphics,
                this.panelX + this.panelWidth / 2 - 15,
                this.panelY + 166
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.seal", this.sealSelection.label()),
                this.panelX + this.panelWidth / 2,
                this.panelY + 197,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.letter.format_hint"),
                this.letterLeft + 88,
                this.letterTop + 194,
                0xFFB8B8B8
        );

        if (this.validationError != null) {
            graphics.drawCenteredString(
                    this.font,
                    this.validationError,
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 135,
                    0xFF5555
            );
        }
    }
}
