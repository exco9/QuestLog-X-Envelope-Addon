package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Prevents copied or duplicated letters from replaying the same magic-circle action. */
public final class MagicCircleSavedData extends SavedData {
    private static final String DATA_NAME = "questlog_envelope_magic_circles";
    private static final String CONSUMED_KEY = "consumed";

    private final CompoundTag consumed;

    public MagicCircleSavedData() {
        this.consumed = new CompoundTag();
    }

    private MagicCircleSavedData(CompoundTag tag) {
        this.consumed = tag.contains(CONSUMED_KEY)
                ? tag.getCompound(CONSUMED_KEY).copy()
                : new CompoundTag();
    }

    private static final Factory<MagicCircleSavedData> FACTORY = new Factory<>(
            MagicCircleSavedData::new,
            MagicCircleSavedData::load,
            null
    );

    public static MagicCircleSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    /** Returns true only for the first successful claim of this action id. */
    public boolean claim(UUID actionId) {
        String key = actionId.toString();
        if (this.consumed.getBoolean(key)) {
            return false;
        }
        this.consumed.putBoolean(key, true);
        setDirty();
        return true;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(CONSUMED_KEY, this.consumed.copy());
        return tag;
    }

    private static MagicCircleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new MagicCircleSavedData(tag);
    }
}
