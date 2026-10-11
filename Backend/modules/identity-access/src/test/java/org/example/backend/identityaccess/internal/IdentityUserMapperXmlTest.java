package org.example.backend.identityaccess.internal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentityUserMapperXmlTest {
    @Test
    void ownsOnlyAuthenticationQueriesAgainstExistingIdentityTables() throws IOException {
        String xml = mapperXml().toLowerCase();
        assertTrue(xml.contains("from sys_user u"));
        assertTrue(xml.contains("left join user_role"));
        assertTrue(xml.contains("left join role"));
        assertTrue(xml.contains("where u.username = #{username}"));
        assertTrue(xml.contains("update sys_user"));
        assertTrue(xml.contains("set password = #{encodedpassword}"));
        assertFalse(xml.contains("delete from"));
        assertFalse(xml.contains("insert into"));
        assertFalse(xml.contains("class_timetable"));
        assertFalse(xml.contains("lab_reservation"));
    }

    private String mapperXml() throws IOException {
        try (var input = getClass().getResourceAsStream("/mapper/IdentityUserMapper.xml")) {
            if (input == null) throw new IOException("IdentityUserMapper.xml not found");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        }
    }
}
