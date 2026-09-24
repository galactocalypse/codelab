package com.codelab.megalith;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "Codelab API", version = "1.0.0", description = "Very serious nothings"))
public class OpenAPIConfiguration {}
