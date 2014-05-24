package de.txtdata.socialradar.fetchers;

import de.txtdata.socialradar.es.ElasticsearchConnectionManager;
import de.txtdata.socialradar.models.AbstractPost;
import de.txtdata.util.Wait;

import java.util.*;

/**
 * A fetcher for Social Media posts, which queries an API at certain time intervals.
 */
public abstract class AbstractFetcher{
	
	protected String apiName = "generic";
	
	protected int sleepAfterFetch               = 10;
    protected int sleepAfterCycle               = 10;
	protected int dedupeListSize                = 10000;

    protected boolean endOfCycle = false;
    protected long sleepUntil = 0;

	protected List<String> deDupeList   = new Vector<>();

    public abstract List<AbstractPost> fetch();
    public abstract String buildQuery();

    public AbstractFetcher() {
        new ElasticsearchConnectionManager();
    }
	
	public void forwardFetching(){
        long now = new Date().getTime()/1000;
        sleepUntil = now + sleepAfterCycle;
        while(true){
            System.out.println(Calendar.getInstance().getTime() + " -- Fetching " + apiName + ":");
            List<AbstractPost> fetchedPosts = this.fetch();
            this.storeInDB(fetchedPosts);
            System.out.println("\tWrote " + fetchedPosts.size() + " posts to DB.");
            this.sleep();
		}
	}

    public void sleep(){
        if (!endOfCycle){
            Wait.forSeconds(sleepAfterFetch);
            return;
        }
        long now = new Date().getTime()/1000;
        long secs = sleepUntil-now;
        System.out.println("\tSleeping "+secs+" seconds.\n");
        while (true){
            now = new Date().getTime()/1000;
            if (now>=sleepUntil){
                sleepUntil = now + sleepAfterCycle;
                return;
            }
            Wait.forSeconds(1);
        }
    }

    protected boolean isOnDeDupeList(String id){
        return deDupeList.contains(id);
    }

    protected void addToDeDupeList(AbstractPost post){
        String id = post.id;
        if (deDupeList.contains(id)) return;
        deDupeList.add(id);
        while (deDupeList.size()>dedupeListSize){
            deDupeList.remove(dedupeListSize);
        }
    }

	public void storeInDB(List<AbstractPost> posts){
        for (AbstractPost post : posts) {
            try {
                ElasticsearchConnectionManager.getInstance().insertPost(post);
            } catch (Exception e) {
                System.out.println("Warning! Cannot write to DB. Is it up and running?");
                e.printStackTrace();
            }
        }
	}

    public static double log(double x, double base){
        return java.lang.Math.log(x) / java.lang.Math.log(base);
    }
}
