package io.github.exco9.questlogenvelope.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.exco9.questlogenvelope.client.LetterRewardEditorScreen;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a compact Envelope-specific editor entry point without replacing
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
    private void questlogEnvelope$addLetterOptions(int panel2X, int panel2Y, CallbackInfo ci) {
        if (!"questlog_envelope:letter".equals(this.editingType) || this.editingEntry == null) {
            return;
        }

        Button options = Button.builder(
                Component.translatable("questlog_envelope.editor.letter.options"),
                button -> {
                    questlogEnvelope$stashQuestlogFields();
                    Minecraft.getInstance().setScreen(new LetterRewardEditorScreen(
                            (QuestEditorScreen) (Object) this,
                            this.editingEntry
                    ));
                }
        ).bounds(panel2X + 15, panel2Y + 138, 130, 16).build();

        this.addRenderableWidget(options);
    }

    /**
     * Switching screens causes Questlog to rebuild its widgets. Preserve the
     * currently typed generic fields in the shared JsonObject first, without
     * committing the reward to the quest list (so Questlog's Cancel semantics
     * remain intact).
     */
    private void questlogEnvelope$stashQuestlogFields() {
        if (this.editingEntry == null) {
            return;
        }

        this.editingEntry.addProperty("type", this.editingType);

        if (this.entryNameBox != null) {
            String name = this.entryNameBox.getValue().trim();
            if (name.isEmpty()) {
                this.editingEntry.remove("name");
            } else {
                this.editingEntry.addProperty("name", name);
            }
        }

        if (this.entryTargetBox != null) {
            String grantedQuest = this.entryTargetBox.getValue().trim();
            if (grantedQuest.isEmpty()) {
                this.editingEntry.remove("grants_quest");
            } else {
                this.editingEntry.addProperty("grants_quest", grantedQuest);
            }
        }

        this.editingEntry.remove("icon");
        if (this.entryIconBox != null) {
            String icon = this.entryIconBox.getValue().trim();
            if (!icon.isEmpty()) {
                if (icon.startsWith("{") && icon.endsWith("}")) {
                    try {
                        JsonElement parsed = JsonParser.parseString(icon);
                        this.editingEntry.add("icon", parsed);
                    } catch (Exception ignored) {
                        JsonObject iconObject = new JsonObject();
                        iconObject.addProperty("item", icon);
                        this.editingEntry.add("icon", iconObject);
                    }
                } else if (icon.contains("textures/") || icon.endsWith(".png")) {
                    JsonObject iconObject = new JsonObject();
                    iconObject.addProperty("texture", icon);
                    this.editingEntry.add("icon", iconObject);
                } else {
                    JsonObject iconObject = new JsonObject();
                    iconObject.addProperty("item", icon);
                    this.editingEntry.add("icon", iconObject);
                }
            }
        }
    }
}
