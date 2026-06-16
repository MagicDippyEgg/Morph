package me.ichun.mods.morph.mixin;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if ((Object)this instanceof Player player) {
            if (MorphApi.getApi() == null) return;
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            if (info != null) {
                MorphState current = info.getCurrentState();
                if (current != null && current.variant != null && !current.variant.id.getPath().equals("player")) {
                    LivingEntity entity = current.getEntity(player.level());
                    if (entity != null) {
                        cir.setReturnValue(entity.getDimensions(pose));
                    }
                }
            }
        }
    }

    @Inject(method = "getEyeHeight(Lnet/minecraft/world/entity/Pose;Lnet/minecraft/world/entity/EntityDimensions;)F", at = @At("HEAD"), cancellable = true)
    public void onGetEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof Player player) {
            if (MorphApi.getApi() == null) return;
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            if (info != null) {
                MorphState current = info.getCurrentState();
                if (current != null && current.variant != null && !current.variant.id.getPath().equals("player")) {
                    LivingEntity entity = current.getEntity(player.level());
                    if (entity != null) {
                        cir.setReturnValue(((LivingEntityAccessor)entity).callGetEyeHeight(pose, dimensions));
                    }
                }
            }
        }
    }
}
