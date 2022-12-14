package de.kleiner3.lasertag.lasertaggame.statistics;

import de.kleiner3.lasertag.lasertaggame.ILasertagPlayer;
import de.kleiner3.lasertag.types.Colors;
import de.kleiner3.lasertag.util.Tuple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Class to calculate the lasertag game stats
 *
 * @author Étienne Muser
 */
public class StatsCalculator {
    private final HashMap<Colors.Color, ? extends List<? extends ILasertagPlayer>> teamMap;

    private GameStats lastGamesStats;

    public StatsCalculator(HashMap<Colors.Color, ? extends List<? extends ILasertagPlayer>> teamMap) {
        this.teamMap = teamMap;
    }

    public void calcStats() {
        lastGamesStats = new GameStats();
        for (Colors.Color teamColor : teamMap.keySet()) {
            var team = teamMap.get(teamColor);
            int teamScore = 0;

            var playersOfThisTeam = new ArrayList<Tuple<String, Integer>>();
            for (var player : team) {
                int playerScore = player.getLasertagScore();
                var playerScoreTuple = new Tuple<>(player.getLasertagUsername(), playerScore);
                teamScore += playerScore;
                lastGamesStats.playerScores.add(playerScoreTuple);
                playersOfThisTeam.add(playerScoreTuple);
            }

            if (playersOfThisTeam.size() > 0) {
                lastGamesStats.teamPlayerScores.put(teamColor.getName(), playersOfThisTeam);
                lastGamesStats.teamScores.add(new Tuple<>(teamColor.getName(), teamScore));
            }
        }

        // Sort stats
        lastGamesStats.teamScores.sort(Comparator.<Tuple<String, Integer>>comparingInt(t -> t.y).reversed());
        lastGamesStats.playerScores.sort(Comparator.<Tuple<String, Integer>>comparingInt(t -> t.y).reversed());
        for (var team : lastGamesStats.teamPlayerScores.values()) {
            team.sort(Comparator.<Tuple<String, Integer>>comparingInt(t -> t.y).reversed());
        }
    }

    public GameStats getLastGamesStats() {
        return lastGamesStats;
    }
}
