package me.ichun.mods.morph.client;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.client.gui.GuiMorphSelector;
import me.ichun.mods.morph.common.packet.PacketMorphInfo;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "morph", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MorphClient {
    public static final KeyMapping OPEN_GUI = new KeyMapping("key.morph.open", GLFW.GLFW_KEY_M, "key.categories.morph");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI);
        MinecraftForge.EVENT_BUS.register(new MorphClient());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (OPEN_GUI.consumeClick()) {
                Minecraft.getInstance().setScreen(new GuiMorphSelector());
            }

            if (Minecraft.getInstance().level != null) {
                Iterator<Map.Entry<Integer, net.minecraft.nbt.CompoundTag>> it = PacketMorphInfo.PENDING_UPDATES.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, net.minecraft.nbt.CompoundTag> entry = it.next();
                    Entity entity = Minecraft.getInstance().level.getEntity(entry.getKey());
                    if (entity instanceof Player) {
                        MorphInfo info = MorphApi.getApi().getMorphInfo((Player) entity);
                        if (info != null) {
                            info.read(entry.getValue());
                            it.remove();
                        }
                    }
                }
            }
        }
    }
}
