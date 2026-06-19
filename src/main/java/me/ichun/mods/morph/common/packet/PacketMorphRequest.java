package me.ichun.mods.morph.common.packet;
import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import java.util.Optional;
public class PacketMorphRequest extends AbstractPacket {
    private String morphId;
    public PacketMorphRequest() {}
    public PacketMorphRequest(String morphId) { this.morphId = morphId; }
    @Override public void writeTo(FriendlyByteBuf buffer) { buffer.writeUtf(morphId); }
    @Override public void readFrom(FriendlyByteBuf buffer) { morphId = buffer.readUtf(); }
    @Override public Optional<Runnable> process(Player player) {
        return Optional.of(() -> {
            if (player instanceof ServerPlayer serverPlayer) {
                PlayerMorphData data = MorphHandler.INSTANCE.getPlayerMorphData(serverPlayer);
                if (data != null) {
                    for (MorphVariant variant : data.acquiredMorphs) { if (variant.id.toString().equals(morphId)) { MorphApi.getApi().morphTo(serverPlayer, variant); return; } }
                    if (morphId.endsWith(":player")) { MorphApi.getApi().demorph(serverPlayer); }
                }
            }
        });
    }
}
