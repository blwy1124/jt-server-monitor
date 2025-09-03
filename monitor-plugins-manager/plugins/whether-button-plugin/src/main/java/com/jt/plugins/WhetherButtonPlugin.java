package com.jt.plugins;

import com.jt.plugins.api.OperationButtonExtension;
import com.jt.plugins.core.SpringPlugin;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

import java.util.Random;

/**
 * @author blwy_qb
 */
public class WhetherButtonPlugin extends SpringPlugin {

    private static final String[] WHETHER = new String[]{"Sunny", "Cloudy", "Raining", "Windy", "Snow"};

    public WhetherButtonPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class WhetherButtonExtension implements OperationButtonExtension {
        @Override
        public String name() {
            return "Whether";
        }

        @Override
        public String onClick() {
            return WHETHER[new Random().nextInt(WHETHER.length)];
        }
    }
}
