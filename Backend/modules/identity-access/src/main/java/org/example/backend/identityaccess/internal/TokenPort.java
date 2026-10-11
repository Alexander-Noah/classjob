package org.example.backend.identityaccess.internal;

import org.example.backend.identityaccess.api.UserView;

public interface TokenPort {
    String issue(UserView user);
    UserView validate(String token);
}
