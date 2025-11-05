package com.payments.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentsLedgerOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:8080");
        server.setDescription("Development server");

        Contact contact = new Contact();
        contact.setName("Payments Ledger API");

        Info info = new Info()
                .title("Simple Payments Ledger API")
                .version("1.0.0")
                .contact(contact)
                .description("An in-memory transactional ledger API for managing accounts and fund transfers with strict atomicity guarantees. " +
                        "Built for ~100K users with account-level locking and deadlock prevention.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
