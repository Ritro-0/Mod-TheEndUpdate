package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import com.theendupdate.screen.GatewayScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
    public static MenuType<GatewayScreenHandler> GATEWAY;

    public static void register() {
        GATEWAY = new MenuType<>(GatewayScreenHandler::new, FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU, Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, "gateway"), GATEWAY);
    }
}


