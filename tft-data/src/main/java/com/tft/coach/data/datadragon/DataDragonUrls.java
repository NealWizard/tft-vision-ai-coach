package com.tft.coach.data.datadragon;

/**
 * URL builders for Riot Data Dragon endpoints.
 */
public final class DataDragonUrls {

    public static final String BASE = "https://ddragon.leagueoflegends.com";
    public static final String VERSIONS = BASE + "/api/versions.json";

    private DataDragonUrls() {}

    public static String dataJson(String version, String locale, DataDragonResource resource) {
        return BASE + "/cdn/" + version + "/data/" + locale + "/" + resource.fileName();
    }
}
