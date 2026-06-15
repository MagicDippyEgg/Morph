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
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
public final class MorphHandler implements IApi {
    public static final MorphHandler INSTANCE = new MorphHandler();
    private MorphSavedData saveData;
    private PlayerMorphData clientData;
    @Override public MorphInfo getMorphInfo(Player player) { return player.getCapability(MorphInfo.CAPABILITY_INSTANCE).orElse(new MorphInfoImpl(player)); }
    @Override public boolean morphTo(ServerPlayer player, MorphVariant variant) {
        MorphInfo info = getMorphInfo(player);
        info.setNextState(new MorphState(variant), 80);
        CompoundTag tag = info.write(new CompoundTag());
        Morph.channel.sendTo(new PacketMorphInfo(player.getId(), tag), player);
        Morph.channel.sendToTracking(new PacketMorphInfo(player.getId(), tag), player);
        return true;
    }
    @Override public boolean demorph(ServerPlayer player) { return morphTo(player, MorphVariant.createPlayerMorph(player.getUUID(), true)); }
    @Override public MorphVariant createVariant(LivingEntity living) {
        if (living == null) return null;
        MorphVariant variant = new MorphVariant(ForgeRegistries.ENTITY_TYPES.getKey(living.getType()));
        if (!(living instanceof Player)) { CompoundTag tag = new CompoundTag(); living.saveWithoutId(tag); variant.nbt = tag; }
        return variant;
    }
    @Override public boolean acquireMorph(ServerPlayer player, MorphVariant variant) {
        if (variant == null) return false;
        PlayerMorphData data = getPlayerMorphData(player);
        if (data != null && data.addVariant(variant) != null) {
            if (saveData != null) saveData.setDirty();
            Morph.channel.sendTo(new PacketAcquisition(variant.id.toString()), player);
            syncToClient(player);
            return true;
        }
        return false;
    }
    public PlayerMorphData getPlayerMorphData(Player player) {
        if (player.level().isClientSide) return clientData;
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
