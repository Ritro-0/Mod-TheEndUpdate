package com.theendupdate;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateMod implements ModInitializer {
    public static final String MOD_ID = "theendupdate";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing TheEndUpdate");
        com.theendupdate.registry.ModBlocks.registerModBlocks();
        com.theendupdate.registry.ModItems.registerModItems();
    }
}


