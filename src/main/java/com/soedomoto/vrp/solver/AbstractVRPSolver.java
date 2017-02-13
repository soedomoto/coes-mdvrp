package com.soedomoto.vrp.solver;

import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;
import com.soedomoto.vrp.model.dao.DistanceMatrix;
import com.soedomoto.vrp.model.dao.Enumerator;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by soedomoto on 03/02/17.
 */
public abstract class AbstractVRPSolver implements VRPSolver, Runnable {
    private final static Logger LOG = Logger.getLogger(AbstractVRPSolver.class.getName());

    protected final Map<Long, Map<Long, Double>> durationMatrix = new LinkedHashMap();
    protected final Map<Long, Map<Long, Double>> distanceMatrix = new LinkedHashMap();

    protected final App app;
    protected final String channel;
    protected String baseDir;
    protected boolean useAllEnumerator = true;
    protected Set<String> otherChannels = new HashSet<String>();
    protected final Jedis cache;

    protected Map<Long, Enumerator> allEnumerators = new LinkedHashMap();
    protected Map<Long, Enumerator> subscribingEnumerators = new LinkedHashMap();
    protected Map<Long, Enumerator> selfEnumerator = new LinkedHashMap();
    protected Map<Long, Enumerator> enumerators = new LinkedHashMap();
    protected Map<Long, CensusBlock> allBses = new LinkedHashMap();
    protected Map<Long, CensusBlock> unassignedBses = new LinkedHashMap();

    public AbstractVRPSolver(App app, String channel, String brokerUrl) throws SQLException, URISyntaxException {
        this.app = app;
        this.channel = channel;
        this.cache = new Jedis(new URI(brokerUrl));
    }

    public AbstractVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl) throws SQLException, URISyntaxException {
        this(app, channel, brokerUrl);
        this.otherChannels = otherChannels;
    }

    public AbstractVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        this(app, channel, otherChannels, brokerUrl);
        this.useAllEnumerator = useAllEnumerator;
    }

    public AbstractVRPSolver(App app, String channel, Set<String> otherChannels, String brokerUrl, String baseDir, boolean useAllEnumerator) throws SQLException, URISyntaxException {
        this(app, channel, otherChannels, brokerUrl, useAllEnumerator);
        this.baseDir = baseDir;
    }

    public void run() {
        try {
            // Prepare Data
            for (Enumerator e : app.getEnumeratorDao().queryForAll()) {
                String dId = cache.get(String.format("enumerator.%s.depot", e.getId()));
                if(dId != null && dId.length() != 0) e.setDepot(Long.parseLong(dId));

                allEnumerators.put(e.getId(), e);
                if(Long.parseLong(channel) == e.getDepot()) {
                    selfEnumerator.put(e.getId(), e);
                }
                if(Long.parseLong(channel) == e.getDepot() || otherChannels.contains(String.valueOf(e.getDepot()))) {
                    subscribingEnumerators.put(e.getId(), e);
                }
            }

            for (CensusBlock bs : app.getCensusBlockDao().queryForAll()) {
                allBses.put(bs.getId(), bs);

                String aTs = cache.get(String.format("location.%s.assign-date", bs.getId()));
                if(aTs == null || aTs.length() == 0) unassignedBses.put(bs.getId(), bs);

//                if (bs.getAssignedTo() == null) unassignedBses.put(bs.getId(), bs);
            }

            if (durationMatrix.size() == 0 || distanceMatrix.size() == 0) {
                for (DistanceMatrix m : app.getDistanceMatrixDao().queryForAll()) {
                    if (!durationMatrix.keySet().contains(m.getFrom()))
                        durationMatrix.put(m.getFrom(), new HashMap());
                    durationMatrix.get(m.getFrom()).put(m.getTo(), m.getDuration());

                    if (!distanceMatrix.keySet().contains(m.getFrom()))
                        distanceMatrix.put(m.getFrom(), new HashMap());
                    distanceMatrix.get(m.getFrom()).put(m.getTo(), m.getDistance());
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
