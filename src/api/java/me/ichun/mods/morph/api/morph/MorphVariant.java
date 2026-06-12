package me.ichun.mods.morph.api.morph;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import me.ichun.mods.morph.api.MorphApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.RandomStringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiConsumer;

public class MorphVariant implements Comparable<MorphVariant>
{
    public static final int IDENTIFIER_LENGTH = 20;
    public static final String IDENTIFIER_DEFAULT_PLAYER_STATE = "default_player_state";
    public static final String NBT_PLAYER_ID = "Morph_Player_ID";
    public static String[] TAGS_TO_TAKE = new String[] { "CustomName", "CustomNameVisible", "ForgeCaps", "ForgeData" }; //Intentionally non-final. If you're going to be adding to this please remember to include tthe originals!

    @Nonnull
    public ResourceLocation id; // the ID of the morph
    @Nonnull
    public CompoundTag nbtMorph; //special morph specific NBT
    public CompoundTag nbtCommon; //common nbt tags shared by all variants
    public ArrayList<Variant> variants; //if populated, thisVariant should not be used.

    public Variant thisVariant; //this is set for a specific variant/render. variants should be left empty.

    public MorphVariant(ResourceLocation id)
    {
        this.id = id;
        this.nbtMorph = new CompoundTag();
        this.variants = new ArrayList<>();
    }

    private MorphVariant()
    {
        this.variants = new ArrayList<>();
    }

    public void setLiving(CompoundTag tag) //Not used for PLAYERS
    {
        nbtCommon = tag;
    }

    public void writeSupportedAttributes(LivingEntity living)
    {
        for(Map.Entry<ResourceLocation, AttributeConfig> e : MorphApi.getApiImpl().getSupportedAttributes().entrySet())
        {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(e.getKey());
            if(attribute != null && living.getAttributes().hasAttribute(attribute))
            {
                AttributeConfig attributeConfig = e.getValue();
                double value = living.getAttributeValue(attribute);
                if(attributeConfig.moreIsBetter) //more is better
                {
                    if(attributeConfig.cap != null && value > attributeConfig.cap)
                    {
                        value = attributeConfig.cap;
                    }
                }
                else //less is better
                {
                    if(attributeConfig.cap != null && value < attributeConfig.cap)
                    {
                        value = attributeConfig.cap;
                    }
                }

                nbtMorph.putDouble("attr_" + e.getKey().toString(), value);
            }
        }
    }

    public static void writeDefaults(LivingEntity living, CompoundTag tag) //taken from Entity.writeWithoutTypeId
    {
        CompoundTag defs = new CompoundTag();

        living.saveWithoutId(defs); //1.20.1: saveWithoutId instead of writeWithoutTypeId

        for(String s : TAGS_TO_TAKE)
        {
            if(defs.contains(s))
            {
                tag.put(s, defs.get(s));
            }
        }
    }

    public boolean hasVariants()
    {
        return !variants.isEmpty();
    }

    public boolean combineVariants(MorphVariant variant)
    {
        if(!isSameMorphType(variant))
        {
            return false;
        }

        //special handling for players
        if(id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()))
        {
            variants.add(variant.thisVariant);
            return true;
        }

        //Compare the tags for living entities.
        addBetterMorphData(variant.nbtMorph);

        CompoundTag variantTag = variant.getCumulativeTags();

        //compare with our commons first to see what doesn't match.
        HashSet<String> uncommons = new HashSet<>();
        for(String key : nbtCommon.getAllKeys())
        {
            Tag varNBT = variantTag.get(key);
            Tag commonNBT = nbtCommon.get(key);

            if(varNBT == null || !varNBT.equals(commonNBT)) //uncommon, mark for cloning in all the other variants
            {
                uncommons.add(key);
            }
            else //common value, remove it from their variants.
            {
                variantTag.remove(key);
            }
        }

        //add the now uncommon to the existing variants, and remove the previous common, it's not common anymore.
        for(String key : uncommons)
        {
            Tag nbt = nbtCommon.get(key);
            for(Variant aVariant : variants)
            {
                aVariant.nbtVariant.put(key, nbt.copy());
            }
            nbtCommon.remove(key);
        }

        //the commons have been stripped so what remains is the variant.
        variant.thisVariant.nbtVariant = variantTag;

        variants.add(variant.thisVariant);

        //we've fixed the uncommons... now get the new commons.
        gatherNewCommons();

        return true;
    }

    public boolean removeVariant(Variant variant)
    {
        boolean flag = false;
        for(int i = variants.size() - 1; i >= 0; i--)
        {
            if(variants.get(i).identifier.equals(variant.identifier))
            {
                variants.remove(i);
                flag = true;
            }
        }

        if(flag && !id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location())) //player morphs don't have commons
        {
            if(variants.size() >= 2)
            {
                gatherNewCommons();
            }
            else if(!variants.isEmpty()) //Only one variant left
            {
                for(String key : nbtCommon.getAllKeys())
                {
                    variants.get(0).nbtVariant.put(key, nbtCommon.get(key).copy());
                }
                nbtCommon = new CompoundTag(); // no more commons
            }
            else //no more variants, aka no more common tags.
            {
                nbtCommon = new CompoundTag();
            }
        }

        return flag;
    }

    public Variant getVariantById(String id)
    {
        for(Variant variant : variants)
        {
            if(variant.identifier.equals(id))
            {
                return variant;
            }
        }

        if(thisVariant != null && thisVariant.identifier.equals(id))
        {
            return thisVariant;
        }

        return null;
    }

    public void gatherNewCommons()
    {
        if (variants.isEmpty()) return;

        CompoundTag firstVariantNbt = variants.get(0).nbtVariant;
        HashSet<String> commonKeys = new HashSet<>(firstVariantNbt.getAllKeys());

        //now we compare
        for (int i = 1; i < variants.size(); i++)
        {
            CompoundTag vNbt = variants.get(i).nbtVariant;
            commonKeys.removeIf(key -> !vNbt.contains(key) || !firstVariantNbt.get(key).equals(vNbt.get(key)));
        }

        //remove from the variants and add to commons
        for(String s : commonKeys)
        {
            nbtCommon.put(s, firstVariantNbt.get(s).copy());
            for(Variant variant : variants)
            {
                variant.nbtVariant.remove(s);
            }
        }
    }

    public boolean containsVariant(MorphVariant variant)
    {
        //special handling for players
        if(id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()))
        {
            for(Variant aVariant : variants)
            {
                if(aVariant.playerUUID.equals(variant.thisVariant.playerUUID))
                {
                    return true;
                }
            }
        }
        else if(!variant.id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()))
        {
            CompoundTag variantTags = variant.getCumulativeTags();

            for(Variant aVariant : variants)
            {
                CompoundTag aVariantTags = getCumulativeTagsWithVariant(aVariant);

                if(variantTags.equals(aVariantTags))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isSameMorphType(MorphVariant variant)
    {
        return id.equals(variant.id);
    }

    private boolean addBetterMorphData(CompoundTag tag) //returns true when the data is better.
    {
        boolean flag = false;

        Map<ResourceLocation, AttributeConfig> supportedAttributes = MorphApi.getApiImpl().getSupportedAttributes();

        for(String key : nbtMorph.getAllKeys())
        {
            if(key.startsWith("attr_")) //it's an attribute key
            {
                ResourceLocation id = new ResourceLocation(key.substring(5));
                if(supportedAttributes.containsKey(id))
                {
                    AttributeConfig attributeConfig = supportedAttributes.get(id);
                    final double value = tag.getDouble(key);
                    if(attributeConfig.moreIsBetter) //more is better
                    {
                        if(nbtMorph.getDouble(key) < value)
                        {
                            nbtMorph.putDouble(key, value);

                            if(attributeConfig.cap != null && value > attributeConfig.cap)
                            {
                                nbtMorph.putDouble(key, attributeConfig.cap);
                            }
                            flag = true;
                        }
                    }
                    else //less is better
                    {
                        if(nbtMorph.getDouble(key) > value)
                        {
                            nbtMorph.putDouble(key, value);

                            if(attributeConfig.cap != null && value < attributeConfig.cap)
                            {
                                nbtMorph.putDouble(key, attributeConfig.cap);
                            }
                            flag = true;
                        }
                    }
                }
            }
        }

        for(String key : tag.getAllKeys())
        {
            if(key.startsWith("attr_") && !nbtMorph.contains(key))
            {
                nbtMorph.put(key, tag.get(key).copy());
                flag = true;
            }
        }
        return flag;
    }

    public boolean hasFavourite()
    {
        for(Variant variant : variants)
        {
            if(variant.isFavourite)
            {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public LivingEntity createEntityInstance(Level level, @Nullable Player player)
    {
        LivingEntity entInstance = null;
        EntityType<?> value = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if(value != null)
        {
            try
            {
                if(value.equals(EntityType.PLAYER))
                {
                    entInstance = level.isClientSide ? createPlayer(level, thisVariant.playerUUID) : new FakePlayer((ServerLevel)level, MorphApi.getApiImpl().getGameProfile(thisVariant.playerUUID, null));

                    if(player != null)
                    {
                        entInstance.getPersistentData().merge(player.getPersistentData());
                    }

                    for(BiConsumer<LivingEntity, CompoundTag> consumer : MorphApi.getApiImpl().getVariantNbtTagReaders())
                    {
                        consumer.accept(entInstance, entInstance.getPersistentData());
                    }
                }
                else
                {
                    CompoundTag tags = getCumulativeTags();
                    Entity ent = value.create(level);
                    if(ent instanceof LivingEntity)
                    {
                        ent.load(tags);

                        entInstance = (LivingEntity)ent;

                        for(BiConsumer<LivingEntity, CompoundTag> consumer : MorphApi.getApiImpl().getVariantNbtTagReaders())
                        {
                            consumer.accept(entInstance, tags);
                        }
                    }
                }
            }
            catch(Throwable t)
            {
                MorphApi.getLogger().error("Error creating Morph entity for ID: {}", id);
                t.printStackTrace();
            }
        }

        if(entInstance == null) //we can't find the entity type or errored out somewhere... have a pig.
        {
            MorphApi.getLogger().error("Cannot find entity type {} have a pig instead!", id);
            entInstance = EntityType.PIG.create(level);
            entInstance.setCustomName(Component.literal("Invalid Morph Pig"));
        }

        entInstance.setId(MorphInfo.getNextEntId()); //to prevent ID collision

        if(player != null)
        {
            entInstance.getPersistentData().putUUID(NBT_PLAYER_ID, player.getGameProfile().getId());
        }

        return entInstance;
    }

    @OnlyIn(Dist.CLIENT)
    private Player createPlayer(Level level, UUID uuid)
    {
        Minecraft mc = Minecraft.getInstance();
        GameProfile gameProfile = MorphApi.getApiImpl().getGameProfile(uuid, null);
        boolean added = false;
        if(mc.getConnection().getPlayerInfo(gameProfile.getId()) == null) //we have to assign a PlayerInfo for the player skin to render.
        {
            //Spoofing PlayerInfo in 1.20.1 is different.
            //For now, let's just create a dummy PlayerInfo if it's missing.
            //This part is tricky because mc.getConnection().getPlayerInfoMap() is usually immutable or private.
            //Actually, in 1.20.1 it's mc.getConnection().getPlayerInfoMap() which is a Map.

            //Added dummy player info to map if possible (not doing it here for now as it needs more deep access)
        }

        RemotePlayer player = new RemotePlayer((ClientLevel)level, gameProfile);
        player.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte)127); //All model parts shown

        return player;
    }

    public MorphVariant getAsVariant(Variant variant)
    {
        MorphVariant morph = createFromNBT(write(new CompoundTag()));
        morph.variants.clear();
        morph.thisVariant = variant;

        return morph;
    }

    public CompoundTag getCumulativeTags()
    {
        return getCumulativeTagsWithVariant(thisVariant);
    }

    public CompoundTag getCumulativeTagsWithVariant(Variant variant)
    {
        CompoundTag tags = new CompoundTag();

        tags.merge(nbtCommon);
        if (variant != null && variant.nbtVariant != null)
        {
            tags.merge(variant.nbtVariant);
        }

        return tags;
    }

    public CompoundTag write(CompoundTag tag)
    {
        tag.putString("id", id.toString());
        tag.put("nbtMorph", nbtMorph);
        if(!id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()))
        {
            tag.put("nbtCommon", nbtCommon);
        }

        tag.putInt("variantCount", variants.size());
        for(int i = 0; i < variants.size(); i++)
        {
            tag.put("variant_" + i, variants.get(i).write(new CompoundTag()));
        }

        if(thisVariant != null)
        {
            tag.put("thisVariant", thisVariant.write(new CompoundTag()));
        }
        return tag;
    }

    public void read(CompoundTag tag)
    {
        id = new ResourceLocation(tag.getString("id"));
        nbtMorph = tag.getCompound("nbtMorph");
        if(!id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()))
        {
            nbtCommon = tag.getCompound("nbtCommon");
        }

        variants.clear();
        int count = tag.getInt("variantCount");
        for(int i = 0; i < count; i++)
        {
            Variant variant = new Variant();
            variant.read(tag.getCompound("variant_" + i));
            variants.add(variant);
        }

        if(tag.contains("thisVariant"))
        {
            Variant variant = new Variant();
            variant.read(tag.getCompound("thisVariant"));
            thisVariant = variant;
        }
    }

    @Override
    public boolean equals(Object obj) //only used for a single variant
    {
        if(obj instanceof MorphVariant)
        {
            MorphVariant variant = (MorphVariant)obj;

            if(id.equals(variant.id) && thisVariant != null && variant.thisVariant != null)
            {
                if(id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()))
                {
                    return thisVariant.playerUUID.equals(variant.thisVariant.playerUUID);
                }
                else
                {
                    return getCumulativeTags().equals(variant.getCumulativeTags());
                }
            }
        }
        return false;
    }

    @Override
    public int compareTo(MorphVariant o)
    {
        if(id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()) && !id.equals(o.id)) //this is a player morph. always first
        {
            return -1; //we're before...
        }
        else if(o.id.equals(EntityType.PLAYER.builtInRegistryHolder().key().location()) && !id.equals(o.id))
        {
            return 1;
        }

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        EntityType<?> otherType = ForgeRegistries.ENTITY_TYPES.getValue(o.id);
        if(type != null)
        {
            if(otherType != null)
            {
                if(EffectiveSide.get().isClient())
                {
                    return I18n.get(type.getDescriptionId()).compareTo(I18n.get(otherType.getDescriptionId()));
                }
                return type.getDescriptionId().compareTo(otherType.getDescriptionId());
            }
            return -1; //we have a type, we're before
        }
        else
        {
            if(otherType == null)
            {
                return 0; //they also don't have a type, no comparator
            }
            return 1;//we don't have a type
        }
    }

    public static MorphVariant createFromNBT(CompoundTag tag)
    {
        MorphVariant variant = new MorphVariant();
        variant.read(tag);
        return variant;
    }

    public static MorphVariant createPlayerMorph(@Nonnull UUID owner, boolean isVariant) //creates the base morph + variant of the player.
    {
        MorphVariant variant = new MorphVariant(EntityType.PLAYER.builtInRegistryHolder().key().location());
        Variant var = new Variant();
        var.playerUUID = owner;
        if(isVariant)
        {
            variant.thisVariant = var;
        }
        else
        {
            variant.variants.add(var);
        }

        return variant;
    }

    @Override
    public int hashCode()
    {
        return thisVariant != null ? thisVariant.hashCode() : super.hashCode();
    }

    public static class Variant
    {
        public String identifier;
        public UUID playerUUID; // for player morphs
        public CompoundTag nbtVariant;
        public boolean isFavourite;

        public Variant()
        {
            this.identifier = RandomStringUtils.randomAscii(IDENTIFIER_LENGTH);
            this.nbtVariant = new CompoundTag();
            this.isFavourite = false;
        }

        public CompoundTag write(CompoundTag tag)
        {
            tag.putString("identifier", identifier);
            if(playerUUID != null)
            {
                tag.putUUID("playerUUID", playerUUID);
            }
            else
            {
                tag.put("nbtVariant", nbtVariant);
            }
            tag.putBoolean("isFavourite", isFavourite);
            return tag;
        }

        public void read(CompoundTag tag)
        {
            identifier = tag.getString("identifier");
            if(tag.hasUUID("playerUUID"))
            {
                playerUUID = tag.getUUID("playerUUID");
            }
            else
            {
                nbtVariant = tag.getCompound("nbtVariant");
            }
            isFavourite = tag.getBoolean("isFavourite");
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj instanceof Variant)
            {
                return playerUUID != null ? playerUUID.equals(((Variant)obj).playerUUID) : nbtVariant.equals(((Variant)obj).nbtVariant);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return identifier.hashCode();
        }
    }
}
