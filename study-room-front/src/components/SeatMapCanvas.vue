<template>
  <div class="seat-map-canvas">
    <svg
      :viewBox="`0 0 ${cw} ${ch}`"
      class="map-svg"
      xmlns="http://www.w3.org/2000/svg"
    >
      <!-- 底图 -->
      <image
        v-if="layout?.canvas?.bgImage"
        :href="layout.canvas.bgImage"
        x="0" y="0"
        :width="cw" :height="ch"
        preserveAspectRatio="xMidYMid meet"
      />

      <!-- 区域 -->
      <g v-for="a in areas" :key="a.id" class="map-area">
        <rect
          :x="a.x" :y="a.y" :width="a.w" :height="a.h"
          :fill="a.fill || 'rgba(102,126,234,0.30)'"
          :stroke="a.stroke || '#667eea'"
          stroke-width="2"
          rx="8"
        />
        <text
          v-if="a.name"
          :x="a.x + a.w / 2" :y="a.y + 24"
          class="area-name"
          text-anchor="middle"
        >{{ a.name }}</text>
      </g>

      <!-- 墙体 -->
      <line
        v-for="w in walls"
        :key="w.id"
        :x1="w.x1" :y1="w.y1" :x2="w.x2" :y2="w.y2"
        :stroke="w.color || '#8a8a96'"
        :stroke-width="w.thickness || 8"
        stroke-linecap="round"
        class="map-wall"
      />

      <!-- 自由画笔路径 -->
      <polyline
        v-for="p in paths"
        :key="p.id"
        :points="pointsToStr(p.points)"
        :stroke="p.color || '#999999'"
        :stroke-width="p.thickness || 5"
        fill="none"
        stroke-linecap="round"
        stroke-linejoin="round"
        class="map-path"
      />

      <!-- 文字标注 -->
      <text
        v-for="t in texts"
        :key="t.id"
        :x="t.x" :y="t.y"
        :font-size="t.fontSize || 24"
        :fill="t.color || '#e8e8ec'"
        class="map-text"
      >{{ t.content }}</text>

      <!-- 座位 -->
      <g
        v-for="s in seats"
        :key="s.id"
        class="map-seat"
        :transform="`translate(${s.x + (s.w || 44) / 2} ${s.y + (s.h || 44) / 2}) rotate(${s.rotation || 0})`"
        @click="onSeatClick(s)"
      >
        <title>{{ seatTitle(s) }}</title>
        <rect
          :x="-(s.w || 44) / 2" :y="-(s.h || 44) / 2"
          :width="s.w || 44" :height="s.h || 44"
          :fill="seatFill(s)"
          stroke="#d8d8e0"
          stroke-width="1.5"
          rx="6"
          class="seat-rect"
        />
        <text
          :x="0" :y="0"
          text-anchor="middle"
          dominant-baseline="central"
          class="seat-no"
          :font-size="Math.max(10, Math.min(14, (s.w || 44) / 4))"
        >{{ s.seatNo }}</text>
        <circle
          v-if="seatData(s)?.myReservationId"
          cx="0" cy="0"
          r="3"
          fill="#ffffff"
          opacity="0.9"
        />
      </g>
    </svg>

    <div v-if="!layout" class="map-empty">该楼层暂无结构图，请管理员在「楼层绘图」中绘制并发布</div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  layout: { type: Object, default: null },
  // 座位实时状态（来自现有 getSeats 接口）
  seats: { type: Array, default: () => [] }
});
const emit = defineEmits(['seat-click']);

const cw = computed(() => Number(props.layout?.canvas?.width) || 1600);
const ch = computed(() => Number(props.layout?.canvas?.height) || 1100);
const areas = computed(() => props.layout?.areas || []);
const walls = computed(() => props.layout?.walls || []);
const paths = computed(() => props.layout?.paths || []);
const texts = computed(() => props.layout?.texts || []);
const seats = computed(() => props.layout?.seats || []);

const seatDataMap = computed(() => {
  const map = new Map();
  (props.seats || []).forEach(s => {
    const id = s.id ?? s.seatId ?? s.seat_id;
    if (id != null) map.set(String(id), s);
  });
  return map;
});

const seatData = (s) => seatDataMap.value.get(String(s.seatId));

const seatRatio = (s) => {
  const r = Number(s?.bookedRatio);
  return Number.isFinite(r) ? r : 0;
};

const seatFill = (s) => {
  const data = seatData(s);
  if (data && Number(data.status) === 3) return '#9e9e9e';
  const r = seatRatio(data);
  if (r <= 0) return '#4caf50';
  if (r >= 1) return '#f44336';
  return '#f5a623';
};

const seatTitle = (s) => {
  const data = seatData(s);
  if (!data) return `${s.seatNo}（未同步数据）`;
  if (Number(data.status) === 3) return `${s.seatNo} · 维修中`;
  const r = seatRatio(data);
  const state = r <= 0 ? '空闲' : r >= 1 ? '已约满' : '部分可约';
  const user = data.userName ? ` · ${data.userName}` : '';
  const mine = data.myReservationId ? ' · 我的预约' : '';
  return `${s.seatNo} · ${state}${user}${mine}`;
};

const onSeatClick = (s) => {
  const data = seatData(s) || { id: s.seatId, seat_no: s.seatNo, seatNo: s.seatNo, status: s.status ?? 0 };
  emit('seat-click', data);
};

const pointsToStr = (points) => {
  return (points || []).map(p => p.join(',')).join(' ');
};
</script>

<style scoped>
.seat-map-canvas {
  width: 100%;
  overflow-x: auto;
  background: var(--bg-primary, #0d0d12);
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  border-radius: 12px;
  padding: 12px;
  box-sizing: border-box;
}
.map-svg {
  width: 100%;
  height: auto;
  display: block;
  border-radius: 8px;
  background: #12121a;
  min-width: 600px;
}
.map-area .area-name {
  fill: var(--text-secondary, #9a9aa5);
  font-size: 18px;
  font-weight: 600;
}
.map-seat { cursor: pointer; }
.map-seat .seat-rect {
  transition: filter 0.15s, stroke 0.15s;
  stroke: var(--border-color, rgba(255,255,255,0.25));
}
.map-seat:hover .seat-rect {
  filter: brightness(1.25);
  stroke: #ffffff;
  stroke-width: 2.5;
}
.seat-no {
  fill: #ffffff;
  font-weight: 600;
  pointer-events: none;
  user-select: none;
}
.map-empty {
  color: var(--text-secondary, #9a9aa5);
  font-size: 14px;
  text-align: center;
  padding: 48px 0;
}
</style>
