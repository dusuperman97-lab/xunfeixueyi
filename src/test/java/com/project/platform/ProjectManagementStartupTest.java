package com.project.platform;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectManagementStartupTest {
    @Test
    void fallsBackToSourceResourcesWhenIdeDoesNotCopyYamlToClasspath() {
        assertEquals(
                "optional:file:src/main/resources/",
                ProjectManagement.defaultStartupProperties().get("spring.config.additional-location")
        );
    }

    @Test
    void secretConfigurationAlsoHasIdeSourceFallback() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yaml"));
        org.junit.jupiter.api.Assertions.assertTrue(
                yaml.contains("optional:file:src/main/resources/application-secret.yml")
        );
    }
}
