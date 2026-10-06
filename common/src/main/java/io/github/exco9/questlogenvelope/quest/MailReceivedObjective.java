package io.github.exco9.questlogenvelope.quest;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.infernalstudios.questlog.core.quests.objectives.Objective;
import org.jetbrains.annotations.Nullable;

/**
 * Questlog objective usable as either a prerequisite or a regular objective.
 *
 * A marked Envelope letter completes this objective when its quest marker matches either:
 * - the explicit `quest` field; or
 * - the parent quest id when `quest` is omitted.
 */
public final class MailReceivedObjective extends Objective {
    @Nullable
    private final ResourceLocation configuredQuestId;

    public MailReceivedObjective(JsonObject definition) {
        super(definition);
        this.configuredQuestId = definition.has("quest")
                ? ResourceLocation.parse(definition.get("quest").getAsString())
                : null;
    }

    public boolean accepts(ResourceLocation receivedQuestId) {
        ResourceLocation expected = configuredQuestId;
        if (expected == null && getParent() != null) {
            expected = getParent().getId();
        }
        return receivedQuestId.equals(expected);
    }

    public void receive() {
        if (!isCompleted()) {
            setUnits(getUnits() + 1);
        }
    }
}
