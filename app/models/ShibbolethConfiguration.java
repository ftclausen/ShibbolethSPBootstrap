package models;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import play.Logger;
import org.jclouds.ContextBuilder;
import org.jclouds.chef.ChefApi;
import org.jclouds.chef.ChefContext;
import org.jclouds.chef.domain.DatabagItem;
import com.google.common.io.Files;
import static com.google.common.base.Charsets.UTF_8;

import play.Play;
import play.data.validation.Constraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;



/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 26/05/14
 * Time: 3:45 PM
 *
 * Things to be done:
 *  * TODO: Make data bag configurable
 *  * TODO: Make connection params configurable
 *  * TODO: Throw more specific exceptions
 *  * TODO: Figure out how to map JSON names to better member variable names
 *
 */
public class ShibbolethConfiguration {

    @Constraints.Required
    public String id;
    @Constraints.Required
    public String entityid;
    // @Constraints.Required
    @JsonProperty("remote_user")
    public String remoteUser;
    @Constraints.Required
    // public String displayName;
    public String displayname;
    @Constraints.Required
    public String idp;
    @JsonProperty("sp-cert")
    public String spCert;
    public String description;
    @JsonProperty("attribute_map")
    public String attributeMap;
    public String logo;
    @JsonProperty("sp-key")
    public String spKey;
    @JsonProperty("federation_metadata")
    public String federationMetadata;

    private static ChefApi api;
    private static ChefContext chefContext;
    private static String dataBag;

    private static ChefContext buildChefContext(String server, String user, String pemFile, String credential) {
        chefContext = ContextBuilder.newBuilder("chef")
                .endpoint("https://" + server)
                .credentials(user, credential)
                .buildView(ChefContext.class);
        return chefContext;
    }

    private static ChefApi getChefServer() throws TimeoutException, IOException {
        String server = Play.application().configuration().getString("application.chef.endpoint");
        Logger.info("Using Chef server : " + server);
        String pemFile = Play.application().configuration().getString("application.chef.pemfile");
        Logger.info("Using key at : " + pemFile);
        String chefUser = Play.application().configuration().getString("application.chef.user");
        Logger.info("Using user : " + chefUser);
        dataBag = Play.application().configuration().getString("application.chef.databag");
        Logger.info("Using data bag : " + dataBag);
        String credential = Files.toString(new File(pemFile), UTF_8);
        if (api == null) {
            Logger.info("Establishing new connection to Chef server...");
            chefContext = buildChefContext(server, chefUser, pemFile, credential);
            api = chefContext.unwrapApi(ChefApi.class);
        } else {
            // Is our connection alive? If so then just re-use otherwise
            // reconnect if possible.
            Set<String> testDatabags = api.listDatabags();
            if (testDatabags == null) {
                Logger.info("Reconnecting to Chef server...");
                chefContext.close(); // Not sure if this will always work
                chefContext = buildChefContext(server, chefUser, pemFile, credential);
                api = chefContext.unwrapApi(ChefApi.class);
            }
        }
        return(api);
    }

    public ShibbolethConfiguration() {

    }

    public ShibbolethConfiguration(String environment, String entityid, String displayname)
            throws TimeoutException, IOException {
        this.id = environment;
        this.entityid = entityid;
        this.displayname = displayname;
        this.api = getChefServer();
        Logger.info("Got Chef API  : " + api);
    }

    public String toString() {
        return String.format("Shibboleth data for %s", displayname);
    }

    public static List<ShibbolethConfiguration> findAll() throws TimeoutException, IOException {
        api = getChefServer();
        Set<String> shibbolethDatabags = api.listDatabagItems(dataBag);
        ObjectMapper mapper = new ObjectMapper();
        List<ShibbolethConfiguration> configsFromChef = new ArrayList<ShibbolethConfiguration>();
        for (String databagItem : shibbolethDatabags) {
            DatabagItem currentDatabagItem = api.getDatabagItem(dataBag, databagItem);
            JsonNode databagItemJson = mapper.readTree(currentDatabagItem.toString());
            Logger.debug("Data bag : " + databagItem +
                    "with entity ID - " + databagItemJson.findPath("entityid").toString());
            configsFromChef.add(new ShibbolethConfiguration(
                    databagItemJson.findPath("id").asText(),
                    databagItemJson.findPath("entityid").asText(),
                    databagItemJson.findPath("displayname").asText()

            ));
        }
        Logger.debug("We have configs from Chef as " + configsFromChef);
        return new ArrayList<ShibbolethConfiguration>(configsFromChef);
    }

    public static ShibbolethConfiguration findByEnv(String environment)
            throws TimeoutException, IOException {
        // TODO: Instead of doing findAll just find the appropriate env directly
        List<ShibbolethConfiguration> configs = findAll();
        for (ShibbolethConfiguration config : configs) {
            if (config.id.equals(environment)) {
                return config;
            }
        }
        return null;
    }

    public static List<ShibbolethConfiguration> findByClientName(String clientName)
            throws TimeoutException, IOException {
        final List<ShibbolethConfiguration> configs = findAll();
        List<ShibbolethConfiguration> results = new ArrayList<ShibbolethConfiguration>();
        for (ShibbolethConfiguration config : configs) {
            if (config.displayname.toLowerCase().contains(clientName.toLowerCase())) {
                results.add(config);
            }
        }
        return results;
    }

    public void save()
            throws IOException, JsonMappingException, JsonGenerationException, TimeoutException {

        Logger.debug("Going to save env as data bag");
        api = getChefServer();
        Set<String> existingDatabags = api.listDatabags();
        if (!existingDatabags.contains(dataBag)) {
            Logger.info("Shibboleth data bag not found - creating...");
            api.createDatabag(dataBag);
        } else {
            Logger.debug("Shibboleth data bag found OK");
        }
        ObjectMapper mapper = new ObjectMapper();

        // Somewhat hackish way to construct the final data bag item because
        // we need to embed our shib config object into the greater data
        // bag JSON structure.
        String finalDatabag = String.format("{\"id\": \"%s\", \"shib_data\": %s }", this.id, mapper.writeValueAsString(this));
        Logger.debug("Going to save data bag as : " + finalDatabag);

        DatabagItem databagItem = api.createDatabagItem(dataBag, new DatabagItem(this.id, finalDatabag));
        if (databagItem.getId() == null) {
            throw new IOException("Error creating data bag");
        }
        Logger.info("Create data bag item for id : " + this.id);
    }

    public static void healthCheck()
            throws TimeoutException, IOException {
        api = getChefServer();
        Set<String> databags = api.listDatabags();
        Logger.debug("Databags found : " + databags.size());
        if (databags != null) {
            Logger.debug("Health Check OK");
        } else {
            Logger.error("Health Check Failed - see thrown exception for details");
        }
    }
}
