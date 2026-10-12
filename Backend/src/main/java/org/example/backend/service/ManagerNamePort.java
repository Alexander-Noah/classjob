package org.example.backend.service;

import java.util.Map;
import java.util.Set;

public interface ManagerNamePort {
    Map<Long, String> findNames(Set<Long> managerUserIds);
}
