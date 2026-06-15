package me.ichun.mods.morph.common.packet;
import me.ichun.mods.ichunutil.common.network.AbstractPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;
public class PacketAcquisition extends AbstractPacket {
    private String name;
    public PacketAcquisition() {}
    public PacketAcquisition(String name) { this.name = name; }
    @Override public void writeTo(FriendlyByteBuf buffer) { buffer.writeUtf(name); }
    @Override public void readFrom(FriendlyByteBuf buffer) { name = buffer.readUtf(); }
    @Override public Optional<Runnable> process(Player player) {
        return Optional.of(() -> { player.displayClientMessage(Component.literal("Acquired morph: " + name), true); });
    }
}
