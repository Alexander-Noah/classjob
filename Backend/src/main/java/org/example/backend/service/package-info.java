/**
 * 业务服务与查询能力契约。CapabilityResult 区分可用结果与能力不可用，调用方需先判断状态。
 * 涉及多表写入的流程应在实际业务服务边界内维护事务，Mapper 仅负责持久化。
 */
package org.example.backend.service;
