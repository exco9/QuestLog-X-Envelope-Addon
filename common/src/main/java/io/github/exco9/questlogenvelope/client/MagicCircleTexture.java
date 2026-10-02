package io.github.exco9.questlogenvelope.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.exco9.questlogenvelope.QuestlogEnvelope;
import io.github.exco9.questlogenvelope.mail.QuestMagicCircle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;

/** Client-side renderer for the resource-pack-friendly magic-circle artwork. */
@Environment(EnvType.CLIENT)
public final class MagicCircleTexture {
    public static final int DISPLAY_SIZE = QuestMagicCircle.DEFAULT_SIZE;

    /** Resource packs can replace this PNG without touching any Java code. */
    public static final ResourceLocation ASSET_TEXTURE = QuestlogEnvelope.id("textures/gui/magic_circle.png");

    private static final ResourceLocation RUNTIME_TEXTURE = QuestlogEnvelope.id("dynamic/magic_circle");
    private static final ResourceLocation RUNTIME_MASK_TEXTURE = QuestlogEnvelope.id("dynamic/magic_circle_mask");

    private static boolean registered;
    private static int sourceWidth = 65;
    private static int sourceHeight = 65;

    private MagicCircleTexture() {
    }

    /** Forces the source artwork and generated tint mask to be rebuilt after a resource reload. */
    public static void invalidate() {
        registered = false;
        sourceWidth = 65;
        sourceHeight = 65;
    }

    public static void render(GuiGraphics graphics, int x, int y) {
        render(graphics, x, y, DISPLAY_SIZE);
    }

    public static void render(GuiGraphics graphics, int x, int y, int size) {
        int safeSize = Math.max(1, size);
        ensureRegistered();
        RenderSystem.enableBlend();
        renderScaled(graphics, RUNTIME_TEXTURE, x, y, safeSize);
        RenderSystem.disableBlend();
    }

    public static void renderTintedFill(GuiGraphics graphics, int x, int y, float progress, int rgb) {
        renderTintedFill(graphics, x, y, DISPLAY_SIZE, progress, rgb);
    }

    /** Reveals a recolored copy of the full circle from bottom to top. */
    public static void renderTintedFill(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            float progress,
            int rgb
    ) {
        int safeSize = Math.max(1, size);
        float clamped = Math.max(0.0F, Math.min(1.0F, progress));
        if (clamped <= 0.0F) {
            return;
        }

        ensureRegistered();
        int fillHeight = Math.max(1, Math.round(safeSize * clamped));
        int fillTop = y + safeSize - fillHeight;

        RenderSystem.enableBlend();
        graphics.enableScissor(x, fillTop, x + safeSize, y + safeSize);
        setShaderColor(rgb, 1.0F);
        renderScaled(graphics, RUNTIME_MASK_TEXTURE, x, y, safeSize);
        resetShaderColor();
        graphics.disableScissor();
        RenderSystem.disableBlend();
    }

    /** Short activation pulse; the permanent final state remains the configured color. */
    public static void renderGlow(
            GuiGraphics graphics,
            int x,
            int y,
            int size,
            int rgb,
            float strength
    ) {
        float clamped = Math.max(0.0F, Math.min(1.0F, strength));
        if (clamped <= 0.0F) {
            return;
        }

        ensureRegistered();
        int padding = 2 + Math.round(2.0F * clamped);
        RenderSystem.enableBlend();
        setShaderColor(rgb, 0.28F * clamped);
        renderScaled(
                graphics,
                RUNTIME_MASK_TEXTURE,
                x - padding,
                y - padding,
                Math.max(1, size + padding * 2)
        );
        resetShaderColor();
        RenderSystem.disableBlend();
    }

    /** Always scales the complete artwork instead of cropping its source region. */
    private static void renderScaled(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        float scaleX = size / (float) sourceWidth;
        float scaleY = size / (float) sourceHeight;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scaleX, scaleY, 1.0F);
        graphics.blit(
                texture,
                0,
                0,
                0.0F,
                0.0F,
                sourceWidth,
                sourceHeight,
                sourceWidth,
                sourceHeight
        );
        graphics.pose().popPose();
    }

    private static void setShaderColor(int rgb, float alpha) {
        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    /** Persistent enchanted sweep uses the same tint and diagonal movement as signature ink. */
    public static void renderActivated(GuiGraphics graphics, int x, int y, int size, int ink, int magicColor,
                                      float fade, float particles, long now) {
        renderTintedFill(graphics, x, y, size, 1, SignatureEffects.blend(ink, magicColor, fade));
        float center = -size + (now % 2800L) / 2800F * size * 3;
        ensureRegistered();
        RenderSystem.enableBlend();
        for (int band = 3; band >= 1; band--) {
            setShaderColor(SignatureEffects.shineColor(magicColor), (band == 1 ? 0.39F : 0.14F) * fade);
            for (int row = 0; row < size; row += 2) {
                int left = Math.max(x, x + (int) (center - row * 0.6F) - band * 2);
                int right = Math.min(x + size, x + (int) (center - row * 0.6F) + band * 2);
                if (left >= right) continue;
                graphics.enableScissor(left, y + row, right, y + Math.min(row + 2, size));
                renderScaled(graphics, RUNTIME_MASK_TEXTURE, x, y, size);
                graphics.disableScissor();
            }
        }
        resetShaderColor();
        RenderSystem.disableBlend();
        if (particles >= 0 && particles < 1) {
            float travel = 1 - (1 - particles) * (1 - particles);
            int tint = (Math.round(255 * (1 - particles)) << 24) | SignatureEffects.shineColor(magicColor);
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6 + 0.2;
                float radius = size * 0.4F + travel * (8 + (i % 3) * 3);
                int sparkX = Math.round(x + size / 2F + (float) Math.cos(angle) * radius);
                int sparkY = Math.round(y + size / 2F + (float) Math.sin(angle) * radius - travel * 3);
                graphics.fill(sparkX - 1, sparkY, sparkX + 2, sparkY + 1, tint);
                if (i % 2 == 0) graphics.fill(sparkX, sparkY - 1, sparkX + 1, sparkY + 2, tint);
            }
        }
    }

    private static void resetShaderColor() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void ensureRegistered() {
        if (registered) {
            return;
        }

        NativeImage source = loadSourceImage();
        NativeImage mask = createWhiteAlphaMask(source);
        sourceWidth = source.getWidth();
        sourceHeight = source.getHeight();

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().register(RUNTIME_TEXTURE, new DynamicTexture(source));
        minecraft.getTextureManager().register(RUNTIME_MASK_TEXTURE, new DynamicTexture(mask));
        registered = true;
    }

    private static NativeImage loadSourceImage() {
        Resource resource = Minecraft.getInstance()
                .getResourceManager()
                .getResource(ASSET_TEXTURE)
                .orElseThrow(() -> new IllegalStateException("Missing magic-circle resource: " + ASSET_TEXTURE));

        try (InputStream stream = resource.open()) {
            return NativeImage.read(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load magic-circle PNG resource", exception);
        }
    }

    /** Builds a tintable white silhouette from the selected artwork's alpha channel. */
    private static NativeImage createWhiteAlphaMask(NativeImage source) {
        NativeImage mask = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getPixelRGBA(x, y);
                int alpha = (pixel >>> 24) & 0xFF;
                mask.setPixelRGBA(x, y, (alpha << 24) | 0x00FFFFFF);
            }
        }
        return mask;
    }
}
