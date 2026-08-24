package io.github.exco9.questlogenvelope.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.exco9.questlogenvelope.client.LetterRewardEditorScreen;
import io.github.exco9.questlogenvelope.client.MailSentObjectiveEditorScreen;
import io.github.exco9.questlogenvelope.client.PackageRewardEditorScreen;
import io.github.exco9.questlogenvelope.client.SealPickerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.infernalstudios.questlog.client.gui.components.NoShadowEditBox;
import org.infernalstudios.questlog.client.gui.screen.QuestEditorScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds compact Envelope-specific editor entry points without replacing Questlog's editor. */
@Mixin(value = QuestEditorScreen.class, remap = false)
public abstract class QuestEditorScreenMixin extends Screen {
    @Shadow(remap = false)
    String editingType;

    @Shadow(remap = false)
    @Nullable
    JsonObject editingEntry;

    @Shadow(remap = false)
    NoShadowEditBox entryNameBox;

    @Shadow(remap = false)
    NoShadowEditBox entryTargetBox;

    @Shadow(remap = false)
    NoShadowEditBox entryAmountBox;

    @Shadow(remap = false)
    NoShadowEditBox entryIconBox;

    protected QuestEditorScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "buildRightPageEditEntry", at = @At("TAIL"), remap = false)
    private void questlogEnvelope$addMailOptions(int panel2X, int panel2Y, CallbackInfo ci) {
        if (this.editingEntry == null) {
            return;
        }

        if (questlogEnvelope$isMailReward(this.editingType)) {
            boolean packageReward = "questlog_envelope:package".equals(this.editingType);
            int optionsWidth = packageReward ? 78 : 130;
            Component label = Component.translatable(packageReward
                    ? "questlog_envelope.editor.package.options_short"
                    : "questlog_envelope.editor.letter.options");

            this.addRenderableWidget(Button.builder(label, button -> {
                JsonObject workingCopy = questlogEnvelope$createWorkingCopy("grants_quest", false);
                QuestEditorScreen parent = (QuestEditorScreen) (Object) this;
                Screen editor = packageReward
                        ? new PackageRewardEditorScreen(parent, workingCopy)
                        : new LetterRewardEditorScreen(parent, workingCopy);
                Minecraft.getInstance().setScreen(editor);
            }).bounds(panel2X + 15, panel2Y + 138, optionsWidth, 16).build());

            if (packageReward) {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("questlog_envelope.editor.seal.button"),
                        button -> {
                            JsonObject workingCopy = questlogEnvelope$createWorkingCopy("grants_quest", false);
                            QuestEditorScreen parent = (QuestEditorScreen) (Object) this;
                            Minecraft.getInstance().setScreen(new SealPickerScreen(parent, workingCopy));
                        }
                ).bounds(panel2X + 97, panel2Y + 138, 48, 16).build());
            }
            return;
        }

        if ("questlog_envelope:mail_sent".equals(this.editingType)) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("questlog_envelope.editor.mail_sent.options"),
                    button -> {
                        JsonObject workingCopy = questlogEnvelope$createWorkingCopy("recipient", true);
                        QuestEditorScreen parent = (QuestEditorScreen) (Object) this;
                        Minecraft.getInstance().setScreen(new MailSentObjectiveEditorScreen(parent, workingCopy));
                    }
            ).bounds(panel2X + 15, panel2Y + 138, 130, 16).build());
        }
    }

    private JsonObject questlogEnvelope$createWorkingCopy(String targetKey, boolean includeAmount) {
        JsonObject workingCopy = this.editingEntry == null
                ? new JsonObject()
                : this.editingEntry.deepCopy();
        questlogEnvelope$stashQuestlogFields(workingCopy, targetKey, includeAmount);
        this.editingEntry = workingCopy;
        return workingCopy;
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;"
            ),
            remap = false
    )
    private String questlogEnvelope$shortRenderedTypeName(
            String value,
            CharSequence target,
            CharSequence replacement
    ) {
        String normal = value.replace(target, replacement);
        if (!"questlog:".contentEquals(target) || replacement.length() != 0) {
            return normal;
        }

        return switch (value) {
            case "questlog_envelope:letter" ->
                    Component.translatable("questlog_envelope.editor.type.letter").getString();
            case "questlog_envelope:package" ->
                    Component.translatable("questlog_envelope.editor.type.package").getString();
            case "questlog_envelope:mail_received" ->
                    Component.translatable("questlog_envelope.editor.type.mail_received").getString();
            case "questlog_envelope:mail_sent" ->
                    Component.translatable("questlog_envelope.editor.type.mail_sent").getString();
            default -> normal;
        };
    }

    @Inject(method = "saveEditingEntry", at = @At("HEAD"), remap = false)
    private void questlogEnvelope$clearEmptyTargets(CallbackInfo ci) {
        if (this.editingEntry == null || this.entryTargetBox == null) {
            return;
        }

        if (questlogEnvelope$isMailReward(this.editingType)
                && this.entryTargetBox.getValue().trim().isEmpty()) {
            this.editingEntry.remove("grants_quest");
        }

        if ("questlog_envelope:mail_sent".equals(this.editingType)
                && this.entryTargetBox.getValue().trim().isEmpty()) {
            this.editingEntry.remove("recipient");
        }
    }

    private void questlogEnvelope$stashQuestlogFields(
            JsonObject target,
            String targetKey,
            boolean includeAmount
    ) {
        target.addProperty("type", this.editingType);

        if (this.entryNameBox != null) {
            String name = this.entryNameBox.getValue().trim();
            if (name.isEmpty()) {
                target.remove("name");
            } else {
                target.addProperty("name", name);
            }
        }

        if (this.entryTargetBox != null) {
            String value = this.entryTargetBox.getValue().trim();
            if (value.isEmpty()) {
                target.remove(targetKey);
            } else {
                target.addProperty(targetKey, value);
            }
        }

        if (includeAmount && this.entryAmountBox != null) {
            String amount = this.entryAmountBox.getValue().trim();
            try {
                int requiredAmount = Math.max(1, Integer.parseInt(amount));
                if (requiredAmount == 1) {
                    target.remove("required_amount");
                } else {
                    target.addProperty("required_amount", requiredAmount);
                }
            } catch (NumberFormatException ignored) {
                // Questlog will keep handling validation for its own amount field.
            }
        }

        target.remove("icon");
        if (this.entryIconBox != null) {
            String icon = this.entryIconBox.getValue().trim();
            if (!icon.isEmpty()) {
                if (icon.startsWith("{") && icon.endsWith("}")) {
                    try {
                        JsonElement parsed = JsonParser.parseString(icon);
                        target.add("icon", parsed);
                    } catch (Exception ignored) {
                        JsonObject iconObject = new JsonObject();
                        iconObject.addProperty("item", icon);
                        target.add("icon", iconObject);
                    }
                } else if (icon.contains("textures/") || icon.endsWith(".png")) {
                    JsonObject iconObject = new JsonObject();
                    iconObject.addProperty("texture", icon);
                    target.add("icon", iconObject);
                } else {
                    JsonObject iconObject = new JsonObject();
                    iconObject.addProperty("item", icon);
                    target.add("icon", iconObject);
                }
            }
        }
    }

    private static boolean questlogEnvelope$isMailReward(String type) {
        return "questlog_envelope:letter".equals(type) || "questlog_envelope:package".equals(type);
    }
}
