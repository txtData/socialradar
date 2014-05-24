package de.txtdata.socialradar.models;

import com.google.gson.Gson;

import java.util.List;

/**
 * Base class for all Social Media posts.
 */
public abstract class AbstractPost {

    public	 	String       id;
    public      String       myType;
    public      long         indexDate;
    private     Coordinates  coordinates    = new Coordinates();
    public      String       locationName;
    public      String       userName; // screen name, not the clear name
    public      String       text;
    public      double       popularity     = -1;

    public AbstractPost(){
        this.myType = this.getClass().getName();
    }

    public boolean equals(Object o){
        if (! (o instanceof AbstractPost)) return false;
        AbstractPost post = (AbstractPost)o;
        if (this.id == null) return false;
        return (this.id.equals(post.id));
    }

    public void setLocationName(String locationName){
        this.locationName = locationName;
    }

    public double getLat(){
        return this.coordinates.lat;
    }

    public double getLon(){
        return this.coordinates.lon;
    }

    public void setCoordinates(final double lat, final double lon){
        this.coordinates = new Coordinates(lat,lon);
    }

    public boolean hasCoordinates(){
        return !this.coordinates.isNull();
    }

    public String getType(){
        return this.getClass().getSimpleName();
    }

    public String asJSON(){
        return new Gson().toJson(this);
    }
}
