package de.kleiner3.lasertag.client.hud;

import de.kleiner3.lasertag.common.types.Tuple;
import de.kleiner3.lasertag.common.types.Vec3;
import de.kleiner3.lasertag.common.util.AdvancedDrawableHelper;
import de.kleiner3.lasertag.lasertaggame.management.LasertagGameManager;
import de.kleiner3.lasertag.lasertaggame.management.settings.SettingDescription;
import de.kleiner3.lasertag.lasertaggame.management.team.TeamConfigManager;
import de.kleiner3.lasertag.lasertaggame.management.team.TeamDto;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

/**
 * Class to implement the team list hud overlay
 *
 * @author Étienne Muser
 */
public class TeamListHudOverlay extends AdvancedDrawableHelper {

    private static final int MAX_NUMBER_TEAMS_PER_ROW = 4;
    private static final int TEAM_WIDTH = 110;
    private static final int TEAM_PADDING = 5;
    private static final int TEXT_PADDING = 2;
    private static final int INTRA_PLAYER_PADDING = 0;
    private static final int TEAM_LIST_TOP_PADDING = 15;
    private static final int NOTE_TOP_PADDING = 25;
    private static final int BACKGROUND_COLOR = 0x66000000;
    private static final int MAX_NUMBER_PLAYERS_IN_WITHOUT_TEAM_LIST = 15;
    private static final MinecraftClient CLIENT = MinecraftClient.getInstance();
    private static final TextRenderer TEXT_RENDERER = CLIENT.textRenderer;

    /**
     * Renders the current team list into the matrix stack
     * @param matrices
     */
    public void render(MatrixStack matrices) {

        // Get the scaled window size
        var scaledWindowWidth = CLIENT.getWindow().getScaledWidth();
        var scaledWindowHeight = CLIENT.getWindow().getScaledHeight();

        drawPlayersWithoutTeam(matrices, scaledWindowWidth, scaledWindowHeight);
        drawSpectators(matrices, scaledWindowHeight);

        var lasertagGameManager = LasertagGameManager.getInstance();

        // If render team list setting is disabled
        if (!lasertagGameManager.getSettingsManager().<Boolean>get(SettingDescription.RENDER_TEAM_LIST)) {
            var text = Text.literal("Render team list setting is disabled").asOrderedText();
            drawCenteredTextWithShadow(matrices, TEXT_RENDERER, text, scaledWindowWidth / 2, NOTE_TOP_PADDING, 0xFFFFFF);
            return;
        }

        var teamManager = lasertagGameManager.getTeamManager();
        var scoreManager = lasertagGameManager.getScoreManager();
        var playerManager = lasertagGameManager.getPlayerManager();

        // Get a list of Tuple<TeamDto, List<Vec3<String, Long, Boolean>>> (One entry is a team) which are not empty
        var teams = teamManager.getTeamMap().entrySet().stream()
                .filter(entry -> entry.getValue().size() > 0)
                .filter(entry -> !entry.getKey().equals(TeamConfigManager.SPECTATORS))
                .map(entry -> {
                    var teamList = entry.getValue().stream()
                            .map(playerUuid -> {
                                // Try to get player from player list
                                var player = CLIENT.getNetworkHandler().getPlayerListEntry(playerUuid);

                                return new Vec3<>(playerManager.getPlayerUsername(playerUuid), scoreManager.getScore(playerUuid), player != null);
                            })
                            .toList();

                    return new Tuple<>(entry.getKey(), teamList);
                })
                .toList();
        var numberOfTeams = teams.size();

        // If there is no team to show
        if (numberOfTeams == 0) {
            var text = Text.literal("No teams to show").asOrderedText();
            drawCenteredTextWithShadow(matrices, TEXT_RENDERER, text, scaledWindowWidth / 2, NOTE_TOP_PADDING, 0xFFFFFF);
            return;
        }

        var maxNumberOfPlayersInTeam = teams.stream()
                .map(team -> team.y().size())
                .max(Comparator.comparingInt(i -> i))
                .get();

        var numberOfRows = (int)Math.ceil((float)numberOfTeams / (float)MAX_NUMBER_TEAMS_PER_ROW);
        var numberOfcolumns = Math.min(numberOfTeams, MAX_NUMBER_TEAMS_PER_ROW);
        var teamListWidth = (numberOfcolumns * TEAM_WIDTH) + ((numberOfcolumns + 1) * TEAM_PADDING);
        var teamHeight = TEXT_PADDING + TEXT_RENDERER.fontHeight + TEXT_PADDING + (maxNumberOfPlayersInTeam * (INTRA_PLAYER_PADDING + TEXT_RENDERER.fontHeight));
        var teamListHeight = (numberOfRows * teamHeight) + ((numberOfRows + 1) * TEAM_PADDING);
        var startX = (int)((scaledWindowWidth / 2.0) - (teamListWidth / 2.0));

        // Draw the background rectangle
        fill(matrices, startX, TEAM_LIST_TOP_PADDING, startX + teamListWidth, TEAM_LIST_TOP_PADDING + teamListHeight, BACKGROUND_COLOR);

        var teamIterator = teams.iterator();
        for (int row = 0; row < numberOfRows; ++row) {
            for (int column = 0; column < MAX_NUMBER_TEAMS_PER_ROW; ++column) {
                if (!teamIterator.hasNext()) {
                    break;
                }

                var team = teamIterator.next();

                var x = column * (TEAM_PADDING + TEAM_WIDTH);
                var y = row * (TEAM_PADDING + teamHeight);

                drawTeam(matrices, team, startX + x, TEAM_LIST_TOP_PADDING + y, teamHeight);
            }
        }
    }

    /**
     * Draws team at given start coordinates
     * @param matrices
     * @param team
     * @param startX
     * @param startY
     * @param teamHeight
     */
    private void drawTeam(MatrixStack matrices,
                                 Tuple<TeamDto, List<Vec3<String, Long, Boolean>>> team,
                                 int startX,
                                 int startY,
                                 int teamHeight) {
        var rectangleStartX = startX + TEAM_PADDING;
        var rectangleStartY = startY + TEAM_PADDING;
        var teamDto = team.x();
        var teamScore = team.y().stream().mapToLong(t -> t.y()).sum();
        var textHeight = TEXT_RENDERER.fontHeight;

        // Draw rectangle of team
        drawRectangle(matrices, rectangleStartX, rectangleStartY, rectangleStartX + TEAM_WIDTH, rectangleStartY + teamHeight, 0xAAFFFFFF);

        // Draw team name
        drawWithShadow(matrices, TEXT_RENDERER, Text.literal(teamDto.name()).asOrderedText(), rectangleStartX + TEXT_PADDING + 1, rectangleStartY + TEXT_PADDING + 1, teamDto.color().getValue());

        // Draw team score
        var teamScoreString = Long.toString(teamScore);
        var scoreStartX = rectangleStartX + TEAM_WIDTH - TEXT_RENDERER.getWidth(teamScoreString) - TEXT_PADDING;
        drawWithShadow(matrices, TEXT_RENDERER, Text.literal(teamScoreString).asOrderedText(), scoreStartX, rectangleStartY + TEXT_PADDING + 1, 0xFFFFFFFF);

        var playerY = rectangleStartY + TEXT_PADDING + textHeight + TEXT_PADDING;
        for (var player : team.y()) {

            var playerNamecolor = 0xFFFFFFFF;

            // If the player is not online
            if (!player.z()) {
                playerNamecolor = 0xFF808080;
            }

            // Draw player name
            TEXT_RENDERER.draw(matrices, player.x(), rectangleStartX + TEXT_PADDING + 1, playerY, playerNamecolor);

            // Draw player score
            var playerScoreString = Long.toString(player.y());
            var playerScoreStartX = rectangleStartX + TEAM_WIDTH - TEXT_RENDERER.getWidth(playerScoreString) - TEXT_PADDING;
            TEXT_RENDERER.draw(matrices, playerScoreString, playerScoreStartX, playerY, 0xFFFFFFFF);

            playerY += (INTRA_PLAYER_PADDING + textHeight);
        }
    }

    private void drawPlayersWithoutTeam(MatrixStack matrices, int scaledWindowWidth, int scaledWindowHeight) {

        // Apply padding
        scaledWindowWidth -= TEAM_PADDING;
        scaledWindowHeight -= TEAM_PADDING;

        // Get the players without team
        var playersWithoutTeam = CLIENT.player.networkHandler.getPlayerList().stream()
                .filter(playerListEntry -> !LasertagGameManager.getInstance().getTeamManager().isPlayerInTeam(playerListEntry.getProfile().getId()))
                .limit(MAX_NUMBER_PLAYERS_IN_WITHOUT_TEAM_LIST)
                .toList();

        var startX = scaledWindowWidth - TEAM_WIDTH;
        var height = TEXT_PADDING + TEXT_RENDERER.fontHeight + TEXT_PADDING + (playersWithoutTeam.size() * (INTRA_PLAYER_PADDING + TEXT_RENDERER.fontHeight));
        var startY = scaledWindowHeight - height;

        // Draw background rect
        fill(matrices, startX, startY, startX + TEAM_WIDTH, startY + height, BACKGROUND_COLOR);

        // Draw header
        TEXT_RENDERER.drawWithShadow(matrices, "Players without team:", startX + TEXT_PADDING, startY + TEXT_PADDING, 0xFFFFFFFF);

        var yPos = startY + TEXT_PADDING + TEXT_RENDERER.fontHeight + TEXT_PADDING;
        for (var player : playersWithoutTeam) {

            // Draw players name
            TEXT_RENDERER.draw(matrices, player.getProfile().getName(), startX + TEXT_PADDING, yPos, 0xFFFFFFFF);

            yPos += (TEXT_RENDERER.fontHeight + INTRA_PLAYER_PADDING);
        }
    }

    private void drawSpectators(MatrixStack matrices, int scaledWindowHeight) {

        // Get the HUD render data
        var teamManager = LasertagGameManager.getInstance().getTeamManager();

        // Get the team
        var spectatorTeam = teamManager.getTeamMap().entrySet().stream()
                .filter(entry -> entry.getKey().equals(TeamConfigManager.SPECTATORS))
                .findFirst()
                .get()
                .getValue().stream()
                .filter(playerUuid -> CLIENT.getNetworkHandler().getPlayerListEntry(playerUuid) != null)
                .limit(MAX_NUMBER_PLAYERS_IN_WITHOUT_TEAM_LIST)
                .toList();

        // Apply padding
        scaledWindowHeight -= TEAM_PADDING;

        var startX = TEAM_PADDING;
        var height = TEXT_PADDING + TEXT_RENDERER.fontHeight + TEXT_PADDING + (spectatorTeam.size() * (INTRA_PLAYER_PADDING + TEXT_RENDERER.fontHeight));
        var startY = scaledWindowHeight - height;

        // Draw background rect
        fill(matrices, startX, startY, startX + TEAM_WIDTH, startY + height, BACKGROUND_COLOR);

        // Draw header
        TEXT_RENDERER.drawWithShadow(matrices, "Spectators:", startX + TEXT_PADDING, startY + TEXT_PADDING, 0xFFFFFFFF);

        var playerManager = LasertagGameManager.getInstance().getPlayerManager();

        var yPos = startY + TEXT_PADDING + TEXT_RENDERER.fontHeight + TEXT_PADDING;
        for (var playerUuid : spectatorTeam) {
            // Draw players name
            TEXT_RENDERER.draw(matrices, playerManager.getPlayerUsername(playerUuid), startX + TEXT_PADDING, yPos, 0xFFFFFFFF);

            yPos += (TEXT_RENDERER.fontHeight + INTRA_PLAYER_PADDING);
        }
    }
}