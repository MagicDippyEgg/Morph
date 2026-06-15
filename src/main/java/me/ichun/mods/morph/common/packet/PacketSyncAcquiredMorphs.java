package me.ichun.mods.morph.common.packet;
import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;
public class PacketSyncAcquiredMorphs extends AbstractPacket {
    private CompoundTag tag;
    public PacketSyncAcquiredMorphs() {}
    public PacketSyncAcquiredMorphs(CompoundTag tag) { this.tag = tag; }
    @Override public void writeTo(FriendlyByteBuf buffer) { buffer.writeNbt(tag); }
    @Override public void readFrom(FriendlyByteBuf buffer) { tag = buffer.readNbt(); }
    @Override public Optional<Runnable> process(Player player) {
        return Optional.of(() -> { MorphHandler.INSTANCE.setClientPlayerMorphData(PlayerMorphData.deserialize(tag)); });
    }
}
