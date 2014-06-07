package de.txtdata.socialradar.es;

import de.txtdata.socialradar.models.AbstractPost;
import de.txtdata.socialradar.models.Tweet;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.percolate.PercolateResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import java.io.IOException;
import java.util.ArrayList;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;

/**
 * Component to register percolators and get alerts when a specific post matches a defined Alert.
 **/
public class AlertManager {

    private final String ALERT_FIELD = "text";
    private final String PERCOLATOR_TYPE = ".percolator";
    private final String ALERT_INDEX = "alert";

    void addTermAlert(String term) {
        try {
            //This is the query we're registering in the percolator
            QueryBuilder qb = matchQuery("text", term);
            //Index the query = register it in the percolator
            ElasticsearchConnectionManager.getClient().prepareIndex(ALERT_INDEX, PERCOLATOR_TYPE,term)
                    .setSource(jsonBuilder()
                            .startObject()
                            .field("query", qb) // Register the query
                            .endObject())
                    .setRefresh(true) // Needed when the query shall be available immediately
                    .execute()
                    .actionGet();
        } catch (IOException e) {
            System.out.println("Error! Can't generate percolator.");
            e.printStackTrace();
        }
    }

    public ArrayList<String> listAllAlertTerms() {
        ArrayList<String> terms = new ArrayList<>();
        QueryBuilder qb = matchAllQuery();
        SearchResponse response = ElasticsearchConnectionManager.getClient().prepareSearch(ALERT_INDEX)
                .setTypes(PERCOLATOR_TYPE)
                .setQuery(qb)
                .addField("_id")
                .execute()
                .actionGet();
        for (SearchHit hit :response.getHits()) {
            terms.add(hit.getId());
        }
        return terms;
    }

    public boolean deleteAlert(String term) {
        DeleteResponse response = ElasticsearchConnectionManager.getClient()
                .prepareDelete(ALERT_INDEX, PERCOLATOR_TYPE, term)
                .execute()
                .actionGet();
        return response.isFound();
    }

    ArrayList<String> fetchMatchingAlertTerms(AbstractPost post){
        ArrayList<String> matchingTerms = new ArrayList<String>();
        try {
            XContentBuilder docBuilder = XContentFactory.jsonBuilder().startObject();
            docBuilder.field("doc").startObject(); //This is needed to designate the document
            docBuilder.field(ALERT_FIELD, post.text);
            docBuilder.endObject(); //End of the doc field
            docBuilder.endObject(); //End of the JSON root object
            //Percolate
            PercolateResponse response = ElasticsearchConnectionManager.getClient().preparePercolate()
                    .setIndices(ALERT_INDEX)
                    .setDocumentType(post.getType())
                    .setSource(docBuilder).execute().actionGet();
            //Iterate over the results and add the registered alert term
            for(PercolateResponse.Match match : response) {
                matchingTerms.add(match.getId().toString());
            }
        } catch (IOException e) {
            System.out.println("Error! Can't check if a post is matching a percolator alert");
            e.printStackTrace();
        }
        return matchingTerms;
    }

    public static void main(String[] args) {
        new ElasticsearchConnectionManager();
        AlertManager alertManager = new AlertManager();
        alertManager.addTermAlert("interesting topic");
        System.out.println("List of registered alerts");
        System.out.println(alertManager.listAllAlertTerms());
        Tweet testTweet = new Tweet();
        testTweet.text = "Hey! I am a tweet speaking about an interesting topic, I wish I would get warned :) ! ";
        System.out.println(testTweet + "\n matching any percolator terms ? ");
        System.out.println(alertManager.fetchMatchingAlertTerms(testTweet));
    }

}
