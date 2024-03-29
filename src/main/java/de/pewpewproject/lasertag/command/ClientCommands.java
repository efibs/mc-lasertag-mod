package de.pewpewproject.lasertag.command;

import de.pewpewproject.lasertag.command.lasertag.CreditsCommand;
import de.pewpewproject.lasertag.command.lasertag.StatsCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * Initializes the client side commands
 *
 * @author Étienne Muser
 */
public class ClientCommands {
    /**
     * Init client side commands
     */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> CreditsCommand.register(dispatcher)));
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> StatsCommand.register(dispatcher)));
    }
}
