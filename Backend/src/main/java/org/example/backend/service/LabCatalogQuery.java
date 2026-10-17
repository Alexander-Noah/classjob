/*
 * 实验室能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import java.util.List;

public interface LabCatalogQuery {
    List<LabView> list(LabAccessScope scope);

    LabView findById(long id, LabAccessScope scope);
}
