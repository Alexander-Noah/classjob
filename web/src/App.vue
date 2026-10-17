<!--
  Web 管理平台的src App视图组件；模板、响应式状态与局部样式在本文件内协同维护。
-->
<script setup>
import { useGlobalButtonGlass } from './composables/useGlobalButtonGlass'
useGlobalButtonGlass()
</script>
<template>
  <RouterView />
</template>
