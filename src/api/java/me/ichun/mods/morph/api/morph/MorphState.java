package me.ichun.mods.morph.api.morph;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public class MorphState {
    public final MorphVariant variant;
    private LivingEntity entityInstance;
    public MorphState(MorphVariant variant) { this.variant = variant; }
    public LivingEntity getEntity(Level level) {
        if (entityInstance == null || entityInstance.level() != level) {
            entityInstance = variant.createEntity(level);
            if (entityInstance != null) { entityInstance.noPhysics = true; }
        }
        return entityInstance;
    }
    public void tick(Player player) {
        LivingEntity entity = getEntity(player.level());
        if (entity != null) {
            entity.xo = entity.getX(); entity.yo = entity.getY(); entity.zo = entity.getZ();
            entity.yRotO = entity.getYRot(); entity.xRotO = entity.getXRot();
            entity.yHeadRotO = entity.yHeadRot; entity.yBodyRotO = entity.yBodyRot;

            entity.setPos(player.getX(), player.getY(), player.getZ());
            entity.setYRot(player.getYRot()); entity.setXRot(player.getXRot());
            entity.yHeadRot = player.yHeadRot; entity.yBodyRot = player.yBodyRot;

            entity.tickCount++;
            entity.walkAnimation.update(player.walkAnimation.speed(), 1.0f);

            // Advance internal animation timers for complex entities
            if (entity.tickCount % 2 == 0) {
                entity.aiStep(); // This might be enough to advance wing flaps etc.
                // We must ensure it doesn't move or do logic. noPhysics helps.
            }
        }
    }
}
