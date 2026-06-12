import os

replacements = {
    # Packages
    "net.minecraft.nbt.CompoundNBT": "net.minecraft.nbt.CompoundTag",
    "net.minecraft.nbt.INBT": "net.minecraft.nbt.Tag",
    "net.minecraft.nbt.ListNBT": "net.minecraft.nbt.ListTag",
    "net.minecraft.world.World": "net.minecraft.world.level.Level",
    "net.minecraft.world.server.ServerWorld": "net.minecraft.server.level.ServerLevel",
    "net.minecraft.client.world.ClientWorld": "net.minecraft.client.multiplayer.ClientLevel",
    "net.minecraft.util.text.ITextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.StringTextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.TranslationTextComponent": "net.minecraft.network.chat.Component",
    "net.minecraft.util.text.TextFormatting": "net.minecraft.ChatFormatting",
    "net.minecraft.util.ResourceLocation": "net.minecraft.resources.ResourceLocation",
    "net.minecraft.util.RegistryKey": "net.minecraft.resources.ResourceKey",
    "net.minecraft.entity.player.PlayerEntity": "net.minecraft.world.entity.player.Player",
    "net.minecraft.entity.player.ServerPlayerEntity": "net.minecraft.server.level.ServerPlayer",
    "net.minecraft.util.math.vector.Vector3d": "net.minecraft.world.phys.Vec3",
    "net.minecraft.util.math.vector.Vector3f": "com.mojang.math.Axis",
    "net.minecraft.entity.LivingEntity": "net.minecraft.world.entity.LivingEntity",
    "net.minecraft.entity.Entity": "net.minecraft.world.entity.Entity",
    "net.minecraft.entity.EntityType": "net.minecraft.world.entity.EntityType",
    "net.minecraft.entity.ai.attributes.Attribute": "net.minecraft.world.entity.ai.attributes.Attribute",
    "net.minecraft.entity.ai.attributes.Attributes": "net.minecraft.world.entity.ai.attributes.Attributes",
    "net.minecraft.entity.ai.attributes.ModifiableAttributeInstance": "net.minecraft.world.entity.ai.attributes.AttributeInstance",
    "net.minecraft.entity.ai.attributes.AttributeModifier": "net.minecraft.world.entity.ai.attributes.AttributeModifier",
    "net.minecraft.client.renderer.MatrixStack": "com.mojang.blaze3d.vertex.PoseStack",
    "net.minecraft.client.renderer.IRenderTypeBuffer": "net.minecraft.client.renderer.MultiBufferSource",
    "net.minecraft.client.renderer.IVertexBuilder": "com.mojang.blaze3d.vertex.VertexConsumer",
    "net.minecraft.util.math.MathHelper": "net.minecraft.util.Mth",
    "net.minecraft.util.math.BlockPos": "net.minecraft.core.BlockPos",
    "net.minecraft.world.level.storage.WorldSavedData": "net.minecraft.world.level.saveddata.SavedData",
    "net.minecraft.network.PacketBuffer": "net.minecraft.network.FriendlyByteBuf",
    "net.minecraftforge.fml.network.NetworkEvent": "net.minecraftforge.network.NetworkEvent",
    "net.minecraftforge.fml.network.PacketDistributor": "net.minecraftforge.network.PacketDistributor",
    "net.minecraft.entity.AgeableEntity": "net.minecraft.world.entity.AgeableMob",
    "net.minecraft.entity.MobEntity": "net.minecraft.world.entity.Mob",
    "net.minecraft.util.Hand": "net.minecraft.world.InteractionHand",
    "net.minecraft.util.HandSide": "net.minecraft.world.entity.HumanoidArm",
    "net.minecraft.inventory.EquipmentSlotType": "net.minecraft.world.entity.EquipmentSlot",
    "net.minecraft.client.renderer.model.ModelRenderer": "net.minecraft.client.model.geom.ModelPart",
    "net.minecraft.client.renderer.entity.model.EntityModel": "net.minecraft.client.model.EntityModel",
    "net.minecraft.item.ItemStack": "net.minecraft.world.item.ItemStack",
    "net.minecraft.item.Items": "net.minecraft.world.item.Items",
    "net.minecraft.util.DamageSource": "net.minecraft.world.damagesource.DamageSource",
    "net.minecraft.world.Explosion": "net.minecraft.world.level.Explosion",
    "net.minecraft.block.Block": "net.minecraft.world.level.block.Block",
    "net.minecraft.block.BlockState": "net.minecraft.world.level.block.state.BlockState",
    "net.minecraft.block.Blocks": "net.minecraft.world.level.block.Blocks",
    "net.minecraft.command": "net.minecraft.commands",

    # Common class name changes in code
    "CompoundNBT": "CompoundTag",
    "INBT": "Tag",
    "ListNBT": "ListTag",
    "PlayerEntity": "Player",
    "ServerPlayerEntity": "ServerPlayer",
    "MatrixStack": "PoseStack",
    "IRenderTypeBuffer": "MultiBufferSource",
    "IVertexBuilder": "VertexConsumer",
    "ITextComponent": "Component",
    "StringTextComponent": "Component",
    "TranslationTextComponent": "Component",
    "TextFormatting": "ChatFormatting",
    "MathHelper": "Mth",
    "PacketBuffer": "FriendlyByteBuf",
    "AgeableEntity": "AgeableMob",
    "MobEntity": "Mob",
    "EquipmentSlotType": "EquipmentSlot",
    "WorldSavedData": "SavedData",
    "ModelRenderer": "ModelPart",
    "EntitySize": "EntityDimensions",

    # Method name changes
    "world.isRemote": "level().isClientSide",
    "getEntityId()": "getId()",
    "getUniqueID()": "getUUID()",
    "setUniqueId": "setUUID()",
    "writeAdditional": "addAdditionalSaveData",
    "readAdditional": "readAdditionalSaveData",
    "getDataManager()": "getEntityData()",
    "recalculateSize()": "refreshDimensions()",
    "getPosX()": "getX()",
    "getPosY()": "getY()",
    "getPosZ()": "getZ()",
    "getPosition()": "blockPosition()",
    "flexible": "scalable",
}

def migrate_file(filepath):
    if not os.path.isfile(filepath): return
    with open(filepath, 'r') as f:
        content = f.read()

    changed = False
    for old, new in replacements.items():
        if old in content:
            content = content.replace(old, new)
            changed = True

    if changed:
        with open(filepath, 'w') as f:
            f.write(content)
        print(f"Migrated {filepath}")

for root, dirs, files in os.walk("src"):
    for file in files:
        if file.endswith(".java"):
            migrate_file(os.path.join(root, file))
