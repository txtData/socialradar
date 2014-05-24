package de.txtdata.socialradar.fetchers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.txtdata.socialradar.Config;
import de.txtdata.socialradar.models.FoursquareTrendingPlace;
import de.txtdata.socialradar.models.AbstractPost;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Fetcher for the 4S API.
 */
public class FoursquareFetcher extends AbstractOffsetFetcher{

    public static void main(String[] args) throws Exception{
		FoursquareFetcher iF = new FoursquareFetcher("Foursquare");
		iF.forwardFetching();
	}

	public FoursquareFetcher(String apiName){
		this.apiName = apiName;
        this.sleepAfterFetch = 10;
        this.sleepAfterCycle = 120;
        this.startLatitude  = 52.543;
        this.startLongitude = 13.280;
        this.lonStep   = 0.036;
        this.latStep   = -0.028;
        this.lonRepeat = 6;
        this.latRepeat = 4;
        if (Config.foursquare_clientSecret==null){
            System.out.println("Please add your Foursquare API credentials to de.txtdata.socialradar.Config.java");
            System.exit(-1);
        }
    }

    public List<AbstractPost> fetch(){
        return fetch(this.buildQuery());
    }

    public List<AbstractPost> fetch(double lat, double lon){
        return fetch(this.buildQuery(lat,lon));
    }

	public List<AbstractPost> fetch(String query) {
        List<AbstractPost> results = new ArrayList<>();
		try{
			URL url = new URL(query);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = "";
            String line;
            while ((line = in.readLine()) != null) {
                response = response + line + "\n";
            }
            in.close();
            JsonElement json = new JsonParser().parse(response);
            JsonArray jarray = json.getAsJsonObject().get("response").getAsJsonObject().getAsJsonArray("venues");
            for(int i=0 ; i < jarray.size(); i++) {
                FoursquareTrendingPlace place = new FoursquareTrendingPlace();
                JsonObject jsonObject = jarray.get(i).getAsJsonObject();
                place.venueID = jsonObject.get("id").getAsString();
                place.id = "4S_"+place.venueID;
                place.setLocationName(jsonObject.get("name").getAsString());
                if (jsonObject.get("canonicalUrl")!=null){
                    place.canonicalURL = jsonObject.get("canonicalUrl").getAsString();
                }

                JsonArray categories = jsonObject.getAsJsonArray("categories");
                if (categories.size()>0){
                    JsonObject category = categories.get(0).getAsJsonObject();
                    place.placeCategory = category.get("name").getAsString();
                }
                place.indexDate =  new Date().getTime()/1000;

                JsonElement stats = jsonObject.get("stats");
                if (stats.isJsonObject()){
                    place.checkinCount = Integer.parseInt(stats.getAsJsonObject().get("checkinsCount").getAsString());
                    place.usersCount = Integer.parseInt(stats.getAsJsonObject().get("usersCount").getAsString());
                    place.tipCount = Integer.parseInt(stats.getAsJsonObject().get("tipCount").getAsString());
                }
                JsonElement hereNow = jsonObject.get("hereNow");
                if (hereNow.isJsonObject()){
                    place.hereNow = Integer.parseInt(hereNow.getAsJsonObject().get("count").getAsString());
                }
                JsonElement location = jsonObject.get("location");
                if (location.isJsonObject()){
                    place.setCoordinates(
                        Double.parseDouble(location.getAsJsonObject().get("lat").getAsString()),
                        Double.parseDouble(location.getAsJsonObject().get("lng").getAsString())
                    );
                }

                place.text =  place.locationName;
                place.popularity = computePopularity(place);

                if (place.hasCoordinates())
                {
                    results.add(place);
                    System.out.println("\t" + place);
                }
            }
        }catch (SocketTimeoutException ste){
            System.out.println("\tTimeout!");
        }catch (Exception e){
			e.printStackTrace();
		}
		return results;
    }

    private static int computePopularity(FoursquareTrendingPlace place){
        double d1 = (place.hereNow / ((double)place.checkinCount+1.0)) * 100;
        double d2 = place.hereNow / ((double)place.tipCount+1.0);
        if (d1>1.0) d1=1.0;
        if (d2>1.0) d2=1.0;
        double d3 = (d1+d2)/2.0;
        int love = (int)(d3*100);
        if (love<=0) love = 1;
        if (love>100) love = 100;
        return love;
    }

    public String buildQuery(double lat, double lon){
        String result = "https://api.foursquare.com/v2/venues/trending"
                +"?ll="+lat+","+lon
                +"&limit=50&radius=2000"
                +"&client_id="+Config.foursquare_clientId
                +"&client_secret="+Config.foursquare_clientSecret
                +"&v=20130713";
        return result;
    }
}
