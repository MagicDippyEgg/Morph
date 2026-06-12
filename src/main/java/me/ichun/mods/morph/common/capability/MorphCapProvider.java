package me.ichun.mods.morph.common.capability;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.common.morph.MorphInfoImpl;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MorphCapProvider implements ICapabilitySerializable<CompoundTag> {
    private final MorphInfo info;
    private final LazyOptional<MorphInfo> optional;

    public MorphCapProvider(Player player) {
        this.info = new MorphInfoImpl(player);
        this.optional = LazyOptional.of(() -> info);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == MorphInfo.CAPABILITY_INSTANCE) return optional.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return info.write(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        info.read(nbt);
    }
}
