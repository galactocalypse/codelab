package com.codelab.movies.autoconfigure;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;

import java.util.Map;

public class MoviesModuleProvider implements CodelabModuleProvider {
  @Override
  public CodelabModule provide() {
    return new CodelabModule(
        "movies",
        "com.codelab.movies",
        "moviesPU",
        "movies",
        Map.of());
  }
}
