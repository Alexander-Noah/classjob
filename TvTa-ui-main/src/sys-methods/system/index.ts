/*
 * 桌面端 system 系统能力适配；调用方式以导入的 Tauri 插件 API 为准。
 * 主要导出：getMacAddress。
 */
import { Command } from '@tauri-apps/plugin-shell';
import { invoke } from '@tauri-apps/api/core';
import { platform } from '@tauri-apps/plugin-os';

const MAC_PATTERN = /([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})/g;

/**
 * 归一化MacAddress。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const normalizeMacAddress = (value: string) => value.trim().toUpperCase().replace(/-/g, ':');

/**
 * 查找FirstMacAddress。
 * 入参：value（值）。
 * @returns 由当前表达式或分支生成的处理结果。
 */
const findFirstMacAddress = (value: string) => {
  const matches = value.match(MAC_PATTERN) || [];
  return normalizeMacAddress(matches.find(/** 判断当前条目是否为查找目标。 入参：item。 */ item => !/^00[:-]00[:-]00[:-]00[:-]00[:-]00$/i.test(item)) || '');
};

/**
 * 获取 MAC 地址 (简单实现)
 */
export const getMacAddress = async (): Promise<string> => {
  try {
    const macAddress = await invoke<string>('get_local_mac_address');
    const normalizedMacAddress = findFirstMacAddress(macAddress);
    if (normalizedMacAddress) return normalizedMacAddress;
  } catch {
    // Fall through to shell commands for older builds or unusual environments.
  }

  const osPlatform = await platform();
  try {
    if (osPlatform === 'macos') {
      const output = await Command.create('networksetup', ['-getmacaddress', 'en0']).execute();
      // Output format: "Ethernet Address: xx:xx:xx:xx:xx:xx (Device: en0)"
      return findFirstMacAddress(output.stdout);
    } else if (osPlatform === 'windows') {
      const output = await Command.create('getmac', []).execute();
      const macAddress = findFirstMacAddress(output.stdout);
      if (macAddress) return macAddress;

      const powershellOutput = await Command.create('powershell', [
        '-NoProfile',
        '-Command',
        "Get-CimInstance Win32_NetworkAdapterConfiguration | Where-Object { $_.MACAddress -and $_.IPEnabled } | Select-Object -First 1 -ExpandProperty MACAddress"
      ]).execute();
      return findFirstMacAddress(powershellOutput.stdout);
    }
    return '';
  } catch (error) {
    return '';
  }
};
