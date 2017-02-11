package com.soedomoto.vrp.pubsub;

import com.google.gson.Gson;
import com.j256.ormlite.stmt.QueryBuilder;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;
import com.soedomoto.vrp.model.dao.DistanceMatrix;
import com.soedomoto.vrp.model.dao.Enumerator;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by soedomoto on 26/01/17.
 */
public class DataCache implements Runnable {
    private final Jedis cache;
    private final App app;
    private final CountDownLatch latch;

    public DataCache(App app, String brokerUrl, CountDownLatch latch) throws URISyntaxException {
        this.app = app;
        this.cache = new Jedis(new URI(brokerUrl));
        this.latch = latch;
    }

    public void create() {
        Executors.newCachedThreadPool().execute(this);
    }

    public void run() {
        int delay = 0;
        boolean initial = true;
        while(true) {
            try {
                final boolean finalInitial = initial;
                Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                    public void run() {
                        try {
                            cacheLocations(finalInitial);
                            cacheDistances();
                            cacheEnumerators(finalInitial);

                            latch.countDown();
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
            initial = false;
        }
    }

    private void cacheLocations(boolean initial) throws SQLException {
        List<CensusBlock> bses = null;
        if(initial) {
            bses = app.getCensusBlockDao().queryBuilder().query();
            for (CensusBlock bs : bses) {
                String aTs = "";
                if(bs.getAssignDate() != null)
                    aTs = String.valueOf(bs.getAssignDate().getTime());

                String vTs = "";
                if(bs.getVisitDate() != null)
                    vTs = String.valueOf(bs.getVisitDate().getTime());

                String vBy = "";
                if(bs.getVisitedBy() != null)
                    vBy = String.valueOf(bs.getVisitedBy());

                if(cache.get(String.format("location.%s.assign-date", bs.getId())) == null) {
                    cache.set(String.format("location.%s.assign-date", bs.getId()), aTs);
                }

                if(cache.get(String.format("location.%s.visit-date", bs.getId())) == null) {
                    cache.set(String.format("location.%s.visit-date", bs.getId()), vTs);
                }

                if(cache.get(String.format("location.%s.visit-by", bs.getId())) == null) {
                    cache.set(String.format("location.%s.visit-by", bs.getId()), vBy);
                }
            }
        } else {
            bses = app.getCensusBlockDao().queryBuilder().query();
            for (CensusBlock bs : bses) {
                String aTs = cache.get(String.format("location.%s.assign-date", bs.getId()));
                String vTs = cache.get(String.format("location.%s.visit-date", bs.getId()));
                String vBy = cache.get(String.format("location.%s.visit-by", bs.getId()));

                if (aTs != null && aTs.length() != 0) {
                    Date date = new Date(Double.valueOf(aTs).longValue());
                    bs.setAssignDate(date);
                }

                if (vTs != null && vTs.length() != 0) {
                    Date date = new Date(Double.valueOf(vTs).longValue());
                    bs.setVisitDate(date);
                }

                if (vBy != null && vBy.length() != 0) {
                    bs.setVisitedBy(Long.valueOf(vBy));
                }

                app.getCensusBlockDao().update(bs);
            }
        }

        bses = app.getCensusBlockDao().queryBuilder().selectColumns("id", "lat", "lon", "service_time").query();
        String strBses = new Gson().toJson(bses);
        cache.del("locations");
        cache.set("locations", strBses);
    }

    private void cacheDistances() throws SQLException {
        List<DistanceMatrix> distances = app.getDistanceMatrixDao().queryForAll();
        String strDistances = new Gson().toJson(distances);
        cache.del("distances");
        cache.set("distances", strDistances);
    }

    private void cacheEnumerators(boolean initial) throws SQLException {
        List<Enumerator> enumerators = null;

        if(initial) {
            enumerators = app.getEnumeratorDao().queryBuilder().query();
            for(Enumerator e : enumerators) {
                String vBy = "";
                if(new Long((e.getDepot())) != null)
                    vBy = String.valueOf(e.getDepot());

                cache.del(String.format("enumerator.%s.depot", e.getId()));
                cache.set(String.format("enumerator.%s.depot", e.getId()), vBy);
            }
        } else {
            enumerators = app.getEnumeratorDao().queryBuilder().query();
            for(Enumerator e : enumerators) {
                String dId = cache.get(String.format("enumerator.%s.depot", e.getId()));
                if (dId != null && dId.length() != 0) {
                    e.setDepot(Long.parseLong(dId));
                    app.getEnumeratorDao().update(e);
                }
            }
        }

        enumerators = app.getEnumeratorDao().queryBuilder().selectColumns("id", "depot").query();
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
