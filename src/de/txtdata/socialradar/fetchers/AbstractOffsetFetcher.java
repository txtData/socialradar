package de.txtdata.socialradar.fetchers;

/**
 * This fetcher starts fetching posts for a small area, and then moves that area with each fetch,
 * in order to over time cover a larger area.
 **/
public abstract class AbstractOffsetFetcher extends AbstractFetcher {

    protected double startLatitude = 52.543;
    protected double startLongitude = 13.280;

    protected double currentLatitude = 52.543;
    protected double currentLongitude = 13.280;


    protected double lonStep =  0.018;
    protected double latStep =  -0.014;
    protected int lonRepeat =  10;
    protected int latRepeat =  6;

    protected int currentLonPos;
    protected int currentLatPos;
    protected int currentOffset;

	public AbstractOffsetFetcher(){
    }

    public String buildQuery(){
        currentLatitude = startLatitude+(currentLatPos *latStep)+(currentOffset * latStep/2);
        currentLongitude = startLongitude+(currentLonPos *lonStep)+(currentOffset * lonStep/2);
        currentLonPos++;
        endOfCycle = false;
        if (currentLonPos >=lonRepeat){
            currentLonPos = 0;
            currentLatPos++;
            if (currentLatPos >=latRepeat){
                currentLatPos = 0;
                currentOffset++;
                if (currentOffset >1){
                    currentOffset =0;
                    endOfCycle = true;
                }
            }
        }
        return this.buildQuery(currentLatitude,currentLongitude);
    }

    public abstract String buildQuery(double lat, double lon);
}
