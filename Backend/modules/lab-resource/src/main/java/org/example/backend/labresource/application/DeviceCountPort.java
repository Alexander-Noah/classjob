package org.example.backend.labresource.application;

import java.util.Map;
import java.util.Set;

public interface DeviceCountPort {
    Map<Long, Integer> countByLabIds(Set<Long> labIds);
}
