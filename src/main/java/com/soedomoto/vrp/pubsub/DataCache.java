package com.soedomoto.vrp.pubsub;

import com.google.gson.Gson;
import com.j256.ormlite.stmt.QueryBuilder;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.model.Enumerator;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by soedomoto on 26/01/17.
 */
public class DataCache implements Runnable {
    private final Jedis cache;
    private final App app;

    public DataCache(App app, String brokerUrl) throws URISyntaxException {
        this.app = app;
        this.cache = new Jedis(new URI(brokerUrl));
    }

    public void create() {
        Executors.newCachedThreadPool().execute(this);
    }

    public void run() {
        int delay = 0;
        while(true) {
            try {
                Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                    public void run() {
                        try {
                            cacheLocations();
                            cacheEnumerators();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }, delay, TimeUnit.SECONDS).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            delay = 30;
        }
    }

    private void cacheLocations() throws SQLException {
        List<CensusBlock> bses = app.getCensusBlockDao().queryBuilder().selectColumns("id", "lat", "lon", "service_time").query();
        String strBses = new Gson().toJson(bses);
        cache.del("locations");
        cache.set("locations", strBses);
    }

    private void cacheEnumerators() throws SQLException {
        List<Enumerator> enumerators = app.getEnumeratorDao().queryBuilder().selectColumns("id", "depot").query();
        String strEnumerators = new Gson().toJson(enumerators);
        cache.del("enumerators");
        cache.set("enumerators", strEnumerators);

        cacheVisits(enumerators);
    }

    private void cacheVisits(final List<Enumerator> enumerators) {
        for(Enumerator en: enumerators) {
            try {
                QueryBuilder<CensusBlock, Long> qb = app.getCensusBlockDao().queryBuilder();
                qb.where().eq("visited_by", Long.valueOf(en.getId()));
                qb.orderBy("visit_date", true);

                List<CensusBlock> visitedLocations = qb.query();
                String strVisitedLocations = new Gson().toJson(visitedLocations);

                cache.del(String.format("enumerator.%s.visits", en.getId()));
                cache.set(String.format("enumerator.%s.visits", en.getId()), strVisitedLocations);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
