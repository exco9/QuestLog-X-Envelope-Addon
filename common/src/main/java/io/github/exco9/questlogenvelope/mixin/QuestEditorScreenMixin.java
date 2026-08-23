package io.github.exco9.questlogenvelope.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.exco9.questlogenvelope.client.LetterRewardEditorScreen;
import io.github.exco9.questlogenvelope.client.PackageRewardEditorScreen;
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

/**
 * Adds compact Envelope-specific editor entry points without replacing
 * Questlog's editor. Questlog remains responsible for type selection,
 * Quest-ID autocomplete, reward display name and icon.
 */
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
    NoShadowEditBox entryIconBox;

    protected QuestEditorScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "buildRightPageEditEntry", at = @At("TAIL"), remap = false)
    private void questlogEnvelope$addMailOptions(int panel2X, int panel2Y, CallbackInfo ci) {
        if (!questlogEnvelope$isMailReward(this.editingType) || this.editingEntry == null) {
            return;
        }

        boolean packageReward = "questlog_envelope:package".equals(this.editingType);
        Component label = Component.translatable(packageReward
                ? "questlog_envelope.editor.package.options"
                : "questlog_envelope.editor.letter.options");

        Button options = Button.builder(label, button -> {
            // Work on a detached copy. If the player later presses Questlog's
            // Cancel button, the original list entry is still untouched.
            JsonObject workingCopy = this.editingEntry.deepCopy();
            questlogEnvelope$stashQuestlogFields(workingCopy);
            this.editingEntry = workingCopy;

            QuestEditorScreen parent = (QuestEditorScreen) (Object) this;
            Screen editor = packageReward
                    ? new PackageRewardEditorScreen(parent, workingCopy)
                    : new LetterRewardEditorScreen(parent, workingCopy);
            Minecraft.getInstance().setScreen(editor);
        }).bounds(panel2X + 15, panel2Y + 138, 130, 16).build();

        this.addRenderableWidget(options);
    }

    /**
     * Questlog strips only the built-in "questlog:" namespace when drawing the
     * type picker. Keep our real ResourceLocation untouched, but replace the
     * rendered text with a short localized label so it fits the 130px list.
     */
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
            default -> normal;
        };
    }

    /**
     * Questlog knows how to save custom target keys, but its generic cleanup
     * list does not know our `grants_quest` key. Explicitly remove it when the
     * editor field is cleared.
     */
    @Inject(method = "saveEditingEntry", at = @At("HEAD"), remap = false)
    private void questlogEnvelope$clearEmptyGrantedQuest(CallbackInfo ci) {
        if (questlogEnvelope$isMailReward(this.editingType)
                && this.editingEntry != null
                && this.entryTargetBox != null
                && this.entryTargetBox.getValue().trim().isEmpty()) {
            this.editingEntry.remove("grants_quest");
        }
    }

    /**
     * Switching screens causes Questlog to rebuild its widgets. Preserve the
     * currently typed generic fields in a detached working copy first.
     */
    private void questlogEnvelope$stashQuestlogFields(JsonObject target) {
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
            String grantedQuest = this.entryTargetBox.getValue().trim();
            if (grantedQuest.isEmpty()) {
                target.remove("grants_quest");
            } else {
                target.addProperty("grants_quest", grantedQuest);
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
