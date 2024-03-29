package de.pewpewproject.lasertag.lasertaggame.state.management.client.implementation;

import de.pewpewproject.lasertag.lasertaggame.state.management.client.IClientLasertagManager;
import de.pewpewproject.lasertag.lasertaggame.state.management.client.IEliminationManager;
import de.pewpewproject.lasertag.lasertaggame.state.synced.implementation.TeamsConfigState;
import de.pewpewproject.lasertag.lasertaggame.team.TeamDto;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of IElimination manager for the lasertag game
 *
 * @author Étienne Muser
 */
public class EliminationManager implements IEliminationManager {

    private IClientLasertagManager clientManager;

    public void setClientManager(IClientLasertagManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public synchronized long getEliminationCount(UUID playerUuid) {
        return clientManager.getSyncedState().getEliminationState().getEliminationCount(playerUuid);
    }

    @Override
    public synchronized void setEliminationCount(UUID playerUuid, long eliminationCount) {
        clientManager.getSyncedState().getEliminationState().setEliminationCount(playerUuid, eliminationCount);
    }

    @Override
    public synchronized List<Integer> getRemainingTeamIds() {

        // Get the game managers
        var teamsManager = clientManager.getTeamsManager();
        var teamsConfigState = clientManager.getSyncedState().getTeamsConfigState();
        var eliminationState = clientManager.getSyncedState().getEliminationState();

        return teamsConfigState.getTeams().stream()
                .filter(team -> !teamsManager.getPlayersOfTeam(team).isEmpty())
                .filter(team -> !team.equals(TeamsConfigState.SPECTATORS))
                .map(TeamDto::id)
                .filter(id -> !eliminationState.isTeamEliminated(id))
                .toList();
    }

    @Override
    public synchronized void setTeamEliminated(int teamId) {

        clientManager.getSyncedState().getEliminationState().eliminateTeam(teamId);
    }

    @Override
    public synchronized void setTeamSurviveTime(int teamId, long surviveTime) {

        clientManager.getSyncedState().getEliminationState().setTeamSurviveTime(teamId, surviveTime);
    }

    @Override
    public synchronized void setPlayerEliminated(UUID playerUuid) {

        clientManager.getSyncedState().getEliminationState().eliminatePlayer(playerUuid);
    }

    @Override
    public synchronized void setPlayerSurviveTime(UUID playerUuid, long surviveTime) {

        clientManager.getSyncedState().getEliminationState().setPlayerSurviveTime(playerUuid, surviveTime);
    }

    @Override
    public boolean isTeamEliminated(int teamId) {
        return clientManager.getSyncedState().getEliminationState().isTeamEliminated(teamId);
    }

    @Override
    public boolean isPlayerEliminated(UUID playerUuid) {
        return clientManager.getSyncedState().getEliminationState().isPlayerEliminated(playerUuid);
    }

    @Override
    public synchronized void reset() {
        clientManager.getSyncedState().getEliminationState().reset();
    }
}
