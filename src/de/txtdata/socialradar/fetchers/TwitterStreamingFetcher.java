package de.txtdata.socialradar.fetchers;

import de.txtdata.socialradar.Config;
import de.txtdata.socialradar.es.ElasticsearchConnectionManager;
import de.txtdata.socialradar.models.Tweet;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterStreamingFetcher {

    private static boolean      storeOnlyPostsWithCoordinates = true;
    private static double[][]   locations     = {{13.000,52.326},{13.683,52.658}}; // Berlin

    public static void main(String[] args){
        new ElasticsearchConnectionManager();
        new TwitterStreamingFetcher();
    }

    public TwitterStreamingFetcher(){
        if (Config.twitter_accessTokenSecret==null){
            System.out.println("Please add your Twitter API credentials to de.txtdata.socialradar.Config.java");
            System.exit(-1);
        }
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
            .setOAuthConsumerKey(Config.twitter_consumerKey)
            .setOAuthConsumerSecret(Config.twitter_consumerSecret)
            .setOAuthAccessToken(Config.twitter_accessToken)
            .setOAuthAccessTokenSecret(Config.twitter_accessTokenSecret);

        StatusListener listener = new StatusListener(){
            public void onStatus(Status status) {
                Tweet tweet = TweetParser.parse(status);
                System.out.println(tweet);
                if (Config.twitter_usersToIgnore.contains(tweet.userName)){
                    System.out.println("\tUser " + tweet.userName + " is on ignore list. Skipping.");
                }else if (tweet.hasCoordinates() || !storeOnlyPostsWithCoordinates){
                    ElasticsearchConnectionManager.getInstance().insertPost(tweet);
                }
            }

            // todo: Delete these tweets.
            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
            }
        };
        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        twitterStream.addListener(listener);
        twitterStream.filter(new FilterQuery().locations(locations));
    }
}
