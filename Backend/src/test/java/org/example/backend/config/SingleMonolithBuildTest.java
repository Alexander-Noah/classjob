/*
 * SingleMonolithBuildTest回归测试；测试夹具与生产配置分离。
 * 覆盖场景：backendPomBuildsOneSpringBootJarWithoutReactorModules。
 */
package org.example.backend.config;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SingleMonolithBuildTest {
    /**
     * backendPomBuildsOneSpringBootJarWithoutReactorModules的处理逻辑。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    @Test
    void backendPomBuildsOneSpringBootJarWithoutReactorModules() throws Exception {
        Document pom = readPom(Path.of("pom.xml"));

        assertEquals("Backend", value(pom, "/project/artifactId"));
        assertEquals("org.springframework.boot", value(pom, "/project/parent/groupId"));
        assertEquals("jar", value(pom, "/project/packaging"));
        assertFalse(pom.getElementsByTagName("module").getLength() > 0);
    }

    /**
     * 读取Pom。
     * @param path 路径参数。
     * @return Document 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private static Document readPom(Path path) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    /**
     * 值的处理逻辑。
     * @param document document参数。
     * @param expression expression参数。
     * @return String 类型的处理结果。
     * @throws Exception 当前调用无法继续完成时抛出。
     */
    private static String value(Document document, String expression) throws Exception {
        return ((String) XPathFactory.newInstance()
                .newXPath()
                .evaluate(expression, document, XPathConstants.STRING))
                .trim();
    }
}
