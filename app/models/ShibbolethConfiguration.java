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
import play.api.libs.json.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;



/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 26/05/14
 * Time: 3:45 PM
 *
 * TODO:
 *  * Make data bag configurable
 *  * Make connection params configurable
 *  * Throw more specific exceptions
 *
 *
 *  [debug] application - Got key : federation_metadata
 [debug] application - Got key : remote_user [DONE as
 [debug] application - Got key : displayname  DONE
 [debug] application - Got key : description DONE
 [debug] application - Got key : idp
 [debug] application - Got key : sp-cert
 [debug] application - Got key : attribute_map
 [debug] application - Got key : logo
 [debug] application - Got key : entityid  DONE
 [debug] application - Got key : id [DONE as environment]
 [debug] application - Got key : sp-key
 */
public class ShibbolethConfiguration {

    @Constraints.Required
    public String id;
    @Constraints.Required
    public String entityId;
    // @Constraints.Required
    // public String remote_user;
    @Constraints.Required
    // public String displayName;
    public String description;
    @Constraints.Required
    public String idp;
    //public String sp-cert;


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
        this.id = environment;
        this.entityId = entityId;
        // this.remoteUserAttribute = remoteUserAttribute;
        this.description = clientName;
        this.api = getChefServer();
        Logger.info("Got Chef API  : " + api);
    }

    public String toString() {
        return String.format("Shibboleth data for %s", description);
    }

    public static List<ShibbolethConfiguration> findAll() throws Exception {
        Set<String> shibbolethDatabags = api.listDatabagItems("shibboleth-sp");
        ObjectMapper mapper = new ObjectMapper();
        List<ShibbolethConfiguration> configsFromChef = new ArrayList<ShibbolethConfiguration>();
        for (String databagItem : shibbolethDatabags) {
            DatabagItem currentDatabagItem = api.getDatabagItem("shibboleth-sp", databagItem);
            JsonNode databagItemJson = mapper.readTree(currentDatabagItem.toString());
            Logger.debug("Data bag : " + databagItem +
                    "with entity ID - " + databagItemJson.findPath("entityid").toString());
            configsFromChef.add(new ShibbolethConfiguration(
                    databagItemJson.findPath("id").asText(),
                    databagItemJson.findPath("entityid").asText(),
                    databagItemJson.findPath("remote_user").asText(),
                    databagItemJson.findPath("displayname").asText()

            ));
        }
        Logger.debug("We have configs from Chef as " + configsFromChef);
        return new ArrayList<ShibbolethConfiguration>(configsFromChef);
    }

    public static ShibbolethConfiguration findByEnv(String environment) throws Exception {
        List<ShibbolethConfiguration> configs = findAll();
        for (ShibbolethConfiguration config : configs) {
            if (config.id.equals(environment)) {
                return config;
            }
        }
        return null;
    }

    public static List<ShibbolethConfiguration> findByClientName(String clientName) throws Exception {
        final List<ShibbolethConfiguration> configs = findAll();
        List<ShibbolethConfiguration> results = new ArrayList<ShibbolethConfiguration>();
        for (ShibbolethConfiguration config : configs) {
            if (config.description.toLowerCase().contains(clientName.toLowerCase())) {
                results.add(config);
            }
        }
        return results;
    }

    public void save() {
        Logger.debug("Going to save env");
        // configs.remove(findByEnv(this.environment));
        // configs.add(this);
    }
}
