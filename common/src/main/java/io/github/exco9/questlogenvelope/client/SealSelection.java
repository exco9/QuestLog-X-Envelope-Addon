package io.github.exco9.questlogenvelope.client;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.client.renderer.SealRenderer;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Small client-side selector/preview for Envelope's built-in seal symbols. */
public final class SealSelection {
    private static final SealRenderer RENDERER = new SealRenderer();

    private final List<ResourceLocation> symbols = new ArrayList<>();
    private int index = -1;

    public SealSelection(@Nullable String initialId) {
        SealSymbol.EMBLEMS.values().stream()
                .map(ResourceKey::location)
                .forEach(symbols::add);
        SealSymbol.LETTERS.values().stream()
                .map(ResourceKey::location)
                .forEach(symbols::add);
        SealSymbol.NUMBERS.values().stream()
                .map(ResourceKey::location)
                .forEach(symbols::add);

        symbols.sort(Comparator.comparing(ResourceLocation::toString));

        ResourceLocation initial = initialId == null || initialId.isBlank()
                ? null
                : ResourceLocation.tryParse(initialId);
        if (initial != null) {
            int found = symbols.indexOf(initial);
            if (found < 0) {
                symbols.add(initial);
                symbols.sort(Comparator.comparing(ResourceLocation::toString));
                found = symbols.indexOf(initial);
            }
            index = found;
        }
    }

    public void next() {
        if (symbols.isEmpty()) {
            index = -1;
            return;
        }
        index++;
        if (index >= symbols.size()) {
            index = -1;
        }
    }

    public void previous() {
        if (symbols.isEmpty()) {
            index = -1;
            return;
        }
        index--;
        if (index < -1) {
            index = symbols.size() - 1;
        }
    }

    public void clear() {
        index = -1;
    }

    @Nullable
    public ResourceLocation get() {
        return index >= 0 && index < symbols.size() ? symbols.get(index) : null;
    }

    public Component label() {
        ResourceLocation id = get();
        if (id == null) {
            return Component.translatable("questlog_envelope.editor.seal.none");
        }

        String path = id.getPath();
        if (path.startsWith("letter/") && path.length() > 7) {
            return Component.literal(path.substring(7).toUpperCase());
        }
        if (path.startsWith("number/") && path.length() > 7) {
            return Component.literal(path.substring(7));
        }

        String pretty = path.replace('_', ' ').replace('/', ' ');
        if (!pretty.isEmpty()) {
            pretty = Character.toUpperCase(pretty.charAt(0)) + pretty.substring(1);
        }
        return Component.literal(pretty);
    }

    public void renderPreview(GuiGraphics graphics, int x, int y) {
        ResourceLocation id = get();
        Minecraft minecraft = Minecraft.getInstance();
        if (id == null || minecraft.level == null) {
            return;
        }

        ResourceKey<SealSymbol> symbolKey = ResourceKey.create(Envelope.Registries.SEAL_SYMBOL, id);
        Holder<SealSymbol> symbol = SealSymbol.get(minecraft.level.registryAccess(), symbolKey).orElse(null);
        if (symbol == null) {
            return;
        }

        Holder<SealMaterial> material = SealMaterial.getOrThrow(
                minecraft.level.registryAccess(),
                SealMaterial.RED_WAX
        );
        RENDERER.render(new Seal(material, symbol, Component.empty()), graphics, x, y);
    }
}
