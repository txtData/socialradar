package de.txtdata.socialradar.models;


import java.util.Date;

/**
 * A post on Instagram.
 */
public class InstagramPost extends AbstractPost {
    public 		String   link;
	public 		String   pictureURL;
	public 		String   userID;
    public      int      commentsCount = -1;
    public      int      likesCount    = -1;
	
	public String toString(){
        String locationString = "";
        if (locationName!=null) locationString = " ("+locationName+")";
        String captionString = "";
        if (text!=null) captionString = "\""+text.replaceAll("\n"," ")+"\"";
		String result = "IN "+ new Date(indexDate*1000) +" "+this.getLat()+","+this.getLon()+locationString+" "+ userName +": "+ captionString +" "+link;
        return result;
    }
}
