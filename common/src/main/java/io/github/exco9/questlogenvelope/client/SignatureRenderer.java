package io.github.exco9.questlogenvelope.client;

import io.github.exco9.questlogenvelope.mail.LetterSignature;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.math.Axis;

/** Shared layout ensures the editor and delivered paper render identically. */
public final class SignatureRenderer {
    private static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath("questlog_envelope", "signature");
    private static final int PADDING = 2;

    private SignatureRenderer() { }

    private static Component text(LetterSignature signature) {
        return Component.literal(signature.text()).setStyle(Style.EMPTY.withFont(FONT));
    }

    public static Bounds bounds(Font font, LetterSignature signature, int paperX, int paperY) {
        return layout(font.width(text(signature)), signature, paperX, paperY,
                Math.max(1, (int) Minecraft.getInstance().getWindow().getGuiScale()));
    }

    public static Bounds layout(int measuredTextWidth, LetterSignature signature, int paperX, int paperY) {
        return layout(measuredTextWidth, signature, paperX, paperY, 2);
    }

    public static Bounds layout(int measuredTextWidth, LetterSignature signature, int paperX, int paperY, int guiScale) {
        int textWidth = Math.max(1, measuredTextWidth);
        float scale = Math.min(signature.size() / 9.0F,
                (QuestMagicCircle.WRITABLE_WIDTH - PADDING * 2) / (float) textWidth);
        int screenScale = Math.max(1, guiScale);
        float pixelScale = (float) Math.floor(scale * screenScale) / screenScale;
        // Preserve fitting for very long names; otherwise each atlas pixel maps to whole screen pixels.
        if (pixelScale > 0) scale = pixelScale;
        int width = Math.min(QuestMagicCircle.WRITABLE_WIDTH, (int) Math.ceil(textWidth * scale) + PADDING * 2);
        // One compact line, including BitScript's descenders.
        int height = (int) Math.ceil(9 * scale) + PADDING * 2;
        return new Bounds(paperX + Math.min(signature.x(), QuestMagicCircle.WRITABLE_WIDTH - width),
                paperY + Math.min(signature.y(), QuestMagicCircle.WRITABLE_HEIGHT - height), width, height, scale);
    }

    public static Bounds render(GuiGraphics graphics, Font font, LetterSignature signature, int paperX, int paperY) {
        return renderWriting(graphics, font, signature, paperX, paperY, 1.0F);
    }

    public static Bounds renderWriting(GuiGraphics graphics, Font font, LetterSignature signature,
                                       int paperX, int paperY, float progress) {
        return renderLayer(graphics, font, signature, paperX, paperY, progress, 255, true, signature.color());
    }

    public static Bounds renderReceived(GuiGraphics graphics, Font font, LetterSignature signature,
                                        int paperX, int paperY, float progress, boolean writing, boolean visible,
                                        boolean signed, float magicProgress, float particleProgress, long now) {
        if (visible && !writing) {
            int ink = signed ? SignatureEffects.blend(signature.color(), signature.magicColor(), magicProgress) : signature.color();
            Bounds box = renderLayer(graphics, font, signature, paperX, paperY, 1, 255, false, ink);
            if (signed && signature.enabled()) {
                renderShine(graphics, font, signature, box, now, magicProgress);
                renderParticles(graphics, signature.magicColor(), box, particleProgress);
            }
            return box;
        }
        Bounds box = renderLayer(graphics, font, signature, paperX, paperY, 1.0F, 90, false, signature.color());
        if (writing) renderWriting(graphics, font, signature, paperX, paperY, progress);
        return box;
    }

    private static Bounds renderLayer(GuiGraphics graphics, Font font, LetterSignature signature,
                                       int paperX, int paperY, float progress, int alpha, boolean feather, int rgb) {
        Bounds box = bounds(font, signature, paperX, paperY);
        if (!signature.enabled()) return box;
        float amount = SignatureEffects.writingPosition(progress);
        int inkWidth = box.width() - PADDING * 2;
        int revealedWidth = amount >= 1.0F ? inkWidth : (int) Math.floor(inkWidth * amount);
        int color = (alpha << 24) | rgb;
        if (signature.framed()) {
            // The contract's printed frame is always opaque, even while its signature is a ghost.
            graphics.renderOutline(box.x(), box.y(), box.width(), box.height(), 0xFF000000 | rgb);
        }
        graphics.enableScissor(paperX, paperY, paperX + QuestMagicCircle.WRITABLE_WIDTH,
                paperY + QuestMagicCircle.WRITABLE_HEIGHT);
        // A nested scissor reveals ink without painting over the letter's body text.
        if (amount < 1.0F) graphics.enableScissor(box.x() + PADDING, box.y(),
                box.x() + PADDING + revealedWidth, box.y() + box.height());
        graphics.pose().pushPose();
        graphics.pose().translate(box.x() + PADDING, box.y() + PADDING, 0);
        graphics.pose().scale(box.scale(), box.scale(), 1);
        graphics.drawString(font, text(signature), 0, 0, color, false);
        graphics.pose().popPose();
        if (amount < 1.0F) graphics.disableScissor();
        graphics.disableScissor();

        if (feather && amount < 1.0F) {
            double stroke = progress * Math.PI * 11;
            float wave = (float) (Math.sin(stroke) * 0.65 + Math.sin(stroke * 0.61 + 0.7) * 0.35);
            // Continuous coordinates avoid jumping a full GUI pixel each time ink is revealed.
            float tipX = box.x() + PADDING + inkWidth * amount;
            float tipY = box.y() + PADDING + 6 * box.scale() + wave * box.scale();
            float featherScale = Math.max(0.8F, Math.min(1.2F, box.scale() * 0.65F));
            graphics.pose().pushPose();
            graphics.pose().translate(tipX, tipY, 150);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(-10 + (float) Math.sin(stroke * 0.61 - 0.4) * 4));
            graphics.pose().scale(featherScale, featherScale, 1);
            // Vanilla feather's nib is at its lower-left corner.
            graphics.renderFakeItem(new ItemStack(Items.FEATHER), -3, -13);
            graphics.pose().popPose();
        }
        return box;
    }

    /** A diagonal, softly edged sweep highlights only existing ink and the printed outline. */
    private static void renderShine(GuiGraphics graphics, Font font, LetterSignature signature,
                                    Bounds box, long now, float strength) {
        float cycle = (now % 2800L) / 2800F;
        float center = -box.height() + cycle * (box.width() + box.height() * 2);
        int light = SignatureEffects.shineColor(signature.magicColor());
        // Overlapping narrow bands form a bright core and gentler shoulders.
        for (int band = 3; band >= 1; band--) {
            int alpha = Math.round((band == 1 ? 100 : 35) * Math.max(0, Math.min(1, strength)));
            if (alpha < 4) continue; // Minecraft treats tiny alpha values as opaque text.
            for (int row = 0; row < box.height(); row += 2) {
                int left = Math.max(box.x(), box.x() + (int) (center - row * 0.6F) - band * 2);
                int right = Math.min(box.x() + box.width(), box.x() + (int) (center - row * 0.6F) + band * 2);
                if (left >= right) continue;
                graphics.enableScissor(left, box.y() + row, right, box.y() + Math.min(row + 2, box.height()));
                if (signature.framed()) graphics.renderOutline(box.x(), box.y(), box.width(), box.height(),
                        (alpha << 24) | light);
                graphics.pose().pushPose();
                graphics.pose().translate(box.x() + PADDING, box.y() + PADDING, 0);
                graphics.pose().scale(box.scale(), box.scale(), 1);
                graphics.drawString(font, text(signature), 0, 0, (alpha << 24) | light, false);
                graphics.pose().popPose();
                graphics.disableScissor();
            }
        }
    }

    /** Screen-space enchantment sparks: one burst after acknowledgement, never on reopening. */
    public static void renderParticles(GuiGraphics graphics, int magicColor, Bounds box, float progress) {
        if (progress < 0 || progress >= 1) return;
        float travel = 1 - (1 - progress) * (1 - progress);
        int alpha = Math.round(255 * (1 - progress));
        int color = (alpha << 24) | SignatureEffects.shineColor(magicColor);
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2 / 12 + 0.3;
            float distance = 3 + travel * (9 + (i % 3) * 4);
            int x = Math.round(box.x() + box.width() * (i % 5 + 1) / 6F + (float) Math.cos(angle) * distance);
            int y = Math.round(box.y() + box.height() / 2F + (float) Math.sin(angle) * distance - travel * 5);
            graphics.fill(x - 1, y, x + 2, y + 1, color);
            if (i % 2 == 0) graphics.fill(x, y - 1, x + 1, y + 2, color);
        }
    }

    public record Bounds(int x, int y, int width, int height, float scale) {
        public boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
}
