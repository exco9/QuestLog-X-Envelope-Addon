package io.github.exco9.questlogenvelope.client;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Uses Ember's native Component/Font integration without making the API a required dependency. */
public final class EmberTextCompat {
    private EmberTextCompat() { }

    @ExpectPlatform
    public static boolean isAvailable() { throw new AssertionError(); }

    public static void renderPreview(GuiGraphics graphics, Font font, String source, int x, int y, int width, int height) {
        // Keep the whole literal intact: Ember parses styled visits before Minecraft wraps it.
        // Never strip markup or write the rendered result back into the editable letter.
        var lines = font.split(Component.literal(source), width);
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            for (int line = 0; line < Math.min(lines.size(), height / font.lineHeight); line++)
                graphics.drawString(font, lines.get(line), x, y + line * font.lineHeight, 0xFF7B593D, false);
        } finally {
            graphics.disableScissor();
        }
    }
}
