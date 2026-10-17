/*
 * 耗材借还的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/ConsumableLoanMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.ConsumableLoanEntity;
import java.util.List;

@Mapper
public interface ConsumableLoanMapper {
    /**
     * 列出当前操作。
     * @return 当前操作得到的记录集合。
     */
    List<ConsumableLoanEntity> list();
    /**
     * 获取当前操作。
     * @param id 标识参数。
     * @return ConsumableLoanEntity 类型的处理结果。
     */
    ConsumableLoanEntity get(@Param("id") Long id);
    /**
     * lock的处理逻辑。
     * @param id 标识参数。
     * @return ConsumableLoanEntity 类型的处理结果。
     */
    ConsumableLoanEntity lock(@Param("id") Long id);
    /**
     * by请求的处理逻辑。
     * @param requestKey 请求键参数。
     * @return ConsumableLoanEntity 类型的处理结果。
     */
    ConsumableLoanEntity byRequest(@Param("requestKey") String requestKey);
    /**
     * 插入当前操作。
     * @param loan 借用参数。
     * @return int 类型的处理结果。
     */
    int insert(ConsumableLoanEntity loan);
    /**
     * 添加Returned。
     * @param id 标识参数。
     * @param quantity quantity参数。
     * @return int 类型的处理结果。
     */
    int addReturned(@Param("id") Long id, @Param("quantity") int quantity);
}
