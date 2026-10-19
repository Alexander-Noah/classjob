package org.example.backend.config;
import org.example.backend.mapper.ConsumableMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchemaInitializerBehaviorTest {
    JdbcTemplate jdbc(Integer columnCount){return mock(JdbcTemplate.class,call->{if(call.getMethod().getName().equals("queryForObject"))return columnCount;return RETURNS_DEFAULTS.answer(call);});}
    List<String> statements(JdbcTemplate jdbc){return mockingDetails(jdbc).getInvocations().stream().filter(call->call.getMethod().getName().equals("execute")).map(call->call.getArgument(0).toString()).toList();}
    @Test void existingColumnsAreNotAlteredAndTableCreationRemainsIdempotent(){JdbcTemplate jdbc=jdbc(1);ConsumableMapper consumables=mock(ConsumableMapper.class);new DatabaseSchemaInitializer(jdbc,consumables).run(null);List<String> statements=statements(jdbc);assertFalse(statements.isEmpty());assertTrue(statements.stream().allMatch(sql->sql.stripLeading().startsWith("create table if not exists")));assertTrue(statements.stream().anyMatch(sql->sql.contains("consumable_loan")&&sql.contains("returned_quantity <= quantity")));assertTrue(statements.stream().anyMatch(sql->sql.contains("ai_assistant_conversation")&&sql.contains("unique key uk_ai_conversation_user_session (user_id, conversation_id)")));verify(consumables).createStockRecordTableIfNotExists();}
    @Test void missingColumnsAreMigratedAndLegacyMaintenanceClassificationIsNarrow(){JdbcTemplate jdbc=jdbc(0);new DatabaseSchemaInitializer(jdbc,mock(ConsumableMapper.class)).run(null);List<String> statements=statements(jdbc);assertTrue(statements.stream().anyMatch(sql->sql.contains("alter table sys_user")&&sql.contains("avatar_url")));assertTrue(statements.stream().anyMatch(sql->sql.contains("alter table device")&&sql.contains("deleted")));assertTrue(statements.stream().anyMatch(sql->sql.contains("alter table notice")&&sql.contains("business_type")));verify(jdbc).update(contains("where order_no regexp '^MR-[0-9]{14}-[0-9]{4}$'"));}
    @Test void unknownColumnCountsAreHandledAsMissingAndDatabaseErrorsPropagate(){JdbcTemplate jdbc=jdbc(null);assertDoesNotThrow(()->new DatabaseSchemaInitializer(jdbc,mock(ConsumableMapper.class)).run(null));assertTrue(statements(jdbc).stream().anyMatch(sql->sql.contains("alter table")));doThrow(new IllegalStateException("database offline")).when(jdbc).execute(anyString());assertThrows(IllegalStateException.class,()->new DatabaseSchemaInitializer(jdbc,mock(ConsumableMapper.class)).run(null));}
}
