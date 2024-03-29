package de.pewpewproject.lasertag.lasertaggame.gamemode;

import de.pewpewproject.lasertag.lasertaggame.gamemode.implementation.CaptureTheFlagGameMode;
import de.pewpewproject.lasertag.lasertaggame.gamemode.implementation.EliminationGameMode;
import de.pewpewproject.lasertag.lasertaggame.gamemode.implementation.MusicalChairsGameMode;
import de.pewpewproject.lasertag.lasertaggame.gamemode.implementation.PointHunterGameMode;

import java.util.Map;

import static java.util.Map.entry;

/**
 * Class holding all game modes
 *
 * @author Étienne Muser
 */
public class GameModes {

    public static final GameMode POINT_HUNTER_GAME_MODE = new PointHunterGameMode();
    public static final GameMode CAPTURE_THE_FLAG_GAME_MODE = new CaptureTheFlagGameMode();
    public static final GameMode MUSICAL_CHAIRS_GAME_MODE = new MusicalChairsGameMode();
    public static final GameMode ELIMINATION = new EliminationGameMode();

    public static final Map<String, GameMode> GAME_MODES = Map.ofEntries(
            entry(ELIMINATION.getTranslatableName(), ELIMINATION),
            entry(CAPTURE_THE_FLAG_GAME_MODE.getTranslatableName(), CAPTURE_THE_FLAG_GAME_MODE),
            entry(MUSICAL_CHAIRS_GAME_MODE.getTranslatableName(), MUSICAL_CHAIRS_GAME_MODE),
            entry(POINT_HUNTER_GAME_MODE.getTranslatableName(), POINT_HUNTER_GAME_MODE)
    );
}
