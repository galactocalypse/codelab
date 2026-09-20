package com.codelab.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MegalithConfiguration {

    @Bean
    public static CodelabModuleRegistrar codelabModuleRegistrar() {
        return new CodelabModuleRegistrar();
    }

}
