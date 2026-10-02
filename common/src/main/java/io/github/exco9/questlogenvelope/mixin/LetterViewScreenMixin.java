package io.github.exco9.questlogenvelope.mixin;

import io.github.exco9.questlogenvelope.client.MagicCircleTexture;
import io.github.exco9.questlogenvelope.client.SignatureRenderer;
import io.github.exco9.questlogenvelope.client.SignatureSigningSession;
import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.mail.SignatureActions;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import io.github.exco9.questlogenvelope.network.MagicCircleNetworking;
import io.github.exco9.questlogenvelope.network.SignatureNetworking;
import io.github.mortuusars.envelope.client.gui.screen.LetterViewScreen;
import io.github.mortuusars.envelope.util.ItemAndStack;
import io.github.mortuusars.envelope.world.item.LetterItem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
    private boolean questlogEnvelope$circleStateObserved;

    @Unique
    private long questlogEnvelope$holdStartedAt;

    @Unique
    private long questlogEnvelope$activationRequestSentAt;

    @Unique
    private long questlogEnvelope$activationFlashStartedAt;

    @Unique
    private final SignatureSigningSession questlogEnvelope$writing = new SignatureSigningSession();

    @Unique
    private @Nullable SoundInstance questlogEnvelope$writingSound;

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void questlogEnvelope$renderMagicCircle(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        LetterSignature signature = LetterSignature.read(this.letter.getItemStack());
        long signatureNow = Util.getMillis();
        boolean signed = questlogEnvelope$isSignatureSigned(signature);
        this.questlogEnvelope$writing.observeSigned(signatureNow, signed);
        if (this.questlogEnvelope$writing.shouldCommit(signatureNow)) {
            if (questlogEnvelope$isCurrentLetter(signature)) {
                SignatureNetworking.sign(this.hand, signature,
                        SignatureActions.getActionId(this.letter.getItemStack()).map(UUID::toString).orElse(""));
                this.questlogEnvelope$writing.requested(signatureNow);
            } else {
                this.questlogEnvelope$writing.reset();
            }
        }
        if (!this.questlogEnvelope$writing.isWriting(signatureNow) && this.questlogEnvelope$writingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(this.questlogEnvelope$writingSound);
            this.questlogEnvelope$writingSound = null;
        }
        SignatureRenderer.Bounds signatureBox = SignatureRenderer.renderReceived(graphics, Minecraft.getInstance().font,
                signature, this.leftPos + 17, this.topPos + 21, this.questlogEnvelope$writing.progress(signatureNow),
                this.questlogEnvelope$writing.isWriting(signatureNow), signed || this.questlogEnvelope$writing.isPending(signatureNow), signed,
                this.questlogEnvelope$writing.magicProgress(signatureNow),
                this.questlogEnvelope$writing.particleProgress(signatureNow), signatureNow);
        if (signature.enabled() && signatureBox.contains(mouseX, mouseY)
                && (!QuestMagicCircle.has(this.letter.getItemStack()) || !questlogEnvelope$isInsideCircle(mouseX, mouseY))
                && !this.questlogEnvelope$writing.isWriting(signatureNow)) {
            String hint = signed ? "signed" : this.questlogEnvelope$writing.isPending(signatureNow) ? "pending"
                    : questlogEnvelope$isCurrentLetter(signature) ? "write_hint" : "hold_hint";
            graphics.renderTooltip(Minecraft.getInstance().font,
                    Component.translatable("questlog_envelope.signature." + hint), mouseX, mouseY);
        }
        Optional<UUID> actionId = QuestMagicCircle.getActionId(this.letter.getItemStack());
        if (actionId.isEmpty()) {
            questlogEnvelope$resetAllState();
            return;
        }

        int size = questlogEnvelope$circleSize();
        int x = questlogEnvelope$circleX();
        int y = questlogEnvelope$circleY();
        int color = QuestMagicCircle.getColor(questlogEnvelope$circleStack());
        int magicColor = QuestMagicCircle.getMagicColor(questlogEnvelope$circleStack());
        long now = Util.getMillis();

        MagicCircleTexture.render(graphics, x, y, size);

        boolean activated = QuestMagicCircle.isActivated(questlogEnvelope$circleStack());
        if (activated) {
            if (!this.questlogEnvelope$wasActivated && this.questlogEnvelope$circleStateObserved) {
                this.questlogEnvelope$activationFlashStartedAt = now;
            }
            this.questlogEnvelope$wasActivated = true;
            this.questlogEnvelope$circleStateObserved = true;
            this.questlogEnvelope$activationSent = false;
            questlogEnvelope$resetHold();

            long age = now - this.questlogEnvelope$activationFlashStartedAt;
            float fade = this.questlogEnvelope$activationFlashStartedAt == 0 ? 1 : Math.min(1, Math.max(0, age / 650F));
            float particles = this.questlogEnvelope$activationFlashStartedAt == 0 ? -1 : age / 850F;
            MagicCircleTexture.renderActivated(graphics, x, y, size, color, magicColor, fade, particles, now);
            long flashAge = now - this.questlogEnvelope$activationFlashStartedAt;
            if (flashAge >= 0L && flashAge < QUESTLOG_ENVELOPE$ACTIVATION_FLASH_MS) {
                float pulse = 1.0F - flashAge / (float) QUESTLOG_ENVELOPE$ACTIVATION_FLASH_MS;
                MagicCircleTexture.renderGlow(graphics, x, y, size, magicColor, pulse);
            }
            return;
        }
        this.questlogEnvelope$wasActivated = false;
        this.questlogEnvelope$circleStateObserved = true;

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
        if (button != 0) {
            return;
        }

        // Circle actions retain priority if the two decorations overlap.
        if (QuestMagicCircle.has(this.letter.getItemStack()) && questlogEnvelope$isInsideCircle(mouseX, mouseY)) {
            if (this.hand == null || QuestMagicCircle.isActivated(questlogEnvelope$circleStack())
                    || this.questlogEnvelope$activationSent) return;
            this.questlogEnvelope$holdingCircle = true;
            this.questlogEnvelope$holdStartedAt = Util.getMillis();
            cir.setReturnValue(true);
            return;
        }

        LetterSignature signature = LetterSignature.read(this.letter.getItemStack());
        if (signature.enabled() && SignatureRenderer.bounds(Minecraft.getInstance().font, signature,
                this.leftPos + 17, this.topPos + 21).contains(mouseX, mouseY)) {
            if (questlogEnvelope$isCurrentLetter(signature)
                    && this.questlogEnvelope$writing.start(Util.getMillis(), questlogEnvelope$isSignatureSigned(signature))) {
                var sounds = Minecraft.getInstance().getSoundManager();
                if (this.questlogEnvelope$writingSound != null) sounds.stop(this.questlogEnvelope$writingSound);
                this.questlogEnvelope$writingSound = SimpleSoundInstance.forUI(
                        SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0F, 0.45F);
                sounds.play(this.questlogEnvelope$writingSound);
            }
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean questlogEnvelope$isCurrentLetter(LetterSignature signature) {
        var player = Minecraft.getInstance().player;
        return this.hand != null && player != null
                && player.getItemInHand(this.hand).getItem() instanceof LetterItem
                && LetterSignature.read(player.getItemInHand(this.hand)).equals(signature)
                && SignatureActions.getActionId(player.getItemInHand(this.hand))
                        .equals(SignatureActions.getActionId(this.letter.getItemStack()));
    }

    @Unique
    private boolean questlogEnvelope$isSignatureSigned(LetterSignature signature) {
        if (LetterSignature.isSigned(this.letter.getItemStack())) return true;
        return questlogEnvelope$isCurrentLetter(signature)
                && LetterSignature.isSigned(Minecraft.getInstance().player.getItemInHand(this.hand));
    }

    @Inject(method = "onClose", at = @At("HEAD"), remap = false)
    private void questlogEnvelope$stopWriting(CallbackInfo ci) {
        this.questlogEnvelope$writing.reset();
        if (this.questlogEnvelope$writingSound != null) {
            Minecraft.getInstance().getSoundManager().stop(this.questlogEnvelope$writingSound);
            this.questlogEnvelope$writingSound = null;
        }
    }

    @Unique
    private ItemStack questlogEnvelope$circleStack() {
        var player = Minecraft.getInstance().player;
        if (player != null && this.hand != null) {
            ItemStack held = player.getItemInHand(this.hand);
            var id = QuestMagicCircle.getActionId(this.letter.getItemStack());
            if (id.isPresent() && id.equals(QuestMagicCircle.getActionId(held))) return held;
        }
        return this.letter.getItemStack();
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
        this.questlogEnvelope$circleStateObserved = false;
        this.questlogEnvelope$activationRequestSentAt = 0L;
        this.questlogEnvelope$activationFlashStartedAt = 0L;
    }
}
