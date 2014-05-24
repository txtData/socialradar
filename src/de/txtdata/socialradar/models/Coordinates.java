package de.txtdata.socialradar.models;

public class Coordinates {

    public double lat;
    public double lon;

    public Coordinates(){};

    public Coordinates(double lat, double lon){
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat(){
        return this.lat;
    }

    public double getLon(){
        return this.lon;
    }

    public boolean isNull(){
        return (this.lat==0.0 && this.lon==0.0);
    }

    @Override
    public String toString(){
        return lat+","+lon;
    }

}
