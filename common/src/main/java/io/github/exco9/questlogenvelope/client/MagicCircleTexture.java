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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/** Client-side renderer for the user-provided 65x65 magic-circle artwork. */
@Environment(EnvType.CLIENT)
public final class MagicCircleTexture {
    public static final int SOURCE_SIZE = 65;
    public static final int DISPLAY_SIZE = QuestMagicCircle.DEFAULT_SIZE;

    private static final ResourceLocation TEXTURE = QuestlogEnvelope.id("dynamic/magic_circle");
    private static final ResourceLocation MASK_TEXTURE = QuestlogEnvelope.id("dynamic/magic_circle_mask");

    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAEEAAABBCAYAAACO98lFAAAAAXNSR0IArs4c6QAAAxhJREFUeJztW1FzgyEIm7v9/7/snrqzVDEJ0fZhueut9cOAgoDr1r7uohOy7aAdVxXFRUd9HXw2e/7R6MNrJ5d9VnnfCsbAlRwz/6M2QzGoA+9P6bZDNaAaCa55JWQeQL3D5gSXXgsyRWxOiMmumlMYDhlsxv9kDgkOYoen3boowlUInzJKrRTRTguyEnayrlf6jkrZTckrZU1NZBVuy0agu6smKTR0GX4pan82ClaoXmZ64GiL8dt2PQH1inJJQi9KVT2IHEw4e442OJWcgh45tmpBG+EqK2ip2j1ju0nULl2AVIR6xSWH4onn20S6UuRKVO3W5ehauC3kbrbUTzy7EvkQRDzKyFbQTtrlaImr3lSv4orOl3kzIrZlZcK0XMcF3ds1ssqjBxwbUOVC+4SX+Wx1GM/Xp30P0EL7TQMNJ/XYsLJVTjQi+ssbwaiKsX3ycnEzHB05Ds6mJ3I+eNuhhgjmzJKL2vOv+Gb6VmOr+Yo92SWr/wTBlnxGMSpYfcmKbERMvhnvDm23vmynmXHEY3HBu58ZD5Oks/E+I9zV/1WCQ5BxK30HmlDT4za7O+xCLYZWrM1MqKqJcKULOcLT56sQVDu7LJlVjsMuSbL2/ukaIyGWk1kyQZAlsxYWOkbTbLySDEf0xfqmhq4IlGeZLOJNR7OE8ExzQoQaEQjnbNwJym6m8VHmsrJVTqnBqpQkZj4ie2sD/uazV+kxYanl7RRGm6RjpXrXncjciXbH/TLG9gmjHNru7rjZ+Wq3utTlOF+VaGDnHsljrjNu9UyBX+ZxKmDOaaVEK3jiOfk1nPM3Re5mDVLo5Nl5cPeMTbqoXSWheJtDF8hWBLQExs22NWOowcr1Fd3kqh5EbgsXMdOIKU1b2WFqYqyezxY8/njv+IMwOypnGpVRn+1k1JaeJjvVTbJzMrts0TELXWVRp+VndlrhILzZGh/JDwhxNT/c5Cihmsi+kqPliJTjGzAqyrKy4s3qJepIDkCgKrV1dMV5NlQrRaWcvc37K1Tq+m68oustUG9z7O3Vhv//mn/Dn+ExXrxm2y9pciph28olSgAAAABJRU5ErkJggg==";

    private static final String MASK_PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAEEAAABBCAYAAACO98lFAAADgUlEQVR42tVb7Y7kIAzDiPd/5dyPk1ajHiGJYzp7lVarne3QYPLhGAozGy9elYfhLaPWy5PG4f8ofvdXg2BJw+3x/+ffIMf9KghWMNA298H5fDdxU4KxXp58FpBsvpCAsQQAdFcDgu+27JiNyXsPtkQVwOaezES8sZF8rswTTsZaMSdYISdEK097xfyC+3+OgY8fz0OYELFbIGQAgGgMaz6nBMQsAPCMS7voRSDHt42tkpxgh3ir5ICb1edkVzjGbABQXTEUAUIDKDw4hLEgGGF4ZRx7VAkThIUx4cWSJWWFkJEe9ruTdLFszO5iNeoPzOEPXeLljrOKYcAQnExYdLzCI14n+9ENBxQSHhpNzi654WCPrHdQMMInACgAtZsgEve1GOUc9y4VoEMIQOgJai+oUOgsVb7iDSthaEUtGuO+QAq1XVO0olUv6rLPioYQetBshoJtktcbFzbPpj1qNbk+w/QyPARNIFBhk4yoMpJh8vaF8a8Ae0VU2SGq8gK2M/U2adJAwP7uw7GKUAcAC1xWAW5qblO0el0uoeQAZa9aQdLrKstREjUnwWWTXcZWRE3WSkpUTPL08sfzHu8zHMKHbca285skeTFngp/GR3uK0W8v+3skqWrvz/2TkM5OQgnIlamSLZACzHZ+i5CodhNge4eOCs2Wa3g8oSqdefX5c3Ws0EFmVtECr6va+3P/Eklnp1rvNTJeK20O4emU6WN1yJAlxQasVxqZe9Q22iKS2E0Sc4uUhbS5Snxu9w6KMbNJ2sYYmESsV+ozQ2PfAoBupfHI1L/pMlbimwn3j5ib0hu6iTaS948gdCUq5b5AteR1pcFQXsu4F6PmQNCwVc9GINtKd0qWamdZrW+gkhPUGxuVY3yV43tQA3lzG06ZI65617xkOEZ8CiWrXpmYtUq25rNNFhq9QKWPqJxPSCdGjLx6yx7LyxzpZwWaMYrq82T670L4IGinPSMrQLePFnUOeCsriCrzU3ZNMruDmPA4KFEY/c2dwWoKsxjH1jDSCMNZkEsJeREJjcnCnd6EGb90kGMRD0EzD6hFlRNg6OYEVhB5Q1RRCjOUqKJ8KcM2VUJ9ZDg2iHxD9hQaJX2P9IDOc2UgdBsbxXkHWYM1Ra5tTSA7322HkOLl0O6LmhCH41dA8MA4GcnIa//Fu9I7A09vvkfE679+a34UJvaVST+vP1iVpdsCvLdTAAAAAElFTkSuQmCC";

    private static boolean registered;

    private MagicCircleTexture() {
    }

    public static void render(GuiGraphics graphics, int x, int y) {
        render(graphics, x, y, DISPLAY_SIZE);
    }

    public static void render(GuiGraphics graphics, int x, int y, int size) {
        int safeSize = Math.max(1, size);
        ensureRegistered();
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, x, y, 0.0F, 0.0F, safeSize, safeSize, SOURCE_SIZE, SOURCE_SIZE);
        RenderSystem.disableBlend();
    }

    public static void renderTintedFill(GuiGraphics graphics, int x, int y, float progress, int rgb) {
        renderTintedFill(graphics, x, y, DISPLAY_SIZE, progress, rgb);
    }

    /** Reveals a recolored copy of the circle from bottom to top as progress reaches 1. */
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

        float red = ((rgb >> 16) & 0xFF) / 255.0F;
        float green = ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        graphics.enableScissor(x, fillTop, x + safeSize, y + safeSize);
        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        graphics.blit(MASK_TEXTURE, x, y, 0.0F, 0.0F, safeSize, safeSize, SOURCE_SIZE, SOURCE_SIZE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.disableScissor();
        RenderSystem.disableBlend();
    }

    private static void ensureRegistered() {
        if (registered) {
            return;
        }

        registerTexture(TEXTURE, PNG_BASE64);
        registerTexture(MASK_TEXTURE, MASK_PNG_BASE64);
        registered = true;
    }

    private static void registerTexture(ResourceLocation id, String encodedPng) {
        byte[] png = Base64.getDecoder().decode(encodedPng);
        try (ByteArrayInputStream stream = new ByteArrayInputStream(png)) {
            NativeImage image = NativeImage.read(stream);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load embedded Questlog Envelope magic-circle texture", exception);
        }
    }
}
