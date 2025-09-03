package com.jt.plugins;

import com.jt.plugins.api.OperationButtonExtension;
import com.jt.plugins.core.SpringPlugin;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import java.util.Arrays;
import java.util.List;

/**
 * @author blwy_qb
 */
public class GreetingButtonPlugin extends SpringPlugin {

    public GreetingButtonPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class GreetingButtonExtension implements OperationButtonExtension {
        @Override
        public String name() {
            return "Greeting";
        }

        @Override
        public String onClick() {
            return "Hello Pf4j-spring!";
        }
    }

    @Extension
    public static class NewYearGreetingButtonExtension implements OperationButtonExtension {
        @Override
        public String name() {
            return "Year of Rabbit";
        }

        @Override
        public String onClick() {
            return "Happy Chinese New Year!";
        }
    }
}
