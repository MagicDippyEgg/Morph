package me.ichun.mods.morph.mixin;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    public void onGetDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;
        if (MorphApi.getApi() == null) return;
        MorphInfo info = MorphApi.getApi().getMorphInfo(player);
        if (info != null && info.getCurrentState() != null && !info.getCurrentState().variant.id.getPath().equals("player")) {
            LivingEntity entity = info.getCurrentState().getEntity(player.level());
            if (entity != null) {
                entity.setPose(pose);
                cir.setReturnValue(entity.getDimensions(pose));
            }
        }
    }

    @Inject(method = "getStandingEyeHeight", at = @At("HEAD"), cancellable = true)
    public void onGetStandingEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (MorphApi.getApi() == null) return;
        MorphInfo info = MorphApi.getApi().getMorphInfo(player);
        if (info != null && info.getCurrentState() != null && !info.getCurrentState().variant.id.getPath().equals("player")) {
            LivingEntity entity = info.getCurrentState().getEntity(player.level());
            if (entity != null) {
                entity.setPose(pose);
                // Call the proxy's eye height logic via accessor
                cir.setReturnValue(((LivingEntityAccessor)entity).callGetEyeHeight(pose, entity.getDimensions(pose)));
            }
        }
    }
}
