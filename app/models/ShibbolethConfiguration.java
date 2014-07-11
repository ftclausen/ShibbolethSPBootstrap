package models;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import play.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.ChefContext;
import org.jclouds.chef.domain.DatabagItem;
import com.google.common.io.Files;
import static com.google.common.base.Charsets.UTF_8;
import play.data.validation.Constraints;
import play.api.libs.json.Json;
import com.fasterxml.jackson.databind.JsValue;


/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 26/05/14
 * Time: 3:45 PM
 *
 * TODO:
 *  * Make data bag configurable
 *  * Make connection params configurable
 */
public class ShibbolethConfiguration {

    @Constraints.Required
    public String environment;
    @Constraints.Required
    public String entityId;
    @Constraints.Required
    public String remoteUserAttribute;
    @Constraints.Required
    public String clientName;
    private static ChefApi api;

    private ChefApi getChefServer() throws IOException {
        String server = "cheflocal";
        String pemFile = "/Users/fclausen/.chef/fclausen.pem";
        String credential = Files.toString(new File(pemFile), UTF_8);
        ChefContext context = ContextBuilder.newBuilder("chef")
                .endpoint("https://" + server)
                .credentials("fclausen", credential)
                .buildView(ChefContext.class);
        ChefApi api = context.unwrapApi(ChefApi.class);
        return(api);
    }

    public ShibbolethConfiguration() {

    }

    public ShibbolethConfiguration(String environment, String entityId, String remoteUserAttribute, String clientName) throws IOException {
        this.environment = environment;
        this.entityId = entityId;
        this.remoteUserAttribute = remoteUserAttribute;
        this.clientName = clientName;
        this.api = getChefServer();
        Logger.info("Got Chef API  : " + api);
    }

    public String toString() {
        return String.format("Shibboleth data for %s", clientName);
    }

    // Mock some data
    private static List<ShibbolethConfiguration> configs;

    static {
        try {
            configs = new ArrayList<ShibbolethConfiguration>();
            configs.add(new ShibbolethConfiguration("fgtd-1234-1234",
                    "https://sp.example.com/shibboleth-sp", "cn", "Client One"));
            configs.add(new ShibbolethConfiguration("fgprd-1111-1111",
                    "https://sp2.example.com/shibboleth-sp", "cn", "Client Two"));
            configs.add(new ShibbolethConfiguration("fgprd-2222-2222",
                    "https://sp3.example.com/shibboleth-sp", "cn", "Client Three"));
        } catch (IOException e) {
            Logger.error("Problem connecting to Chef server : " + e.getLocalizedMessage());
        }
    }

    public static List<ShibbolethConfiguration> findAll() throws Exception {
        Set<String> shibbolethDatabags = api.listDatabagItems("shibboleth-sp");
        for (String databagItem : shibbolethDatabags) {
            DatabagItem currentDatabagItem = api.getDatabagItem("shibboleth-sp", databagItem);
            // JsonNode currentDatabagJson = currentDatabagItem
            JsValue databagItemJson = Json.parse(currentDatabagItem.toString());
            // Logger.debug(currentDatabagItem.toString());
            Logger.debug("Data bag : " + databagItem);
        }
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
