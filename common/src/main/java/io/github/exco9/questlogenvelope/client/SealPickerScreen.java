package io.github.exco9.questlogenvelope.client;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/** Compact native-Envelope wax seal selector for reward entries. */
public final class SealPickerScreen extends Screen {
    private final Screen parent;
    private final JsonObject rewardEntry;
    private final SealSelection selection;

    public SealPickerScreen(Screen parent, JsonObject rewardEntry) {
        super(Component.translatable("questlog_envelope.editor.seal.title"));
        this.parent = parent;
        this.rewardEntry = rewardEntry;
        this.selection = new SealSelection(
                rewardEntry.has("seal") ? rewardEntry.get("seal").getAsString() : null
        );
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int top = Math.max(20, this.height / 2 - 70);

        this.addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> this.selection.previous()
        ).bounds(centerX - 92, top + 46, 38, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("questlog_envelope.editor.seal.none"),
                button -> this.selection.clear()
        ).bounds(centerX - 48, top + 46, 96, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> this.selection.next()
        ).bounds(centerX + 54, top + 46, 38, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.cancel"),
                button -> onClose()
        ).bounds(centerX - 92, top + 108, 88, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> saveAndClose()
        ).bounds(centerX + 4, top + 108, 88, 20).build());
    }

    private void saveAndClose() {
        ResourceLocation seal = this.selection.get();
        if (seal == null) {
            this.rewardEntry.remove("seal");
        } else {
            this.rewardEntry.addProperty("seal", seal.toString());
        }

        // Legacy field from the old, incorrect wax-seal implementation.
        this.rewardEntry.remove("magic_seal");

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
        int top = Math.max(20, this.height / 2 - 70);
        graphics.fill(centerX - 104, top - 10, centerX + 104, top + 140, 0xD0101010);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int top = Math.max(20, this.height / 2 - 70);
        graphics.drawCenteredString(this.font, this.title, centerX, top, 0xFFFFFF);

        this.selection.renderPreview(graphics, centerX - 15, top + 12);
        graphics.drawCenteredString(
                this.font,
                Component.translatable("questlog_envelope.editor.seal", this.selection.label()),
                centerX,
                top + 80,
                0xFFFFFF
        );
    }
}
