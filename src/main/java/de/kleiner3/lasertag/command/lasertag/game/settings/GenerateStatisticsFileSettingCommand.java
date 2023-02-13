package de.kleiner3.lasertag.command.lasertag.game.settings;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.kleiner3.lasertag.lasertaggame.settings.LasertagSettingsManager;
import de.kleiner3.lasertag.lasertaggame.settings.SettingNames;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * The generate statistics file setting
 *
 * @author Étienne Muser
 */
public class GenerateStatisticsFileSettingCommand {
    @SuppressWarnings("SameReturnValue")
    private static int execute(CommandContext<ServerCommandSource> context) {
        var value = BoolArgumentType.getBool(context, "value");
        LasertagSettingsManager.set(context.getSource().getServer(), SettingNames.GEN_STATS_FILE, value);
        context.getSource().getServer().getPlayerManager().broadcast(Text.literal("Lasertag setting generateStatsFile is now set to " + value), false);
        return Command.SINGLE_SUCCESS;
    }

    static void register(LiteralArgumentBuilder<ServerCommandSource> lab) {
        lab.then(literal("generateStatsFile")
                .then(argument("value", bool())
                        .executes(GenerateStatisticsFileSettingCommand::execute)));
    }
}
