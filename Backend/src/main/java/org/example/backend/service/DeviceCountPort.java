package org.example.backend.service;

import java.util.Map;
import java.util.Set;

public interface DeviceCountPort {
    Map<Long, Integer> countByLabIds(Set<Long> labIds);
}
