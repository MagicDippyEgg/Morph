package me.ichun.mods.morph.common.morph;

import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphState;
import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class MorphInfoImpl implements MorphInfo {
    private MorphState current;
    private MorphState next;
    private int transitionTicks;
    private int transitionTime;

    public MorphInfoImpl(Player player) {
        this.current = new MorphState(MorphVariant.createPlayerMorph(player.getUUID(), true));
    }

    @Override public MorphState getCurrentState() { return current; }
    @Override public MorphState getNextState() { return next; }
    @Override public int getTransitionTicks() { return transitionTicks; }
    @Override public int getTransitionTime() { return transitionTime; }

    @Override
    public void setNextState(MorphState state, int transitionTime) {
        this.next = state;
        this.transitionTime = transitionTime;
        this.transitionTicks = 0;
    }

    @Override
    public void tick() {
        if (next != null) {
            transitionTicks++;
            if (transitionTicks >= transitionTime) {
                current = next;
                next = null;
                transitionTicks = 0;
            }
        }
    }

    @Override
    public CompoundTag write(CompoundTag tag) {
        if (current != null) tag.put("current", current.variant.serialize());
        if (next != null) {
            tag.put("next", next.variant.serialize());
            tag.putInt("ticks", transitionTicks);
            tag.putInt("time", transitionTime);
        }
        return tag;
    }

    @Override
    public void read(CompoundTag tag) {
        if (tag.contains("current")) current = new MorphState(MorphVariant.deserialize(tag.getCompound("current")));
        if (tag.contains("next")) {
            next = new MorphState(MorphVariant.deserialize(tag.getCompound("next")));
            transitionTicks = tag.getInt("ticks");
            transitionTime = tag.getInt("time");
        }
    }
}
