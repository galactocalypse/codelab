package com.codelab.core;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;
import java.util.List;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;

public class ModuleUtils {

  public static List<CodelabModule> loadModules(ResourceLoader resourceLoader) {
    return SpringFactoriesLoader.loadFactories(
            CodelabModuleProvider.class, resourceLoader.getClassLoader())
        .stream()
        .map(CodelabModuleProvider::provide)
        .toList();
  }
}
