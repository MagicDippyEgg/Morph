package me.ichun.mods.morph.common.morph;
import me.ichun.mods.morph.api.IApi;
import me.ichun.mods.morph.api.morph.*;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.packet.*;
import me.ichun.mods.morph.common.morph.save.MorphSavedData;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class MorphHandler implements IApi {
    public static final MorphHandler INSTANCE = new MorphHandler();
    private MorphSavedData saveData;
    private PlayerMorphData clientData;

    private static final UUID MORPH_MOD_UUID = UUID.fromString("648D437C-00AA-418E-9366-0744B9E34661");
    private static final List<String> TRANSIENT_KEYS = Arrays.asList(
        "Health", "DeathTime", "HurtTime", "HurtByTimestamp", "Pos", "Motion", "Rotation", "UUID", "OnGround", "Air", "Fire", "FallDistance", "Invulnerable", "PortalCooldown", "AbsorptionAmount", "FallFlying", "Attributes", "Brain"
    );

    @Override public MorphInfo getMorphInfo(Player player) { return player.getCapability(MorphInfo.CAPABILITY_INSTANCE).orElse(new MorphInfoImpl(player)); }

    @Override public boolean morphTo(ServerPlayer player, MorphVariant variant) {
        MorphInfo info = getMorphInfo(player);
        info.setNextState(new MorphState(variant), 80);
        CompoundTag tag = info.write(new CompoundTag());
        Morph.channel.sendTo(new PacketMorphInfo(player.getId(), tag), player);
        Morph.channel.sendToTracking(new PacketMorphInfo(player.getId(), tag), player);

        updatePlayerAttributes(player, variant);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), Morph.Sounds.MORPH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f); player.refreshDimensions();
        return true;
    }

    public void updatePlayerAttributes(ServerPlayer player, MorphVariant variant) {
        updateAttribute(player, variant, Attributes.MAX_HEALTH);
        updateAttribute(player, variant, Attributes.MOVEMENT_SPEED);
        updateAttribute(player, variant, ForgeMod.STEP_HEIGHT_ADDITION.get());

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private void updateAttribute(ServerPlayer player, MorphVariant variant, Attribute attribute) {
        AttributeInstance playerAttr = player.getAttribute(attribute);
        if (playerAttr == null) return;
        playerAttr.removeModifier(MORPH_MOD_UUID);

        if (!variant.id.getPath().equals("player")) {
            LivingEntity dummy = variant.createEntity(player.level());
            if (dummy != null) {
                AttributeInstance dummyAttr = dummy.getAttribute(attribute);
                if (dummyAttr != null) {
                    double dummyVal = dummyAttr.getValue();
                    double playerBase = playerAttr.getBaseValue();
                    double diff = dummyVal - playerBase;
                    if (Math.abs(diff) > 0.0001) {
                        playerAttr.addTransientModifier(new AttributeModifier(MORPH_MOD_UUID, "Morph Modifier", diff, AttributeModifier.Operation.ADDITION));
                    }
                }
            }
        }
    }

    @Override public boolean demorph(ServerPlayer player) {
        return morphTo(player, MorphVariant.createPlayerMorph(player.getUUID(), true));
    }

    @Override public MorphVariant createVariant(LivingEntity living) {
        if (living == null) return null;
        MorphVariant variant = new MorphVariant(ForgeRegistries.ENTITY_TYPES.getKey(living.getType()));
        if (!(living instanceof Player)) {
            CompoundTag tag = new CompoundTag();
            living.saveWithoutId(tag);
            for (String key : TRANSIENT_KEYS) { tag.remove(key); }
            variant.nbt = tag;
        }
        return variant;
    }

    @Override public boolean acquireMorph(ServerPlayer player, MorphVariant variant) {
        if (variant == null) return false;
        PlayerMorphData data = getPlayerMorphData(player);
        if (data != null && data.addVariant(variant) != null) {
            if (saveData != null) { saveData.setDirty(); }
            Morph.channel.sendTo(new PacketAcquisition(variant.id.toString()), player);
            syncToClient(player);
            return true;
        }
        return false;
    }

    public PlayerMorphData getPlayerMorphData(Player player) {
        if (player.level().isClientSide) return clientData;
        if (saveData == null && player.getServer() != null) {
            saveData = MorphSavedData.get(player.getServer().overworld());
        }
        if (saveData == null) return new PlayerMorphData(player.getUUID());
        return saveData.playerMorphs.computeIfAbsent(player.getUUID(), PlayerMorphData::new);
    }

    public void setSaveData(MorphSavedData data) { this.saveData = data; }
    public void setClientPlayerMorphData(PlayerMorphData data) { this.clientData = data; }

    public void syncToClient(ServerPlayer player) {
        PlayerMorphData data = getPlayerMorphData(player);
        if (data != null) { Morph.channel.sendTo(new PacketSyncAcquiredMorphs(data.serialize()), player); }
    }

    @Override public ArrayList<me.ichun.mods.morph.api.mob.trait.Trait<?>> getTraitsForVariant(MorphVariant variant, Player player) { return new ArrayList<>(); }
}
