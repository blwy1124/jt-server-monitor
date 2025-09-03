package com.jt.plugins.core;

import org.pf4j.PluginManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个 {@link SpringExtensionFactory}，它始终返回一个特定的实例。可选：您可以指定要为其使用单例的扩展类。
 *
 * @author blwy_qb
 */
public class SingletonSpringExtensionFactory extends SpringExtensionFactory {

    private final List<String> extensionClassNames;

    private Map<String, Object> cache;

    public SingletonSpringExtensionFactory(PluginManager pluginManager) {
        this(pluginManager, true);
    }

    public SingletonSpringExtensionFactory(PluginManager pluginManager, String... extensionClassNames) {
        this(pluginManager, true, extensionClassNames);
    }

    public SingletonSpringExtensionFactory(PluginManager pluginManager, boolean autowire, String... extensionClassNames) {
        super(pluginManager, autowire);

        this.extensionClassNames = Arrays.asList(extensionClassNames);

        // simple cache implementation
        cache = new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> extensionClass) {
        String extensionClassName = extensionClass.getName();
        if (cache.containsKey(extensionClassName)) {
            return (T) cache.get(extensionClassName);
        }

        T extension = super.create(extensionClass);
        if (extensionClassNames.isEmpty() || extensionClassNames.contains(extensionClassName)) {
            cache.put(extensionClassName, extension);
        }

        return extension;
    }

}
