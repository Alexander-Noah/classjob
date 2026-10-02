package org.example.backend.config;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyBuildCompatibilityTest {
    @Test
    void rootSpringBootRunDelegatesToLegacyAppFromBackendWorkingDirectory() throws Exception {
        Document parentPom = readPom(Path.of("..", "pom.xml"));
        Document legacyPom = readPom(Path.of("pom.xml"));

        assertAll(
                () -> assertEquals("true", value(parentPom,
                        "/project/build/plugins/plugin[artifactId='spring-boot-maven-plugin']/configuration/skip")),
                () -> assertEquals("false", value(legacyPom,
                        "/project/build/plugins/plugin[artifactId='spring-boot-maven-plugin']/configuration/skip")),
                () -> assertEquals("${maven.multiModuleProjectDirectory}", value(legacyPom,
                        "/project/build/plugins/plugin[artifactId='spring-boot-maven-plugin']/configuration/workingDirectory"))
        );
    }

    @Test
    void reactorPackageKeepsDocumentedRootArtifactPath() throws Exception {
        Document legacyPom = readPom(Path.of("pom.xml"));

        assertAll(
                () -> assertEquals("${maven.multiModuleProjectDirectory}/target", value(legacyPom,
                        "/project/build/directory")),
                () -> assertEquals("Backend-0.0.1-SNAPSHOT", value(legacyPom,
                        "/project/build/finalName"))
        );
    }

    private static Document readPom(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static String value(Document document, String expression) throws Exception {
        return ((String) XPathFactory.newInstance()
                .newXPath()
                .evaluate(expression, document, XPathConstants.STRING))
                .trim();
    }
}
