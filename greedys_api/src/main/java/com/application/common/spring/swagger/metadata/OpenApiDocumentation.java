package com.application.common.spring.swagger.metadata;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenApiDocumentation {

    @Value("${app.api.version:1.0.0}")
    private String apiVersion;

    @Value("${app.api.title:Greedy's API}")
    private String apiTitle;

    @Value("${app.api.description:API for Greedy's Restaurant Management System}")
    private String apiDescription;

    @Value("${server.port:8080}")
    private String serverPort;

    // Fields for builder pattern
    private String summary;
    private String description;
    private String operationId;
    private boolean deprecated;
    private List<String> tags;

    public OpenAPI createOpenAPI() {
        return new OpenAPI()
                .info(createInfo())
                .servers(createServers());
    }

    private Info createInfo() {
        return new Info()
                .title(apiTitle)
                .description(apiDescription)
                .version(apiVersion)
                .contact(createContact())
                .license(createLicense());
    }

    private Contact createContact() {
        return new Contact()
                .name("Development Team")
                .email("dev@greedys.com")
                .url("https://greedys.com");
    }

    private License createLicense() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> createServers() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");

        Server devServer = new Server()
                .url("https://dev-api.greedys.com")
                .description("Development server");

        Server prodServer = new Server()
                .url("https://api.greedys.com")
                .description("Production server");

        return List.of(localServer, devServer, prodServer);
    }

    public static OpenApiDocumentation createDefault(String methodName) {
        OpenApiDocumentation doc = new OpenApiDocumentation();
        doc.apiTitle = "Generated API";
        doc.apiDescription = "Auto-generated documentation for method: " + methodName;
        doc.apiVersion = "1.0.0";
        doc.serverPort = "8080";
        return doc;
    }
}
