<template>
  <!-- 星空粒子背景 -->
  <ParticlesBackground />
  <!-- 这里就是路由页面的显示位置 -->
  <router-view />
  <!-- 桌面宠物：小夜 / 小晨，可拖拽、可互动、可随时切换 -->
  <!-- avoid-selector：散步时避开左侧导航栏，可按需修改或删除 -->
  <PetCompanion avoid-selector=".sidebar" />
</template>

<script setup lang="ts">
import ParticlesBackground from '@/components/ParticlesBackground.vue'
import PetCompanion from '@/components/PetCompanion.vue'
</script>

<style scoped>
/* 可以在这里加一些全局样式，比如去掉默认边距 */
body {
  margin: 0;
  padding: 0;
}
</style>
