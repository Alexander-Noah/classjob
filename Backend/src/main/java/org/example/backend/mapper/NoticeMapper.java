/*
 * 通知公告的 MyBatis 持久化契约。
 * SQL 定义位于 resources/mapper/NoticeMapper.xml，修改参数或字段需同步接口与 XML。
 */
package org.example.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.backend.entity.NoticeEntity;
import org.example.backend.entity.NoticeRecipientEntity;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper {
    /**
     * 列出通知。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeEntity> listNotices();

    /**
     * 获取通知。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    NoticeEntity getNotice(@Param("id") Long id);

    /**
     * 插入通知。
     * @param notice 通知参数。
     * @return int 类型的处理结果。
     */
    int insertNotice(NoticeEntity notice);

    /**
     * 更新通知。
     * @param id 标识参数。
     * @param notice 通知参数。
     * @return int 类型的处理结果。
     */
    int updateNotice(@Param("id") Long id, @Param("notice") NoticeEntity notice);

    /**
     * 删除通知。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int deleteNotice(@Param("id") Long id);

    /**
     * 删除All通知。
     * @return int 类型的处理结果。
     */
    int deleteAllNotices();

    /**
     * publish通知的处理逻辑。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int publishNotice(@Param("id") Long id);

    /**
     * 更新通知状态。
     * @param id 标识参数。
     * @param status 状态参数。
     * @return int 类型的处理结果。
     */
    int updateNoticeStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 归档通知的处理逻辑。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int archiveNotice(@Param("id") Long id);

    /**
     * logicalDelete通知的处理逻辑。
     * @param id 标识参数。
     * @return int 类型的处理结果。
     */
    int logicalDeleteNotice(@Param("id") Long id);

    /**
     * 删除RecipientsBy通知。
     * @param noticeId 通知标识参数。
     * @return int 类型的处理结果。
     */
    int deleteRecipientsByNotice(@Param("noticeId") Long noticeId);

    /**
     * 删除AllRecipients。
     * @return int 类型的处理结果。
     */
    int deleteAllRecipients();

    /**
     * 插入RecipientsFor角色。
     * @param noticeId 通知标识参数。
     * @param targetRole 目标角色参数。
     * @return int 类型的处理结果。
     */
    int insertRecipientsForRole(@Param("noticeId") Long noticeId, @Param("targetRole") String targetRole);

    /**
     * 同步用户Recipients。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @return int 类型的处理结果。
     */
    int syncUserRecipients(@Param("userId") Integer userId, @Param("roleCode") String roleCode);

    /**
     * 列出Recipients。
     * @param noticeId 通知标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeRecipientEntity> listRecipients(@Param("noticeId") Long noticeId);

    /**
     * 列出用户通知。
     * @param userId 用户标识参数。
     * @param readStatus read状态参数。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeRecipientEntity> listUserNotices(
            @Param("userId") Integer userId,
            @Param("readStatus") String readStatus
    );

    /**
     * 标记Read。
     * @param noticeId 通知标识参数。
     * @param userId 用户标识参数。
     * @return int 类型的处理结果。
     */
    int markRead(@Param("noticeId") Long noticeId, @Param("userId") Integer userId);

    /**
     * 获取通知Stats。
     * @param userId 用户标识参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getNoticeStats(@Param("userId") Integer userId);

    /**
     * 统计通知By来源。
     * @param sourceModule 来源Module参数。
     * @param sourceId 来源标识参数。
     * @return int 类型的处理结果。
     */
    int countNoticeBySource(@Param("sourceModule") String sourceModule, @Param("sourceId") String sourceId);

    /**
     * 列出Low库存Consumables。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listLowStockConsumables();

    /**
     * 列出待处理预约。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listPendingReservations();

    /**
     * 列出活动Repairs。
     * @return 当前操作得到的记录集合。
     */
    List<Map<String, Object>> listActiveRepairs();
}
