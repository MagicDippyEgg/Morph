package me.ichun.mods.morph.api.morph;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
public class MorphState {
    public final MorphVariant variant;
    private LivingEntity entityInstance;
    public MorphState(MorphVariant variant) { this.variant = variant; }
    public LivingEntity getEntity(Level level) {
        if (entityInstance == null || entityInstance.level() != level) { entityInstance = variant.createEntity(level); }
        return entityInstance;
    }
}
