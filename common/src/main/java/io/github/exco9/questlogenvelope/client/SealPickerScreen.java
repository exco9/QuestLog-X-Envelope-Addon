package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Compact native-Envelope seal selector for reward entries. */
public final class SealPickerScreen extends Screen {
    private final Screen parent;
    private final JsonObject rewardEntry;
    private final SealSelection selection;
    private boolean magicSeal;

    public SealPickerScreen(Screen parent, JsonObject rewardEntry) {
        super(Component.translatable("questlog_envelope.editor.seal.title"));
        this.parent = parent;
        this.rewardEntry = rewardEntry;
        this.selection = new SealSelection(
                rewardEntry.has("seal") ? rewardEntry.get("seal").getAsString() : null
        );
        this.magicSeal = rewardEntry.has("magic_seal") && rewardEntry.get("magic_seal").getAsBoolean();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = Math.max(20, this.height / 2 - 88);

        this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> this.selection.previous()
        ).bounds(centerX - 92, top + 46, 38, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.seal.none"),
                button -> {
                    this.selection.clear();
                    this.magicSeal = false;
                }
        ).bounds(centerX - 48, top + 46, 96, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> this.selection.next()
        ).bounds(centerX + 54, top + 46, 38, 20).build());

        this.addRenderableWidget(Button.builder(magicSealLabel(), button -> {
            this.magicSeal = !this.magicSeal;
            if (this.magicSeal && this.selection.get() == null) {
                this.selection.next();
            }
            button.setMessage(magicSealLabel());
        }).bounds(centerX - 92, top + 102, 184, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(centerX - 92, top + 144, 88, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(centerX + 4, top + 144, 88, 20).build());
    }

    private Component magicSealLabel() {
        return Component.translatable(
                "questlog_envelope.editor.magic_seal",
                Component.translatable(this.magicSeal ? "options.on" : "options.off")
        );
    }

    private void saveAndClose() {
        ResourceLocation seal = this.selection.get();
        if (seal == null) {
            this.rewardEntry.remove("seal");
            this.rewardEntry.remove("magic_seal");
        } else {
            this.rewardEntry.addProperty("seal", seal.toString());
            if (this.magicSeal) {
                this.rewardEntry.addProperty("magic_seal", true);
            } else {
                this.rewardEntry.remove("magic_seal");
            }
        }

        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
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
        int centerX = this.width / 2;
        int top = Math.max(20, this.height / 2 - 88);
        graphics.fill(centerX - 104, top - 10, centerX + 104, top + 176, 0xD0101010);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int top = Math.max(20, this.height / 2 - 88);
        graphics.drawCenteredString(this.font, this.title, centerX, top, 0xFFFFFF);

        this.selection.renderPreview(graphics, centerX - 15, top + 12);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.seal", this.selection.label()),
                centerX,
                top + 80,
                0xFFFFFF
        );
        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.magic_seal.hint"),
                centerX,
                top + 126,
                0xFFB0B0B0
        );
    }
}
