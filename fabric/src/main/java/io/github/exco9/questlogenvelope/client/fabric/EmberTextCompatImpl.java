package io.github.exco9.questlogenvelope.client.fabric;

import net.fabricmc.loader.api.FabricLoader;

public final class EmberTextCompatImpl {
    private EmberTextCompatImpl() { }
    public static boolean isAvailable() { return FabricLoader.getInstance().isModLoaded("emberstextapi"); }
}
