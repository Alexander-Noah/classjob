package org.example.backend.config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LocalSecretsFailureBehaviorTest {
    @TempDir Path temp;
    void developmentDirectory() throws Exception{Files.writeString(temp.resolve("pom.xml"),"<project/>");Path source=temp.resolve("src/main/java/org/example/backend/BackendApplication.java");Files.createDirectories(source.getParent());Files.writeString(source,"// test fixture");}
    @Test void unreadableExistingSecretsFailInsteadOfSilentlyReplacingSigningKeys() throws Exception{developmentDirectory();Path target=temp.resolve(".local-dev-secrets.properties");Files.writeString(target,"test-only=true");try(var files=mockStatic(Files.class,CALLS_REAL_METHODS)){files.when(()->Files.newInputStream(target)).thenThrow(new IOException("denied"));assertTrue(assertThrows(IllegalStateException.class,()->LocalDevelopmentSecretsInitializer.ensure(temp)).getMessage().contains("Failed to read"));}assertEquals("test-only=true",Files.readString(target));}
    @Test void unwritableSecretsFailWithoutStartingWithMissingKeys() throws Exception{developmentDirectory();Path target=temp.resolve(".local-dev-secrets.properties");try(var files=mockStatic(Files.class,CALLS_REAL_METHODS)){files.when(()->Files.newOutputStream(target)).thenThrow(new IOException("denied"));Files.deleteIfExists(target);assertTrue(assertThrows(IllegalStateException.class,()->LocalDevelopmentSecretsInitializer.ensure(temp)).getMessage().contains("Failed to write"));}assertFalse(Files.exists(target));}
}
