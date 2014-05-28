package de.txtdata.socialradar.es;

import com.google.gson.Gson;
import de.txtdata.socialradar.Config;
import de.txtdata.socialradar.models.AbstractPost;
import de.txtdata.util.Wait;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.elasticsearch.client.Requests.createIndexRequest;
import static org.elasticsearch.client.Requests.deleteIndexRequest;
import static org.elasticsearch.client.Requests.indicesExistsRequest;

/**
 * Manages connections to Elasticsearch
 **/
public class ElasticsearchConnectionManager {

    private static final int    PORT             = 9300;
    private static final String POST_INDEX       = "posts";
    private static final String MAPPINGS_FILE    = "data\\es\\mapping.json";
    private static final String SETTINGS_FILE    = "data\\es\\settings.json";

    private static ElasticsearchConnectionManager INSTANCE;
    private static Client CLIENT;

    public ElasticsearchConnectionManager(){
        initialize();
    }

    public static ElasticsearchConnectionManager getInstance(){
        return INSTANCE;
    }

    public static Client getClient(){
        return CLIENT;
    }

    public String getPostIndexName(){
        return POST_INDEX;
    }

    public void insertPost(AbstractPost post){
        String json = new Gson().toJson(post);
        IndexRequest indexRequest = new IndexRequest(POST_INDEX, post.getType(), post.id);
        indexRequest.source(json);
        CLIENT.index(indexRequest).actionGet();
    }

    public List<AbstractPost> queryForPosts(double lon, double lat, double distanceInMeters, int timeInMins){
        List<AbstractPost> results = new ArrayList<>();
        long timeCutoff = ((new Date().getTime() / 1000) - timeInMins * 60);
        long now = (new Date().getTime() / 1000) + 60;  // + 60 is just to make sure to not miss anything

        GeoDistanceFilterBuilder gdFilter = FilterBuilders.geoDistanceFilter("coordinates")
                .point(lon,lat)
                .distance(distanceInMeters, DistanceUnit.METERS)
                .optimizeBbox("memory")
                .geoDistance(GeoDistance.ARC);

        RangeFilterBuilder rFilter = new RangeFilterBuilder("indexDate")
                .from(timeCutoff)
                .to(now);

        AndFilterBuilder andFilter = FilterBuilders.andFilter(rFilter,gdFilter);

        SearchResponse response = CLIENT.prepareSearch(POST_INDEX)
                .setQuery(QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(), andFilter))
                .setSize(100)
                .addSort(new GeoDistanceSortBuilder("coordinates").point(lon, lat))
                .execute()
                .actionGet();

        for (SearchHit hit : response.getHits()){
            try{
                String type = (String)hit.getSource().get("myType");
                AbstractPost post = (AbstractPost) new Gson().fromJson(hit.getSourceAsString(), Class.forName(type));
                results.add(post);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return results;
    }

    private void initialize(){
        if (INSTANCE!=null) return;
        try {
            System.setProperty("es.path.data", Config.elasticsearch_indexLocation);
            CLIENT = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", PORT));
            if (!CLIENT.admin().indices().exists(indicesExistsRequest(POST_INDEX)).actionGet().isExists()){
                setUpIndex(POST_INDEX);
            }
            INSTANCE = this;
        }catch (Exception e) {
            System.out.println("Error! Cannot initialize ElasticSearchConnectionManager.");
            e.printStackTrace();
        }
    }

    private void setUpIndex(String indexName){
        try{
            if (CLIENT.admin().indices().exists(indicesExistsRequest(indexName)).actionGet().isExists()){
                CLIENT.admin().indices().delete(deleteIndexRequest(indexName)).actionGet();
                Wait.forSeconds(5);
                System.out.println("Index '"+indexName+"' deleted.");
            }
            CLIENT.admin().indices().create(createIndexRequest(indexName)).actionGet();
            Wait.forSeconds(5);
            System.out.println("Index '"+indexName+"' (re)created.");

            Path mappingPath = Paths.get(MAPPINGS_FILE);
            System.out.println("Loading mappings from "+mappingPath);
            String mappingString = new String(Files.readAllBytes(mappingPath), StandardCharsets.UTF_8);
            CLIENT.admin().indices().preparePutMapping(indexName).setType("_default_").setSource(mappingString).execute().actionGet();
            System.out.println("Post mapping set.");
        }catch(Exception e){
            System.out.println("Error! Cannot set up index '"+indexName+"'.");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        new ElasticsearchConnectionManager();
    }

}
