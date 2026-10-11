package org.example.backend.identityaccess.application.port.out;

import org.example.backend.identityaccess.api.UserView;

public interface TokenPort {
    String issue(UserView user);
    UserView validate(String token);
}
