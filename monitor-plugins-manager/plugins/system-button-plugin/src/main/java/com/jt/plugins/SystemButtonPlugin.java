package com.jt.plugins;

import com.jt.plugins.api.OperationButtonExtension;
import com.jt.plugins.core.SpringPlugin;
import org.pf4j.Extension;
import org.pf4j.PluginWrapper;

/**
 * @author blwy_qb
 */
public class SystemButtonPlugin extends SpringPlugin {

    public SystemButtonPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class SystemButtonExtension implements OperationButtonExtension {

        @Override
        public String name() {
            return "System";
        }

        @Override
        public String onClick() {
            return System.getProperty("os.name") + "-" + System.getProperty("os.arch");
        }
    }
}
