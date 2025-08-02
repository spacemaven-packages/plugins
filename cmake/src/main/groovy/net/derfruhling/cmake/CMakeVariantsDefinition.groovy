package net.derfruhling.cmake

import org.gradle.api.Action
import org.gradle.api.provider.MapProperty

abstract class CMakeVariantsDefinition {
    abstract MapProperty<String, Map<String, Object>> getVariants()

    void variant(
            Map<String, ?> params,
            String name,
            Action<CMakeConfiguration> action
    ) {
        params.put("configure", action)
        variants.put(name, params)
    }

    void variant(
            Map<String, ?> params,
            String name
    ) {
        variants.put(name, params)
    }

    /**
     * Equivalent to:<br>
     * <code lang="groovy">variant &lt;name&gt;, isDebuggable: false, isOptimized: false</code>
     * @deprecated You really should be providing parameters for this variant
     */
    @Deprecated
    void variant(String name) {
        variants.put(name, Map.of())
    }

    void defaultVariants() {
        variant 'release', isDebuggable: false, isOptimized: true
        variant 'debug', isDebuggable: true, isOptimized: false
    }
}
