package me.ichun.mods.morph.common.core;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.api.morph.MorphVariant;
import me.ichun.mods.morph.common.Morph;
import me.ichun.mods.morph.common.capability.MorphCapProvider;
import me.ichun.mods.morph.common.command.MorphCommand;
import me.ichun.mods.morph.common.morph.MorphHandler;
import me.ichun.mods.morph.common.morph.save.MorphSavedData;
import me.ichun.mods.morph.common.morph.save.PlayerMorphData;
import me.ichun.mods.morph.common.packet.PacketMorphInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class EventHandlerServer {
    @SubscribeEvent public void onAttachCaps(AttachCapabilitiesEvent<Entity> event) { if (event.getObject() instanceof Player) { event.addCapability(new ResourceLocation(Morph.MOD_ID, "morph"), new MorphCapProvider((Player) event.getObject())); } }
    @SubscribeEvent public void onRegisterCommands(RegisterCommandsEvent event) { MorphCommand.register(event.getDispatcher()); }

    @SubscribeEvent public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(event.player);
            info.tick();
            if (info.getCurrentState() != null && info.getCurrentState().variant != null) {
                String path = info.getCurrentState().variant.id.getPath();
                Player player = event.player;

                boolean canFly = path.equals("bat") || path.equals("parrot") || path.equals("ghast") || path.equals("blaze") || path.equals("bee") || path.equals("phantom") || path.equals("ender_dragon") || path.equals("wither") || path.equals("allay") || path.equals("vex");
                if (canFly) {
                    if (!player.getAbilities().mayfly) { player.getAbilities().mayfly = true; player.onUpdateAbilities(); }
                } else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false; player.getAbilities().flying = false; player.onUpdateAbilities();
                }

                boolean isAquatic = path.equals("squid") || path.contains("fish") || path.equals("dolphin") || path.equals("guardian") || path.equals("elder_guardian") || path.equals("axolotl") || path.equals("glow_squid");
                if (isAquatic) { if (player.isEyeInFluid(FluidTags.WATER)) { player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 4)); } }

                boolean isWaterSensitive = path.equals("enderman") || path.equals("blaze") || path.equals("strider") || path.equals("magma_cube") || path.equals("snow_golem");
                if (isWaterSensitive && player.isInWaterRainOrBubble()) { player.hurt(player.level().damageSources().drown(), 1.0F); }

                boolean hasSlowFall = path.equals("chicken") || path.equals("phantom") || path.equals("allay");
                if (hasSlowFall && !player.onGround() && player.getDeltaMovement().y < 0) {
                    Vec3 vec3 = player.getDeltaMovement(); player.setDeltaMovement(vec3.x, Math.max(vec3.y, -0.05D), vec3.z); player.fallDistance = 0;
                }

                boolean canClimb = path.equals("spider") || path.equals("cave_spider");
                if (canClimb && player.horizontalCollision) { Vec3 vec3 = player.getDeltaMovement(); player.setDeltaMovement(vec3.x, 0.15D, vec3.z); }

                if (player.level().isDay() && !player.level().isRaining()) {
                    boolean isUndead = path.equals("zombie") || path.equals("skeleton") || path.equals("zombie_villager") || path.equals("husk") || path.equals("drowned") || path.equals("phantom") || path.equals("skeleton_horse") || path.equals("zombie_horse");
                    if (isUndead) {
                        float f = player.getLightLevelDependentMagicValue();
                        BlockPos pos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
                        if (f > 0.5F && player.level().canSeeSky(pos)) { if (player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) { player.setSecondsOnFire(8); } }
                    }
                }
            }
        }
    }

    @SubscribeEvent public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof Player player) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            if (info.getCurrentState() != null && info.getCurrentState().variant != null) {
                String path = info.getCurrentState().variant.id.getPath();
                boolean isFireProof = path.equals("blaze") || path.equals("ghast") || path.equals("magma_cube") || path.equals("zombified_piglin") || path.equals("piglin") || path.equals("piglin_brute") || path.equals("strider") || path.equals("wither") || path.equals("wither_skeleton");
                if (isFireProof && (event.getSource().is(DamageTypes.IN_FIRE) || event.getSource().is(DamageTypes.ON_FIRE) || event.getSource().is(DamageTypes.LAVA) || event.getSource().is(DamageTypes.HOT_FLOOR))) { event.setCanceled(true); }
            }
        }
    }

    @SubscribeEvent public void onTargetChange(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && event.getEntity() instanceof Mob mob) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            if (info.getCurrentState() != null && info.getCurrentState().variant != null) {
                String path = info.getCurrentState().variant.id.getPath();
                // Same type neutrality
                if (info.getCurrentState().variant.id.equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()))) { event.setCanceled(true); return; }
                // Boss intimidation (Fear)
                if (path.equals("wither") || path.equals("ender_dragon") || path.equals("warden")) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent public void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            if (info.getCurrentState() != null && info.getCurrentState().variant != null) {
                String path = info.getCurrentState().variant.id.getPath();
                if (path.equals("bat") || path.equals("parrot") || path.equals("bee") || path.equals("vex") || path.equals("allay") || path.equals("chicken")) { event.setCanceled(true); }
            }
        }
    }

    @SubscribeEvent public void onEntityDeath(LivingDeathEvent event) { if (event.getSource().getEntity() instanceof ServerPlayer player) { MorphApi.getApi().acquireMorph(player, MorphApi.getApi().createVariant(event.getEntity())); } }
    @SubscribeEvent public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerMorphData data = MorphHandler.INSTANCE.getPlayerMorphData(player);
            MorphInfo info = MorphApi.getApi().getMorphInfo(player);
            if (data.currentVariant != null && !data.currentVariant.id.getPath().equals("player")) { info.setNextState(new MorphState(data.currentVariant), 1); }
            Morph.channel.sendTo(new PacketMorphInfo(player.getId(), info.write(new CompoundTag())), player);
            MorphHandler.INSTANCE.syncToClient(player);
            if (info.getCurrentState() != null) { MorphHandler.INSTANCE.updatePlayerAttributes(player, info.getCurrentState().variant); }
        }
    }
    @SubscribeEvent public void onStartTracking(PlayerEvent.StartTracking event) { if (event.getTarget() instanceof Player trackedPlayer && event.getEntity() instanceof ServerPlayer tracker) { MorphInfo info = MorphApi.getApi().getMorphInfo(trackedPlayer); Morph.channel.sendTo(new PacketMorphInfo(trackedPlayer.getId(), info.write(new CompoundTag())), tracker); } }
    @SubscribeEvent public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(MorphInfo.CAPABILITY_INSTANCE).ifPresent(oldInfo -> {
                event.getEntity().getCapability(MorphInfo.CAPABILITY_INSTANCE).ifPresent(newInfo -> {
                    newInfo.read(oldInfo.write(new CompoundTag()));
                    if (newInfo.getCurrentState() != null) { MorphHandler.INSTANCE.updatePlayerAttributes((ServerPlayer) event.getEntity(), newInfo.getCurrentState().variant); }
                });
            });
        }
    }
    @SubscribeEvent public void onWorldLoad(LevelEvent.Load event) { if (!event.getLevel().isClientSide() && event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) { if (level.dimension() == Level.OVERWORLD) { MorphHandler.INSTANCE.setSaveData(MorphSavedData.get(level)); } } }
}
