package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.client.MagicCircleTexture;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import io.github.exco9.questlogenvelope.network.MagicCircleNetworking;
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
    private static final long QUESTLOG_ENVELOPE$REQUEST_VISUAL_TIMEOUT_MS = 1500L;
    @Unique
    private static final long QUESTLOG_ENVELOPE$ACTIVATION_FLASH_MS = 550L;

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
    private boolean questlogEnvelope$wasActivated;

    @Unique
    private long questlogEnvelope$holdStartedAt;

    @Unique
    private long questlogEnvelope$activationRequestSentAt;

    @Unique
    private long questlogEnvelope$activationFlashStartedAt;

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
            questlogEnvelope$resetAllState();
            return;
        }

        int size = questlogEnvelope$circleSize();
        int x = questlogEnvelope$circleX();
        int y = questlogEnvelope$circleY();
        int color = QuestMagicCircle.getColor(this.letter.getItemStack());
        long now = Util.getMillis();

        MagicCircleTexture.render(graphics, x, y, size);

        boolean activated = QuestMagicCircle.isActivated(this.letter.getItemStack());
        if (activated) {
            if (!this.questlogEnvelope$wasActivated) {
                this.questlogEnvelope$activationFlashStartedAt = now;
            }
            this.questlogEnvelope$wasActivated = true;
            this.questlogEnvelope$activationSent = false;
            questlogEnvelope$resetHold();

            MagicCircleTexture.renderTintedFill(graphics, x, y, size, 1.0F, color);
            long flashAge = now - this.questlogEnvelope$activationFlashStartedAt;
            if (flashAge >= 0L && flashAge < QUESTLOG_ENVELOPE$ACTIVATION_FLASH_MS) {
                float pulse = 1.0F - flashAge / (float) QUESTLOG_ENVELOPE$ACTIVATION_FLASH_MS;
                MagicCircleTexture.renderGlow(graphics, x, y, size, color, pulse);
            }
            return;
        }
        this.questlogEnvelope$wasActivated = false;

        // Keep the circle visually complete for a short round-trip window after
        // sending the payload. If the server rejects it, the client naturally
        // falls back to the inactive state and the user can retry.
        if (this.questlogEnvelope$activationSent) {
            if (now - this.questlogEnvelope$activationRequestSentAt
                    <= QUESTLOG_ENVELOPE$REQUEST_VISUAL_TIMEOUT_MS) {
                MagicCircleTexture.renderTintedFill(graphics, x, y, size, 1.0F, color);
                return;
            }
            this.questlogEnvelope$activationSent = false;
        }

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

        long elapsed = Math.max(0L, now - this.questlogEnvelope$holdStartedAt);
        int holdMillis = QuestMagicCircle.getHoldMillis(this.letter.getItemStack());
        float progress = Math.min(1.0F, elapsed / (float) holdMillis);

        MagicCircleTexture.renderTintedFill(
                graphics,
                x,
                y,
                size,
                progress,
                color
        );

        if (progress >= 1.0F && !this.questlogEnvelope$activationSent && this.hand != null) {
            this.questlogEnvelope$activationSent = true;
            this.questlogEnvelope$activationRequestSentAt = now;
            questlogEnvelope$resetHold();
            MagicCircleNetworking.sendActivation(actionId.get());
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

        if (!QuestMagicCircle.has(this.letter.getItemStack())
                || QuestMagicCircle.isActivated(this.letter.getItemStack())
                || this.questlogEnvelope$activationSent) {
            return;
        }

        this.questlogEnvelope$holdingCircle = true;
        this.questlogEnvelope$holdStartedAt = Util.getMillis();
        cir.setReturnValue(true);
    }

    @Unique
    private int questlogEnvelope$circleSize() {
        return QuestMagicCircle.getSize(this.letter.getItemStack());
    }

    @Unique
    private int questlogEnvelope$circleX() {
        return this.leftPos + 17 + QuestMagicCircle.getXOffset(this.letter.getItemStack());
    }

    @Unique
    private int questlogEnvelope$circleY() {
        return this.topPos + 21 + QuestMagicCircle.getYOffset(this.letter.getItemStack());
    }

    @Unique
    private boolean questlogEnvelope$isInsideCircle(double mouseX, double mouseY) {
        int x = questlogEnvelope$circleX();
        int y = questlogEnvelope$circleY();
        int size = questlogEnvelope$circleSize();
        return mouseX >= x
                && mouseX < x + size
                && mouseY >= y
                && mouseY < y + size;
    }

    @Unique
    private void questlogEnvelope$resetHold() {
        this.questlogEnvelope$holdingCircle = false;
        this.questlogEnvelope$holdStartedAt = 0L;
    }

    @Unique
    private void questlogEnvelope$resetAllState() {
        questlogEnvelope$resetHold();
        this.questlogEnvelope$activationSent = false;
        this.questlogEnvelope$wasActivated = false;
        this.questlogEnvelope$activationRequestSentAt = 0L;
        this.questlogEnvelope$activationFlashStartedAt = 0L;
    }
}
