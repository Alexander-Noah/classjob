package org.example.backend.identityaccess.api;

public interface AuthenticationUseCase {
    AuthenticatedSession authenticate(LoginCommand command);
}
