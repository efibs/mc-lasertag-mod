package de.pewpewproject.lasertag.lasertaggame.gamemode.implementation;

import de.pewpewproject.lasertag.common.types.ScoreHolding;
import de.pewpewproject.lasertag.common.types.Tuple;
import de.pewpewproject.lasertag.lasertaggame.gamemode.PointBasedGameMode;
import de.pewpewproject.lasertag.lasertaggame.settings.SettingDescription;
import de.pewpewproject.lasertag.lasertaggame.state.management.server.IServerLasertagManager;
import de.pewpewproject.lasertag.lasertaggame.team.TeamDto;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * The default game mode. Point hunter. Team-based, time-limited game mode. You earn points for your team
 * by hitting lasertargets or players of other teams. The team with the most points wins.
 *
 * @author Étienne Muser
 */
public class PointHunterGameMode extends PointBasedGameMode {
    public PointHunterGameMode() {
        super("gameMode.point_hunter", false, true);
    }

    @Override
    public List<Tuple<SettingDescription, Object>> getOverwrittenSettings() {

        var list = new LinkedList<Tuple<SettingDescription, Object>>();

        list.add(new Tuple<>(SettingDescription.WEAPON_COOLDOWN, 4L));
        list.add(new Tuple<>(SettingDescription.PLAYER_DEACTIVATE_TIME, 5L));
        list.add(new Tuple<>(SettingDescription.LASERTARGET_DEACTIVATE_TIME, 7L));
        list.add(new Tuple<>(SettingDescription.RESPAWN_PENALTY, 0L));

        return list;
    }

    @Override
    public List<SettingDescription> getRelevantSettings() {
        var list = super.getRelevantSettings();

        // From time limited
        list.add(SettingDescription.PLAY_TIME);

        return list;
    }

    @Override
    public void onTick(MinecraftServer server) {
        // On tick not used in this game mode
    }

    @Override
    public int getWinnerTeamId() {

        // Get the managers
        var gameManager = MinecraftClient.getInstance().world.getClientLasertagManager();
        var teamsManager = gameManager.getTeamsManager();
        var syncedState = gameManager.getSyncedState();
        var teamsConfigState = syncedState.getTeamsConfigState();
        var scoreManager = gameManager.getScoreManager();

        return teamsConfigState.getTeams().stream()
                .filter(team -> !teamsManager.getPlayersOfTeam(team).isEmpty())
                .map(team -> new Tuple<>(team, teamsManager.getPlayersOfTeam(team).stream().map(scoreManager::getScore).reduce(Long::sum).orElseThrow()))
                .max(Comparator.comparingLong(Tuple::y))
                .map(tuple -> tuple.x().id())
                .orElse(-1);
    }

    @Override
    public ScoreHolding getTeamFinalScore(TeamDto team, IServerLasertagManager gameManager) {

        var scoreManager = gameManager.getScoreManager();
        var teamScore = (Long) gameManager.getTeamsManager().getPlayersOfTeam(team).stream()
                .mapToLong(scoreManager::getScore).sum();
        return new PointHunterScore(teamScore);
    }

    @Override
    public ScoreHolding getPlayerFinalScore(UUID playerUuid, IServerLasertagManager gameManager) {
        return new PointHunterScore(gameManager.getScoreManager().getScore(playerUuid));
    }

    public static class PointHunterScore implements ScoreHolding {

        private final Long value;

        public PointHunterScore(long value) {
            this.value = value;
        }

        @Override
        public String getValueString() {
            return Long.toString(this.value);
        }

        @Override
        public int compareTo(@NotNull ScoreHolding o) {

            if (!(o instanceof PointHunterScore otherPointHunterScore)) {
                return 0;
            }

            return this.value.compareTo(otherPointHunterScore.value);
        }
    }
}
