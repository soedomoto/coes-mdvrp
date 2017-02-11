package com.soedomoto.vrp.pubsub;

import com.google.gson.Gson;
import com.soedomoto.vrp.App;
import com.soedomoto.vrp.model.dao.CensusBlock;
import com.soedomoto.vrp.model.dao.Enumerator;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by soedomoto on 26/01/17.
 */
public class ChannelSubscriber {
    private final String brokerUrl;
    private final App app;

    public ChannelSubscriber(App app, String brokerUrl) throws URISyntaxException {
        this.app = app;
        this.brokerUrl = brokerUrl;
    }

    public void visit() {
        Executors.newFixedThreadPool(1).execute(new Runnable() {
            public void run() {
                try {
                    Jedis visitSubscriber = new Jedis(new URI(brokerUrl));
                    final Jedis visitPublisher = new Jedis(new URI(brokerUrl));
                    visitSubscriber.psubscribe(new JedisPubSub() {
                        @Override
                        public void onPMessage(String pattern, String channel, String message) {
                            try {
                                String enumeratorId = channel.replace("enumerator.", "").replace(".visit", "");
                                String[] msgs = message.split(",");
                                String bsDestId = msgs[0].trim();
                                String timestamp = msgs[1].trim();

                                Date date = new Date(Double.valueOf(timestamp).longValue());

                                CensusBlock bs = app.getCensusBlockDao().queryForId(Long.valueOf(bsDestId));
                                Enumerator enumerator = app.getEnumeratorDao().queryForId(Long.valueOf(enumeratorId));

                                if(bs.getVisitedBy() != null) {
                                    List<Long> responses = new ArrayList();
                                    responses.add(bs.getVisitedBy());
                                    responses.add(enumerator.getDepot());

                                    visitPublisher.publish(String.format(
                                            "enumerator.%s.visit.callback", enumeratorId), new Gson().toJson(responses));
                                    return;
                                }

                                bs.setVisitedBy(Long.valueOf(enumeratorId));
                                bs.setVisitDate(date);
                                int updated = app.getCensusBlockDao().update(bs);
                                if(updated > 0) {
                                    enumerator.setDepot(Long.valueOf(bsDestId));
                                    updated = app.getEnumeratorDao().update(enumerator);
                                    if(updated > 0) {
                                        List<Long> responses = new ArrayList();
                                        responses.add(bs.getVisitedBy());
                                        responses.add(enumerator.getDepot());

                                        visitPublisher.publish(String.format(
                                                "enumerator.%s.visit.callback", enumeratorId), new Gson().toJson(responses));
                                        return;
                                    }
                                }

                                visitPublisher.publish(String.format(
                                        "enumerator.%s.visit.callback", enumeratorId), String.valueOf(0));
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }, "enumerator.*.visit");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void clientLogger(final String clientLogDir) {
        Executors.newFixedThreadPool(1).execute(new Runnable() {
            public void run() {
                try {
                    Jedis jedis = new Jedis(new URI(brokerUrl));
                    jedis.psubscribe(new JedisPubSub() {
                        @Override
                        public void onPMessage(String pattern, String channel, String message) {
                            String enumeratorId = channel.replace("log.", "");

                            try {
                                File clientLogFile = new File(clientLogDir + File.separator + enumeratorId + ".log");
                                clientLogFile.getParentFile().mkdirs();
                                PrintWriter out = new PrintWriter(new FileWriter(clientLogFile, true));
                                out.println(message);
                                out.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, "log.*");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
