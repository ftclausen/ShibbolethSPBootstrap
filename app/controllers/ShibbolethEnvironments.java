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


import java.util.List;

public class ShibbolethEnvironments extends Controller {

    public static Result list() {
        Logger.info("Listing!");
        try {
            List<ShibbolethConfiguration> environmentsList = ShibbolethConfiguration.findAll();
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
            return internalServerError("Error fulfilling request : " + e.getMessage());
        }

        // return ok(views.html.ShibbolethEnvironments.list.render(environmentsList));
        return ok("The list goes here");
    }

    private static final Form<ShibbolethConfiguration> envForm = Form.form(ShibbolethConfiguration.class);

    public static Result newEnvironment() {
        return ok(views.html.ShibbolethEnvironments.details.render(envForm));
    }

    public static Result viewEnvironment(String env) {
        return TODO;
    }

    public static Result editEnvironment(String env) {
        final ShibbolethConfiguration targetEnv = ShibbolethConfiguration.findByEnv(env);
        if (targetEnv == null) {
            return notFound(String.format("Environment %s does not exist.", targetEnv));
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
        String id = json.findPath("id").textValue();
        if (id == null) {
            result.put("status", "KO");
            result.put("message", "Missing parameter [id]");
            return badRequest();
        } else {
            result.put("status", "OK");
            result.put("message", "Received ID : " + id);
            return ok(result);
        }
    }
}
