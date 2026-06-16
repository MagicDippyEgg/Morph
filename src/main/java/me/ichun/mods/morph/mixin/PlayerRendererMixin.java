package me.ichun.mods.morph.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public PlayerRendererMixin(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float shadowSize) { super(context, model, shadowSize); }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    public void onRender(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (MorphApi.getApi() == null) return;
        MorphInfo info = MorphApi.getApi().getMorphInfo(player);
        if (info != null) {
            MorphState current = info.getCurrentState();
            if (current != null && current.variant != null && !current.variant.id.getPath().equals("player")) {
                LivingEntity entity = current.getEntity(player.level());
                if (entity != null) {
                    ci.cancel();

                    entity.setPos(player.getX(), player.getY(), player.getZ());
                    entity.xo = player.xo; entity.yo = player.yo; entity.zo = player.zo;
                    entity.setYRot(player.getYRot());
                    entity.setXRot(player.getXRot());
                    entity.yRotO = player.yRotO;
                    entity.xRotO = player.xRotO;
                    entity.yHeadRot = player.yHeadRot;
                    entity.yHeadRotO = player.yHeadRotO;
                    entity.yBodyRot = player.yBodyRot;
                    entity.yBodyRotO = player.yBodyRotO;

                    entity.walkAnimation.setSpeed(player.walkAnimation.speed());
                    entity.walkAnimation.position(player.walkAnimation.position());

                    entity.swingTime = player.swingTime;
                    entity.swingingArm = player.swingingArm;
                    entity.tickCount = player.tickCount;

                    entity.setPose(player.getPose());
                    entity.setShiftKeyDown(player.isShiftKeyDown());
                    entity.setSprinting(player.isSprinting());

                    if (player.isUsingItem()) {
                        ((LivingEntityAccessor)entity).callSetLivingEntityFlag(1, true);
                        ((LivingEntityAccessor)entity).setUseItemRemaining(((LivingEntityAccessor)player).getUseItemRemaining());
                    } else {
                        ((LivingEntityAccessor)entity).callSetLivingEntityFlag(1, false);
                    }

                    EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                    EntityRenderer<? super LivingEntity> renderer = dispatcher.getRenderer(entity);
                    if (renderer != null) {
                        float bodyYaw = Mth.lerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
                        poseStack.pushPose();
                        if (info.getTransitionTicks() < info.getTransitionTime()) {
                            float scale = (float)Math.sin(((float)info.getTransitionTicks() / info.getTransitionTime()) * Math.PI / 2.0);
                            poseStack.scale(scale, scale, scale);
                        }
                        renderer.render(entity, bodyYaw, partialTicks, poseStack, buffer, packedLight);
                        poseStack.popPose();
                    }
                }
            }
        }
    }
}
