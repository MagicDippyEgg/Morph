package me.ichun.mods.morph.api.morph;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public interface MorphInfo {
    Capability<MorphInfo> CAPABILITY_INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});

    MorphState getCurrentState();
    MorphState getNextState();
    int getTransitionTicks();
    int getTransitionTime();

    void setNextState(MorphState state, int transitionTime);
    void tick();
    CompoundTag write(CompoundTag tag);
    void read(CompoundTag tag);
}
