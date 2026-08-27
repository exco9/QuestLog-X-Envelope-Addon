package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.client.MagicCircleTexture;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import io.github.mortuusars.envelope.client.gui.screen.LetterViewScreen;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

/** Draws an independent magic circle on received letters and handles hold activation. */
@Mixin(value = LetterViewScreen.class, remap = false)
public abstract class LetterViewScreenMixin {
    @Unique
    private static final long QUESTLOG_ENVELOPE$HOLD_TIME_MS = 3000L;

    @Shadow(remap = false)
    @Final
    protected ItemAndStack<LetterItem> letter;

    @Shadow(remap = false)
    @Final
    @Nullable
    protected InteractionHand hand;

    @Shadow(remap = false)
    protected int leftPos;

    @Shadow(remap = false)
    protected int topPos;

    @Unique
    private boolean questlogEnvelope$holdingCircle;

    @Unique
    private boolean questlogEnvelope$activationSent;

    @Unique
    private long questlogEnvelope$holdStartedAt;

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void questlogEnvelope$renderMagicCircle(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        Optional<UUID> actionId = QuestMagicCircle.getActionId(this.letter.getItemStack());
        if (actionId.isEmpty()) {
            questlogEnvelope$resetHold();
            return;
        }

        int x = questlogEnvelope$circleX();
        int y = questlogEnvelope$circleY();
        MagicCircleTexture.render(graphics, x, y);

        if (!this.questlogEnvelope$holdingCircle) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean leftButtonStillDown = GLFW.glfwGetMouseButton(
                minecraft.getWindow().getWindow(),
                GLFW.GLFW_MOUSE_BUTTON_LEFT
        ) == GLFW.GLFW_PRESS;

        if (!leftButtonStillDown || !questlogEnvelope$isInsideCircle(mouseX, mouseY)) {
            questlogEnvelope$resetHold();
            return;
        }

        long elapsed = Math.max(0L, Util.getMillis() - this.questlogEnvelope$holdStartedAt);
        float progress = Math.min(1.0F, elapsed / (float) QUESTLOG_ENVELOPE$HOLD_TIME_MS);

        int progressWidth = Math.round(MagicCircleTexture.DISPLAY_SIZE * progress);
        graphics.fill(
                x,
                y + MagicCircleTexture.DISPLAY_SIZE - 3,
                x + progressWidth,
                y + MagicCircleTexture.DISPLAY_SIZE,
                0xD0FFFFFF
        );

        if (progress >= 1.0F && !this.questlogEnvelope$activationSent && this.hand != null) {
            if (minecraft.player != null && minecraft.player.connection != null) {
                this.questlogEnvelope$activationSent = true;
                minecraft.player.connection.sendCommand(
                        "questlog_envelope magic_circle activate " + actionId.get()
                );
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void questlogEnvelope$startMagicCircleHold(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (button != 0 || this.hand == null || !questlogEnvelope$isInsideCircle(mouseX, mouseY)) {
            return;
        }

        if (!QuestMagicCircle.has(this.letter.getItemStack())) {
            return;
        }

        this.questlogEnvelope$holdingCircle = true;
        this.questlogEnvelope$activationSent = false;
        this.questlogEnvelope$holdStartedAt = Util.getMillis();
        cir.setReturnValue(true);
    }

    @Unique
    private int questlogEnvelope$circleX() {
        return this.leftPos + 17 + 142 - MagicCircleTexture.DISPLAY_SIZE - 3;
    }

    @Unique
    private int questlogEnvelope$circleY() {
        return this.topPos + 21 + 144 - MagicCircleTexture.DISPLAY_SIZE - 3;
    }

    @Unique
    private boolean questlogEnvelope$isInsideCircle(double mouseX, double mouseY) {
        int x = questlogEnvelope$circleX();
        int y = questlogEnvelope$circleY();
        return mouseX >= x
                && mouseX < x + MagicCircleTexture.DISPLAY_SIZE
                && mouseY >= y
                && mouseY < y + MagicCircleTexture.DISPLAY_SIZE;
    }

    @Unique
    private void questlogEnvelope$resetHold() {
        this.questlogEnvelope$holdingCircle = false;
        this.questlogEnvelope$activationSent = false;
        this.questlogEnvelope$holdStartedAt = 0L;
    }
}
