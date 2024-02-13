package de.kleiner3.lasertag.lasertaggame.state.management.server.implementation;

import de.kleiner3.lasertag.LasertagMod;
import de.kleiner3.lasertag.common.types.Tuple;
import de.kleiner3.lasertag.lasertaggame.settings.SettingDescription;
import de.kleiner3.lasertag.lasertaggame.state.management.server.IMusicalChairsManager;
import de.kleiner3.lasertag.lasertaggame.state.management.server.synced.*;
import de.kleiner3.lasertag.lasertaggame.state.server.IMusicalChairsState;
import de.kleiner3.lasertag.lasertaggame.state.server.implementation.MusicalChairsState;
import de.kleiner3.lasertag.lasertaggame.state.synced.ITeamsConfigState;
import de.kleiner3.lasertag.lasertaggame.state.synced.implementation.UIState;
import de.kleiner3.lasertag.lasertaggame.team.TeamDto;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

/**
 * Implementation of IMusicalChairsManager for the server lasertag game
 *
 * @author Étienne Muser
 */
public class MusicalChairsManager implements IMusicalChairsManager {

    /**
     * The number of lasertag ticks since the last phase change.
     * Initialize with -1 as there will be a tick at 0 seconds
     * into the game.
     */
    private long ticksSinceLastPhaseChange = -1L;

    private final ISettingsManager settingsManager;

    private final ITeamsManager teamsManager;

    private final IScoreManager scoreManager;

    private final IGameModeManager gameModeManager;

    private final ITeamsConfigState teamsConfigState;

    private final UIState uiState;

    private final IRemainingTeamsManager remainingTeamsManager;

    private final ServerWorld world;

    private final IMusicalChairsState musicalChairsState;

    public MusicalChairsManager(ISettingsManager settingsManager,
                                ITeamsManager teamsManager,
                                IScoreManager scoreManager,
                                IGameModeManager gameModeManager,
                                ITeamsConfigState teamsConfigState,
                                UIState uiState,
                                IRemainingTeamsManager remainingTeamsManager,
                                ServerWorld world
    ) {
        this.settingsManager = settingsManager;
        this.teamsManager = teamsManager;
        this.scoreManager = scoreManager;
        this.gameModeManager = gameModeManager;
        this.teamsConfigState = teamsConfigState;
        this.uiState = uiState;
        this.remainingTeamsManager = remainingTeamsManager;
        this.world = world;
        this.musicalChairsState = new MusicalChairsState();
    }


    @Override
    public void tick() {

        LasertagMod.LOGGER.info("[MusicalChairsManager] Tick...");

        // Increment time since last phase change
        ++ticksSinceLastPhaseChange;

        // If the phase is now over
        if (ticksSinceLastPhaseChange >= settingsManager.<Long>get(SettingDescription.PHASE_DURATION)) {

            LasertagMod.LOGGER.info("[MusicalChairsManager] Phase end.");

            // Reset the time since last phase change
            ticksSinceLastPhaseChange = 0L;

            // Handle phase change
            handlePhaseChange();
        }
    }

    @Override
    public Long getTeamSurvivedTime(TeamDto team) {
        return musicalChairsState.getTeamSurviveTime(team);
    }

    @Override
    public long getPlayerTotalScore(UUID playerUuid) {
        return musicalChairsState.getPlayerOverallScore(playerUuid);
    }

    @Override
    public void onPlayerScored(UUID playerUuid, long score) {

        // Get the old score
        var oldScore = musicalChairsState.getPlayerOverallScore(playerUuid);

        // Calculate the new score
        var newScore = oldScore + score;

        // Set the new score
        musicalChairsState.setPlayerOverallScore(playerUuid, newScore);
    }

    @Override
    public void resetTicksSinceLastPhase() {
        ticksSinceLastPhaseChange = -1L;
    }

    private void handlePhaseChange() {

        // If the scores should be reset at the end of the phase
        if (settingsManager.<Boolean>get(SettingDescription.RESET_SCORES_AT_PHASE_END)) {

            scoreManager.resetScores();
        }

        // Get the non-eliminated teams
        var nonEliminatedTeams = teamsConfigState.getTeams().stream()
                .filter(remainingTeamsManager::remains)
                .toList();

        var sb = new StringBuilder("[MusicalChairsManager] Remaining teams: [");
        nonEliminatedTeams.forEach(t -> sb.append(t.name()).append(", "));
        sb.append("]");
        LasertagMod.LOGGER.info(sb.toString());

        // Get the teams with the fewest amount of points
        var toBeEliminatedTeams = nonEliminatedTeams.stream()
                // Map to (Score, Team) tuple
                .map(team -> new Tuple<>(teamsManager.getPlayersOfTeam(team).stream()
                        .map(scoreManager::getScore).mapToLong(a -> a)
                        .sum(), team))
                // Collect the teams with the least amount of points
                .collect(ArrayDeque::new,
                        (Queue<Tuple<Long, TeamDto>> queue, Tuple<Long, TeamDto> tuple) -> {
                            if (!queue.isEmpty() && queue.peek().x().compareTo(tuple.x()) > 0) {
                                queue.clear();
                            }
                            if (queue.isEmpty() || queue.peek().x().equals(tuple.x())) {
                                queue.offer(tuple);
                            }
                        },
                        (left, right) -> {
                            if (left.peek().x().compareTo(right.peek().x()) > 0) {
                                left.clear();
                            }
                            if (left.isEmpty() || left.peek().x().equals(right.peek().x()))
                            {
                                left.addAll(right);
                            }
                        })
                // Map back to stream of teams
                .stream()
                .map(Tuple::y)
                .toList();

        // If all teams are about to be eliminated
        if (nonEliminatedTeams.size() == toBeEliminatedTeams.size()) {

            LasertagMod.LOGGER.info("[MusicalChairsManager] Tie between all teams present. No team gets eliminated");
            LasertagMod.LOGGER.info("Number of remaining teams: " + nonEliminatedTeams.size());
            LasertagMod.LOGGER.info("Number of to be eliminated teams: " + toBeEliminatedTeams.size());

            // Eliminate no team - tiebreaker
            return;
        }

        // Eliminate all the teams that need to be eliminated
        toBeEliminatedTeams.forEach(team -> {

            LasertagMod.LOGGER.info("[MusicalChairsManager] Eliminating team '" + team.name() + "'.");

            musicalChairsState.setTeamSurviveTime(team, uiState.gameTime);
            sendTeamIsOutMessage(team);
            putTeamToSpectators(team);
            remainingTeamsManager.removeTeam(team);
        });

        // Check if game ended
        gameModeManager.getGameMode().checkGameOver(world.getServer());
    }

    private void sendTeamIsOutMessage(TeamDto team) {

        boolean sendMessage = settingsManager.get(SettingDescription.SEND_TEAM_OUT_MESSAGE);

        if (!sendMessage) {
            return;
        }

        var msg = Text.literal("Team ");
        var teamName = Text.literal(team.name()).setStyle(Style.EMPTY.withColor(team.color().getValue()));
        msg.append(teamName);
        msg.append(" got eliminated!");
        world.getServer().getPlayerManager().broadcast(msg, false);
    }

    private void putTeamToSpectators(TeamDto team) {

        teamsManager.getPlayersOfTeam(team).forEach(playerUuid -> {

            // Get the player
            var player = world.getPlayerByUuid(playerUuid);

            // Sanity check
            if (player == null) {
                return;
            }

            // Cast to server player entity
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return;
            }

            // Set player to spectator game mode
            serverPlayer.changeGameMode(GameMode.SPECTATOR);
        });
    }
}