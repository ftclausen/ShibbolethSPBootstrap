package controllers;

/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 23/05/14
 * Time: 4:30 PM
 */

import com.fasterxml.jackson.databind.node.ObjectNode;
import models.ShibbolethConfiguration;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.BodyParser;
import play.Logger;


import java.util.Iterator;
import java.util.List;

public class ShibbolethEnvironments extends Controller {

    public static Result list() {
        List<ShibbolethConfiguration> environmentsList;
        try {
            environmentsList = ShibbolethConfiguration.findAll();
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
            return internalServerError("Error listing environments : " + e.getMessage());
        }

        return ok(views.html.ShibbolethEnvironments.list.render(environmentsList));
        // return ok("The list goes here");
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
        shibbolethConfiguration.save();
        flash("success", String.format("Saved Shibboleth Environment %s", shibbolethConfiguration));
        return redirect(controllers.routes.ShibbolethEnvironments.list());
    }

    // Check http://www.playframework.com/documentation/2.2.0/JavaJsonRequests
    // for the next part
    @BodyParser.Of(BodyParser.Json.class)
    public static Result saveJson() {
        JsonNode json = request().body().asJson();
        ObjectNode result = Json.newObject();
        // Now we need some massaging to extract the nested
        // "shib_data" embedded json document and map that to
        // our model
        JsonNode shib_data = json.findPath("shib_data");
        //Logger.debug("Got keys " )
        Iterator fields =  shib_data.fieldNames();
        while(fields.hasNext()) {
            Logger.debug("Got key : " + fields.next());
        }
        Logger.debug("Attempting to save posted Json for ID " + shib_data.findPath("id").asText());
        if (shib_data.findPath("id") == null) {
            result.put("status", "KO");
            result.put("message", "Missing parameter [id]");
            Logger.error("Bad Json request received : " + json.toString());
            return badRequest(result);
        } else {
            result.put("status", "OK");
            result.put("message", "Received ID : " + shib_data.findPath("id").asText());
            return ok(result);
        }
    }
}
