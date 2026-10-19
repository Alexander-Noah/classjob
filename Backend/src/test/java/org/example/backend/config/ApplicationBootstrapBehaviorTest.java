package org.example.backend.config;
import org.example.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ApplicationBootstrapBehaviorTest {
    @Test void localProfileIsAddedOnlyWhenDevelopmentSecretsAreInitialized(){for(boolean local:new boolean[]{true,false})try(var initializer=mockStatic(LocalDevelopmentSecretsInitializer.class);var applications=mockConstruction(SpringApplication.class)){initializer.when(()->LocalDevelopmentSecretsInitializer.ensure(java.nio.file.Path.of(""))).thenReturn(local);BackendApplication.main(new String[]{"--server.port=0"});assertEquals(1,applications.constructed().size());SpringApplication app=applications.constructed().get(0);verify(app).run(new String[]{"--server.port=0"});if(local)verify(app).setAdditionalProfiles("local");else verify(app,never()).setAdditionalProfiles(any(String[].class));}}
}
