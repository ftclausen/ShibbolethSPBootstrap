package controllers;

/**
 * Author: Friedrich "Fred" Clausen (friedrich.clausen@blackboard.com)
 * Date: 23/05/14
 * Time: 4:30 PM
 */

import play.mvc.Controller;
import play.mvc.Result;

public class ShibbolethEnvironments extends Controller {
    public static Result list() {
        return ok();
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
