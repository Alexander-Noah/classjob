CREATE TABLE IF NOT EXISTS teacher_host (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  lab_id BIGINT NOT NULL COMMENT '实验室ID',
  teacher_device_id VARCHAR(120) NOT NULL COMMENT '教师端设备ID',
  host_ip VARCHAR(64) NOT NULL COMMENT '教师端内网IP',
  port INT NOT NULL DEFAULT 8765 COMMENT '教师端本地WebSocket端口',
  status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态：online在线，offline离线',
  token VARCHAR(500) DEFAULT NULL COMMENT '客户端授权令牌快照',
  last_heartbeat_time DATETIME NOT NULL COMMENT '最近心跳时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_teacher_host_lab_device (lab_id, teacher_device_id),
  KEY idx_teacher_host_lab_status_time (lab_id, status, last_heartbeat_time),
  CONSTRAINT fk_teacher_host_lab FOREIGN KEY (lab_id) REFERENCES lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室教师端主机登记表';

CREATE TABLE IF NOT EXISTS student_client (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  lab_id BIGINT NOT NULL COMMENT '实验室ID',
  student_device_id VARCHAR(120) NOT NULL COMMENT '学生端设备ID',
  host_name VARCHAR(120) DEFAULT NULL COMMENT '学生端主机名',
  ip_address VARCHAR(64) DEFAULT NULL COMMENT '学生端内网IP',
  status VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态：online在线，offline离线',
  token VARCHAR(500) DEFAULT NULL COMMENT '客户端授权令牌快照',
  last_heartbeat_time DATETIME NOT NULL COMMENT '最近心跳时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_student_client_lab_device (lab_id, student_device_id),
  KEY idx_student_client_lab_status_time (lab_id, status, last_heartbeat_time),
  CONSTRAINT fk_student_client_lab FOREIGN KEY (lab_id) REFERENCES lab(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室学生端在线状态表';
