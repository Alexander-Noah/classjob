/**
 * HTTP 接口入口：解析参数、读取当前身份并调用服务，统一响应结构由 result 包提供。
 * 跨资源校验和状态变更应留在服务层，避免不同入口出现不一致的业务规则。
 */
package org.example.backend.controller;
