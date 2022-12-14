package de.kleiner3.lasertag.command.lasertag.game.settings;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.kleiner3.lasertag.LasertagConfig;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * The lasertarget hit score setting
 *
 * @author Étienne Muser
 */
public class LasertargetHitScoreSettingCommand {
    @SuppressWarnings("SameReturnValue")
    private static int execute(CommandContext<ServerCommandSource> context) {
        var value = IntegerArgumentType.getInteger(context, "score");
        LasertagConfig.getInstance().setLasertargetHitScore(context.getSource().getServer(), value);
        context.getSource().getServer().getPlayerManager().broadcast(Text.literal("Lasertag setting lasertargetHitScore is now set to " + value), false);
        return Command.SINGLE_SUCCESS;
    }

    static void register(LiteralArgumentBuilder<ServerCommandSource> lab) {
        lab.then(literal("lasertargetHitScore")
                .then(argument("score", integer())
                        .executes(LasertargetHitScoreSettingCommand::execute)));
    }
}
