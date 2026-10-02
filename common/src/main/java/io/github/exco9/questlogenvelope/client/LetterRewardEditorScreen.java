package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import io.github.mortuusars.envelope.client.gui.screen.LetterEditScreen;
import io.github.mortuusars.envelope.client.gui.widget.textbox.TextBox;
import io.github.mortuusars.envelope.client.gui.widget.textbox.text.FormattedString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Questlog letter editor using Envelope's native paper and formatting UI. */
public final class LetterRewardEditorScreen extends Screen {
    private static final int HANDLE_RADIUS = 4;
    private static final int SNAP_GRID = 4;

    private final Screen parent;
    private final JsonObject rewardEntry;
    private final SealSelection sealSelection;
    private final SignatureEditorControls signatureControls;
    private List<AbstractWidget> decorationWidgets = List.of();
    private boolean signatureSelected;
    private boolean emberPreview;
    private final @Nullable Runnable saveQuest;

    private EditBox senderBox;
    private EditBox letterTitleBox;
    private EditBox magicCircleColorBox;
    private EditBox magicCircleMagicColorBox;
    private Button signatureButton;
    private EditBox magicCircleCommandBox;
    private EditBox magicCircleHoldBox;
    private Button magicCircleResetButton;
    private TextBox textBox;
    private boolean autoClaim;
    private boolean magicCircle;

    private int magicCircleX;
    private int magicCircleY;
    private int magicCircleSize;
    private CircleDragMode circleDragMode = CircleDragMode.NONE;
    private double moveGrabOffsetX;
    private double moveGrabOffsetY;
    private double resizeCenterX;
    private double resizeCenterY;

    private int letterLeft;
    private int letterTop;
    private int panelX;
    private int panelY;
    private int panelWidth;

    @Nullable
    private Component validationError;

    public LetterRewardEditorScreen(Screen parent, JsonObject rewardEntry) {
        this(parent, rewardEntry, null);
    }

    public LetterRewardEditorScreen(Screen parent, JsonObject rewardEntry, @Nullable Runnable saveQuest) {
        super(Component.translatable("questlog_envelope.editor.letter.title"));
        this.parent = parent;
        this.rewardEntry = rewardEntry;
        this.saveQuest = saveQuest;
        this.signatureControls = new SignatureEditorControls(rewardEntry);
        this.autoClaim = rewardEntry.has("auto_claim") && rewardEntry.get("auto_claim").getAsBoolean();
        this.magicCircle = rewardEntry.has("magic_circle") && rewardEntry.get("magic_circle").getAsBoolean();

        this.magicCircleSize = QuestMagicCircle.clampSize(
                getInt(rewardEntry, "magic_circle_size", QuestMagicCircle.DEFAULT_SIZE)
        );
        this.magicCircleX = QuestMagicCircle.clampX(
                getInt(rewardEntry, "magic_circle_x", QuestMagicCircle.defaultX(this.magicCircleSize)),
                this.magicCircleSize
        );
        this.magicCircleY = QuestMagicCircle.clampY(
                getInt(rewardEntry, "magic_circle_y", QuestMagicCircle.defaultY(this.magicCircleSize)),
                this.magicCircleSize
        );

        this.sealSelection = new SealSelection(
                rewardEntry.has("seal") ? rewardEntry.get("seal").getAsString() : null
        );
    }

    @Override
    protected void init() {
        int letterWidth = 176;
        int letterHeight = 192;
        int panelHeight = 324;
        this.panelWidth = 210;
        int gap = 12;
        int totalWidth = letterWidth + gap + this.panelWidth;
        int top = Math.max(4, (this.height - panelHeight) / 2);

        this.letterLeft = Math.max(4, (this.width - totalWidth) / 2);
        this.letterTop = top + (panelHeight - letterHeight) / 2;
        this.panelX = this.letterLeft + letterWidth + gap;
        this.panelY = top;

        if (this.panelX + this.panelWidth > this.width - 4) {
            this.panelX = Math.max(4, this.width - this.panelWidth - 4);
            this.letterLeft = 4;
        }

        this.textBox = this.addRenderableWidget(new TextBox(
                this.font,
                writableLeft(),
                writableTop(),
                QuestMagicCircle.WRITABLE_WIDTH,
                QuestMagicCircle.WRITABLE_HEIGHT
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
            this.circleDragMode = CircleDragMode.NONE;
            button.setMessage(magicCircleLabel());
            updateMagicCircleFields();
        }).bounds(this.panelX, this.panelY + 111, 126, 18).build());

        this.magicCircleHoldBox = new EditBox(
                this.font,
                this.panelX + 130,
                this.panelY + 111,
                44,
                18,
                Component.translatable("questlog_envelope.editor.magic_circle.hold")
        );
        this.magicCircleHoldBox.setMaxLength(5);
        this.magicCircleHoldBox.setValue(formatHoldSeconds(getDouble(
                this.rewardEntry,
                "magic_circle_hold_seconds",
                QuestMagicCircle.DEFAULT_HOLD_MILLIS / 1000.0
        )));
        this.magicCircleHoldBox.setHint(Component.literal("3.0"));
        this.addRenderableWidget(this.magicCircleHoldBox);

        this.magicCircleResetButton = this.addRenderableWidget(Button.builder(
                Component.literal("R"),
                button -> resetMagicCircleLayout()
        ).bounds(this.panelX + 178, this.panelY + 111, 32, 18).build());
        this.magicCircleResetButton.setTooltip(Tooltip.create(
                Component.translatable("questlog_envelope.editor.magic_circle.reset_layout")
        ));

        this.magicCircleColorBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 146,
                102,
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

        this.magicCircleMagicColorBox = new EditBox(this.font, this.panelX + 108, this.panelY + 146,
                102, 18, Component.translatable("questlog_envelope.editor.signature.magic_color"));
        this.magicCircleMagicColorBox.setMaxLength(8);
        this.magicCircleMagicColorBox.setValue(getString("magic_circle_magic_color",
                formatColor(io.github.exco9.questlogenvelope.mail.LetterSignature.DEFAULT_MAGIC_COLOR)));
        this.magicCircleMagicColorBox.setTooltip(Tooltip.create(
                Component.translatable("questlog_envelope.editor.magic_circle.magic_color_hint")));
        this.addRenderableWidget(this.magicCircleMagicColorBox);

        this.magicCircleCommandBox = new EditBox(
                this.font,
                this.panelX,
                this.panelY + 180,
                this.panelWidth,
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
        ).bounds(this.panelX, this.panelY + 228, 28, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.seal.reset"),
                button -> this.sealSelection.clear()
        ).bounds(this.panelX + 34, this.panelY + 228, this.panelWidth - 68, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> this.sealSelection.next()
        ).bounds(this.panelX + this.panelWidth - 28, this.panelY + 228, 28, 20).build());

        this.signatureButton = this.addRenderableWidget(Button.builder(
                signatureLabel(),
                button -> {
                    this.signatureControls.addSignature();
                    this.validationError = null;
                    selectSignature(true);
                }
        ).bounds(this.panelX, this.panelY + 202, this.panelWidth, 18).build());
        this.decorationWidgets = this.children().stream()
                .filter(child -> child instanceof AbstractWidget widget && widget.getY() >= this.panelY + 111)
                .map(child -> (AbstractWidget) child).toList();
        this.signatureControls.rebuild(this.font, this.panelX, this.panelY, this.panelWidth,
                widget -> this.addRenderableWidget(widget), () -> selectSignature(false), () -> this.validationError = null);
        selectSignature(this.signatureSelected);

        if (EmberTextCompat.isAvailable()) {
            this.addRenderableWidget(Button.builder(emberPreviewLabel(), button -> {
                this.emberPreview = !this.emberPreview;
                this.textBox.visible = !this.emberPreview;
                this.setFocused(null);
                button.setMessage(emberPreviewLabel());
            }).bounds(this.letterLeft, this.letterTop - 22, 176, 18).build());
        } else this.emberPreview = false;
        this.textBox.visible = !this.emberPreview;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> this.onClose()
        ).bounds(this.panelX, this.panelY + 300, (this.panelWidth - 6) / 2, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable(this.saveQuest == null ? "gui.done" : "questlog_envelope.editor.letter.save_quest"),
                button -> saveAndClose()
        ).bounds(
                this.panelX + (this.panelWidth + 6) / 2,
                this.panelY + 300,
                (this.panelWidth - 6) / 2,
                20
        ).build());

        for (EditBox box : new EditBox[]{this.senderBox, this.letterTitleBox, this.magicCircleColorBox,
                this.magicCircleCommandBox, this.magicCircleHoldBox, this.magicCircleMagicColorBox}) {
            box.setResponder(value -> this.validationError = null);
        }
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
        String magicColor = this.magicCircleMagicColorBox.getValue();
        String command = this.magicCircleCommandBox == null
                ? getString("magic_circle_command", "")
                : this.magicCircleCommandBox.getValue();
        String hold = this.magicCircleHoldBox == null
                ? formatHoldSeconds(getDouble(
                        this.rewardEntry,
                        "magic_circle_hold_seconds",
                        QuestMagicCircle.DEFAULT_HOLD_MILLIS / 1000.0
                ))
                : this.magicCircleHoldBox.getValue();

        this.circleDragMode = CircleDragMode.NONE;
        super.resize(minecraft, width, height);

        this.textBox.getEditor().setText(message);
        this.textBox.getEditor().setCursorPos(cursor, false);
        this.senderBox.setValue(sender);
        this.letterTitleBox.setValue(title);
        this.magicCircleColorBox.setValue(color);
        this.magicCircleMagicColorBox.setValue(magicColor);
        this.magicCircleCommandBox.setValue(command);
        this.magicCircleHoldBox.setValue(hold);
    }

    private Component autoClaimLabel() {
        return Component.translatable(
                "questlog_envelope.editor.letter.auto_claim",
                Component.translatable(this.autoClaim ? "options.on" : "options.off")
        );
    }

    private Component signatureLabel() {
        return Component.translatable("questlog_envelope.editor.signature.toggle",
                Component.translatable(this.signatureControls.isEnabled() ? "options.on" : "options.off"));
    }

    private Component emberPreviewLabel() {
        return Component.translatable("questlog_envelope.editor.letter.ember_preview",
                Component.translatable(this.emberPreview ? "options.on" : "options.off"));
    }

    private void selectSignature(boolean selected) {
        this.signatureSelected = selected && this.signatureControls.isEnabled();
        this.circleDragMode = CircleDragMode.NONE;
        this.setFocused(null);
        for (AbstractWidget widget : this.decorationWidgets) widget.visible = !this.signatureSelected;
        this.signatureControls.setVisible(this.signatureSelected);
        if (this.signatureButton != null) this.signatureButton.setMessage(signatureLabel());
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
        if (this.magicCircleMagicColorBox != null) this.magicCircleMagicColorBox.active = this.magicCircle;
        if (this.magicCircleCommandBox != null) {
            this.magicCircleCommandBox.active = this.magicCircle;
        }
        if (this.magicCircleHoldBox != null) {
            this.magicCircleHoldBox.active = this.magicCircle;
        }
        if (this.magicCircleResetButton != null) {
            this.magicCircleResetButton.active = this.magicCircle;
        }
    }

    private void resetMagicCircleLayout() {
        this.circleDragMode = CircleDragMode.NONE;
        this.magicCircleSize = QuestMagicCircle.DEFAULT_SIZE;
        this.magicCircleX = QuestMagicCircle.defaultX(this.magicCircleSize);
        this.magicCircleY = QuestMagicCircle.defaultY(this.magicCircleSize);
    }

    private void saveAndClose() {
        this.validationError = this.signatureControls.validate();
        if (this.validationError != null) {
            selectSignature(true);
            return;
        }
        JsonObject pending = this.rewardEntry.deepCopy();
        String sender = this.senderBox.getValue().trim();
        if (!sender.isEmpty() && ResourceLocation.tryParse(sender) == null) {
            selectSignature(false);
            this.validationError = Component.translatable("questlog_envelope.editor.letter.invalid_sender");
            return;
        }

        if (sender.isEmpty()) {
            pending.remove("sender");
        } else {
            pending.addProperty("sender", sender);
        }

        String title = this.letterTitleBox.getValue().trim();
        if (title.isEmpty()) {
            pending.remove("title");
        } else {
            pending.addProperty("title", title);
        }

        String text = this.textBox.getEditor().getText().toString();
        if (text.isEmpty()) {
            pending.remove("text");
        } else {
            pending.addProperty("text", text);
        }

        pending.remove("font_size");
        pending.remove("magic_seal");

        ResourceLocation seal = this.sealSelection.get();
        if (seal == null) {
            pending.remove("seal");
        } else {
            pending.addProperty("seal", seal.toString());
        }

        if (this.magicCircle) {
            @Nullable Integer color = parseColor(this.magicCircleColorBox.getValue());
            if (color == null) {
                selectSignature(false);
                this.validationError = Component.translatable(
                        "questlog_envelope.editor.magic_circle.invalid_color"
                );
                return;
            }

            @Nullable Double holdSeconds = parseHoldSeconds(this.magicCircleHoldBox.getValue());
            if (holdSeconds == null
                    || holdSeconds * 1000.0 < QuestMagicCircle.MIN_HOLD_MILLIS
                    || holdSeconds * 1000.0 > QuestMagicCircle.MAX_HOLD_MILLIS) {
                selectSignature(false);
                this.validationError = Component.translatable(
                        "questlog_envelope.editor.magic_circle.invalid_hold",
                        formatHoldSeconds(QuestMagicCircle.MIN_HOLD_MILLIS / 1000.0),
                        formatHoldSeconds(QuestMagicCircle.MAX_HOLD_MILLIS / 1000.0)
                );
                return;
            }

            this.magicCircleSize = QuestMagicCircle.clampSize(this.magicCircleSize);
            this.magicCircleX = QuestMagicCircle.clampX(this.magicCircleX, this.magicCircleSize);
            this.magicCircleY = QuestMagicCircle.clampY(this.magicCircleY, this.magicCircleSize);

            pending.addProperty("magic_circle", true);
            Integer magicColor = parseColor(this.magicCircleMagicColorBox.getValue());
            if (magicColor == null) {
                selectSignature(false);
                this.validationError = Component.translatable("questlog_envelope.editor.magic_circle.invalid_color");
                return;
            }
            pending.addProperty("magic_circle_color", formatColor(color));
            pending.addProperty("magic_circle_magic_color", formatColor(magicColor));
            pending.addProperty("magic_circle_x", this.magicCircleX);
            pending.addProperty("magic_circle_y", this.magicCircleY);
            pending.addProperty("magic_circle_size", this.magicCircleSize);
            pending.addProperty("magic_circle_hold_seconds", holdSeconds);

            String command = this.magicCircleCommandBox.getValue().trim();
            if (command.isEmpty()) {
                pending.remove("magic_circle_command");
            } else {
                pending.addProperty("magic_circle_command", command);
            }
        } else {
            pending.remove("magic_circle");
            pending.remove("magic_circle_color");
            pending.remove("magic_circle_magic_color");
            pending.remove("magic_circle_command");
            pending.remove("magic_circle_x");
            pending.remove("magic_circle_y");
            pending.remove("magic_circle_size");
            pending.remove("magic_circle_hold_seconds");
        }

        pending.addProperty("auto_claim", this.autoClaim);
        this.signatureControls.save(pending, this.font);
        this.rewardEntry.entrySet().clear();
        for (var entry : pending.entrySet()) this.rewardEntry.add(entry.getKey(), entry.getValue());
        this.validationError = null;

        if (this.saveQuest != null) {
            this.saveQuest.run();
        } else if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private String getString(String key, String fallback) {
        return this.rewardEntry.has(key) && this.rewardEntry.get(key).isJsonPrimitive()
                ? this.rewardEntry.get(key).getAsString()
                : fallback;
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject object, String key, double fallback) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return fallback;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
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

    private static @Nullable Double parseHoldSeconds(String raw) {
        String value = raw == null ? "" : raw.trim().replace(',', '.');
        if (value.isEmpty()) {
            return QuestMagicCircle.DEFAULT_HOLD_MILLIS / 1000.0;
        }
        try {
            double seconds = Double.parseDouble(value);
            return Double.isFinite(seconds) ? seconds : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatColor(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static String formatHoldSeconds(double seconds) {
        if (Math.abs(seconds - Math.rint(seconds)) < 0.0001) {
            return String.format("%.1f", seconds);
        }
        return String.format("%.2f", seconds).replaceAll("0+$", "").replaceAll("\\.$", ".0");
    }

    private int writableLeft() {
        return this.letterLeft + 17;
    }

    private int writableTop() {
        return this.letterTop + 21;
    }

    private int circleScreenX() {
        return writableLeft() + this.magicCircleX;
    }

    private int circleScreenY() {
        return writableTop() + this.magicCircleY;
    }

    private int circleCenterScreenX() {
        return circleScreenX() + this.magicCircleSize / 2;
    }

    private int circleCenterScreenY() {
        return circleScreenY() + this.magicCircleSize / 2;
    }

    private static boolean isInsideHandle(double mouseX, double mouseY, int centerX, int centerY) {
        return mouseX >= centerX - HANDLE_RADIUS
                && mouseX <= centerX + HANDLE_RADIUS
                && mouseY >= centerY - HANDLE_RADIUS
                && mouseY <= centerY + HANDLE_RADIUS;
    }

    private void beginMove(double mouseX, double mouseY) {
        this.circleDragMode = CircleDragMode.MOVE;
        this.moveGrabOffsetX = mouseX - circleCenterScreenX();
        this.moveGrabOffsetY = mouseY - circleCenterScreenY();
        this.setFocused(null);
    }

    private void beginResize() {
        this.circleDragMode = CircleDragMode.RESIZE;
        this.resizeCenterX = this.magicCircleX + this.magicCircleSize / 2.0;
        this.resizeCenterY = this.magicCircleY + this.magicCircleSize / 2.0;
        this.setFocused(null);
    }

    private void dragMove(double mouseX, double mouseY) {
        double localCenterX = mouseX - writableLeft() - this.moveGrabOffsetX;
        double localCenterY = mouseY - writableTop() - this.moveGrabOffsetY;
        int nextX = (int) Math.round(localCenterX - this.magicCircleSize / 2.0);
        int nextY = (int) Math.round(localCenterY - this.magicCircleSize / 2.0);

        if (Screen.hasShiftDown()) {
            nextX = snap(nextX, SNAP_GRID);
            nextY = snap(nextY, SNAP_GRID);
        }

        this.magicCircleX = QuestMagicCircle.clampX(nextX, this.magicCircleSize);
        this.magicCircleY = QuestMagicCircle.clampY(nextY, this.magicCircleSize);
    }

    private void dragResize(double mouseX, double mouseY) {
        double localX = mouseX - writableLeft();
        double localY = mouseY - writableTop();
        int requestedSize = (int) Math.round(2.0 * Math.max(
                Math.abs(localX - this.resizeCenterX),
                Math.abs(localY - this.resizeCenterY)
        ));
        if (Screen.hasShiftDown()) {
            requestedSize = snap(requestedSize, SNAP_GRID);
        }

        int maxCenteredSize = (int) Math.floor(2.0 * Math.min(
                Math.min(this.resizeCenterX, QuestMagicCircle.WRITABLE_WIDTH - this.resizeCenterX),
                Math.min(this.resizeCenterY, QuestMagicCircle.WRITABLE_HEIGHT - this.resizeCenterY)
        ));
        int effectiveMax = Math.max(
                QuestMagicCircle.MIN_SIZE,
                Math.min(QuestMagicCircle.MAX_SIZE, maxCenteredSize)
        );
        int nextSize = Math.max(QuestMagicCircle.MIN_SIZE, Math.min(effectiveMax, requestedSize));

        this.magicCircleSize = QuestMagicCircle.clampSize(nextSize);
        this.magicCircleX = QuestMagicCircle.clampX(
                (int) Math.round(this.resizeCenterX - this.magicCircleSize / 2.0),
                this.magicCircleSize
        );
        this.magicCircleY = QuestMagicCircle.clampY(
                (int) Math.round(this.resizeCenterY - this.magicCircleSize / 2.0),
                this.magicCircleSize
        );
    }

    private static int snap(int value, int grid) {
        return (int) Math.round(value / (double) grid) * grid;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.signatureControls.mouseClicked(
                this.font, writableLeft(), writableTop(), mouseX, mouseY, button)) {
            selectSignature(true);
            this.setFocused(null);
            return true;
        }
        if (button == 0 && this.signatureSelected && mouseX >= writableLeft() && mouseX < writableLeft() + QuestMagicCircle.WRITABLE_WIDTH
                && mouseY >= writableTop() && mouseY < writableTop() + QuestMagicCircle.WRITABLE_HEIGHT) {
            selectSignature(false);
        }
        if (button == 0 && this.magicCircle && !this.signatureSelected) {
            if (isInsideHandle(mouseX, mouseY, circleScreenX(), circleScreenY())) {
                beginResize();
                return true;
            }

            if (isInsideHandle(mouseX, mouseY, circleCenterScreenX(), circleCenterScreenY())) {
                beginMove(mouseX, mouseY);
                return true;
            }
        }

        if (this.getFocused() instanceof TextBox box
                && box.formattingToolbarMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.signatureSelected && this.signatureControls.mouseDragged(
                this.font, writableLeft(), writableTop(), mouseX, mouseY, button)) return true;
        if (button == 0 && this.magicCircle && !this.signatureSelected) {
            if (this.circleDragMode == CircleDragMode.MOVE) {
                dragMove(mouseX, mouseY);
                return true;
            }
            if (this.circleDragMode == CircleDragMode.RESIZE) {
                dragResize(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.signatureControls.mouseReleased(button)) return true;
        if (button == 0 && this.circleDragMode != CircleDragMode.NONE) {
            this.circleDragMode = CircleDragMode.NONE;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
                this.panelY + 324,
                0xB0101010
        );
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        if (this.emberPreview) EmberTextCompat.renderPreview(graphics, this.font,
                this.textBox.getEditor().getText().toString(), writableLeft(), writableTop(),
                QuestMagicCircle.WRITABLE_WIDTH, QuestMagicCircle.WRITABLE_HEIGHT);

        this.signatureControls.render(graphics, this.font, writableLeft(), writableTop(), this.signatureSelected);

        if (this.magicCircle) {
            int x = circleScreenX();
            int y = circleScreenY();
            int size = this.magicCircleSize;

            MagicCircleTexture.render(graphics, x, y, size);
            if (!this.signatureSelected) drawCircleEditorOverlay(graphics, x, y, size, mouseX, mouseY);
        }

        graphics.drawCenteredString(this.font,
                Component.translatable("questlog_envelope.editor.letter.settings"),
                this.panelX + this.panelWidth / 2, this.panelY + 6, 0xFFFFFF);
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
        if (!this.signatureSelected) {
            graphics.drawString(this.font, Component.translatable("questlog_envelope.editor.signature.magic_color"),
                    this.panelX + 108, this.panelY + 135, 0xFFFFFF, false);
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
                    this.panelX,
                    this.panelY + 168,
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
                    this.panelY + 252
            );
        }

        if (this.validationError == null) {
            if (!this.signatureSelected) graphics.drawCenteredString(
                    this.font,
                    Component.translatable("questlog_envelope.editor.seal", this.sealSelection.label()),
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 286,
                    0xFFFFFF
            );
        } else {
            graphics.drawCenteredString(
                    this.font,
                    this.validationError,
                    this.panelX + this.panelWidth / 2,
                    this.panelY + 286,
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

        if (this.signatureSelected) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("questlog_envelope.editor.signature.drag_hint"),
                    this.letterLeft + 88, this.letterTop + 204, 0xFFB8B8B8);
        }
        if (this.magicCircle && !this.signatureSelected) {
            graphics.drawCenteredString(
                    this.font,
                    Component.translatable("questlog_envelope.editor.magic_circle.drag_hint"),
                    this.letterLeft + 88,
                    this.letterTop + 204,
                    0xFFB8B8B8
            );
            renderMagicCircleTooltips(graphics, mouseX, mouseY);
        }
    }

    private void drawCircleEditorOverlay(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            int mouseX,
            int mouseY
    ) {
        int edgeColor = 0xA0FFFFFF;
        graphics.fill(x - 1, y - 1, x + size + 1, y, edgeColor);
        graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, edgeColor);
        graphics.fill(x - 1, y, x, y + size, edgeColor);
        graphics.fill(x + size, y, x + size + 1, y + size, edgeColor);

        boolean resizeHovered = isInsideHandle(mouseX, mouseY, x, y);
        boolean moveHovered = isInsideHandle(mouseX, mouseY, x + size / 2, y + size / 2);
        drawHandle(
                graphics,
                x,
                y,
                false,
                resizeHovered || this.circleDragMode == CircleDragMode.RESIZE
        );
        drawHandle(
                graphics,
                x + size / 2,
                y + size / 2,
                true,
                moveHovered || this.circleDragMode == CircleDragMode.MOVE
        );

        if (this.circleDragMode == CircleDragMode.RESIZE) {
            Component value = Component.translatable(
                    "questlog_envelope.editor.magic_circle.size_value",
                    this.magicCircleSize,
                    this.magicCircleSize
            );
            int textWidth = this.font.width(value);
            int labelX = x + size / 2 - textWidth / 2;
            int labelY = Math.max(this.letterTop + 3, y - 12);
            graphics.fill(labelX - 3, labelY - 2, labelX + textWidth + 3, labelY + 10, 0xB0000000);
            graphics.drawString(this.font, value, labelX, labelY, 0xFFFFFF, false);
        }
    }

    private static void drawHandle(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            boolean centerHandle,
            boolean highlighted
    ) {
        graphics.fill(
                centerX - HANDLE_RADIUS - 1,
                centerY - HANDLE_RADIUS - 1,
                centerX + HANDLE_RADIUS + 2,
                centerY + HANDLE_RADIUS + 2,
                0xD0000000
        );
        graphics.fill(
                centerX - HANDLE_RADIUS,
                centerY - HANDLE_RADIUS,
                centerX + HANDLE_RADIUS + 1,
                centerY + HANDLE_RADIUS + 1,
                highlighted ? 0xFFFFD86A : 0xE0FFFFFF
        );
        if (centerHandle) {
            graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, 0xFF202020);
        } else {
            graphics.fill(centerX - 2, centerY - 2, centerX + 1, centerY + 1, 0xFF202020);
        }
    }

    private void renderMagicCircleTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.circleDragMode != CircleDragMode.NONE) {
            return;
        }

        if (isInsideHandle(mouseX, mouseY, circleScreenX(), circleScreenY())) {
            graphics.renderTooltip(
                    this.font,
                    Component.translatable("questlog_envelope.editor.magic_circle.resize_handle"),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isInsideHandle(mouseX, mouseY, circleCenterScreenX(), circleCenterScreenY())) {
            graphics.renderTooltip(
                    this.font,
                    Component.translatable("questlog_envelope.editor.magic_circle.move_handle"),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (this.magicCircleHoldBox != null && this.magicCircleHoldBox.isMouseOver(mouseX, mouseY)) {
            graphics.renderTooltip(
                    this.font,
                    Component.translatable(
                            "questlog_envelope.editor.magic_circle.hold_tooltip",
                            formatHoldSeconds(QuestMagicCircle.MIN_HOLD_MILLIS / 1000.0),
                            formatHoldSeconds(QuestMagicCircle.MAX_HOLD_MILLIS / 1000.0)
                    ),
                    mouseX,
                    mouseY
            );
        }
    }

    private enum CircleDragMode {
        NONE,
        MOVE,
        RESIZE
    }
}
