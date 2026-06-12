package me.ichun.mods.morph.api.mob.trait;

import net.minecraft.world.entity.LivingEntity;

public abstract class Trait<T extends Trait<T>> {
    public LivingEntity livingInstance;
}
