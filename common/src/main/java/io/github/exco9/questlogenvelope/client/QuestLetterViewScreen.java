package io.github.exco9.questlogenvelope.client;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.exco9.questlogenvelope.mail.QuestMailMarker;
import io.github.mortuusars.envelope.client.gui.screen.LetterViewScreen;
import io.github.mortuusars.envelope.world.item.component.LetterContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Letter view that only changes layout scale for Questlog-authored mail. */
public final class QuestLetterViewScreen extends LetterViewScreen {
    private final float questFontScale;

    public QuestLetterViewScreen(ItemStack letter, @Nullable InteractionHand hand) {
        super(letter, hand);
        this.questFontScale = QuestMailMarker.getFontScale(letter);
    }

    @Override
    protected void init() {
        super.init();

        if (this.questFontScale == 1.0F) {
            return;
        }

        // Logical dimensions are inverse-scaled so the final rendered text still
        // fits Envelope's normal 142x144 paper writing area.
        this.maxTextWidth = Math.max(1, (int) Math.floor(142.0F / this.questFontScale));
        this.maxTextHeight = Math.max(1, (int) Math.floor(144.0F / this.questFontScale));
        this.maxTextLines = Math.max(1, this.maxTextHeight / this.font.lineHeight);
        createLines(this.letter.getOrDefault(
                io.github.mortuusars.envelope.Envelope.DataComponents.LETTER_CONTENT,
                LetterContent.EMPTY
        ).text());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (this.questFontScale == 1.0F) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            return;
        }

        // LetterViewScreen has no child widgets. Its super.render() call only
        // services Screen renderables, so for scaled quest letters we render the
        // same content directly to avoid drawing the normal-size text underneath.
        final int originX = this.leftPos + 17;
        final int originY = this.topPos + 21;
        int textColor = 0xFF7B593D;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(originX, originY, 0);
        guiGraphics.pose().scale(this.questFontScale, this.questFontScale, 1.0F);

        for (int i = 0; i < Math.min(this.lines.size(), this.maxTextLines); i++) {
            guiGraphics.drawString(
                    this.font,
                    this.lines.get(i),
                    0,
                    i * this.font.lineHeight,
                    textColor,
                    false
            );
        }
        guiGraphics.pose().popPose();

        if (this.isTattered) {
            RenderSystem.enableBlend();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            guiGraphics.blit(TATTERED_OVERLAY, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
            guiGraphics.pose().popPose();
            RenderSystem.disableBlend();
        }

        @Nullable Style style = getComponentStyleAtScaled(mouseX, mouseY);
        if (style != null && style.getHoverEvent() != null) {
            guiGraphics.renderComponentHoverEffect(this.font, style, mouseX, mouseY);
        } else if (this.lines.size() > this.maxTextLines
                && mouseX >= originX && mouseX < originX + 142
                && mouseY >= originY && mouseY < originY + 144) {
            List<FormattedCharSequence> leftovers = this.lines.stream().skip(this.maxTextLines).toList();
            guiGraphics.renderTooltip(this.font, leftovers, mouseX, mouseY);
        }
    }

    @Nullable
    private Style getComponentStyleAtScaled(double mouseX, double mouseY) {
        if (this.lines.isEmpty()) {
            return null;
        }

        double localX = (mouseX - (this.leftPos + 17)) / this.questFontScale;
        double localY = (mouseY - (this.topPos + 21)) / this.questFontScale;
        if (localX < 0 || localX >= this.maxTextWidth || localY < 0 || localY >= this.maxTextHeight) {
            return null;
        }

        int linesCount = Math.min(this.lines.size(), this.maxTextLines);
        if (localY < this.font.lineHeight * linesCount + linesCount) {
            int clickedLine = (int) localY / this.font.lineHeight;
            if (clickedLine >= 0 && clickedLine < this.lines.size()) {
                return this.font.getSplitter().componentStyleAtWidth(
                        this.lines.get(clickedLine),
                        (int) localX
                );
            }
        }
        return null;
    }
}
