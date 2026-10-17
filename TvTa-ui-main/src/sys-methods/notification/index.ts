/*
 * 桌面端 notification 系统能力适配；调用方式以导入的 Tauri 插件 API 为准。
 * 主要导出：sendSystemNotification。
 */
import { sendNotification as tauriSendNotification, isPermissionGranted, requestPermission } from '@tauri-apps/plugin-notification';

/**
 * 发送系统级通知
 * @param title 标题
 * @param body 内容
 */
export const sendSystemNotification = async (title: string, body: string) => {
  let permissionGranted = await isPermissionGranted();
  
  if (!permissionGranted) {
    const permission = await requestPermission();
    permissionGranted = permission === 'granted';
  }

  if (permissionGranted) {
    tauriSendNotification({
      title,
      body,
    });
  }
};
