package io.github.exco9.questlogenvelope.mail;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/** Durable cargo; the courier carries only an id, never nested item contents. */
public final class GroupedMailSavedData extends SavedData {
    public static final int COLLECTION_TICKS = 5;
    private static final Factory<GroupedMailSavedData> FACTORY = new Factory<>(
            GroupedMailSavedData::new, GroupedMailSavedData::load, null);
    private final Map<UUID, Batch> batches = new LinkedHashMap<>();

    public static GroupedMailSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                FACTORY, "questlog_envelope_grouped_mail");
    }

    public UUID enqueue(UUID recipient, String name, ItemStack mail, boolean express, long now) {
        if (mail.isEmpty()) throw new IllegalArgumentException("Cannot queue empty mail");
        Batch batch = batches.values().stream()
                .filter(candidate -> !candidate.dispatched && candidate.recipient.equals(recipient))
                .findFirst().orElseGet(() -> {
                    var created = new Batch(UUID.randomUUID(), recipient, name, now + COLLECTION_TICKS);
                    batches.put(created.id, created);
                    return created;
                });
        batch.mail.add(mail.copyWithCount(1));
        batch.express |= express;
        setDirty();
        return batch.id;
    }

    public List<Batch> ready(long now) {
        return batches.values().stream().filter(batch -> !batch.dispatched && batch.ready <= now).toList();
    }

    public UUID beginDelivery(UUID id) {
        Batch batch = batches.get(id);
        if (batch == null || batch.dispatched) throw new IllegalStateException("Batch already dispatched");
        batch.dispatched = true;
        batch.attempt = UUID.randomUUID();
        setDirty();
        return batch.attempt;
    }

    public Optional<Batch> find(UUID id, UUID attempt) {
        Batch batch = batches.get(id);
        return batch != null && batch.dispatched && attempt.equals(batch.attempt)
                ? Optional.of(batch) : Optional.empty();
    }

    /** Advance only after a real item is accepted. Saved progress prevents retrying earlier items. */
    public boolean deliver(UUID id, UUID attempt, Predicate<ItemStack> accept) {
        Batch batch = find(id, attempt).orElse(null);
        if (batch == null) return true; // Consume a stale courier without replaying its cargo.
        while (batch.next < batch.mail.size()) {
            if (!accept.test(batch.first())) return false;
            batch.next++;
            setDirty();
        }
        batches.remove(id);
        setDirty();
        return true;
    }

    public boolean retry(UUID id, UUID attempt, long now) {
        Batch batch = find(id, attempt).orElse(null);
        if (batch == null) return false;
        batch.dispatched = false;
        batch.attempt = null;
        batch.ready = now + COLLECTION_TICKS;
        setDirty();
        return true;
    }

    static GroupedMailSavedData load(CompoundTag root, HolderLookup.Provider registries) {
        var data = new GroupedMailSavedData();
        for (Tag entry : root.getList("batches", Tag.TAG_COMPOUND)) {
            var tag = (CompoundTag) entry;
            var batch = new Batch(tag.getUUID("id"), tag.getUUID("recipient"),
                    tag.getString("name"), tag.getLong("ready"));
            for (Tag mail : tag.getList("mail", Tag.TAG_COMPOUND)) {
                ItemStack stack = ItemStack.parseOptional(registries, (CompoundTag) mail);
                if (stack.isEmpty()) throw new IllegalStateException("Cannot restore grouped mail item");
                batch.mail.add(stack);
            }
            batch.express = tag.getBoolean("express");
            batch.dispatched = tag.getBoolean("dispatched");
            batch.attempt = tag.hasUUID("attempt") ? tag.getUUID("attempt") : null;
            batch.next = Math.max(0, tag.getInt("next"));
            if (batch.next < batch.mail.size()) data.batches.put(batch.id, batch);
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        var entries = new ListTag();
        for (Batch batch : batches.values()) {
            var tag = new CompoundTag();
            tag.putUUID("id", batch.id);
            tag.putUUID("recipient", batch.recipient);
            tag.putString("name", batch.name);
            tag.putLong("ready", batch.ready);
            tag.putBoolean("express", batch.express);
            tag.putBoolean("dispatched", batch.dispatched);
            if (batch.attempt != null) tag.putUUID("attempt", batch.attempt);
            tag.putInt("next", batch.next);
            var mail = new ListTag();
            for (ItemStack stack : batch.mail) mail.add(stack.save(registries));
            tag.put("mail", mail);
            entries.add(tag);
        }
        root.put("batches", entries);
        return root;
    }

    public static final class Batch {
        private final UUID id;
        private final UUID recipient;
        private final String name;
        private final List<ItemStack> mail = new ArrayList<>();
        private long ready;
        private boolean express;
        private boolean dispatched;
        private UUID attempt;
        private int next;

        private Batch(UUID id, UUID recipient, String name, long ready) {
            this.id = id;
            this.recipient = recipient;
            this.name = name;
            this.ready = ready;
        }

        public UUID id() { return id; }
        public UUID recipient() { return recipient; }
        public String recipientName() { return name; }
        public boolean express() { return express; }
        public int remaining() { return mail.size() - next; }
        public ItemStack first() { return mail.get(next).copy(); }
    }
}
