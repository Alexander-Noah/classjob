package org.example.backend.service;

import java.util.List;

public interface LabCatalogQuery {
    List<LabView> list(LabAccessScope scope);

    LabView findById(long id, LabAccessScope scope);
}
