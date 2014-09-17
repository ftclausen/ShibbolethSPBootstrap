package controllers;

/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 23/05/14
 * Time: 4:30 PM
 */

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.ShibbolethConfiguration;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import scala.util.control.*;


import java.io.IOException;
import java.lang.Exception;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class ShibbolethEnvironments extends Controller {

    public static Result list() {
        List<ShibbolethConfiguration> environmentsList;
        try {
            environmentsList = ShibbolethConfiguration.findAll();
        } catch (java.util.concurrent.TimeoutException e) {
            Logger.error("Timeout connecting to Chef server");
            return internalServerError("Timeout connecting to Chef server");
        } catch (IOException e) {
            Logger.error("Error listing envs : " + e.getMessage());
            return internalServerError("Error listing environments : " + e.getMessage());
        } catch (Exception e) {
            Logger.error("Error connecting to Chef server : " + e.getMessage());
            return internalServerError("Error connecting to Chef server : " + e.getMessage());
        }
        return ok(views.html.ShibbolethEnvironments.list.render(environmentsList));
    }

    private static final Form<ShibbolethConfiguration> envForm = Form.form(ShibbolethConfiguration.class);

    public static Result newEnvironment() {
        return ok(views.html.ShibbolethEnvironments.details.render(envForm));
    }

    public static Result viewEnvironment(String env) {
        return TODO;
    }

    public static Result editEnvironment(String env) {
        final ShibbolethConfiguration targetEnv;
        try {
            targetEnv = ShibbolethConfiguration.findByEnv(env);
            if (targetEnv == null) {
                return notFound(String.format("Environment %s does not exist.", targetEnv));
            }
        } catch (Exception e) {
            return internalServerError("Error retrieving environment to edit : " + e.getMessage());
        }

        Form<ShibbolethConfiguration> filledForm = envForm.fill(targetEnv);
        return ok(views.html.ShibbolethEnvironments.details.render(filledForm));
    }

    public static Result save() {
        Form<ShibbolethConfiguration> boundForm = envForm.bindFromRequest();
        if (boundForm.hasErrors()) {
            flash("error", "Please correct the form below.");
            return(badRequest(views.html.ShibbolethEnvironments.details.render(boundForm)));
        }
        ShibbolethConfiguration shibbolethConfiguration = boundForm.get();
        // TODO: Convert to json and have the object save itself
        flash("success", String.format("Saved Shibboleth Environment %s", shibbolethConfiguration));
        return redirect(controllers.routes.ShibbolethEnvironments.list());
    }

    // Check http://www.playframework.com/documentation/2.2.0/JavaJsonRequests
    // for the next part
    @BodyParser.Of(BodyParser.Json.class)
    public static Result saveJson(boolean overwrite) {
        JsonNode json;
        ObjectNode result = null;
        JsonNode shib_data;
        ShibbolethConfiguration shibbolethConfiguration;
        Logger.debug("Attempting to extract data from posted results");
        try {
            json = request().body().asJson();
            result = Json.newObject();
            shib_data = json.findPath("shib_data");
        } catch (NullPointerException e) {
            result.put("status", "ERROR");
            result.put("message", "Not a valid JSON document - is the content type set to \"application/json\"?");
            return badRequest(result);
        }

        if (overwrite) {
            Logger.debug("Will be overwriting existing data bag");
            try {
                shibbolethConfiguration = ShibbolethConfiguration.findByEnv(shib_data.findPath("id").asText());
                shibbolethConfiguration.delete();
            } catch (TimeoutException|IOException e) {
                Logger.error("Could not contact chef server " + e.getMessage());
            }
        } else {
            Logger.debug("Will not overwrite existing data bag");
        }

        Iterator fields =  shib_data.fieldNames();
        while(fields.hasNext()) {
            Logger.debug("Got key : " + fields.next());
        }
        Logger.debug("Attempting to save posted Json for ID " + shib_data.findPath("id").asText());
        if (shib_data.findPath("id") == null) {
            result.put("status", "ERROR");
            result.put("message", "Missing parameter [id]");
            Logger.error("Bad Json request received : " + json.toString());
            return badRequest(result);
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            // Logger.debug("Attempting to map the following JSON : " + shib_data.toString());
            shibbolethConfiguration = mapper.readValue(shib_data.toString(), ShibbolethConfiguration.class);
        } catch (JsonMappingException e) {
            result.put("status", "ERROR");
            result.put("message", "Could not map JSON : " + e.getMessage());
            return badRequest(result);
        } catch (IOException IoException) {
            result.put("status", "ERROR");
            result.put("message", "Could not map JSON : " + IoException.getMessage());
            return badRequest(result);
        }
        result.put("status", "OK");
        result.put("message", "Received ID : " + shib_data.findPath("id").asText());
        try {
            shibbolethConfiguration.save();
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("message", "Cannot save data bag : " + e.getMessage());
        }
        return ok(result);
    }

    public static Result healthCheck() {
        ObjectNode result = Json.newObject();
        // Assume the health check is not OK.
        // For some reason if the Chef server host is up but the server
        // itself is not running we can't catch that as an exception. We just
        // get o.j.h.h.BackoffLimitedRetryHandler - Cannot retry after server error
        result.put("status", "ERROR");
        result.put("message", "Issue connecting to Chef server");
        try {
            ShibbolethConfiguration.healthCheck();
            result.removeAll();
            result.put("status", "OK");
            result.put("message", "Health Check OK");
        } catch (Exception e) {
            if (e instanceof IOException) {
                Logger.error("Can't open chef credentials : " + e.getMessage());
                result.removeAll();
                result.put("status", "ERROR");
                result.put("message", "Can't open chef credentials : " + e.getMessage());
                return badRequest(result);
            } else if (e instanceof TimeoutException) {
                Logger.error("Can't connect to Chef server : " + e.getMessage());
                result.removeAll();
                result.put("status", "ERROR");
                result.put("message", "Can't connect to Chef server : " + e.getMessage());
                return badRequest(result);
            }
        }
        return ok(result);
    }
}
