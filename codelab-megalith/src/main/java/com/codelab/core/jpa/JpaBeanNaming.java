package com.codelab.core.jpa;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.core.CodelabBeanNaming;

public class JpaBeanNaming {

    static String dataSourceBeanName(CodelabModule module) {
        return CodelabBeanNaming.deriveBeanName(module, "DataSource");
    }

    static String emfBeanName(CodelabModule module) {
        return CodelabBeanNaming.deriveBeanName(module, "EntityManagerFactory");
    }

    static String txBeanName(CodelabModule module) {
        return CodelabBeanNaming.deriveBeanName(module, "TransactionManager");
    }

}
