package com.theendupdate.registry;

import com.theendupdate.TemplateMod;
import com.theendupdate.screen.GatewayScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public final class ModScreenHandlers {
    public static ScreenHandlerType<GatewayScreenHandler> GATEWAY;

    public static void register() {
        GATEWAY = new ScreenHandlerType<>(GatewayScreenHandler::new, FeatureFlags.VANILLA_FEATURES);
        Registry.register(Registries.SCREEN_HANDLER, Identifier.of(TemplateMod.MOD_ID, "gateway"), GATEWAY);
    }
}


