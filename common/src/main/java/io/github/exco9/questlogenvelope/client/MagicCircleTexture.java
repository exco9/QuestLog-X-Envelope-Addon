package io.github.exco9.questlogenvelope.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.exco9.questlogenvelope.QuestlogEnvelope;
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
    public static final int DISPLAY_SIZE = 52;

    private static final ResourceLocation TEXTURE = QuestlogEnvelope.id("dynamic/magic_circle");
    private static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAEEAAABBCAYAAACO98lFAAAAAXNSR0IArs4c6QAAAxhJREFUeJztW1FzgyEIm7v9/7/snrqzVDEJ0fZhueut9cOAgoDr1r7uohOy7aAdVxXFRUd9HXw2e/7R6MNrJ5d9VnnfCsbAlRwz/6M2QzGoA+9P6bZDNaAaCa55JWQeQL3D5gSXXgsyRWxOiMmumlMYDhlsxv9kDgkOYoen3boowlUInzJKrRTRTguyEnayrlf6jkrZTckrZU1NZBVuy0agu6smKTR0GX4pan82ClaoXmZ64GiL8dt2PQH1inJJQi9KVT2IHEw4e442OJWcgh45tmpBG+EqK2ip2j1ju0nULl2AVIR6xSWH4onn20S6UuRKVO3W5ehauC3kbrbUTzy7EvkQRDzKyFbQTtrlaImr3lSv4orOl3kzIrZlZcK0XMcF3ds1ssqjBxwbUOVC+4SX+Wx1GM/Xp30P0EL7TQMNJ/XYsLJVTjQi+ssbwaiKsX3ycnEzHB05Ds6mJ3I+eNuhhgjmzJKL2vOv+Gb6VmOr+Yo92SWr/wTBlnxGMSpYfcmKbERMvhnvDm23vmynmXHEY3HBu58ZD5Oks/E+I9zV/1WCQ5BxK30HmlDT4za7O+xCLYZWrM1MqKqJcKULOcLT56sQVDu7LJlVjsMuSbL2/ukaIyGWk1kyQZAlsxYWOkbTbLySDEf0xfqmhq4IlGeZLOJNR7OE8ExzQoQaEQjnbNwJym6m8VHmsrJVTqnBqpQkZj4ie2sD/uazV+kxYanl7RRGm6RjpXrXncjciXbH/TLG9gmjHNru7rjZ+Wq3utTlOF+VaGDnHsljrjNu9UyBX+ZxKmDOaaVEK3jiOfk1nPM3Re5mDVLo5Nl5cPeMTbqoXSWheJtDF8hWBLQExs22NWOowcr1Fd3kqh5EbgsXMdOIKU1b2WFqYqyezxY8/njv+IMwOypnGpVRn+1k1JaeJjvVTbJzMrts0TELXWVRp+VndlrhILzZGh/JDwhxNT/c5Cihmsi+kqPliJTjGzAqyrKy4s3qJepIDkCgKrV1dMV5NlQrRaWcvc37K1Tq+m68oustUG9z7O3Vhv//mn/Dn+ExXrxm2y9pciph28olSgAAAABJRU5ErkJggg==";

    private static boolean registered;

    private MagicCircleTexture() {
    }

    public static void render(GuiGraphics graphics, int x, int y) {
        ensureRegistered();
        RenderSystem.enableBlend();
        graphics.blit(TEXTURE, x, y, 0.0F, 0.0F, DISPLAY_SIZE, DISPLAY_SIZE, SOURCE_SIZE, SOURCE_SIZE);
        RenderSystem.disableBlend();
    }

    private static void ensureRegistered() {
        if (registered) {
            return;
        }

        byte[] png = Base64.getDecoder().decode(PNG_BASE64);
        try (ByteArrayInputStream stream = new ByteArrayInputStream(png)) {
            NativeImage image = NativeImage.read(stream);
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(image));
            registered = true;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load embedded Questlog Envelope magic-circle texture", exception);
        }
    }
}
