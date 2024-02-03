package de.kleiner3.lasertag.lasertaggame.state.synced.implementation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import de.kleiner3.lasertag.lasertaggame.settings.SettingDescription;

import java.util.HashMap;

/**
 * The state of the settings.
 *
 * @author Étienne Muser
 */
public class SettingsState extends HashMap<String, Object> {

    public static SettingsState createBaseSettings() {
        var settings = new SettingsState();

        for (var setting : SettingDescription.values()) {
            if (setting.getDataType().isEnum()) {
                settings.put(setting.getName(), ((Enum<?>)setting.getBaseValue()).name());
            } else {
                settings.put(setting.getName(), setting.getBaseValue());
            }
        }

        return settings;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static SettingsState fromJson(String json) {
        return new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .create()
                .fromJson(json, SettingsState.class);
    }
}
