package me.ichun.mods.morph.common.packet;

import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;

public class PacketMorphInfo extends AbstractPacket {
    private int entityId;
    private CompoundTag tag;

    public PacketMorphInfo() {}
    public PacketMorphInfo(int entityId, CompoundTag tag) {
        this.entityId = entityId;
        this.tag = tag;
    }

    @Override
    public void writeTo(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeNbt(tag);
    }

    @Override
    public void readFrom(FriendlyByteBuf buffer) {
        entityId = buffer.readInt();
        tag = buffer.readNbt();
    }

    @Override
    public Optional<Runnable> process(Player player) {
        return Optional.of(() -> {
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof Player) {
                MorphInfo info = MorphApi.getApi().getMorphInfo((Player) entity);
                if (info != null) info.read(tag);
            }
        });
    }
}
