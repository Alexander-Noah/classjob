/*
 * 实验室预约实现模块。
 */
package org.example.backend.mapper;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.example.backend.entity.ReservationStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ReservationStatusTypeHandler extends BaseTypeHandler<ReservationStatus> {
    /**
     * 设置非NullParameter。
     * @param statement statement参数。
     * @param index 索引参数。
     * @param status 状态参数。
     * @param jdbcType JDBC类型参数。
     * @throws SQLException 当前调用无法继续完成时抛出。
     */
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, ReservationStatus status, JdbcType jdbcType)
            throws SQLException {
        statement.setString(index, status.storageValue());
    }

    /**
     * 获取Nullable结果。
     * @param resultSet 结果集合参数。
     * @param columnName column名称参数。
     * @return ReservationStatus 类型的处理结果。
     * @throws SQLException 当前调用无法继续完成时抛出。
     */
    @Override
    public ReservationStatus getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return status(resultSet.getString(columnName));
    }

    /**
     * 获取Nullable结果。
     * @param resultSet 结果集合参数。
     * @param columnIndex column索引参数。
     * @return ReservationStatus 类型的处理结果。
     * @throws SQLException 当前调用无法继续完成时抛出。
     */
    @Override
    public ReservationStatus getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return status(resultSet.getString(columnIndex));
    }

    /**
     * 获取Nullable结果。
     * @param statement statement参数。
     * @param columnIndex column索引参数。
     * @return ReservationStatus 类型的处理结果。
     * @throws SQLException 当前调用无法继续完成时抛出。
     */
    @Override
    public ReservationStatus getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return status(statement.getString(columnIndex));
    }

    /**
     * 状态的处理逻辑。
     * @param value 值参数。
     * @return ReservationStatus 类型的处理结果。
     */
    private ReservationStatus status(String value) {
        return value == null ? null : ReservationStatus.fromStorageValue(value);
    }
}
