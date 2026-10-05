package org.example.backend.reservation;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationModuleBoundaryTest {
    private static final Set<String> ALLOWED_INTERNAL_ARTIFACTS = Set.of(
            "platform-kernel",
            "platform-security",
            "platform-web-support");

    @Test
    void modulePomDeclaresOnlyPlatformInternalDependencies() throws Exception {
        assertAllowedInternalArtifacts(internalArtifacts(parsePom(Path.of("pom.xml"))));
    }

    @Test
    void rejectsBusinessDependenciesResolvedThroughProjectGroupId() throws Exception {
        Set<String> internalArtifacts = internalArtifacts(parsePom("""
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion><groupId>org.example</groupId>
                  <properties><internal.group>org.example</internal.group></properties>
                  <dependencies>
                    <dependency><groupId>${project.groupId}</groupId><artifactId>lab-resource</artifactId></dependency>
                    <dependency><groupId>${internal.group}</groupId><artifactId>teaching-schedule</artifactId></dependency>
                  </dependencies>
                </project>
                """));

        assertTrue(internalArtifacts.contains("lab-resource"));
        assertTrue(internalArtifacts.contains("teaching-schedule"));
        assertThrows(AssertionError.class, () -> assertAllowedInternalArtifacts(internalArtifacts));
    }

    @Test
    void resolvesPomAndParentGroupAliasesForDirectDependencies() throws Exception {
        Set<String> internalArtifacts = internalArtifacts(parsePom("""
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion><parent><groupId>org.example</groupId><artifactId>parent</artifactId><version>1</version></parent>
                  <dependencies>
                    <dependency><groupId>${pom.groupId}</groupId><artifactId>alias-pom</artifactId></dependency>
                    <dependency><groupId>${project.parent.groupId}</groupId><artifactId>alias-project-parent</artifactId></dependency>
                    <dependency><groupId>${pom.parent.groupId}</groupId><artifactId>alias-pom-parent</artifactId></dependency>
                    <dependency><groupId>${parent.groupId}</groupId><artifactId>alias-parent</artifactId></dependency>
                  </dependencies>
                </project>
                """));

        assertEquals(Set.of("alias-pom", "alias-project-parent", "alias-pom-parent", "alias-parent"), internalArtifacts);
    }

    @Test
    void failsClosedWhenADirectDependencyGroupIdCannotBeResolved() throws Exception {
        Document pom = parsePom("""
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion>
                  <dependencies><dependency><groupId>${missing.group}</groupId><artifactId>outside-module</artifactId></dependency></dependencies>
                </project>
                """);

        assertThrows(IllegalArgumentException.class, () -> internalArtifacts(pom));
    }

    @Test
    void ignoresDependencyManagementAndPluginDependenciesWhenCheckingDirectDependencies() throws Exception {
        Set<String> internalArtifacts = internalArtifacts(parsePom("""
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                  <modelVersion>4.0.0</modelVersion><groupId>org.example</groupId>
                  <dependencies>
                    <dependency><groupId>org.example</groupId><artifactId>platform-kernel</artifactId></dependency>
                    <dependency><groupId>org.example</groupId><artifactId>platform-security</artifactId></dependency>
                    <dependency><groupId>org.example</groupId><artifactId>platform-web-support</artifactId></dependency>
                  </dependencies>
                  <dependencyManagement><dependencies><dependency><groupId>org.example</groupId><artifactId>lab-resource</artifactId></dependency></dependencies></dependencyManagement>
                  <build><plugins><plugin><dependencies><dependency><groupId>org.example</groupId><artifactId>legacy-app</artifactId></dependency></dependencies></plugin></plugins></build>
                </project>
                """));

        assertEquals(ALLOWED_INTERNAL_ARTIFACTS, internalArtifacts);
    }

    @Test
    void publicModuleCodeDoesNotImportLegacyApplicationTypes() throws IOException {
        Path sourceRoot = Path.of("src/main/java/org/example/backend/reservation");
        try (Stream<Path> sources = Files.walk(sourceRoot)) {
            assertFalse(sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .anyMatch(source -> source.contains("org.example.backend.entity")
                            || source.contains("org.example.backend.mapper")
                            || source.contains("org.example.backend.service")
                            || source.contains("org.example.backend.controller")
                            || source.contains("org.example.backend.security.")));
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Set<String> internalArtifacts(Document pom) {
        Element project = pom.getDocumentElement();
        MavenGroupProperties properties = mavenGroupProperties(project);
        Element dependencies = directChild(project, "dependencies");
        Set<String> internalArtifacts = new LinkedHashSet<>();
        if (dependencies == null) {
            return internalArtifacts;
        }
        for (Node node = dependencies.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (!(node instanceof Element dependency) || !"dependency".equals(dependency.getLocalName())) {
                continue;
            }
            String groupId = resolveGroupId(requiredDirectText(dependency, "groupId"), properties, new HashSet<>());
            if ("org.example".equals(groupId)) {
                internalArtifacts.add(requiredDirectText(dependency, "artifactId"));
            }
        }
        return internalArtifacts;
    }

    private void assertAllowedInternalArtifacts(Set<String> internalArtifacts) {
        assertEquals(ALLOWED_INTERNAL_ARTIFACTS, internalArtifacts,
                "reservation may depend directly only on platform artifacts, never legacy-app or another business module");
    }

    private Document parsePom(String xml) throws Exception {
        return pomFactory().newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private Document parsePom(Path path) throws Exception {
        return pomFactory().newDocumentBuilder().parse(path.toFile());
    }

    private DocumentBuilderFactory pomFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);
        return factory;
    }

    private MavenGroupProperties mavenGroupProperties(Element project) {
        Element parent = directChild(project, "parent");
        Map<String, String> values = new LinkedHashMap<>();
        Element properties = directChild(project, "properties");
        if (properties != null) {
            for (Node node = properties.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element property) {
                    values.put(property.getLocalName(), property.getTextContent().trim());
                }
            }
        }
        return new MavenGroupProperties(
                directText(project, "groupId"),
                parent == null ? null : directText(parent, "groupId"),
                Map.copyOf(values));
    }

    private String resolveGroupId(String value, MavenGroupProperties properties, Set<String> resolving) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isBlank()) {
            throw new IllegalArgumentException("Direct dependency groupId is missing");
        }
        if (!candidate.contains("${")) {
            return candidate;
        }
        if (!candidate.startsWith("${") || !candidate.endsWith("}") || candidate.indexOf("${", 2) >= 0) {
            throw new IllegalArgumentException("Cannot resolve direct dependency groupId: " + candidate);
        }
        String key = candidate.substring(2, candidate.length() - 1);
        if (!resolving.add(key)) {
            throw new IllegalArgumentException("Cyclic direct dependency groupId property: " + key);
        }
        String replacement = switch (key) {
            case "project.groupId", "pom.groupId" -> properties.projectGroupId() == null
                    ? properties.parentGroupId() : properties.projectGroupId();
            case "project.parent.groupId", "pom.parent.groupId", "parent.groupId" -> properties.parentGroupId();
            default -> properties.values().get(key);
        };
        if (replacement == null || replacement.isBlank()) {
            throw new IllegalArgumentException("Cannot resolve direct dependency groupId property: " + key);
        }
        return resolveGroupId(replacement, properties, resolving);
    }

    private Element directChild(Element parent, String localName) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element child && localName.equals(child.getLocalName())) {
                return child;
            }
        }
        return null;
    }

    private String directText(Element parent, String localName) {
        Element child = directChild(parent, localName);
        return child == null ? null : child.getTextContent().trim();
    }

    private String requiredDirectText(Element parent, String localName) {
        String value = directText(parent, localName);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Direct dependency " + localName + " is missing");
        }
        return value;
    }

    private record MavenGroupProperties(String projectGroupId, String parentGroupId, Map<String, String> values) {
    }
}
