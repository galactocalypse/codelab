package com.codelab.movies.autoconfigure;

import com.codelab.common.spring.persistence.CodelabModule;
import com.codelab.common.spring.persistence.CodelabModuleProvider;

public class MoviesModuleProvider implements CodelabModuleProvider {
    @Override
    public CodelabModule provide() {
        return new CodelabModule(
                "movies",
                "moviesPU",
                "com.codelab.movies.entity",
                "com.codelab.movies.repository",
                "codelab"
        );
    }
}
