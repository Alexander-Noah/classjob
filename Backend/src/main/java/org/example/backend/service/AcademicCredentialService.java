/*
 * 教务凭据能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.AcademicCredentialView;
import org.example.backend.entity.AcademicScheduleImportRequest;

public interface AcademicCredentialService {
    /**
     * 保存凭据。
     * @param request 请求参数。
     * @return AcademicCredentialView 类型的处理结果。
     */
    AcademicCredentialView saveCredential(AcademicScheduleImportRequest request);

    /**
     * 获取凭据视图。
     * @param credentialKey 凭据键参数。
     * @return AcademicCredentialView 类型的处理结果。
     */
    AcademicCredentialView getCredentialView(String credentialKey);

    /**
     * 删除凭据。
     * @param credentialKey 凭据键参数。
     */
    void deleteCredential(String credentialKey);

    /**
     * 应用Saved凭据。
     * @param request 请求参数。
     */
    void applySavedCredential(AcademicScheduleImportRequest request);
}
