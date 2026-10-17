/*
 * 通知公告能力契约，供调用方依赖接口而非具体实现。
 */
package org.example.backend.service;

import org.example.backend.entity.NoticeEntity;
import org.example.backend.entity.NoticeRecipientEntity;

import java.util.List;
import java.util.Map;

public interface NoticeService {
    /**
     * 列出通知。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeEntity> listNotices();

    /**
     * 创建通知。
     * @param notice 通知参数。
     * @return NoticeEntity 类型的处理结果。
     */
    NoticeEntity createNotice(NoticeEntity notice);

    /**
     * 更新通知。
     * @param id 标识参数。
     * @param notice 通知参数。
     * @return NoticeEntity 类型的处理结果。
     */
    NoticeEntity updateNotice(Long id, NoticeEntity notice);

    /**
     * 删除通知。
     * @param id 标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeEntity> deleteNotice(Long id);

    /**
     * replace通知的处理逻辑。
     * @param notices 通知参数。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeEntity> replaceNotices(List<NoticeEntity> notices);

    /**
     * 重置通知。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeEntity> resetNotices();

    /**
     * publish通知的处理逻辑。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    NoticeEntity publishNotice(Long id);

    /**
     * withdraw通知的处理逻辑。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    NoticeEntity withdrawNotice(Long id);

    /**
     * 归档通知的处理逻辑。
     * @param id 标识参数。
     * @return NoticeEntity 类型的处理结果。
     */
    NoticeEntity archiveNotice(Long id);

    /**
     * 列出Recipients。
     * @param noticeId 通知标识参数。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeRecipientEntity> listRecipients(Long noticeId);

    /**
     * 列出用户通知。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @param readStatus read状态参数。
     * @return 当前操作得到的记录集合。
     */
    List<NoticeRecipientEntity> listUserNotices(Integer userId, String roleCode, String readStatus);

    /**
     * 标记Read。
     * @param noticeId 通知标识参数。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @return NoticeRecipientEntity 类型的处理结果。
     */
    NoticeRecipientEntity markRead(Long noticeId, Integer userId, String roleCode);

    /**
     * 获取通知Stats。
     * @param userId 用户标识参数。
     * @param roleCode 角色编码参数。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> getNoticeStats(Integer userId, String roleCode);

    /**
     * 同步BusinessReminders。
     * @return 当前操作返回的键值结果。
     */
    Map<String, Object> syncBusinessReminders();
}
