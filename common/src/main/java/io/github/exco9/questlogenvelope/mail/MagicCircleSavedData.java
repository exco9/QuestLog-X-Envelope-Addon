package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores server-authoritative magic-circle actions and one-shot consumption state.
 *
 * The physical letter only carries an action id and visual data. Quest ids and
 * commands live here so a client cannot forge a privileged command by editing
 * ItemStack custom data.
 */
public final class MagicCircleSavedData extends SavedData {
    private static final String DATA_NAME = "questlog_envelope_magic_circles";
    private static final String ACTIONS_KEY = "actions";
    private static final String CONSUMED_KEY = "consumed";
    private static final String OWNER_KEY = "owner";
    private static final String QUEST_KEY = "quest";
    private static final String COMMAND_KEY = "command";

    private final CompoundTag actions;
    private final CompoundTag consumed;

    public MagicCircleSavedData() {
        this.actions = new CompoundTag();
        this.consumed = new CompoundTag();
    }

    private MagicCircleSavedData(CompoundTag tag) {
        this.actions = tag.contains(ACTIONS_KEY, Tag.TAG_COMPOUND)
                ? tag.getCompound(ACTIONS_KEY).copy()
                : new CompoundTag();
        this.consumed = tag.contains(CONSUMED_KEY, Tag.TAG_COMPOUND)
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

    public void register(
            UUID actionId,
            UUID owner,
            @Nullable ResourceLocation questId,
            @Nullable String command
    ) {
        CompoundTag action = new CompoundTag();
        action.putUUID(OWNER_KEY, owner);
        if (questId != null) {
            action.putString(QUEST_KEY, questId.toString());
        }
        if (command != null && !command.isBlank()) {
            action.putString(COMMAND_KEY, command.trim());
        }

        this.actions.put(actionId.toString(), action);
        setDirty();
    }

    public Optional<ActionDefinition> getAction(UUID actionId) {
        String key = actionId.toString();
        if (!this.actions.contains(key, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }

        CompoundTag action = this.actions.getCompound(key);
        if (!action.hasUUID(OWNER_KEY)) {
            return Optional.empty();
        }

        @Nullable ResourceLocation questId = null;
        if (action.contains(QUEST_KEY, Tag.TAG_STRING)) {
            questId = ResourceLocation.tryParse(action.getString(QUEST_KEY));
            if (questId == null) {
                return Optional.empty();
            }
        }

        @Nullable String command = null;
        if (action.contains(COMMAND_KEY, Tag.TAG_STRING)) {
            String value = action.getString(COMMAND_KEY).trim();
            if (!value.isEmpty()) {
                command = value;
            }
        }

        return Optional.of(new ActionDefinition(action.getUUID(OWNER_KEY), questId, command));
    }

    public boolean isConsumed(UUID actionId) {
        return this.consumed.getBoolean(actionId.toString());
    }

    /** Returns true only for the first valid claim of a registered action id. */
    public boolean claim(UUID actionId) {
        String key = actionId.toString();
        if (this.consumed.getBoolean(key) || !this.actions.contains(key, Tag.TAG_COMPOUND)) {
            return false;
        }

        this.actions.remove(key);
        this.consumed.putBoolean(key, true);
        setDirty();
        return true;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put(ACTIONS_KEY, this.actions.copy());
        tag.put(CONSUMED_KEY, this.consumed.copy());
        return tag;
    }

    private static MagicCircleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new MagicCircleSavedData(tag);
    }

    public record ActionDefinition(
            UUID owner,
            @Nullable ResourceLocation questId,
            @Nullable String command
    ) {
    }
}
