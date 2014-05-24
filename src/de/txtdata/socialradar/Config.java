package de.txtdata.socialradar;

import java.util.Arrays;
import java.util.List;

/**
 * Global configurations.
 */
public class Config {
    public static String twitter_consumerKey = null;
    public static String twitter_consumerSecret = null;
    public static String twitter_accessToken = null;
    public static String twitter_accessTokenSecret = null;

    public static String instagram_clientKey    = null;

    public static String foursquare_clientId =  null;
    public static String foursquare_clientSecret = null;

    public static String elasticsearch_indexLocation = "C:\\data\\index";

    public static List<String> twitter_usersToIgnore = Arrays.asList(
            "trendinaliaDE",
            "pairsonnalitesD",
            "ptext",
            "ssbot",
            "_BB_RADIO_MUSIC",
            "RadioTeddyMusic",
            "030_Berlin"
    );
}
