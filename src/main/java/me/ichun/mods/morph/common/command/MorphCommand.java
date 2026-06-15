package me.ichun.mods.morph.common.command;
import com.mojang.brigadier.CommandDispatcher;
import me.ichun.mods.morph.api.MorphApi;
import me.ichun.mods.morph.api.morph.MorphVariant;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.server.level.ServerPlayer;
public class MorphCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("morph").requires(source -> source.hasPermission(2)).then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("type", ResourceLocationArgument.id()).executes(context -> {
            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) { MorphApi.getApi().morphTo(player, new MorphVariant(ResourceLocationArgument.getId(context, "type"))); } return 1;
        }))));
        dispatcher.register(Commands.literal("demorph").requires(source -> source.hasPermission(2)).then(Commands.argument("targets", EntityArgument.players()).executes(context -> {
            for (ServerPlayer player : EntityArgument.getPlayers(context, "targets")) { MorphApi.getApi().demorph(player); } return 1;
        })));
    }
}
