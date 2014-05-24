package de.txtdata.socialradar;

import de.txtdata.socialradar.es.ElasticsearchConnectionManager;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BaseQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

import java.util.Collection;
import java.util.Date;
import java.util.TreeMap;

/**
 * Class to detect hotspots.
 */
public class HotspotFinder {

    public HotspotFinder(){
        new ElasticsearchConnectionManager();
    }

    public void findHotspots(int timeSpanInMinutes){
        this.findHotspots(timeSpanInMinutes, null);
    }

    public void findHotspots(int timeSpanInMinutes, String queryString){
        long timeCutoff = ((new Date().getTime()/1000) - timeSpanInMinutes * 60);
        long now = (new Date().getTime()/1000) + 60;  // + 60 is just to make sure to not miss anything
        RangeFilterBuilder nrFilter = new RangeFilterBuilder("indexDate")
                .from(timeCutoff)
                .to(now);

        BaseQueryBuilder qb;
        if (queryString!=null) {
            qb = QueryBuilders.queryString(queryString);
        }else{
            qb = QueryBuilders.matchAllQuery();
        }

        SearchResponse response = ElasticsearchConnectionManager.getClient()
                .prepareSearch(ElasticsearchConnectionManager.getInstance().getPostIndexName())
                .setQuery(QueryBuilders.filteredQuery(qb, nrFilter))
                .addAggregation(terms("geohash").field("coordinates.geohash").include(".{7}").size(50))
                .execute()
                .actionGet();

        Terms  terms = response.getAggregations().get("geohash");
        Collection<Terms.Bucket> buckets = terms.getBuckets();

        TreeMap<Double,Hotspot> sortedHotspots = new TreeMap<>();
        for (Terms.Bucket bucket : buckets){
            String hash = bucket.getKey();
            Hotspot hotspot = new Hotspot(hash, timeSpanInMinutes);
            sortedHotspots.put(hotspot.getScore(),hotspot);
        }
        for (Hotspot hotspot  : sortedHotspots.descendingMap().values()){
            if (hotspot.getScore()>0.1) {
                System.out.println(hotspot.toString(true));
            }
        }
    }

    public static void main(String[] args){
        HotspotFinder hf = new HotspotFinder();
        hf.findHotspots(120);
    }

}
