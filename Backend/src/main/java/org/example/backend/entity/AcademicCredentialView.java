package org.example.backend.entity;

import lombok.Data;

@Data
public class AcademicCredentialView {
    private String credentialKey;
    private String usernameMasked;
    private Boolean configured;
    private Boolean usable;
    private String status;
    private String message;
    private String updatedAt;
}
