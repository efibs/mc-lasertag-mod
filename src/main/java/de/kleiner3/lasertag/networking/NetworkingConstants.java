package de.kleiner3.lasertag.networking;

import de.kleiner3.lasertag.LasertagMod;
import net.minecraft.util.Identifier;

/**
 * Class containing all networking constants
 *
 * @author Étienne Muser
 */
public class NetworkingConstants {
    // ===== Entities ===============
    public static final Identifier LASER_RAY_SPAWNED = new Identifier(LasertagMod.ID, "laser_ray_spawned");

    // ===== Lasertag game ==========
    public static final Identifier LASERTAG_GAME_TEAM_OR_SCORE_UPDATE = new Identifier(LasertagMod.ID, "lasertag_game_team_or_score_update");
    // TODO: Send and recieve settings changed
    public static final Identifier LASERTAG_SETTINGS_CHANGED = new Identifier(LasertagMod.ID, "lasertag_settings_changed");


    // ===== General ================
    public static final Identifier ERROR_MESSAGE = new Identifier(LasertagMod.ID, "error_message");

    // ===== Events =================
    public static final Identifier PLAY_WEAPON_FIRED_SOUND = new Identifier(LasertagMod.ID, "play_weapon_fired_sound");
    public static final Identifier PLAY_WEAPON_FAILED_SOUND = new Identifier(LasertagMod.ID, "play_weapon_failed_sound");
    public static final Identifier PLAY_PLAYER_SCORED_SOUND = new Identifier(LasertagMod.ID, "play_player_scored_sound");
    public static final Identifier GAME_STARTED = new Identifier(LasertagMod.ID, "game_started");
    public static final Identifier PROGRESS = new Identifier(LasertagMod.ID, "progress");
}
