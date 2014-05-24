package de.txtdata.socialradar.models;

/**
 * A place that is currently trending on Foursquare.
 */
public class FoursquareTrendingPlace extends AbstractPost {

    public      String placeCategory;
	public 		String canonicalURL;
    public      String venueID;

	public 		int   checkinCount;
	public 		int   usersCount;
	public		int   tipCount;
    public		int   hereNow;
	
	public String toString(){
		return String.format("4S %s,%s \"%s\" (%s), here now:%d", this.getLat(), this.getLon(), locationName, placeCategory, hereNow);
	}
}
