package com.theendupdate.registry;

import com.theendupdate.TheEndUpdate;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModParticles {

    public static final SimpleParticleType TETHER_TRAIL = register("tether_trail", FabricParticleTypes.simple());
    public static final SimpleParticleType NEBULA_VENT_SMOKE = register("nebula_vent_smoke", FabricParticleTypes.simple());
    public static final SimpleParticleType QUANTUM_SPARK = register("quantum_spark", FabricParticleTypes.simple());

    private ModParticles() {
    }

    private static SimpleParticleType register(String name, SimpleParticleType type) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(TheEndUpdate.MOD_ID, name), type);
    }

    public static void registerModParticles() {
        // Ensures static init; registration happens in field initializers
    }
}
