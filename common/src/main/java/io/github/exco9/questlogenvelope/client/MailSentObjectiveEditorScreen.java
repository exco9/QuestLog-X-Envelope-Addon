package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Extra filters for questlog_envelope:mail_sent. */
public final class MailSentObjectiveEditorScreen extends Screen {
    private final Screen parent;
    private final JsonObject entry;

    private String mailKind;
    private EditBox textContainsBox;
    private EditBox itemBox;
    private EditBox itemCountBox;
    private Button mailKindButton;

    private int panelX;
    private int panelY;
    private int panelWidth;

    @Nullable
    private Component validationError;

    public MailSentObjectiveEditorScreen(Screen parent, JsonObject entry) {
        super(Component.translatable("questlog_envelope.editor.mail_sent.title"));
        this.parent = parent;
        this.entry = entry;
        this.mailKind = normalizeKind(getString("mail_kind", "any"));
    }

    @Override
    protected void init() {
        this.panelWidth = 260;
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = Math.max(16, (this.height - 208) / 2);

        this.mailKindButton = this.addRenderableWidget(Button.builder(kindLabel(), button -> {
            this.mailKind = switch (this.mailKind) {
                case "any" -> "letter";
                case "letter" -> "package";
                default -> "any";
            };
            button.setMessage(kindLabel());
        }).bounds(this.panelX, this.panelY + 31, this.panelWidth, 20).build());

        this.textContainsBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 78,
                this.panelWidth,
                20,
                Component.translatable("questlog_envelope.editor.mail_sent.text_contains")
        );
        this.textContainsBox.setMaxLength(512);
        this.textContainsBox.setValue(getString("text_contains", ""));
        this.textContainsBox.setHint(Component.translatable("questlog_envelope.editor.mail_sent.text_hint"));
        this.addRenderableWidget(this.textContainsBox);

        this.itemBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 125,
                188,
                20,
                Component.translatable("questlog_envelope.editor.mail_sent.item")
        );
        this.itemBox.setMaxLength(128);
        this.itemBox.setValue(getString("item", ""));
        this.itemBox.setHint(Component.literal("minecraft:diamond"));
        this.addRenderableWidget(this.itemBox);

        this.itemCountBox = new EditBox(
                this.font,
                this.panelX + 196,
                this.panelY + 125,
                64,
                20,
                Component.translatable("questlog_envelope.editor.mail_sent.item_count")
        );
        this.itemCountBox.setMaxLength(6);
        this.itemCountBox.setValue(Integer.toString(getInt("item_count", 1)));
        this.addRenderableWidget(this.itemCountBox);

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(this.panelX, this.panelY + 174, 126, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(this.panelX + 134, this.panelY + 174, 126, 20).build());
    }

    private Component kindLabel() {
        return Component.translatable(
                "questlog_envelope.editor.mail_sent.kind",
                Component.translatable("questlog_envelope.editor.mail_sent.kind." + this.mailKind)
        );
    }

    private void saveAndClose() {
        String item = this.itemBox.getValue().trim();
        if (!item.isEmpty() && ResourceLocation.tryParse(item) == null) {
            this.validationError = Component.translatable("questlog_envelope.editor.mail_sent.invalid_item");
            return;
        }

        int itemCount;
        try {
            itemCount = Math.max(1, Integer.parseInt(this.itemCountBox.getValue().trim()));
        } catch (NumberFormatException exception) {
            this.validationError = Component.translatable("questlog_envelope.editor.mail_sent.invalid_count");
            return;
        }

        if ("any".equals(this.mailKind)) {
            this.entry.remove("mail_kind");
        } else {
            this.entry.addProperty("mail_kind", this.mailKind);
        }

        String text = this.textContainsBox.getValue().trim();
        if (text.isEmpty()) {
            this.entry.remove("text_contains");
        } else {
            this.entry.addProperty("text_contains", text);
        }

        if (item.isEmpty()) {
            this.entry.remove("item");
            this.entry.remove("item_count");
        } else {
            this.entry.addProperty("item", item);
            if (itemCount == 1) {
                this.entry.remove("item_count");
            } else {
                this.entry.addProperty("item_count", itemCount);
            }
        }

        this.validationError = null;
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private String getString(String key, String fallback) {
        return this.entry.has(key) && this.entry.get(key).isJsonPrimitive()
                ? this.entry.get(key).getAsString()
                : fallback;
    }

    private int getInt(String key, int fallback) {
        if (!this.entry.has(key) || !this.entry.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return this.entry.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String normalizeKind(String value) {
        return switch (value == null ? "" : value.toLowerCase()) {
            case "letter" -> "letter";
            case "package" -> "package";
            default -> "any";
        };
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        graphics.fill(
                this.panelX - 10,
                this.panelY - 10,
                this.panelX + this.panelWidth + 10,
                this.panelY + 204,
                0xD0101010
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.mail_sent.title"),
                this.panelX + this.panelWidth / 2,
                this.panelY + 5,
                0xFFFFFF
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.mail_sent.text_contains"),
                this.panelX,
                this.panelY + 66,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.mail_sent.item"),
                this.panelX,
                this.panelY + 113,
                0xFFFFFF,
                false
        );
        graphics.drawString(
                this.font,
                Component.translatable("questlog_envelope.editor.mail_sent.item_count"),
                this.panelX + 196,
                this.panelY + 113,
                0xFFFFFF,
                false
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.mail_sent.recipient_hint"),
                this.panelX + this.panelWidth / 2,
                this.panelY + 151,
                0xFFB0B0B0
        );

        if (this.validationError != null) {
            graphics.drawCenteredString(
                    this.font,
                    this.validationError,
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 163,
                    0xFF5555
            );
        }
    }
}
