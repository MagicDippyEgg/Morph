package me.ichun.mods.morph.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.morph.client.render.MorphRenderHandler;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelRenderer.class)
public abstract class ModelRendererMixin
{
    @Inject(method = "render(Lcom.mojang.blaze3d.vertex.PoseStack;Lcom.mojang.blaze3d.vertex.VertexConsumer;IIFFFF)V", at = @At(value = "INVOKE", target = "Lnet.minecraft.client.model.geom.ModelPart;doRender(Lcom.mojang.blaze3d.vertex.PoseStack$Entry;Lcom.mojang.blaze3d.vertex.VertexConsumer;IIFFFF)V"), cancellable = true)
    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha, CallbackInfo ci)
    {
        if(MorphRenderHandler.currentCapture != null)
        {
            MorphRenderHandler.currentCapture.capture((ModelRenderer)(Object)this, matrixStackIn);

            //call the code after doRender and return, do not allow the ModelRenderer to render.
            for(ModelRenderer modelrenderer : ((ModelRenderer)(Object)this).childModels) {
                modelrenderer.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
            }

            matrixStackIn.pop();

            //return call
            ci.cancel();
        }
    }
}
