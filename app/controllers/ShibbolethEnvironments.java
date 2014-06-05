package controllers;

/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 23/05/14
 * Time: 4:30 PM
 */

import models.ShibbolethConfiguration;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;

public class ShibbolethEnvironments extends Controller {
    public static Result list() {
        List<ShibbolethConfiguration> environmentsList = ShibbolethConfiguration.findAll();
        return ok(views.html.ShibbolethEnvironments.list.render(environmentsList));
    }

    public static Result newEnvironment() {
        return TODO;
    }

    public static Result viewEnvironment(String env) {
        return TODO;
    }

    public static Result editEnvironment(String env) {
        return TODO;
    }

    public static Result save() {
        return TODO;
    }
}
