<template>
  <div class="fe-panel">
    <div class="fe-panel-head">
      <span class="fe-panel-title">{{ title }}</span>
      <button type="button" class="fe-panel-del" title="删除选中对象" @click="emit('delete')">🗑 删除</button>
    </div>
    <div class="fe-panel-body">
      <!-- 区域 -->
      <template v-if="areaObj">
        <div class="fe-field">
          <label>名称</label>
          <input :value="areaObj.name || ''" placeholder="如 A 区" @input="on('name', $event)" />
        </div>
        <div class="fe-field">
          <label>名称字号</label>
          <input
            type="number"
            min="8"
            max="200"
            :value="areaObj.nameSize || 18"
            @change="onNum('nameSize', $event, 18)"
          />
        </div>
        <div class="fe-field">
          <label>名称对齐</label>
          <select :value="areaObj.nameAlign || 'left'" @change="on('nameAlign', $event)">
            <option value="left">左对齐</option>
            <option value="center">居中</option>
            <option value="right">右对齐</option>
          </select>
        </div>
        <div class="fe-field">
          <label>名称颜色</label>
          <input type="color" :value="toColor(areaObj.nameColor, '#ffffff')" @input="on('nameColor', $event)" />
        </div>
        <div class="fe-field">
          <label>填充色</label>
          <input type="color" :value="toColor(areaObj.fill, '#667eea')" @input="on('fill', $event)" />
          <input type="text" :value="areaObj.fill || ''" placeholder="rgba(...)" @change="on('fill', $event)" />
        </div>
        <div class="fe-field">
          <label>描边色</label>
          <input type="color" :value="toColor(areaObj.stroke, '#667eea')" @input="on('stroke', $event)" />
        </div>
        <div class="fe-field">
          <label>位置 X</label>
          <input type="number" :value="round(areaObj.x)" @change="onNum('x', $event)" />
        </div>
        <div class="fe-field">
          <label>位置 Y</label>
          <input type="number" :value="round(areaObj.y)" @change="onNum('y', $event)" />
        </div>
        <div class="fe-field">
          <label>宽 W</label>
          <input type="number" min="10" :value="round(areaObj.w)" @change="onNum('w', $event)" />
        </div>
        <div class="fe-field">
          <label>高 H</label>
          <input type="number" min="10" :value="round(areaObj.h)" @change="onNum('h', $event)" />
        </div>
      </template>

      <!-- 座位 -->
      <template v-else-if="seatObj">
        <div class="fe-field">
          <label>座位编号</label>
          <input :value="seatObj.seatNo || ''" @input="on('seatNo', $event)" />
        </div>
        <div class="fe-field">
          <label>外观形状</label>
          <select :value="seatObj.shape || 'rect'" @change="on('shape', $event)">
            <option value="rect">方形</option>
            <option value="round">圆形</option>
            <option value="diamond">菱形</option>
          </select>
        </div>
        <div class="fe-field">
          <label>类型</label>
          <select :value="seatObj.seatType || 1" @change="onSelectNum('seatType', $event)">
            <option :value="1">1-普通</option>
            <option :value="2">2-靠窗</option>
            <option :value="3">3-带插座</option>
          </select>
        </div>
        <div class="fe-field">
          <label>所属区域</label>
          <select :value="seatObj.areaId ?? ''" @change="onAreaSelect($event)">
            <option value="">（无）</option>
            <option v-for="ar in areas" :key="ar.id" :value="ar.id">{{ ar.areaName }}</option>
          </select>
        </div>
        <div class="fe-field">
          <label>状态</label>
          <select :value="seatObj.status ?? 0" @change="onSelectNum('status', $event)">
            <option :value="0">正常可约</option>
            <option :value="3">维修中</option>
          </select>
        </div>
        <div class="fe-field">
          <label>编号字号</label>
          <input
            type="number"
            min="6"
            max="80"
            :value="seatObj.labelSize ?? ''"
            placeholder="自动"
            title="留空时按座位大小自适应"
            @change="onSizeOrNull('labelSize', $event)"
          />
        </div>
        <div class="fe-field">
          <label>编号对齐</label>
          <select :value="seatObj.labelAlign || 'center'" @change="on('labelAlign', $event)">
            <option value="left">左对齐</option>
            <option value="center">居中</option>
            <option value="right">右对齐</option>
          </select>
        </div>
        <div class="fe-field">
          <label>编号颜色</label>
          <input type="color" :value="toColor(seatObj.labelColor, '#ffffff')" @input="on('labelColor', $event)" />
        </div>
        <div class="fe-field">
          <label>旋转°</label>
          <input
            type="number"
            step="5"
            min="-180"
            max="180"
            :value="seatObj.rotation || 0"
            @change="onNum('rotation', $event)"
          />
        </div>
        <div class="fe-field">
          <label>尺寸</label>
          <input type="number" min="24" max="120" :value="seatSize" @change="onSize($event)" />
        </div>
        <div class="fe-field">
          <label>位置 X</label>
          <input type="number" :value="round(seatObj.x)" @change="onNum('x', $event)" />
        </div>
        <div class="fe-field">
          <label>位置 Y</label>
          <input type="number" :value="round(seatObj.y)" @change="onNum('y', $event)" />
        </div>
        <p v-if="seatObj.seatId" class="fe-panel-note">已同步数据库座位 #{{ seatObj.seatId }}</p>
      </template>

      <!-- 通用图形 -->
      <template v-else-if="graphicObj">
        <div class="fe-field">
          <label>种类</label>
          <span class="fe-static">{{ graphicKindLabel }}</span>
        </div>
        <div class="fe-field">
          <label>填充色</label>
          <input type="color" :value="toColor(graphicObj.fill, '#8f96ab')" @input="on('fill', $event)" />
          <input type="text" :value="graphicObj.fill || ''" placeholder="rgba(...)" @change="on('fill', $event)" />
        </div>
        <div class="fe-field">
          <label>描边色</label>
          <input type="color" :value="toColor(graphicObj.stroke, '#8f96ab')" @input="on('stroke', $event)" />
        </div>
        <div class="fe-field">
          <label>描边粗</label>
          <input
            type="number"
            min="1"
            max="20"
            :value="graphicObj.strokeWidth || 1.5"
            @change="onNum('strokeWidth', $event, 1.5)"
          />
        </div>
        <div class="fe-field">
          <label>旋转°</label>
          <input
            type="number"
            step="5"
            min="-180"
            max="180"
            :value="graphicObj.rotation || 0"
            @change="onNum('rotation', $event)"
          />
        </div>
        <div class="fe-field">
          <label>位置 X</label>
          <input type="number" :value="round(graphicObj.x)" @change="onNum('x', $event)" />
        </div>
        <div class="fe-field">
          <label>位置 Y</label>
          <input type="number" :value="round(graphicObj.y)" @change="onNum('y', $event)" />
        </div>
        <div class="fe-field">
          <label>宽 W</label>
          <input type="number" min="4" :value="round(graphicObj.w)" @change="onNum('w', $event)" />
        </div>
        <div class="fe-field">
          <label>高 H</label>
          <input type="number" min="4" :value="round(graphicObj.h)" @change="onNum('h', $event)" />
        </div>
      </template>

      <!-- 墙体 -->
      <template v-else-if="wallObj">
        <div class="fe-field">
          <label>颜色</label>
          <input type="color" :value="toColor(wallObj.color, '#8a8a96')" @input="on('color', $event)" />
        </div>
        <div class="fe-field">
          <label>粗细</label>
          <input type="number" min="2" max="40" :value="wallObj.thickness || 8" @change="onNum('thickness', $event)" />
        </div>
      </template>

      <!-- 画笔路径 -->
      <template v-else-if="pathObj">
        <div class="fe-field">
          <label>颜色</label>
          <input type="color" :value="toColor(pathObj.color, '#999999')" @input="on('color', $event)" />
        </div>
        <div class="fe-field">
          <label>粗细</label>
          <input type="number" min="1" max="30" :value="pathObj.thickness || 5" @change="onNum('thickness', $event)" />
        </div>
      </template>

      <!-- 文字 -->
      <template v-else-if="textObj">
        <div class="fe-field">
          <label>内容</label>
          <textarea :value="textObj.content" rows="2" @input="on('content', $event)"></textarea>
        </div>
        <div class="fe-field">
          <label>字号</label>
          <input type="number" min="10" max="120" :value="textObj.fontSize || 24" @change="onNum('fontSize', $event)" />
        </div>
        <div class="fe-field">
          <label>颜色</label>
          <input type="color" :value="toColor(textObj.color, '#e6e6ee')" @input="on('color', $event)" />
        </div>
        <div class="fe-field">
          <label>位置 X</label>
          <input type="number" :value="round(textObj.x)" @change="onNum('x', $event)" />
        </div>
        <div class="fe-field">
          <label>位置 Y</label>
          <input type="number" :value="round(textObj.y)" @change="onNum('y', $event)" />
        </div>
      </template>

      <div class="fe-panel-tip">
        <p>拖动对象可移动位置；框选可一次移动多个</p>
        <p>Delete 删除 · Ctrl+C/X/V 复制/剪切/粘贴</p>
        <p>Ctrl+Z / Ctrl+Y 撤销重做 · 空格拖动或抓手工具平移画布 · 右键单击切换抓手</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AreaInfo } from '@/types/api'
import type { EditorKind } from './types'
import type {
  LayoutAreaShape,
  LayoutGraphicShape,
  LayoutPathShape,
  LayoutSeatShape,
  LayoutTextShape,
  LayoutWallShape
} from '@/types/layout'

const props = defineProps<{
  kind: EditorKind
  obj: LayoutAreaShape | LayoutSeatShape | LayoutWallShape | LayoutPathShape | LayoutTextShape | LayoutGraphicShape
  areas: AreaInfo[]
}>()

const emit = defineEmits<{
  (e: 'change', key: string, value: string | number | null): void
  (e: 'delete'): void
}>()

const TITLES: Record<EditorKind, string> = {
  area: '区域属性',
  seat: '座位属性',
  wall: '墙体属性',
  path: '画笔属性',
  text: '文字属性',
  shape: '图形属性'
}
const title = computed(() => TITLES[props.kind])

const areaObj = computed(() => (props.kind === 'area' ? (props.obj as LayoutAreaShape) : null))
const seatObj = computed(() => (props.kind === 'seat' ? (props.obj as LayoutSeatShape) : null))
const graphicObj = computed(() => (props.kind === 'shape' ? (props.obj as LayoutGraphicShape) : null))
const wallObj = computed(() => (props.kind === 'wall' ? (props.obj as LayoutWallShape) : null))
const pathObj = computed(() => (props.kind === 'path' ? (props.obj as LayoutPathShape) : null))
const textObj = computed(() => (props.kind === 'text' ? (props.obj as LayoutTextShape) : null))

const GRAPHIC_KIND_LABELS: Record<string, string> = {
  rect: '矩形',
  roundRect: '圆角矩形',
  ellipse: '椭圆',
  circle: '圆形',
  triangle: '三角形',
  diamond: '菱形'
}
const graphicKindLabel = computed(() =>
  graphicObj.value ? GRAPHIC_KIND_LABELS[graphicObj.value.kind] || graphicObj.value.kind : ''
)

const seatSize = computed(() => {
  const o = seatObj.value
  if (!o) return 44
  return Math.round(o.w || o.h || 44)
})

const round = (v: number | undefined): number => Math.round(v ?? 0)
const toColor = (v: string | undefined, fallback: string): string => {
  // <input type=color> 只接受 #rrggbb
  if (v && /^#[0-9a-fA-F]{6}$/.test(v)) return v
  return fallback
}

function on(key: string, e: Event): void {
  const el = e.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
  emit('change', key, el.value)
}
function onNum(key: string, e: Event, fb = 0): void {
  const el = e.target as HTMLInputElement
  const v = Number(el.value)
  emit('change', key, Number.isFinite(v) ? v : fb)
}
/** 可留空（null=自动）的数字输入 */
function onSizeOrNull(key: string, e: Event): void {
  const el = e.target as HTMLInputElement
  const raw = el.value.trim()
  if (raw === '') {
    emit('change', key, null)
    return
  }
  const v = Number(raw)
  emit('change', key, Number.isFinite(v) ? v : 0)
}
function onSelectNum(key: string, e: Event): void {
  const el = e.target as HTMLSelectElement
  const v = Number(el.value)
  emit('change', key, Number.isFinite(v) ? v : 0)
}
function onAreaSelect(e: Event): void {
  const el = e.target as HTMLSelectElement
  emit('change', 'areaId', el.value === '' ? null : Number(el.value))
}
function onSize(e: Event): void {
  const el = e.target as HTMLInputElement
  const v = Number(el.value)
  if (Number.isFinite(v) && v >= 10) emit('change', 'size', v)
}
</script>

<style scoped>
.fe-panel {
  width: 256px;
  flex: 0 0 auto;
  border-left: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  background: var(--bg-glass, rgba(13, 13, 18, 0.55));
  display: flex;
  flex-direction: column;
  max-height: 100%;
  overflow: hidden;
}
.fe-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color, rgba(255, 255, 255, 0.08));
}
.fe-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary, #e8e8ec);
}
.fe-panel-del {
  border: 1px solid rgba(244, 67, 54, 0.5);
  background: rgba(244, 67, 54, 0.12);
  color: #f2554a;
  border-radius: 6px;
  font-size: 12px;
  padding: 3px 8px;
  cursor: pointer;
}
.fe-panel-del:hover {
  background: rgba(244, 67, 54, 0.25);
}
.fe-panel-body {
  padding: 10px 12px 12px;
  overflow: auto;
  flex: 1;
}
.fe-field {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.fe-field label {
  width: 58px;
  flex: 0 0 auto;
  font-size: 12px;
  color: var(--text-secondary, #9a9aa5);
}
.fe-field input,
.fe-field select,
.fe-field textarea {
  flex: 1;
  min-width: 0;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.14));
  border-radius: 6px;
  color: var(--text-primary, #e8e8ec);
  font-size: 12px;
  padding: 5px 6px;
  box-sizing: border-box;
}
.fe-field textarea {
  resize: vertical;
  font-family: inherit;
}
.fe-field input[type='color'] {
  flex: 0 0 38px;
  padding: 2px;
  height: 26px;
}
.fe-field input[type='color'] + input {
  flex: 1;
}
.fe-static {
  flex: 1;
  font-size: 12px;
  color: var(--text-primary, #e8e8ec);
}
.fe-panel-note {
  font-size: 11px;
  color: #4caf80;
  margin-top: 4px;
}
.fe-panel-tip {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-color, rgba(255, 255, 255, 0.1));
  font-size: 11px;
  line-height: 1.7;
  color: var(--text-secondary, #9a9aa5);
}
.fe-panel-tip p {
  margin: 0;
}
</style>
