package eu.yodan.timeout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

@Slf4j
@Configuration
public class SwaggerUIConfig {

    @Configuration
    public class SwaggerUiConfig {
        @Bean
        public Docket api() {
            return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
        }

        @EventListener(ApplicationReadyEvent.class)
        public void notifyLocationOfSwaggerUI() {
            // TODO: find a way to generate this URL from swagger
            log.info("Swagger UI available at: http://localhost:8080/swagger-ui/");
        }
    }
}


