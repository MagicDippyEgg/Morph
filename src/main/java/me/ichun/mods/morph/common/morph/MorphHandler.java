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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class MorphHandler implements IApi {
    public static final MorphHandler INSTANCE = new MorphHandler();
    private MorphSavedData saveData;
    private PlayerMorphData clientData;

    public static final UUID HEALTH_MOD_UUID = UUID.fromString("648D437C-00AA-418E-9366-0744B9E34661");
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

        updatePlayerHealth(player, variant);
        return true;
    }

    public void updatePlayerHealth(ServerPlayer player, MorphVariant variant) {
        AttributeInstance attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        attr.removeModifier(HEALTH_MOD_UUID);

        if (!variant.id.getPath().equals("player")) {
            LivingEntity dummy = variant.createEntity(player.level());
            if (dummy != null) {
                float morphMaxHealth = dummy.getMaxHealth();
                float baseHealth = (float) attr.getBaseValue();
                float diff = morphMaxHealth - baseHealth;
                attr.addTransientModifier(new AttributeModifier(HEALTH_MOD_UUID, "Morph Health", diff, AttributeModifier.Operation.ADDITION));
            }
        }

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
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
