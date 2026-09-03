<template>
  <!-- 星空粒子背景 -->
  <ParticlesBackground />
  <!-- 这里就是路由页面的显示位置 -->
  <router-view />
</template>

<script setup>
import ParticlesBackground from '@/components/ParticlesBackground.vue';
</script>

<style scoped>
/* 可以在这里加一些全局样式，比如去掉默认边距 */
body {
  margin: 0;
  padding: 0;
}
</style>