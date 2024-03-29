package de.pewpewproject.lasertag.lasertaggame.state.management.client;

import de.pewpewproject.lasertag.lasertaggame.state.synced.ISyncedState;

/**
 * Interface for a client lasertag manager.
 *
 * @author Étienne Muser
 */
public interface IClientLasertagManager {

    IActivationManager getActivationManager();

    ICaptureTheFlagManager getCaptureTheFlagManager();

    IGameModeManager getGameModeManager();

    IScoreManager getScoreManager();

    ISettingsManager getSettingsManager();

    ISettingsPresetsNameManager getSettingsPresetsNameManager();

    ITeamsManager getTeamsManager();

    IUIStateManager getUIStateManager();

    IEliminationManager getEliminationManager();

    ILasertargetsManager getLasertargetsManager();

    ISyncedState getSyncedState();

    /**
     * The a new synced state on the client
     *
     * @param syncedState The new synced state
     */
    void setSyncedState(ISyncedState syncedState);
}
