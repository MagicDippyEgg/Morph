package me.ichun.mods.morph.common.morph.save;
import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
public class PlayerMorphData {
    public final UUID uuid;
    public final List<MorphVariant> acquiredMorphs = new ArrayList<>();
    public PlayerMorphData(UUID uuid) { this.uuid = uuid; acquiredMorphs.add(MorphVariant.createPlayerMorph(uuid, true)); }
    public MorphVariant addVariant(MorphVariant variant) {
        if (variant.id.getPath().equals("player")) return null;
        for (MorphVariant v : acquiredMorphs) { if (v.id.equals(variant.id)) return null; }
        acquiredMorphs.add(variant); return variant;
    }
    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag(); tag.putUUID("uuid", uuid);
        ListTag list = new ListTag(); for (MorphVariant v : acquiredMorphs) list.add(v.serialize()); tag.put("morphs", list);
        return tag;
    }
    public static PlayerMorphData deserialize(CompoundTag tag) {
        PlayerMorphData data = new PlayerMorphData(tag.getUUID("uuid")); data.acquiredMorphs.clear();
        ListTag list = tag.getList("morphs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) data.acquiredMorphs.add(MorphVariant.deserialize(list.getCompound(i)));

        boolean hasPlayer = false;
        for (MorphVariant v : data.acquiredMorphs) { if (v.id.getPath().equals("player")) { hasPlayer = true; break; } }
        if (!hasPlayer) data.acquiredMorphs.add(0, MorphVariant.createPlayerMorph(data.uuid, true));

        return data;
    }
}
