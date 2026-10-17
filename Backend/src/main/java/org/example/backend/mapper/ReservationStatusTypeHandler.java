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
    @Override
    public void setNonNullParameter(PreparedStatement statement, int index, ReservationStatus status, JdbcType jdbcType)
            throws SQLException {
        statement.setString(index, status.storageValue());
    }

    @Override
    public ReservationStatus getNullableResult(ResultSet resultSet, String columnName) throws SQLException {
        return status(resultSet.getString(columnName));
    }

    @Override
    public ReservationStatus getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
        return status(resultSet.getString(columnIndex));
    }

    @Override
    public ReservationStatus getNullableResult(CallableStatement statement, int columnIndex) throws SQLException {
        return status(statement.getString(columnIndex));
    }

    private ReservationStatus status(String value) {
        return value == null ? null : ReservationStatus.fromStorageValue(value);
    }
}
