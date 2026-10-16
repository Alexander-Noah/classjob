package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ConsumableLoanEntity;
import java.util.List;

@Mapper
public interface ConsumableLoanMapper {
    List<ConsumableLoanEntity> list();
    ConsumableLoanEntity get(@Param("id") Long id);
    ConsumableLoanEntity lock(@Param("id") Long id);
    ConsumableLoanEntity byRequest(@Param("requestKey") String requestKey);
    int insert(ConsumableLoanEntity loan);
    int addReturned(@Param("id") Long id, @Param("quantity") int quantity);
}
