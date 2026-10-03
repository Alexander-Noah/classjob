package org.example.backend.labresource.api;

import java.util.List;

public interface LabCatalogQuery {
    List<LabView> list(LabAccessScope scope);

    LabView findById(long id, LabAccessScope scope);
}
