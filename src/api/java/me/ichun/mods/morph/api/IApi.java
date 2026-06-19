package me.ichun.mods.morph.api;
import me.ichun.mods.morph.api.mob.trait.Trait;
import me.ichun.mods.morph.api.morph.MorphInfo;
import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
public interface IApi {
    MorphInfo getMorphInfo(Player player);
    boolean morphTo(ServerPlayer player, MorphVariant variant);
    boolean demorph(ServerPlayer player);
    MorphVariant createVariant(LivingEntity living);
    boolean acquireMorph(ServerPlayer player, MorphVariant variant);
    ArrayList<Trait<?>> getTraitsForVariant(MorphVariant variant, Player player);
}
