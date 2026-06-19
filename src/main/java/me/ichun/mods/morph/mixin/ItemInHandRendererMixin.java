package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (MorphApi.getApi() == null) return;
        MorphInfo info = MorphApi.getApi().getMorphInfo(player);
        if (info != null && info.getCurrentState() != null && !info.getCurrentState().variant.id.getPath().equals("player")) {
            // Cancel rendering to hide the human hand in first person
            ci.cancel();
        }
    }
}
