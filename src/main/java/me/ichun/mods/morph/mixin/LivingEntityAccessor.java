package me.ichun.mods.morph.mixin;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("getEyeHeight")
    float callGetEyeHeight(Pose pose, EntityDimensions dimensions);

    @Accessor("useItemRemaining")
    int getUseItemRemaining();

    @Accessor("useItemRemaining")
    void setUseItemRemaining(int time);

    @Invoker("setLivingEntityFlag")
    void callSetLivingEntityFlag(int flag, boolean value);
}
