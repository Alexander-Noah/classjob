package org.example.backend.entity;

import lombok.Data;

@Data
public class MaintenanceRecord {
    private String id;
    private String recordNo;
    private Long labId;
    private Long deviceId;
    private String lab;
    private String device;
    private String maintenanceType;
    private String maintenanceDate;
    private String maintainer;
    private String content;
    private String result;
    private String remark;
    private String updatedAt;
}
