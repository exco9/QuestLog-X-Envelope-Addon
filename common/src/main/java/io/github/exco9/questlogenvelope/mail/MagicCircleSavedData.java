package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores server-authoritative magic-circle actions and one-shot consumption state.
 *
 * The physical letter only carries an action id and visual data. Executable
 * actions stay here so clients cannot forge privileged commands by editing an
 * ItemStack. Consumed ids are deliberately retained indefinitely: expiring a
 * valid consumed UUID would allow an old duplicated letter to become usable
 * again. Cleanup therefore only removes malformed/incoherent records.
 */
public final class MagicCircleSavedData extends SavedData {
    private static final int DATA_VERSION = 2;

    private static final String DATA_NAME = "questlog_envelope_magic_circles";
    private static final String VERSION_KEY = "version";
    private static final String ACTIONS_KEY = "actions";
    private static final String CONSUMED_KEY = "consumed";
    private static final String OWNER_KEY = "owner";
    private static final String ENTRIES_KEY = "entries";
    private static final String TYPE_KEY = "type";
    private static final String VALUE_KEY = "value";

    // Legacy v1 fields, read for migration only.
    private static final String LEGACY_QUEST_KEY = "quest";
    private static final String LEGACY_COMMAND_KEY = "command";

    public static final String ACTION_QUEST = "quest";
    public static final String ACTION_COMMAND = "command";

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

        if (sanitize()) {
            setDirty();
        }
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

        ListTag entries = new ListTag();
        if (questId != null) {
            addEntry(entries, ACTION_QUEST, questId.toString());
        }
        if (command != null && !command.isBlank()) {
            addEntry(entries, ACTION_COMMAND, command.trim());
        }
        action.put(ENTRIES_KEY, entries);

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

        List<ActionEntry> entries = new ArrayList<>();
        if (action.contains(ENTRIES_KEY, Tag.TAG_LIST)) {
            ListTag list = action.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                CompoundTag entry = list.getCompound(index);
                if (!entry.contains(TYPE_KEY, Tag.TAG_STRING)
                        || !entry.contains(VALUE_KEY, Tag.TAG_STRING)) {
                    continue;
                }

                String type = entry.getString(TYPE_KEY).trim();
                String value = entry.getString(VALUE_KEY).trim();
                if (!type.isEmpty() && !value.isEmpty()) {
                    entries.add(new ActionEntry(type, value));
                }
            }
        }

        // Seamless migration for worlds created by the first implementation.
        if (entries.isEmpty()) {
            if (action.contains(LEGACY_QUEST_KEY, Tag.TAG_STRING)) {
                String quest = action.getString(LEGACY_QUEST_KEY).trim();
                if (ResourceLocation.tryParse(quest) != null) {
                    entries.add(new ActionEntry(ACTION_QUEST, quest));
                }
            }
            if (action.contains(LEGACY_COMMAND_KEY, Tag.TAG_STRING)) {
                String command = action.getString(LEGACY_COMMAND_KEY).trim();
                if (!command.isEmpty()) {
                    entries.add(new ActionEntry(ACTION_COMMAND, command));
                }
            }
        }

        return Optional.of(new ActionDefinition(action.getUUID(OWNER_KEY), List.copyOf(entries)));
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

    /**
     * Removes only records that can never be valid. Valid consumed ids are not
     * aged out, preserving the one-shot guarantee even for duplicated old mail.
     */
    private boolean sanitize() {
        boolean changed = false;

        for (String key : new HashSet<>(this.actions.getAllKeys())) {
            if (!isUuid(key)
                    || !this.actions.contains(key, Tag.TAG_COMPOUND)
                    || !this.actions.getCompound(key).hasUUID(OWNER_KEY)
                    || this.consumed.getBoolean(key)) {
                this.actions.remove(key);
                changed = true;
            }
        }

        for (String key : new HashSet<>(this.consumed.getAllKeys())) {
            if (!isUuid(key)) {
                this.consumed.remove(key);
                changed = true;
            }
        }

        return changed;
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void addEntry(ListTag entries, String type, String value) {
        CompoundTag entry = new CompoundTag();
        entry.putString(TYPE_KEY, type);
        entry.putString(VALUE_KEY, value);
        entries.add(entry);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(VERSION_KEY, DATA_VERSION);
        tag.put(ACTIONS_KEY, this.actions.copy());
        tag.put(CONSUMED_KEY, this.consumed.copy());
        return tag;
    }

    private static MagicCircleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        return new MagicCircleSavedData(tag);
    }

    public record ActionDefinition(UUID owner, List<ActionEntry> actions) {
    }

    public record ActionEntry(String type, String value) {
    }
}
