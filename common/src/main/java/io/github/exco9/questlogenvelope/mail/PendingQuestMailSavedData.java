package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Persists delivered quest-mail markers until Questlog has consumed them. */
public final class PendingQuestMailSavedData extends SavedData {
    private static final String DATA_NAME = "questlog_envelope_pending_mail";
    private static final String PENDING_KEY = "pending";

    private final CompoundTag pending;

    public PendingQuestMailSavedData() {
        this.pending = new CompoundTag();
    }

    private PendingQuestMailSavedData(CompoundTag tag) {
        this.pending = tag.contains(PENDING_KEY) ? tag.getCompound(PENDING_KEY).copy() : new CompoundTag();
    }

    private static final Factory<PendingQuestMailSavedData> FACTORY = new Factory<>(
            PendingQuestMailSavedData::new,
            PendingQuestMailSavedData::load,
            null
    );

    public static PendingQuestMailSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void record(String playerName, ResourceLocation questId) {
        String playerKey = normalize(playerName);
        CompoundTag playerData = pending.getCompound(playerKey);
        String questKey = questId.toString();
        playerData.putInt(questKey, playerData.getInt(questKey) + 1);
        pending.put(playerKey, playerData);
        setDirty();
    }

    public int getCount(String playerName, ResourceLocation questId) {
        CompoundTag playerData = pending.getCompound(normalize(playerName));
        return playerData.getInt(questId.toString());
    }

    public Set<ResourceLocation> getQuestIds(String playerName) {
        CompoundTag playerData = pending.getCompound(normalize(playerName));
        Set<ResourceLocation> result = new HashSet<>();
        for (String key : playerData.getAllKeys()) {
            try {
                result.add(ResourceLocation.parse(key));
            } catch (RuntimeException ignored) {
                // Ignore malformed persisted keys rather than breaking player login.
            }
        }
        return result;
    }

    public boolean consumeOne(String playerName, ResourceLocation questId) {
        String playerKey = normalize(playerName);
        CompoundTag playerData = pending.getCompound(playerKey);
        String questKey = questId.toString();
        int count = playerData.getInt(questKey);
        if (count <= 0) {
            return false;
        }

        if (count == 1) {
            playerData.remove(questKey);
        } else {
            playerData.putInt(questKey, count - 1);
        }

        if (playerData.isEmpty()) {
            pending.remove(playerKey);
        } else {
            pending.put(playerKey, playerData);
        }

        setDirty();
        return true;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(PENDING_KEY, pending.copy());
        return tag;
    }

    private static PendingQuestMailSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new PendingQuestMailSavedData(tag);
    }

    private static String normalize(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }
}
