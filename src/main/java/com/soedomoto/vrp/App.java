package com.soedomoto.vrp;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.soedomoto.vrp.model.CensusBlock;
import com.soedomoto.vrp.model.DistanceMatrix;
import com.soedomoto.vrp.model.Enumerator;
import com.soedomoto.vrp.model.Subscriber;
import com.soedomoto.vrp.pubsub.ChannelSubscriber;
import com.soedomoto.vrp.pubsub.DataCache;
import com.soedomoto.vrp.pubsub.VRPWorker;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by soedomoto on 19/01/17.
 */
public class App {
    private final static Logger LOG = Logger.getLogger(App.class.getName());

    private Dao<Enumerator, Long> enumeratorDao;
    private Dao<CensusBlock, Long> censusBlockDao;
    private Dao<DistanceMatrix, Long> distanceMatrixDao;
    private Dao<Subscriber, Long> subscriberDao;

    private int maxIteration = 500;

    public App(CommandLine cmd) {
        File outDir = new File(cmd.getOptionValue("O"));
        if(! outDir.isAbsolute()) outDir = new File(System.getProperty("user.dir"), cmd.getOptionValue("O"));

        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").format(new Date());
        if(cmd.hasOption("t")) outDir = new File(outDir, now);

        if(cmd.hasOption("I")) maxIteration = Integer.parseInt(cmd.getOptionValue("I"));

        final String BASE_DIR = outDir.getAbsolutePath();
        final String JDBC_URL = cmd.getOptionValue("db");
        final String BROKER_URL = cmd.getOptionValue("B");

        try {
            // Setup DAO
            ConnectionSource connectionSource = new JdbcConnectionSource(JDBC_URL);
            enumeratorDao = DaoManager.createDao(connectionSource, Enumerator.class);
            censusBlockDao = DaoManager.createDao(connectionSource, CensusBlock.class);
            distanceMatrixDao = DaoManager.createDao(connectionSource, DistanceMatrix.class);
            subscriberDao = DaoManager.createDao(connectionSource, Subscriber.class);

            TableUtils.createTableIfNotExists(connectionSource, Enumerator.class);
            TableUtils.createTableIfNotExists(connectionSource, CensusBlock.class);
            TableUtils.createTableIfNotExists(connectionSource, DistanceMatrix.class);
            TableUtils.createTableIfNotExists(connectionSource, Subscriber.class);

            // Cache important data
            new DataCache(App.this, BROKER_URL).create();

            // Create client logger
            String clientLogDir = BASE_DIR + File.separator + "client_log";
            ChannelSubscriber subs = new ChannelSubscriber(App.this, BROKER_URL);
            subs.clientLogger(clientLogDir);
            subs.visit();

            // Depot watcher
            // new DepotWatcher(App.this, BROKER_URL).watch("depot.*");
            new VRPWorker(App.this, BROKER_URL).start();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option dbOpt = new Option("db", "jdbc-url", true, "JDBC URL of database");
        dbOpt.setRequired(true);
        options.addOption(dbOpt);

        Option outOpt = new Option("O", "output-dir", true, "Output directory");
        outOpt.setRequired(true);
        options.addOption(outOpt);

        Option brokerOpt = new Option("B", "broker-url", true, "Redis broker URL");
        brokerOpt.setRequired(false);
        options.addOption(brokerOpt);

        Option portOpt = new Option("P", "port", true, "Port of server");
        portOpt.setRequired(false);
        options.addOption(portOpt);

        Option itOpt = new Option("I", "iteration", true, "Number of maxIteration in searching solution");
        itOpt.setRequired(false);
        options.addOption(itOpt);

        Option tsOpt = new Option("t", "use-timestamp", false, "Append timestamp to output directory");
        tsOpt.setRequired(false);
        options.addOption(tsOpt);

        Option helpOpt = new Option("h", "help", false, "Print help");
        helpOpt.setRequired(false);
        options.addOption(helpOpt);

        String header = "Run location recommendation server\n\n";
        String footer = "\nPlease report issues at soedomoto@gmail.com";

        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse( options, args);

            if(cmd.hasOption("h")) {
                formatter.printHelp("vrp-publisher", header, options, footer, true);
                return;
            }

            new App(cmd);
        } catch (ParseException e) {
            LOG.error(e.getMessage());

            formatter.printHelp("vrp-publisher", header, options, footer, true);
        }
    }

    public int getMaxIteration() {
        return maxIteration;
    }

    public void setMaxIteration(int maxIteration) {
        this.maxIteration = maxIteration;
    }

    public Dao<Enumerator, Long> getEnumeratorDao() {
        return enumeratorDao;
    }

    public void setEnumeratorDao(Dao<Enumerator, Long> enumeratorDao) {
        this.enumeratorDao = enumeratorDao;
    }

    public Dao<CensusBlock, Long> getCensusBlockDao() {
        return censusBlockDao;
    }

    public void setCensusBlockDao(Dao<CensusBlock, Long> censusBlockDao) {
        this.censusBlockDao = censusBlockDao;
    }

    public Dao<DistanceMatrix, Long> getDistanceMatrixDao() {
        return distanceMatrixDao;
    }

    public void setDistanceMatrixDao(Dao<DistanceMatrix, Long> distanceMatrixDao) {
        this.distanceMatrixDao = distanceMatrixDao;
    }

    public Dao<Subscriber, Long> getSubscriberDao() {
        return subscriberDao;
    }

    public void setSubscriberDao(Dao<Subscriber, Long> subscriberDao) {
        this.subscriberDao = subscriberDao;
    }
}
