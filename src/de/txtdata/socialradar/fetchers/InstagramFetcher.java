package de.txtdata.socialradar.fetchers;

import com.google.gson.*;
import de.txtdata.socialradar.Config;
import de.txtdata.socialradar.models.AbstractPost;
import de.txtdata.socialradar.models.InstagramPost;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class InstagramFetcher extends AbstractOffsetFetcher{

    private String apiEndpoint = "https://api.instagram.com/v1/media/";

	public InstagramFetcher(String apiName){
		this.apiName = apiName;
        this.sleepAfterFetch = 5;
        this.sleepAfterCycle = 120;
        this.startLatitude  = 52.543;
        this.startLongitude = 13.280;
        this.lonStep = 0.018;
        this.latStep = -0.014;
        this.lonRepeat = 12;
        this.latRepeat = 6;
        this.currentLonPos = 6;
        this.currentLatPos = 0;
        this.currentOffset = 1;
        if (Config.instagram_clientKey==null){
            System.out.println("Please add your Instagram API credentials to de.txtdata.socialradar.Config.java");
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
		try {
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
            JsonArray jarray = json.getAsJsonObject().getAsJsonArray("data");
            for (int i = 0; i < jarray.size(); i++) {
                InstagramPost post = new InstagramPost();
                JsonObject jsonObject = jarray.get(i).getAsJsonObject();

                // create unique ID
                post.id = "IG_" + jsonObject.get("id").getAsString();
                if (this.isOnDeDupeList(post.id)) {
                    continue;
                } else {
                    this.deDupeList.add(post.id);
                }

                // get date post was created
                post.indexDate = Long.parseLong(jsonObject.get("created_time").getAsString());

                // get url to Instagram page
                post.link = jsonObject.get("link").getAsString();

                // get image caption (if any)
                JsonElement caption = jsonObject.get("caption");
                if (caption.isJsonObject()) {
                    post.text = caption.getAsJsonObject().get("text").getAsString();
                }

                // get username and user ID
                JsonElement user = jsonObject.get("user");
                if (user.isJsonObject()) {
                    post.userName = user.getAsJsonObject().get("username").getAsString();
                    post.userID = user.getAsJsonObject().get("id").getAsString();
                }

                // get location (coordinates and location name)
                JsonObject location = jsonObject.getAsJsonObject("location");
                if (!location.isJsonNull()) {
                    post.setCoordinates(
                            Double.parseDouble(location.get("latitude").getAsString()),
                            Double.parseDouble(location.get("longitude").getAsString())
                    );
                    if (location.get("name") != null) {
                        post.setLocationName(location.get("name").getAsString());
                    }
                }

                // get image URL
                post.pictureURL = jsonObject.getAsJsonObject("images").getAsJsonObject("low_resolution").get("url").getAsString();

                // get number of comments
                JsonObject comments = jsonObject.getAsJsonObject("comments");
                if (!comments.isJsonNull()) {
                    post.commentsCount = Integer.parseInt(comments.get("count").getAsString());
                }

                // get number of likes
                JsonObject likes = jsonObject.getAsJsonObject("likes");
                if (!likes.isJsonNull()) {
                    post.likesCount = Integer.parseInt(likes.get("count").getAsString());
                }
                post.popularity = InstagramFetcher.computePopularity(post);

                System.out.println("\t" + post);

                // Add post to results
                if (post.hasCoordinates()) {
                    results.add(post);
                }
            }
        }catch (SocketTimeoutException ste){
            System.out.println("\tTimeout!");
        }catch (Exception e){
			e.printStackTrace();
		}
		return results;
	}

    private static int computePopularity(InstagramPost photo){
        int love = photo.commentsCount*10 + photo.likesCount;
        love = (int)log(love,1.1);
        if (love<=0)  love = 1;
        if (love>=99) love = 100;
        return love;
    }

    public String buildQuery(double lat, double lon){
        String result = this.apiEndpoint + "search?lat=" + lat + "&lng=" + lon + "&count=100&distance=5000&client_id=" + Config.instagram_clientKey;
        return result;
    }

    public static void main(String[] args) throws Exception{
        InstagramFetcher iF = new InstagramFetcher("Instagram");
        iF.forwardFetching();
    }
}
