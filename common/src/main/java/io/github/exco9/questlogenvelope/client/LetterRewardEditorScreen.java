package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
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

/** Questlog letter editor using Envelope's native paper and formatting UI. */
public final class LetterRewardEditorScreen extends Screen {
    private final Screen parent;
    private final JsonObject rewardEntry;
    private final SealSelection sealSelection;

    private EditBox senderBox;
    private EditBox letterTitleBox;
    private EditBox magicCircleColorBox;
    private EditBox magicCircleCommandBox;
    private TextBox textBox;
    private boolean autoClaim;
    private boolean magicCircle;

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
        this.magicCircle = rewardEntry.has("magic_circle") && rewardEntry.get("magic_circle").getAsBoolean();
        this.sealSelection = new SealSelection(
                rewardEntry.has("seal") ? rewardEntry.get("seal").getAsString() : null
        );
    }

    @Override
    protected void init() {
        int letterWidth = 176;
        int letterHeight = 192;
        int panelHeight = 264;
        this.panelWidth = 190;
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
                this.panelY + 29,
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
                this.panelY + 64,
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
        }).bounds(this.panelX, this.panelY + 89, this.panelWidth, 18).build());

        this.addRenderableWidget(Button.builder(magicCircleLabel(), button -> {
            this.magicCircle = !this.magicCircle;
            button.setMessage(magicCircleLabel());
            updateMagicCircleFields();
        }).bounds(this.panelX, this.panelY + 111, this.panelWidth, 18).build());

        this.magicCircleColorBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 146,
                72,
                18,
                Component.translatable("questlog_envelope.editor.magic_circle.color")
        );
        this.magicCircleColorBox.setMaxLength(8);
        this.magicCircleColorBox.setValue(getString(
                "magic_circle_color",
                formatColor(QuestMagicCircle.DEFAULT_COLOR)
        ));
        this.magicCircleColorBox.setHint(Component.literal("#55AAFF"));
        this.addRenderableWidget(this.magicCircleColorBox);

        this.magicCircleCommandBox = new EditBox(
                this.font,
                this.panelX + 78,
                this.panelY + 146,
                this.panelWidth - 78,
                18,
                Component.translatable("questlog_envelope.editor.magic_circle.command")
        );
        this.magicCircleCommandBox.setMaxLength(512);
        this.magicCircleCommandBox.setValue(getString("magic_circle_command", ""));
        this.magicCircleCommandBox.setHint(Component.translatable(
                "questlog_envelope.editor.magic_circle.command_hint"
        ));
        this.addRenderableWidget(this.magicCircleCommandBox);
        updateMagicCircleFields();

        this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> this.sealSelection.previous()
        ).bounds(this.panelX, this.panelY + 171, 28, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.seal.none"),
                button -> this.sealSelection.clear()
        ).bounds(this.panelX + 34, this.panelY + 171, this.panelWidth - 68, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> this.sealSelection.next()
        ).bounds(this.panelX + this.panelWidth - 28, this.panelY + 171, 28, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> this.onClose()
        ).bounds(this.panelX, this.panelY + 241, (this.panelWidth - 6) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(
                this.panelX + (this.panelWidth + 6) / 2,
                this.panelY + 241,
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
        String sender = this.senderBox == null ? getString("sender", "") : this.senderBox.getValue();
        String title = this.letterTitleBox == null ? getString("title", "Letter") : this.letterTitleBox.getValue();
        String color = this.magicCircleColorBox == null
                ? getString("magic_circle_color", formatColor(QuestMagicCircle.DEFAULT_COLOR))
                : this.magicCircleColorBox.getValue();
        String command = this.magicCircleCommandBox == null
                ? getString("magic_circle_command", "")
                : this.magicCircleCommandBox.getValue();

        super.resize(minecraft, width, height);

        this.textBox.getEditor().setText(message);
        this.textBox.getEditor().setCursorPos(cursor, false);
        this.senderBox.setValue(sender);
        this.letterTitleBox.setValue(title);
        this.magicCircleColorBox.setValue(color);
        this.magicCircleCommandBox.setValue(command);
    }

    private Component autoClaimLabel() {
        return Component.translatable(
                "questlog_envelope.editor.letter.auto_claim",
                Component.translatable(this.autoClaim ? "options.on" : "options.off")
        );
    }

    private Component magicCircleLabel() {
        return Component.translatable(
                "questlog_envelope.editor.magic_circle",
                Component.translatable(this.magicCircle ? "options.on" : "options.off")
        );
    }

    private void updateMagicCircleFields() {
        if (this.magicCircleColorBox != null) {
            this.magicCircleColorBox.active = this.magicCircle;
        }
        if (this.magicCircleCommandBox != null) {
            this.magicCircleCommandBox.active = this.magicCircle;
        }
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

        this.rewardEntry.remove("font_size");
        this.rewardEntry.remove("magic_seal");

        ResourceLocation seal = this.sealSelection.get();
        if (seal == null) {
            this.rewardEntry.remove("seal");
        } else {
            this.rewardEntry.addProperty("seal", seal.toString());
        }

        if (this.magicCircle) {
            @Nullable Integer color = parseColor(this.magicCircleColorBox.getValue());
            if (color == null) {
                this.validationError = Component.translatable(
                        "questlog_envelope.editor.magic_circle.invalid_color"
                );
                return;
            }

            this.rewardEntry.addProperty("magic_circle", true);
            this.rewardEntry.addProperty("magic_circle_color", formatColor(color));

            String command = this.magicCircleCommandBox.getValue().trim();
            if (command.isEmpty()) {
                this.rewardEntry.remove("magic_circle_command");
            } else {
                this.rewardEntry.addProperty("magic_circle_command", command);
            }
        } else {
            this.rewardEntry.remove("magic_circle");
            this.rewardEntry.remove("magic_circle_color");
            this.rewardEntry.remove("magic_circle_command");
        }

        this.rewardEntry.addProperty("auto_claim", this.autoClaim);
        this.validationError = null;

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private String getString(String key, String fallback) {
        return this.rewardEntry.has(key) && this.rewardEntry.get(key).isJsonPrimitive()
                ? this.rewardEntry.get(key).getAsString()
                : fallback;
    }

    private static @Nullable Integer parseColor(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return QuestMagicCircle.DEFAULT_COLOR;
        }
        if (value.startsWith("#")) {
            value = value.substring(1);
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            value = value.substring(2);
        }
        if (value.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatColor(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                this.panelY + 264,
                0xB0101010
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.magicCircle) {
            MagicCircleTexture.render(
                    graphics,
                    this.letterLeft + 17 + 142 - MagicCircleTexture.DISPLAY_SIZE - 3,
                    this.letterTop + 21 + 144 - MagicCircleTexture.DISPLAY_SIZE - 3
            );
        }

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
                this.panelY + 18,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.letter.letter_title"),
                this.panelX,
                this.panelY + 53,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.magic_circle.color"),
                this.panelX,
                this.panelY + 135,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.magic_circle.command"),
                this.panelX + 78,
                this.panelY + 135,
                0xFFFFFF,
                false
        );

        @Nullable Integer previewColor = parseColor(
                this.magicCircleColorBox == null ? "" : this.magicCircleColorBox.getValue()
        );
        graphics.fill(
                this.panelX + 61,
                this.panelY + 135,
                this.panelX + 69,
                this.panelY + 143,
                0xFF000000 | (previewColor == null ? 0xFF5555 : previewColor)
        );

        this.sealSelection.renderPreview(
                graphics,
                this.panelX + this.panelWidth / 2 - 15,
                this.panelY + 195
        );

        if (this.validationError == null) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("questlog_envelope.editor.seal", this.sealSelection.label()),
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 227,
                    0xFFFFFF
            );
        } else {
            graphics.drawCenteredString(
                    this.font,
                    this.validationError,
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 227,
                    0xFF5555
            );
        }

        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.letter.format_hint"),
                this.letterLeft + 88,
                this.letterTop + 194,
                0xFFB8B8B8
        );
    }
}
