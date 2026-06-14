package me.ichun.mods.morph.common;

import me.ichun.mods.ichunutil.loader.forge.PacketChannelForge;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.common.core.EventHandlerServer;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("morph")
public class Morph {
    public static final String MOD_ID = "morph";
    public static PacketChannelForge channel;

    public Morph() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::registerCaps);

        MorphApi.setApiImpl(MorphHandler.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new EventHandlerServer());

        channel = new PacketChannelForge(new ResourceLocation(MOD_ID, "channel"), 1,
            PacketMorphInfo.class, PacketAcquisition.class, PacketMorphRequest.class, PacketSyncAcquiredMorphs.class);
    }

    private void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(MorphInfo.class);
    }
}
