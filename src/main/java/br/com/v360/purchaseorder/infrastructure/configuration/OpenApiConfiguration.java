package br.com.v360.purchaseorder.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI purchaseOrderConnectorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("V360 Purchase Order Connector API")
                        .description("API para importação, consulta e conferência de pedidos de compra normalizados.")
                        .version("1.0.0"));
    }
}

