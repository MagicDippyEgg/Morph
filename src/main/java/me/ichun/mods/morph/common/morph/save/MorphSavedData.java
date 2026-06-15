package me.ichun.mods.morph.common.morph.save;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public class MorphSavedData extends SavedData {
    public final Map<UUID, PlayerMorphData> playerMorphs = new HashMap<>();
    public static MorphSavedData get(ServerLevel level) { return level.getDataStorage().computeIfAbsent(MorphSavedData::load, MorphSavedData::new, "morph_data"); }
    public static MorphSavedData load(CompoundTag tag) {
        MorphSavedData data = new MorphSavedData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) { PlayerMorphData pData = PlayerMorphData.deserialize(list.getCompound(i)); data.playerMorphs.put(pData.uuid, pData); }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PlayerMorphData pData : playerMorphs.values()) { list.add(pData.serialize()); }
        tag.put("players", list);
        return tag;
    }
}
