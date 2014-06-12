package controllers;

/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 23/05/14
 * Time: 4:30 PM
 */

import models.ShibbolethConfiguration;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.*;

import java.util.List;

public class ShibbolethEnvironments extends Controller {
    public static Result list() {
        List<ShibbolethConfiguration> environmentsList = ShibbolethConfiguration.findAll();
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
}
