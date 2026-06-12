package me.ichun.mods.morph.api.morph;

import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.mob.trait.Trait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

public class MorphState implements Comparable<MorphState>
{
    public MorphVariant variant;
    private LivingEntity entInstance;
    private final Collection<ItemEntity> entInstanceDropCapture = new ArrayList<>();
    public float renderedShadowSize;
    public ArrayList<Trait<?>> traits = new ArrayList<>();

    private MorphState(){}

    public MorphState(MorphVariant variant, Player player)
    {
        this.variant = variant;
        this.traits = MorphApi.getApiImpl().getTraitsForVariant(variant, player);
    }

    //For Traits
    public void activateHooks()
    {
        for(Trait<?> trait : traits)
        {
            trait.addHooks();
        }
    }
    public void deactivateHooks()
    {
        for(Trait<?> trait : traits)
        {
            trait.removeHooks();
        }
    }

    public void tick(Player player, boolean resetInventory)
    {
        LivingEntity livingInstance = getEntityInstance(player.level(), player);
        livingInstance.captureDrops(entInstanceDropCapture); //We don't want our mob instance to drop items
        entInstanceDropCapture.clear(); //Have the items, GC.

        syncEntityPosRotWithPlayer(livingInstance, player);

        syncInventory(livingInstance, player, true); //reset the inventory so the entity doesn't actually use our equipment when ticking.

        // In 1.20.1, tick() is fine. canUpdate() might be gone or renamed.
        livingInstance.tick();

        syncEntityWithPlayer(livingInstance, player);

        if(!resetInventory)
        {
            syncInventory(livingInstance, player, false); //sync the inventory for rendering purposes.
        }

        livingInstance.getEntityData().setDirty(); //we don't want to flood the client with packets for an entity it can't find.
    }

    public void tickTraits()
    {
        for(Trait<?> trait : traits)
        {
            trait.doTick(1F);
        }
    }

    @Nonnull
    public LivingEntity getEntityInstance(Level level, @Nullable Player player)
    {
        if(entInstance == null || entInstance.level() != level)
        {
            entInstance = variant.createEntityInstance(level, player);

            for(Trait<?> trait : traits)
            {
                trait.livingInstance = entInstance;
            }
        }

        return entInstance;
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.put("variant", variant.write(new CompoundTag()));
        return tag;
    }

    public void read(CompoundTag tag)
    {
        variant = MorphVariant.createFromNBT(tag.getCompound("variant"));
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof MorphState)
        {
            MorphState state = (MorphState)obj;
            return Objects.equals(variant, state.variant);
        }
        return false;
    }

    @Override
    public int compareTo(MorphState o)
    {
        return variant.compareTo(o.variant);
    }

    public static MorphState createFromNbt(CompoundTag tag)
    {
        MorphState state = new MorphState();
        state.read(tag);
        return state;
    }

    public static void syncEntityPosRotWithPlayer(LivingEntity living, Player player)
    {
        living.tickCount = player.tickCount;

        living.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        living.xo = player.xo;
        living.yo = player.yo;
        living.zo = player.zo;

        living.xOld = player.xOld;
        living.yOld = player.yOld;
        living.zOld = player.zOld;

        living.yRotO = player.yRotO;
        living.xRotO = player.xRotO;

        living.setYHeadRot(player.getYHeadRot());
        living.yHeadRotO = player.yHeadRotO;

        living.yBodyRot = player.yBodyRot;
        living.yBodyRotO = player.yBodyRotO;

        //Clear potions so they don't get ticked when we tick this entity
        living.removeAllEffects();
    }

    public static void syncEntityWithPlayer(LivingEntity living, Player player)
    {
        syncEntityPosRotWithPlayer(living, player); //resync with the player position in case the entity moved whilst ticking.

        //Others
        living.walkDist = player.walkDist;
        living.walkDistO = player.walkDistO;

        living.setDeltaMovement(player.getDeltaMovement());

        //Entity stuff
        living.horizontalCollision = player.horizontalCollision;
        living.verticalCollision = player.verticalCollision;
        living.setOnGround(player.onGround());
        living.setShiftKeyDown(player.isShiftKeyDown());
        living.setSwimming(player.isSwimming());
        living.setSprinting(player.isSprinting());

        living.setHealth(living.getMaxHealth() * (player.getHealth() / player.getMaxHealth()));
        living.hurtTime = player.hurtTime;
        living.deathTime = player.deathTime;

        //LivingRender related stuff
        living.swingTime = player.swingTime;
        living.swinging = player.swinging;
        living.swingingArm = player.swingingArm;
        living.attackAnim = player.attackAnim;
        living.oAttackAnim = player.oAttackAnim;

        // ridingEntity is gone, use getVehicle()
        // living.ridingEntity = player.ridingEntity;

        Pose pose = living.getPose();
        living.setPose(player.getPose());

        if(pose != living.getPose())
        {
            living.refreshDimensions();
        }

        if(player.getSleepingPos().isPresent())
        {
            living.setSleepingPos(player.getSleepingPos().get());
        }
        else
        {
            living.clearSleepingPos();
        }

        living.setInvisible(player.isInvisible());

        living.setGlowingTag(player.isGlowing());

        //EntityRendererManager stuff
        living.setRemainingFireTicks(player.getRemainingFireTicks());

        //Sync potions for rendering purposes
        living.getActiveEffectsMap().putAll(player.getActiveEffectsMap());

        specialEntityPlayerSync(living, player);
    }

    public static void specialEntityPlayerSync(LivingEntity living, Player player)
    {
        for(BiConsumer<LivingEntity, Player> consumer : MorphApi.getApiImpl().getModPlayerMorphSyncConsumers())
        {
            consumer.accept(living, player);
        }
    }

    public static void syncInventory(LivingEntity living, Player player, boolean reset)
    {
        if(living instanceof Player)
        {
            Player playerEntity = (Player)living;

            //player entity plays sound when equipping items.
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                boolean shouldReset = reset && (value == EquipmentSlot.MAINHAND || value == EquipmentSlot.OFFHAND);
                if(!ItemStack.matches(living.getItemBySlot(value), shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value)))
                {
                    ItemStack copy = shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value).copy();
                    if (value == EquipmentSlot.MAINHAND) {
                        playerEntity.getInventory().items.set(playerEntity.getInventory().selected, copy);
                    } else if (value == EquipmentSlot.OFFHAND) {
                        playerEntity.getInventory().offhand.set(0, copy);
                    } else if (value.getType() == EquipmentSlot.Type.ARMOR) {
                        playerEntity.getInventory().armor.set(value.getIndex(), copy);
                    }
                }
            }
        }
        else
        {
            for(EquipmentSlot value : EquipmentSlot.values())
            {
                boolean shouldReset = reset && (value == EquipmentSlot.MAINHAND || value == EquipmentSlot.OFFHAND);
                if(!ItemStack.matches(living.getItemBySlot(value), shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value)))
                {
                    living.setItemSlot(value, shouldReset ? ItemStack.EMPTY : player.getItemBySlot(value).copy());
                }
            }
        }

        if(player.isUsingItem())
        {
            if(player.getUseItemRemainingTicks() == 1)
            {
                InteractionHand hand = player.getUsedItemHand();
                living.startUsingItem(hand);
            }
        }
        else
        {
            living.stopUsingItem();
        }
    }
}
