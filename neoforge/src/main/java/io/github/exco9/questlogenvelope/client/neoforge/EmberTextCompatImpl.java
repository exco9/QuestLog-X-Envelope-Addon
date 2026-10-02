package io.github.exco9.questlogenvelope.client.neoforge;

import net.neoforged.fml.ModList;

public final class EmberTextCompatImpl {
    private EmberTextCompatImpl() { }
    public static boolean isAvailable() { return ModList.get().isLoaded("emberstextapi"); }
}
