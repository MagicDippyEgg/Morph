package me.ichun.mods.morph.api.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class MorphState {
    public final MorphVariant variant;
    private LivingEntity entityInstance;

    public MorphState(MorphVariant variant) {
        this.variant = variant;
    }

    public LivingEntity getEntity(Level level) {
        if (entityInstance == null || entityInstance.level() != level) {
            entityInstance = variant.createEntity(level);
        }
        return entityInstance;
    }
}
