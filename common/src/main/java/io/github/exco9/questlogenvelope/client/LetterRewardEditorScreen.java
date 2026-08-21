package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Small companion editor opened from Questlog's reward editor when the
 * questlog_envelope:letter reward is selected.
 *
 * Questlog keeps ownership of the reward entry. This screen only mutates the
 * Envelope-specific JSON keys when the user presses Done.
 */
public final class LetterRewardEditorScreen extends Screen {
    private final Screen parent;
    private final JsonObject rewardEntry;

    private EditBox senderBox;
    private EditBox letterTitleBox;
    private MultiLineEditBox bodyBox;
    private boolean autoClaim;

    @Nullable
    private Component validationError;

    public LetterRewardEditorScreen(Screen parent, JsonObject rewardEntry) {
        super(Component.translatable("questlog_envelope.editor.letter.title"));
        this.parent = parent;
        this.rewardEntry = rewardEntry;
        this.autoClaim = rewardEntry.has("auto_claim") && rewardEntry.get("auto_claim").getAsBoolean();
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(360, this.width - 30);
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(28, (this.height - 238) / 2);

        this.senderBox = new EditBox(
                this.font,
                x,
                y + 34,
                panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.letter.sender")
        );
        this.senderBox.setMaxLength(128);
        this.senderBox.setValue(getString("sender", ""));
        this.senderBox.setHint(Component.translatable("questlog_envelope.editor.letter.sender_hint"));
        this.addRenderableWidget(this.senderBox);

        this.letterTitleBox = new EditBox(
                this.font,
                x,
                y + 76,
                panelWidth,
                18,
                Component.translatable("questlog_envelope.editor.letter.letter_title")
        );
        this.letterTitleBox.setMaxLength(128);
        this.letterTitleBox.setValue(getString("title", "Letter"));
        this.addRenderableWidget(this.letterTitleBox);

        this.bodyBox = new MultiLineEditBox(
                this.font,
                x,
                y + 118,
                panelWidth,
                58,
                Component.translatable("questlog_envelope.editor.letter.body"),
                Component.empty()
        );
        this.bodyBox.setCharacterLimit(4096);
        this.bodyBox.setValue(getString("text", ""));
        this.addRenderableWidget(this.bodyBox);

        Button autoClaimButton = Button.builder(autoClaimLabel(), button -> {
            this.autoClaim = !this.autoClaim;
            button.setMessage(autoClaimLabel());
        }).bounds(x, y + 184, panelWidth, 18).build();
        this.addRenderableWidget(autoClaimButton);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> this.onClose()
        ).bounds(x, y + 210, (panelWidth - 6) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(x + (panelWidth + 6) / 2, y + 210, (panelWidth - 6) / 2, 20).build());
    }

    private Component autoClaimLabel() {
        return Component.translatable(
                "questlog_envelope.editor.letter.auto_claim",
                Component.translatable(this.autoClaim ? "options.on" : "options.off")
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

        String text = this.bodyBox.getValue();
        if (text.isEmpty()) {
            this.rewardEntry.remove("text");
        } else {
            this.rewardEntry.addProperty("text", text);
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
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int panelWidth = Math.min(360, this.width - 30);
        int x = (this.width - panelWidth) / 2;
        int y = Math.max(28, (this.height - 238) / 2);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, y + 4, 0xFFFFFF);
        graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.letter.sender"), x, y + 23, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.letter.letter_title"), x, y + 65, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.letter.body"), x, y + 107, 0xFFFFFF, false);

        if (this.validationError != null) {
            graphics.drawCenteredString(this.font, this.validationError, this.width / 2, y + 232, 0xFF5555);
        }
    }
}
