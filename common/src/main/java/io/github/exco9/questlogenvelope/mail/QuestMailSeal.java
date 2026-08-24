package io.github.exco9.questlogenvelope.mail;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.item.Sealable;
import io.github.mortuusars.envelope.world.item.component.seal.Seal;
import io.github.mortuusars.envelope.world.item.component.seal.SealMaterial;
import io.github.mortuusars.envelope.world.item.component.seal.SealSymbol;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Applies Envelope's native wax seals to Questlog reward mail. */
public final class QuestMailSeal {
    private QuestMailSeal() {
    }

    public static ItemStack apply(
            ServerPlayer player,
            ItemStack mail,
            @Nullable ResourceLocation symbolId
    ) {
        if (symbolId == null || !(mail.getItem() instanceof Sealable sealable)) {
            return mail;
        }

        ResourceKey<SealSymbol> symbolKey = ResourceKey.create(
                Envelope.Registries.SEAL_SYMBOL,
                symbolId
        );

        Holder<SealSymbol> symbol = SealSymbol.get(player.registryAccess(), symbolKey).orElse(null);
        if (symbol == null) {
            Envelope.LOGGER.warn(
                    "Questlog Envelope seal symbol '{}' is not registered; sending mail without a seal.",
                    symbolId
            );
            return mail;
        }

        if (!sealable.canSeal(player.level(), mail)) {
            return mail;
        }

        Holder<SealMaterial> material = SealMaterial.getOrThrow(
                player.registryAccess(),
                SealMaterial.RED_WAX
        );

        // Automated quest mail is not physically stamped by a player, so keep
        // the signature neutral. The visual/material/symbol are fully native Envelope.
        Seal seal = new Seal(material, symbol, Component.empty());
        return sealable.seal(player.level(), mail, seal);
    }
}
