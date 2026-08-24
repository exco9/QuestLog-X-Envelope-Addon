package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/** Prevents duplicated/copy-pasted physical mail from replaying a magic seal action. */
public final class MagicSealSavedData extends SavedData {
    private static final String DATA_NAME = "questlog_envelope_magic_seals";
    private static final String CONSUMED_KEY = "consumed";

    private final CompoundTag consumed;

    public MagicSealSavedData() {
        this.consumed = new CompoundTag();
    }

    private MagicSealSavedData(CompoundTag tag) {
        this.consumed = tag.contains(CONSUMED_KEY)
                ? tag.getCompound(CONSUMED_KEY).copy()
                : new CompoundTag();
    }

    private static final Factory<MagicSealSavedData> FACTORY = new Factory<>(
            MagicSealSavedData::new,
            MagicSealSavedData::load,
            null
    );

    public static MagicSealSavedData get(ServerLevel level) {
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

    private static MagicSealSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new MagicSealSavedData(tag);
    }
}
