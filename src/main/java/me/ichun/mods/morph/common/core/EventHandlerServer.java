package me.ichun.mods.morph.common.core;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.capability.MorphCapProvider;
import me.ichun.mods.morph.common.command.MorphCommand;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.MorphSavedData;
import me.ichun.mods.morph.common.packet.PacketMorphInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EventHandlerServer {
    @SubscribeEvent public void onAttachCaps(AttachCapabilitiesEvent<Entity> event) { if (event.getObject() instanceof Player) { event.addCapability(new ResourceLocation(Morph.MOD_ID, "morph"), new MorphCapProvider((Player) event.getObject())); } }
    @SubscribeEvent public void onRegisterCommands(RegisterCommandsEvent event) { MorphCommand.register(event.getDispatcher()); }

    @SubscribeEvent public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(event.player);
            info.tick();

            // Passive Traits Logic
            if (info.getCurrentState() != null && info.getCurrentState().variant != null) {
                String path = info.getCurrentState().variant.id.getPath();
                Player player = event.player;

                // Flight (Bat, Parrot, Ghast, Blaze, etc.)
                boolean canFly = path.equals("bat") || path.equals("parrot") || path.equals("ghast") || path.equals("blaze") || path.equals("bee") || path.equals("phantom");
                if (canFly) {
                    if (!player.getAbilities().mayfly) {
                        player.getAbilities().mayfly = true;
                        player.onUpdateAbilities();
                    }
                } else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }

                // Water Breathing (Fish, Squid, etc.)
                boolean isAquatic = path.equals("squid") || path.contains("fish") || path.equals("dolphin") || path.equals("guardian") || path.equals("elder_guardian");
                if (isAquatic) {
                    if (player.isEyeInFluid(FluidTags.WATER)) {
                        player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 4));
                    }
                }
            }
        }
    }

    @SubscribeEvent public void onEntityDeath(LivingDeathEvent event) { if (event.getSource().getEntity() instanceof ServerPlayer player) { MorphApi.getApi().acquireMorph(player, MorphApi.getApi().createVariant(event.getEntity())); } }

    @SubscribeEvent public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            Morph.channel.sendTo(new PacketMorphInfo(player.getId(), info.write(new CompoundTag())), player);
            MorphHandler.INSTANCE.syncToClient(player);
            if (info.getCurrentState() != null) { MorphHandler.INSTANCE.updatePlayerAttributes(player, info.getCurrentState().variant); }
            for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                if (other != player) {
                    MorphInfo otherInfo = MorphApi.getApi().getMorphInfo(other);
                    Morph.channel.sendTo(new PacketMorphInfo(other.getId(), otherInfo.write(new CompoundTag())), player);
                }
            }
        }
    }

    @SubscribeEvent public void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Player trackedPlayer && event.getEntity() instanceof ServerPlayer tracker) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(trackedPlayer);
            Morph.channel.sendTo(new PacketMorphInfo(trackedPlayer.getId(), info.write(new CompoundTag())), tracker);
        }
    }

    @SubscribeEvent public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(MorphInfo.CAPABILITY_INSTANCE).ifPresent(oldInfo -> {
                event.getEntity().getCapability(MorphInfo.CAPABILITY_INSTANCE).ifPresent(newInfo -> {
                    newInfo.read(oldInfo.write(new CompoundTag()));
                    if (newInfo.getCurrentState() != null) {
                        MorphHandler.INSTANCE.updatePlayerAttributes((ServerPlayer) event.getEntity(), newInfo.getCurrentState().variant);
                    }
                });
            });
        }
    }
    @SubscribeEvent public void onWorldLoad(LevelEvent.Load event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
            if (level.dimension() == Level.OVERWORLD) {
                MorphHandler.INSTANCE.setSaveData(MorphSavedData.get(level));
            }
        }
    }
}
