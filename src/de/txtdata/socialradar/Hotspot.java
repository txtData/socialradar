package de.txtdata.socialradar;

import ch.hsr.geohash.GeoHash;
import de.txtdata.socialradar.es.ElasticsearchConnectionManager;
import de.txtdata.socialradar.models.AbstractPost;
import de.txtdata.socialradar.models.Coordinates;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A Hotspot is made up of several posts, which are posted in spatial and temporal proximity of each other.
 */
public class Hotspot{

    private static final Locale LOCALE = Locale.ENGLISH;

    private Coordinates centralCoordinates;
    private String displayLocation;
    private double score = -1;
    private String uniqueUserDebugString = null;  // 3_5_12 would mean: 3 unique users in inner circle, 5 in middle circle, 12 in outer.

    private List<AbstractPost> innerPosts = null;
    private List<AbstractPost> middlePosts = null;
    private List<AbstractPost> outerPosts = null;

    public Hotspot(String geoHashString, int smallestTimeFrame){
        GeoHash geoHash = GeoHash.fromGeohashString(geoHashString);
        double cLat = geoHash.getBoundingBoxCenterPoint().getLatitude();
        double cLon = geoHash.getBoundingBoxCenterPoint().getLongitude();
        this.centralCoordinates = new Coordinates(cLat,cLon);
        this.computeScore(smallestTimeFrame);
        this.displayLocation = this.getMostCommonLocationName();
    }

    public double getScore(){
        return this.score;
    }

    public String toString(){
        return this.toString(false);
    }

    public String toString(boolean withPosts){
        String s = String.format(LOCALE,"Hotspot around '%s', Score:%f\t%s\n", this.displayLocation, this.score, uniqueUserDebugString);
        if (!withPosts){
            return s;
        }else{
            StringBuilder sb = new StringBuilder(s);
                sb.append("  ").append("Within 100m:\n");
                for (AbstractPost post : innerPosts){
                    sb.append("  ").append(post.toString()).append("\n");
                }
                sb.append("  ").append("Within 200m:\n");
                for (AbstractPost post : middlePosts){
                    if (!innerPosts.contains(post)) sb.append("  ").append(post.toString()).append("\n");
                }
                sb.append("  ").append("Within 300m:\n");
                for (AbstractPost post : outerPosts){
                    if (!innerPosts.contains(post) && !middlePosts.contains(post)) sb.append("  ").append(post.toString()).append("\n");
                }
            return sb.toString();
        }
    }

    private void computeScore(int smallestTimeFrame){
        this.innerPosts = ElasticsearchConnectionManager.getInstance().queryForPosts(centralCoordinates.getLat(), centralCoordinates.getLon(), 100, smallestTimeFrame);
        double postsInInnerCircle = this.getDistinctPostersCount(this.innerPosts);

        this.middlePosts = ElasticsearchConnectionManager.getInstance().queryForPosts(centralCoordinates.getLat(), centralCoordinates.getLon(), 200, smallestTimeFrame *2);
        double postsInMiddleCircle = this.getDistinctPostersCount(this.middlePosts);

        this.outerPosts = ElasticsearchConnectionManager.getInstance().queryForPosts(centralCoordinates.getLat(), centralCoordinates.getLon(), 300, smallestTimeFrame *3);
        double postsInOuterCircle = this.getDistinctPostersCount(this.outerPosts);

        uniqueUserDebugString = (int)postsInInnerCircle+"_"+(int)postsInMiddleCircle+"_"+(int)postsInOuterCircle;

        double relation1to2 = postsInInnerCircle/(postsInMiddleCircle+1);
        double relation2to3 = postsInMiddleCircle/(postsInOuterCircle+3);
        double absolute = (Math.log10(postsInInnerCircle)+Math.log10(postsInOuterCircle))/2.0;
        this.score =  (Math.max(relation1to2, relation2to3)*2 + absolute)/3.0;
    }

    private String getMostCommonLocationName(){
        Bag bag = new HashBag();
        for (AbstractPost post : this.innerPosts){
            if (post.locationName!=null && !post.locationName.equals("Berlin")){
                bag.add(post.locationName,1);
            }
        }
        for (AbstractPost post : this.middlePosts){
            if (post.locationName!=null && !post.locationName.equals("Berlin")){
                bag.add(post.locationName,2);
            }
        }
        int highest = -1;
        String locationName = null;
        for (Object o: bag.uniqueSet()){
            int count = bag.getCount(o);
            String name = (String) o;
            if (count>highest){
                highest = count;
                locationName = name;
            }
        }
        if (locationName==null) locationName = "unknown location";
        return locationName;
    }

    private int getDistinctPostersCount(List<AbstractPost> posts){
        List<String> usernames = new ArrayList<>();
        for (AbstractPost post: posts){
            if (!usernames.contains(post.userName)) usernames.add(post.userName);
        }
        return usernames.size();
    }
}
