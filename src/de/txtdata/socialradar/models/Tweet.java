package de.txtdata.socialradar.models;

import java.util.Date;

/**
 * A tweet.
 */
public class Tweet extends AbstractPost {
	public 		String   twitterId;
	public      Date     createdAt;
	public		String	 fromUserId;

    public      int      followersCount;
    public      int      retweetCount;
    public      int      favoriteCount;
    public      boolean  isReply;
    public      String   source;
    public      String   location;
    public      boolean  isRetweet;
    public      String   language;
    public      String   fullURL;
    public      String   mediaURL;

	
	public String toString(){
        String sText = text.replaceAll("\n"," ");
        if (this.hasCoordinates() && this.locationName!=null)
            return "TW "+createdAt+" "+this.getLat()+","+this.getLon()+" ("+locationName+") "+userName+": "+sText;
		else if (this.hasCoordinates())
            return "TW "+createdAt+" "+this.getLat()+","+this.getLon()+" "+userName+": "+sText;
        else if (this.locationName!=null)
            return "TW "+createdAt+" "+locationName+" "+userName+": "+sText;
        else
            return "TW "+createdAt+" [NO LOCATION] "+userName+": "+sText;
	}
}
