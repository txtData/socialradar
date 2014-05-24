package de.txtdata.socialradar.fetchers;

import de.txtdata.socialradar.models.Tweet;
import twitter4j.Status;

/**
 * Creates a Tweet class from a Twitter4J status
 */
public class TweetParser {

    public static Tweet parse(Status status){
        Tweet tweet = new Tweet();
        tweet.createdAt = status.getCreatedAt();
        tweet.indexDate = status.getCreatedAt().getTime()/1000;
        tweet.id = "TW_"+status.getId();
        tweet.twitterId = String.valueOf(status.getId());
        tweet.text = status.getText();
        tweet.userName = status.getUser().getScreenName();
        tweet.fromUserId = status.getUser().getId()+"_";

        tweet.followersCount = status.getUser().getFollowersCount();
        tweet.retweetCount = status.getRetweetCount();
        tweet.favoriteCount = status.getFavoriteCount();
        tweet.isReply = (status.getInReplyToUserId()!=-1);
        tweet.source =  status.getSource();
        tweet.isRetweet = status.isRetweet();
        tweet.location = status.getUser().getLocation();
        tweet.language = status.getUser().getLang();

        if (status.getURLEntities().length>0){
            tweet.fullURL = status.getURLEntities()[0].getDisplayURL();
        }

        if (status.getGeoLocation()!=null){
            tweet.setCoordinates(status.getGeoLocation().getLatitude(), status.getGeoLocation().getLongitude());
        }else{
            tweet.setCoordinates(0,0);
        }
        if (status.getPlace()!=null){
            tweet.setLocationName(status.getPlace().getFullName());
        }

        if (status.getMediaEntities().length>0){
            tweet.mediaURL = status.getMediaEntities()[0].getMediaURL()+":small";
        }
        tweet.popularity = TweetParser.computePopularity(tweet);
        return tweet;
    }

    private static int computePopularity(Tweet tweet){
        int c =  tweet.retweetCount*18 + tweet.retweetCount*10 + (tweet.followersCount/10);
        int love = (int) AbstractFetcher.log(c, 1.2);
        if (love<=0) love = 1;
        if (love>=99) love = 100;
        return love;
    }
}
