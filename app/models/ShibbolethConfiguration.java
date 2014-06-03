package models;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 26/05/14
 * Time: 3:45 PM
 */
public class ShibbolethConfiguration {

    public String environment;
    public String entityId;
    public String remoteUserAttribute;
    public String clientName;

    public ShibbolethConfiguration() {}

    public ShibbolethConfiguration(String environment, String entityId, String remoteUserAttribute, String clientName) {
        this.environment = environment;
        this.entityId = entityId;
        this.remoteUserAttribute = remoteUserAttribute;
        this.clientName = clientName;
    }

    public String toString() {
        return String.format("Shibboleth data for %s", clientName);
    }

    // Mock some data
    private static List<ShibbolethConfiguration> configs;

    static {
        configs = new ArrayList<ShibbolethConfiguration>();
        configs.add(new ShibbolethConfiguration("fgtd-1234-1234",
                                                "https://sp.example.com/shibboleth-sp", "cn", "Client One"));
        configs.add(new ShibbolethConfiguration("fgprd-1111-1111",
                                                "https://sp2.example.com/shibboleth-sp", "cn", "Client Two"));
        configs.add(new ShibbolethConfiguration("fgprd-2222-2222",
                                                "https://sp3.example.com/shibboleth-sp", "cn", "Client Three"));
    }

    public static List<ShibbolethConfiguration> findAll() {
        return new ArrayList<ShibbolethConfiguration>(configs);
    }

    public static ShibbolethConfiguration findByEnv(String environment) {
        for (ShibbolethConfiguration config : configs) {
            if (config.environment.equals(environment)) {
                return config;
            }
        }
        return null;
    }

    public static List<ShibbolethConfiguration> findByClientName(String clientName) {
        final List<ShibbolethConfiguration> results = new ArrayList<ShibbolethConfiguration>();
        for (ShibbolethConfiguration config : configs) {
            if (config.clientName.toLowerCase().contains(clientName.toLowerCase())) {
                results.add(config);
            }
        }
        return results;
    }

    public static boolean remove (ShibbolethConfiguration config) {
        return configs.remove(config);
    }

    public void save() {
        configs.remove(findByEnv(this.environment));
        configs.add(this);
    }
}
