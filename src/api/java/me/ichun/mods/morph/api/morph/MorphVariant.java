package me.ichun.mods.morph.api.morph;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;
public class MorphVariant {
    public final ResourceLocation id;
    public CompoundTag nbt;
    public UUID playerUuid;
    public MorphVariant(ResourceLocation id) { this.id = id; }
    public static MorphVariant createPlayerMorph(UUID uuid, boolean isSelf) {
        MorphVariant variant = new MorphVariant(new ResourceLocation("minecraft", "player"));
        variant.playerUuid = uuid;
        return variant;
    }
    public LivingEntity createEntity(Level level) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) return null;
        Entity entity = type.create(level);
        if (entity instanceof LivingEntity living) {
            if (nbt != null) { living.load(nbt); }
            return living;
        }
        return null;
    }
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        if (nbt != null) tag.put("nbt", nbt);
        if (playerUuid != null) tag.putUUID("uuid", playerUuid);
        return tag;
    }
    public static MorphVariant deserialize(CompoundTag tag) {
        MorphVariant variant = new MorphVariant(new ResourceLocation(tag.getString("id")));
        if (tag.contains("nbt")) variant.nbt = tag.getCompound("nbt");
        if (tag.hasUUID("uuid")) variant.playerUuid = tag.getUUID("uuid");
        return variant;
    }
}
