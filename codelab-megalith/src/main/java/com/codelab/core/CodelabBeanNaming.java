package com.codelab.core;

import com.codelab.common.spring.persistence.CodelabModule;

public final class CodelabBeanNaming {

    private CodelabBeanNaming() {}

    /**
     * Standard nomenclature: <module>.<InterfaceSimpleName>
     * e.g. "inventory.OrderCreatedPublisher"
     *
     * Namespaced by module to avoid collisions if two modules independently
     * define an interface with the same simple name (plausible — "CreatedPublisher"
     * style naming tends to converge across teams).
     */
    public static String deriveBeanName(CodelabModule module, Class<?> publisherIface) {
        return deriveBeanName(module, publisherIface.getSimpleName());
    }

    public static String deriveBeanName(CodelabModule module, String className) {
        return module.name() + "." + className;
    }

}
