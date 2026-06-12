package me.ichun.mods.morph.client;

import me.ichun.mods.morph.client.gui.GuiMorphSelector;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

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
        if (event.phase == TickEvent.Phase.END && OPEN_GUI.consumeClick()) {
            Minecraft.getInstance().setScreen(new GuiMorphSelector());
        }
    }
}
