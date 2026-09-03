<template>
  <div class="layout-editor">
    <div class="editor-topbar">
      <div class="topbar-left">
        <span class="topbar-label">编辑楼层</span>
        <select v-model="selectedFloorId" @change="onFloorChange" class="floor-select">
          <option v-for="f in floors" :key="f.id" :value="f.id">{{ f.floor_name || f.floorName }}</option>
        </select>
      </div>
      <div class="topbar-right">
        <button class="tb-btn" @click="undo" :disabled="!canUndo" title="撤销 Ctrl+Z">↩️ 撤销</button>
        <button class="tb-btn" @click="redo" :disabled="!canRedo" title="重做 Ctrl+Shift+Z">↪️ 重做</button>
        <span class="tb-sep"></span>
        <button class="tb-btn" @click="zoomOut" title="缩小">−</button>
        <span class="tb-zoom">{{ Math.round(zoom * 100) }}%</span>
        <button class="tb-btn" @click="zoomIn" title="放大">＋</button>
        <button class="tb-btn" @click="fitView" title="适应窗口">⛶ 适应</button>
        <span class="tb-sep"></span>
        <button class="tb-btn primary" @click="handleSaveDraft" :disabled="saving">💾 保存草稿</button>
        <button class="tb-btn publish" @click="handlePublish" :disabled="saving">🚀 发布</button>
      </div>
    </div>

    <div class="editor-body">
      <div class="tool-panel">
        <div class="tool-group">
          <button class="tool-btn" :class="{ active: mode === 'select' }" @click="setMode('select')" title="选择/移动 (V)">
            <span class="tool-icon">🖱️</span><span class="tool-name">选择</span>
          </button>
          <button class="tool-btn" :class="{ active: mode === 'pan' }" @click="setMode('pan')" title="平移画布 (H)">
            <span class="tool-icon">✋</span><span class="tool-name">平移</span>
          </button>
        </div>
        <div class="tool-divider"></div>
        <div class="tool-group">
          <button class="tool-btn" :class="{ active: mode === 'area' }" @click="setMode('area')" title="绘制区域/房间">
            <span class="tool-icon">📦</span><span class="tool-name">区域</span>
          </button>
          <button class="tool-btn" :class="{ active: mode === 'wall' }" @click="setMode('wall')" title="绘制墙体/隔断">
            <span class="tool-icon">📏</span><span class="tool-name">墙体</span>
          </button>
          <button class="tool-btn" :class="{ active: mode === 'path' }" @click="setMode('path')" title="自由画笔">
            <span class="tool-icon">✏️</span><span class="tool-name">画笔</span>
          </button>
        </div>
        <div class="tool-divider"></div>
        <div class="tool-group">
          <button class="tool-btn" :class="{ active: mode === 'seat' }" @click="setMode('seat')" title="放置座位">
            <span class="tool-icon">🪑</span><span class="tool-name">座位</span>
          </button>
          <div class="seat-type-row" v-if="mode === 'seat'">
            <select v-model="seatType" class="mini-select" title="座位类型">
              <option :value="1">普通座</option>
              <option :value="2">靠窗座</option>
              <option :value="3">插座座</option>
            </select>
          </div>
          <button class="tool-btn" :class="{ active: mode === 'text' }" @click="setMode('text')" title="添加文字">
            <span class="tool-icon">🔤</span><span class="tool-name">文字</span>
          </button>
        </div>
        <div class="tool-divider"></div>
        <div class="tool-group">
          <label class="tool-btn upload-btn" title="上传楼层底图">
            <span class="tool-icon">🖼️</span><span class="tool-name">底图</span>
            <input type="file" accept="image/*" class="hidden-file" @change="handleBgUpload" />
          </label>
          <button class="tool-btn danger" @click="deleteSelected" :disabled="!hasSelection" title="删除选中 (Delete)">
            <span class="tool-icon">🗑️</span><span class="tool-name">删除</span>
          </button>
          <button class="tool-btn" @click="duplicateSelected" :disabled="!hasSelection" title="复制选中 (Ctrl+D)">
            <span class="tool-icon">📋</span><span class="tool-name">复制</span>
          </button>
        </div>
        <div class="tool-divider"></div>
        <div class="tool-hint">
          <p>💡 提示</p>
          <p>· 滚轮缩放，空格+拖拽平移</p>
          <p>· 双击文字可编辑</p>
          <p>· Ctrl+Z 撤销</p>
          <p>· 发布时自动同步座位</p>
        </div>
      </div>

      <div class="canvas-wrap" ref="canvasWrapEl">
        <div v-if="!selectedFloorId" class="canvas-empty"><p>请先选择要编辑的楼层</p></div>
        <canvas ref="canvasEl"></canvas>
      </div>

      <div class="prop-panel">
        <div v-if="!selected" class="prop-empty">
          <p>🏷️ 未选中对象</p>
          <p class="prop-sub">选择画布中的元素后可编辑属性</p>
        </div>
        <div v-else class="prop-content">
          <h4 class="prop-title">{{ propTitle }}</h4>
          <div class="prop-row"><label>X</label><input type="number" v-model.number="propX" @change="applyProp('x')" /></div>
          <div class="prop-row"><label>Y</label><input type="number" v-model.number="propY" @change="applyProp('y')" /></div>
          <div class="prop-row" v-if="selected.dataType === 'seat'"><label>旋转</label><input type="number" v-model.number="propRotation" @change="applyProp('rotation')" /></div>
          <div class="prop-sep"></div>
          <template v-if="selected.dataType === 'area'">
            <div class="prop-row"><label>名称</label><input v-model="propName" @change="applyProp('name')" /></div>
            <div class="prop-row"><label>宽度</label><input type="number" v-model.number="propW" @change="applyProp('w')" /></div>
            <div class="prop-row"><label>高度</label><input type="number" v-model.number="propH" @change="applyProp('h')" /></div>
            <div class="prop-row"><label>填充色</label><input type="color" v-model="propFill" @change="applyProp('fill')" /></div>
            <div class="prop-row"><label>边框色</label><input type="color" v-model="propStroke" @change="applyProp('stroke')" /></div>
          </template>
          <template v-else-if="selected.dataType === 'seat'">
            <div class="prop-row"><label>编号</label><input v-model="propSeatNo" @change="applyProp('seatNo')" /></div>
            <div class="prop-row">
              <label>类型</label>
              <select v-model.number="propSeatType" @change="applyProp('seatType')">
                <option :value="1">普通座</option><option :value="2">靠窗座</option><option :value="3">插座座</option>
              </select>
            </div>
            <div class="prop-row">
              <label>所属区域</label>
              <select v-model="propAreaId" @change="applyProp('areaId')">
                <option :value="null">未分配</option>
                <option v-for="a in areas" :key="a.id" :value="a.id">{{ a.area_name || a.areaName }}</option>
              </select>
            </div>
            <div class="prop-row">
              <label>状态</label>
              <select v-model.number="propSeatStatus" @change="applyProp('seatStatus')">
                <option :value="0">正常</option><option :value="3">维修中</option>
              </select>
            </div>
            <div class="prop-row"><label>尺寸</label><input type="number" v-model.number="propSeatSize" @change="applyProp('seatSize')" /></div>
            <div class="prop-note" v-if="selected.data?.seatId">已关联数据库座位 #{{ selected.data.seatId }}</div>
            <div class="prop-note" v-else>⚠️ 新座位，发布后自动入库</div>
          </template>
          <template v-else-if="selected.dataType === 'wall' || selected.dataType === 'path'">
            <div class="prop-row"><label>粗细</label><input type="number" v-model.number="propThickness" @change="applyProp('thickness')" /></div>
            <div class="prop-row"><label>颜色</label><input type="color" v-model="propColor" @change="applyProp('color')" /></div>
          </template>
          <template v-else-if="selected.dataType === 'text'">
            <div class="prop-row"><label>内容</label><input v-model="propContent" @change="applyProp('content')" /></div>
            <div class="prop-row"><label>字号</label><input type="number" v-model.number="propFontSize" @change="applyProp('fontSize')" /></div>
            <div class="prop-row"><label>颜色</label><input type="color" v-model="propColor" @change="applyProp('color')" /></div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { Canvas, Rect, Line, Textbox, Group, PencilBrush, FabricImage, Point, Path } from 'fabric';
import { getFloors, getAreas } from '@/api/seat';
import { getDraftLayout, saveDraftLayout, publishLayout } from '@/api/floorLayout';

const emit = defineEmits(['toast']);

// ---------- 基础状态 ----------
const canvasEl = ref(null);
const canvasWrapEl = ref(null);
const floors = ref([]);
const areas = ref([]);
const selectedFloorId = ref(null);
const saving = ref(false);
const mode = ref('select');
const seatType = ref(1);
const zoom = ref(1);
const floorNumber = ref(1);

let canvas = null;
let sceneW = 1600;
let sceneH = 1100;
let bgImageSrc = '';
let historyStack = [];
let historyIndex = -1;
let historyTimer = null;
let isSpaceDown = false;
let isPanning = false;
let panStart = null;
let drawStart = null;
let tmpDrawObj = null;
let loadingLock = false;

// ---------- 选中对象与属性面板 ----------
const selected = ref(null);
const propX = ref(0); const propY = ref(0); const propRotation = ref(0);
const propName = ref(''); const propW = ref(0); const propH = ref(0);
const propFill = ref('#667eea'); const propStroke = ref('#667eea');
const propSeatNo = ref(''); const propSeatType = ref(1); const propAreaId = ref(null);
const propSeatStatus = ref(0); const propSeatSize = ref(44);
const propThickness = ref(8); const propColor = ref('#888888');
const propContent = ref(''); const propFontSize = ref(24);

const hasSelection = computed(() => {
  if (!canvas) return false;
  const objs = canvas.getActiveObjects();
  return objs && objs.length > 0;
});
const canUndo = computed(() => historyIndex > 0);
const canRedo = computed(() => historyIndex < historyStack.length - 1);
const propTitle = computed(() => {
  if (!selected.value) return '';
  const map = { area: '区域属性', seat: '座位属性', wall: '墙体属性', path: '画笔属性', text: '文字属性' };
  return map[selected.value.dataType] || '对象属性';
});

// ---------- 工具函数 ----------
const uid = (p) => p + '-' + Date.now().toString(36) + Math.random().toString(36).slice(2, 6);
const toast = (msg, type = 'info') => emit('toast', { message: msg, type });
const getObjData = (obj) => obj?.data || {};

// ---------- 楼层与区域 ----------
const loadFloors = async () => {
  try {
    const res = await getFloors();
    let data = res;
    if (data?.code === 200) data = data.data;
    floors.value = Array.isArray(data) ? data : [];
    if (floors.value.length && !selectedFloorId.value) {
      selectedFloorId.value = floors.value[0].id;
    }
    if (selectedFloorId.value) await onFloorChange();
  } catch (e) {
    toast('楼层加载失败: ' + (e.message || ''), 'error');
  }
};

const loadAreas = async () => {
  if (!selectedFloorId.value) { areas.value = []; return; }
  try {
    const res = await getAreas(selectedFloorId.value);
    let data = res;
    if (data?.code === 200) data = data.data;
    areas.value = Array.isArray(data) ? data : [];
  } catch (e) {
    areas.value = [];
  }
};

const onFloorChange = async () => {
  await loadAreas();
  const f = floors.value.find(x => x.id === selectedFloorId.value);
  floorNumber.value = f ? Number(f.floor_number ?? f.floorNumber ?? 1) : 1;
  if (!canvas) return;
  try {
    const res = await getDraftLayout(selectedFloorId.value);
    let data = res;
    if (data?.code === 200) data = data.data;
    if (data && data.layoutJson) {
      await loadBusinessJson(data.layoutJson);
    } else {
      clearCanvas();
    }
  } catch (e) {
    clearCanvas();
  }
};

// ---------- 画布初始化 ----------
const applyZoom = (z) => {
  z = Math.min(3, Math.max(0.2, z));
  zoom.value = z;
  canvas.setZoom(1);
  canvas.setDimensions({ width: sceneW * z, height: sceneH * z }, { cssOnly: true });
  canvas.requestRenderAll();
};

const zoomIn = () => applyZoom(zoom.value * 1.2);

// 统一管理场景尺寸与缩放：backing store 用场景尺寸，CSS 尺寸随缩放变化
const applySceneSize = () => {
  canvas.setDimensions({ width: sceneW, height: sceneH }, { backstoreOnly: true });
  applyZoom(zoom.value);
};
const fitAfterLoad = () => setTimeout(fitView, 100);
const zoomOut = () => applyZoom(zoom.value / 1.2);
const fitView = () => {
  const wrap = canvasWrapEl.value;
  if (!wrap) return;
  const w = wrap.clientWidth - 40;
  const h = wrap.clientHeight - 40;
  const z = Math.min(w / sceneW, h / sceneH, 1.2);
  applyZoom(Math.max(0.2, z));
};

const initCanvas = () => {
  canvas = new Canvas(canvasEl.value, {
    width: sceneW,
    height: sceneH,
    backgroundColor: '#12121a',
    selection: true,
    preserveObjectStacking: true
  });
  canvas.freeDrawingBrush = new PencilBrush(canvas);
  canvas.freeDrawingBrush.width = 5;
  canvas.freeDrawingBrush.color = '#999999';
  window.__canvas = canvas; // debug
  canvas.on('mouse:down', onMouseDown);
  canvas.on('mouse:move', onMouseMove);
  canvas.on('mouse:up', onMouseUp);
  canvas.on('mouse:wheel', onWheel);
  canvas.on('mouse:down:before', (opt) => {
    if (isSpaceDown || mode.value === 'pan') {
      isPanning = true;
      panStart = { x: opt.e.clientX, y: opt.e.clientY };
    }
  });

  canvas.on('selection:created', onSelectionChange);
  canvas.on('selection:updated', onSelectionChange);
  canvas.on('selection:cleared', onSelectionClear);

  canvas.on('object:modified', onObjectModified);
  canvas.on('object:added', onObjectAdded);
  canvas.on('object:removed', onObjectRemoved);

  window.addEventListener('keydown', onKeyDown);
  window.addEventListener('keyup', onKeyUp);

  setTimeout(fitView, 100);
  if (selectedFloorId.value) onFloorChange();
};

const disposeCanvas = () => {
  if (canvas) {
    canvas.dispose();
    canvas = null;
  }
  window.removeEventListener('keydown', onKeyDown);
  window.removeEventListener('keyup', onKeyUp);
};
// ---------- 鼠标事件 ----------
const onMouseDown = (opt) => {
  if (mode.value === 'pan' || isSpaceDown) return;
  if (mode.value === 'seat') { addSeatAt(opt.scenePoint); return; }
  if (mode.value === 'text') { addTextAt(opt.scenePoint); return; }
  if (mode.value === 'area') {
    drawStart = { x: opt.scenePoint.x, y: opt.scenePoint.y };
    tmpDrawObj = new Rect({
      left: drawStart.x, top: drawStart.y, width: 1, height: 1,
      fill: 'rgba(102,126,234,0.38)', stroke: '#667eea', strokeWidth: 2, rx: 6, ry: 6,
      originX: 'left', originY: 'top',
      dataType: 'area', data: { id: uid('a'), name: '新区域' }
    });
    canvas.add(tmpDrawObj);
    return;
  }
  if (mode.value === 'wall') {
    drawStart = { x: opt.scenePoint.x, y: opt.scenePoint.y };
    tmpDrawObj = new Line([drawStart.x, drawStart.y, drawStart.x, drawStart.y], {
      stroke: '#8a8a96', strokeWidth: 8,
      originX: 'left', originY: 'top',
      dataType: 'wall', data: { id: uid('w') }
    });
    canvas.add(tmpDrawObj);
  }
};

const onMouseMove = (opt) => {
  if (isPanning) {
    const dx = opt.e.clientX - panStart.x;
    const dy = opt.e.clientY - panStart.y;
    panStart = { x: opt.e.clientX, y: opt.e.clientY };
    const vpt = canvas.viewportTransform;
    vpt[4] += dx;
    vpt[5] += dy;
    canvas.requestRenderAll();
    return;
  }
  if (!tmpDrawObj) return;
  const p = opt.scenePoint;
  if (mode.value === 'area') {
    const left = Math.min(drawStart.x, p.x);
    const top = Math.min(drawStart.y, p.y);
    tmpDrawObj.set({ left, top, width: Math.abs(p.x - drawStart.x), height: Math.abs(p.y - drawStart.y) });
  } else if (mode.value === 'wall') {
    tmpDrawObj.set({ x2: p.x, y2: p.y });
  }
  tmpDrawObj.setCoords();
  canvas.requestRenderAll();
};

const onMouseUp = () => {
  isPanning = false;
  if (tmpDrawObj) {
    if (mode.value === 'area' && (tmpDrawObj.width < 20 || tmpDrawObj.height < 20)) {
      canvas.remove(tmpDrawObj);
    } else {
      tmpDrawObj.setCoords();
      recordHistory();
      canvas.setActiveObject(tmpDrawObj);
    }
    tmpDrawObj = null;
    drawStart = null;
  }
};

const onWheel = (opt) => {
  const e = opt.e;
  if (!e.ctrlKey && !e.metaKey) return;
  e.preventDefault();
  e.stopPropagation();
  const delta = e.deltaY > 0 ? -1 : 1;
  const z = Math.min(3, Math.max(0.2, zoom.value * Math.pow(1.1, delta)));
  applyZoom(z);
};

// ---------- 选中与属性 ----------
const onSelectionChange = (opt) => {
  const objs = opt.selected || [];
  if (objs.length !== 1) { selected.value = null; return; }
  syncPropPanel(objs[0]);
};
const onSelectionClear = () => { selected.value = null; };

const syncPropPanel = (obj) => {
  const data = getObjData(obj);
  selected.value = { obj, dataType: obj.dataType, data };
  propX.value = Math.round(obj.left ?? 0);
  propY.value = Math.round(obj.top ?? 0);
  propRotation.value = Math.round(obj.angle || 0);
  if (obj.dataType === 'area') {
    propName.value = data.name || '';
    propW.value = Math.round(obj.width * (obj.scaleX || 1));
    propH.value = Math.round(obj.height * (obj.scaleY || 1));
    propFill.value = obj.fill || '#667eea';
    propStroke.value = obj.stroke || '#667eea';
  } else if (obj.dataType === 'seat') {
    propSeatNo.value = data.seatNo || '';
    propSeatType.value = data.seatType || 1;
    propAreaId.value = data.areaId ?? null;
    propSeatStatus.value = data.status ?? 0;
    const rect = obj.getObjects ? obj.getObjects()[0] : null;
    propSeatSize.value = Math.round((rect ? rect.width : 44) * (obj.scaleX || 1));
  } else if (obj.dataType === 'wall' || obj.dataType === 'path') {
    propThickness.value = obj.strokeWidth || 8;
    propColor.value = obj.stroke || '#888888';
  } else if (obj.dataType === 'text') {
    propContent.value = obj.text || '';
    propFontSize.value = obj.fontSize || 24;
    propColor.value = obj.fill || '#e8e8ec';
  }
};

const applyProp = (key) => {
  const obj = selected.value?.obj;
  if (!obj) return;
  const data = getObjData(obj);
  switch (key) {
    case 'x': obj.set({ left: propX.value }); break;
    case 'y': obj.set({ top: propY.value }); break;
    case 'rotation': obj.set({ angle: propRotation.value }); break;
    case 'name': data.name = propName.value; break;
    case 'w': obj.set({ width: propW.value, scaleX: 1 }); break;
    case 'h': obj.set({ height: propH.value, scaleY: 1 }); break;
    case 'fill': obj.set({ fill: propFill.value }); break;
    case 'stroke': obj.set({ stroke: propStroke.value }); break;
    case 'seatNo': {
      data.seatNo = propSeatNo.value;
      const label = obj.getObjects ? obj.getObjects()[1] : null;
      if (label) label.set({ text: propSeatNo.value });
      break;
    }
    case 'seatType': data.seatType = propSeatType.value; refreshSeatFill(obj); break;
    case 'areaId': data.areaId = propAreaId.value; break;
    case 'seatStatus': data.status = propSeatStatus.value; refreshSeatFill(obj); break;
    case 'seatSize': {
      const rect = obj.getObjects ? obj.getObjects()[0] : null;
      if (rect) {
        rect.set({ width: propSeatSize.value, height: propSeatSize.value });
        const label = obj.getObjects ? obj.getObjects()[1] : null;
        if (label) label.set({ width: propSeatSize.value, left: -propSeatSize.value / 2, top: -propSeatSize.value / 2 + 5 });
        obj.set({ scaleX: 1, scaleY: 1 });
      }
      break;
    }
    case 'thickness': obj.set({ strokeWidth: propThickness.value }); break;
    case 'color':
      obj.set({ stroke: propColor.value });
      if (obj.dataType === 'text') obj.set({ fill: propColor.value });
      break;
    case 'content': obj.set({ text: propContent.value }); data.content = propContent.value; break;
    case 'fontSize': obj.set({ fontSize: propFontSize.value }); data.fontSize = propFontSize.value; break;
  }
  obj.setCoords();
  canvas.requestRenderAll();
  recordHistory();
};
// ---------- 座位 ----------
const seatColor = (type) => {
  return type === 2 ? '#3d6fd1' : type === 3 ? '#7a5cd6' : '#2f7d4f';
};
const refreshSeatFill = (group) => {
  const rect = group.getObjects ? group.getObjects()[0] : null;
  if (!rect) return;
  const data = getObjData(group);
  rect.set({ fill: data.status === 3 ? '#6b6b76' : seatColor(data.seatType) });
};

const nextSeatNo = () => {
  const used = new Set();
  canvas.getObjects().forEach(o => {
    if (o.dataType === 'seat' && getObjData(o).seatNo) used.add(getObjData(o).seatNo);
  });
  let n = 1;
  while (true) {
    const no = floorNumber.value + '-' + String(n).padStart(2, '0');
    if (!used.has(no)) return no;
    n++;
  }
};

const addSeatAt = (point) => {
  const size = 44;
  const seatNo = nextSeatNo();
  const rect = new Rect({
    left: 0, top: 0, width: size, height: size,
    fill: seatColor(seatType.value), stroke: '#d8d8e0', strokeWidth: 2, rx: 6, ry: 6
  });
  const label = new Textbox(seatNo, {
    left: -size / 2, top: -size / 2 + 5, width: size,
    fontSize: 11, fill: '#ffffff', textAlign: 'center',
    selectable: false, evented: false
  });
  const group = new Group([rect, label], {
    left: Math.round(point.x - size / 2),
    originX: 'left', originY: 'top',
    top: Math.round(point.y - size / 2),
    dataType: 'seat',
    data: { id: uid('s'), seatId: null, seatNo, seatType: seatType.value, areaId: null, status: 0 }
  });
  canvas.add(group);
  canvas.setActiveObject(group);
  canvas.requestRenderAll();
  recordHistory();
};

const addTextAt = (point) => {
  const tb = new Textbox('双击编辑文字', {
    left: point.x, top: point.y,
    fontSize: 24, fill: '#e8e8ec', width: 180,
    originX: 'left', originY: 'top',
    dataType: 'text', data: { id: uid('t'), content: '双击编辑文字' }
  });
  canvas.add(tb);
  canvas.setActiveObject(tb);
  canvas.requestRenderAll();
  recordHistory();
};

// ---------- 底图 ----------
const handleBgUpload = (e) => {
  const file = e.target.files?.[0];
  e.target.value = '';
  if (!file) return;
  const reader = new FileReader();
  reader.onload = async () => {
    const dataUrl = reader.result;
    try {
      const img = await FabricImage.fromURL(dataUrl);
      const scale = Math.min(1, 2000 / img.width, 1400 / img.height);
      const finalUrl = await compressImage(dataUrl, Math.round(img.width * scale), Math.round(img.height * scale));
      const img2 = await FabricImage.fromURL(finalUrl);
      sceneW = Math.max(sceneW, Math.round(img2.width));
      sceneH = Math.max(sceneH, Math.round(img2.height));
      img2.set({ left: 0, top: 0, originX: 'left', originY: 'top', selectable: false, evented: false, dataType: 'bg', data: { src: finalUrl } });
      const oldBg = canvas.getObjects().find(o => o.dataType === 'bg');
      if (oldBg) canvas.remove(oldBg);
      canvas.add(img2);
      img2.sendToBack();
      bgImageSrc = finalUrl;
      applySceneSize();
      recordHistory();
      toast('底图已上传', 'success');
    } catch (err) {
      toast('底图加载失败', 'error');
    }
  };
  reader.readAsDataURL(file);
};

const compressImage = (dataUrl, w, h) => {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => {
      const c = document.createElement('canvas');
      c.width = w; c.height = h;
      c.getContext('2d').drawImage(img, 0, 0, w, h);
      try { resolve(c.toDataURL('image/jpeg', 0.85)); }
      catch (err) { resolve(dataUrl); }
    };
    img.onerror = () => resolve(dataUrl);
    img.src = dataUrl;
  });
};

// ---------- 删除 / 复制 ----------
const deleteSelected = () => {
  const objs = canvas.getActiveObjects();
  if (!objs.length) return;
  objs.forEach(o => canvas.remove(o));
  canvas.discardActiveObject();
  selected.value = null;
  canvas.requestRenderAll();
  recordHistory();
};

const duplicateSelected = () => {
  const objs = canvas.getActiveObjects();
  if (!objs.length) return;
  const clones = [];
  objs.forEach(o => {
    const clone = o.clone();
    clone.dataType = o.dataType;
        clone.set({ left: (o.left || 0) + 30, top: (o.top || 0) + 30 });
    const data = { ...getObjData(o), id: uid('o') };
    if (o.dataType === 'seat') {
      data.seatId = null;
      data.seatNo = nextSeatNo();
      clone.data = data;
      const label = clone.getObjects ? clone.getObjects()[1] : null;
      if (label) label.set({ text: data.seatNo });
    } else {
      clone.data = data;
    }
    clone.setCoords();
    canvas.add(clone);
    clones.push(clone);
  });
  canvas.discardActiveObject();
  if (clones.length === 1) canvas.setActiveObject(clones[0]);
  canvas.requestRenderAll();
  recordHistory();
};

// ---------- 历史记录（撤销/重做） ----------
const recordHistory = () => {
  if (loadingLock) return;
  if (historyTimer) clearTimeout(historyTimer);
  historyTimer = setTimeout(() => {
    const json = toBusinessJson();
    historyStack = historyStack.slice(0, historyIndex + 1);
    historyStack.push(json);
    if (historyStack.length > 60) historyStack.shift();
    historyIndex = historyStack.length - 1;
  }, 200);
};

const undo = async () => {
  if (historyIndex > 0) { historyIndex--; await loadBusinessJson(historyStack[historyIndex]); }
};
const redo = async () => {
  if (historyIndex < historyStack.length - 1) { historyIndex++; await loadBusinessJson(historyStack[historyIndex]); }
};
// ---------- 业务 JSON 转换 ----------
const toBusinessJson = () => {
  const areasArr = [], walls = [], paths = [], seats = [], texts = [];
  canvas.getObjects().forEach(o => {
    if (o.dataType === 'area') {
      const data = getObjData(o);
      areasArr.push({
        id: data.id, name: data.name || '', x: Math.round(o.left), y: Math.round(o.top),
        w: Math.round(o.width * (o.scaleX || 1)), h: Math.round(o.height * (o.scaleY || 1)),
        fill: o.fill, stroke: o.stroke
      });
    } else if (o.dataType === 'wall') {
      const data = getObjData(o);
      walls.push({
        id: data.id, x1: Math.round(o.x1), y1: Math.round(o.y1),
        x2: Math.round(o.x2), y2: Math.round(o.y2),
        thickness: o.strokeWidth, color: o.stroke
      });
    } else if (o.dataType === 'path') {
      const data = getObjData(o);
      const pts = (o.path || []).map(p => [Math.round(p[p.length - 2]), Math.round(p[p.length - 1])]);
      paths.push({ id: data.id, points: pts, color: o.stroke, thickness: o.strokeWidth });
    } else if (o.dataType === 'seat') {
      const data = getObjData(o);
      const rect = o.getObjects ? o.getObjects()[0] : null;
      seats.push({
        id: data.id, seatId: data.seatId ?? null, seatNo: data.seatNo || '',
        seatType: data.seatType ?? 1, areaId: data.areaId ?? null,
        x: Math.round(o.left), y: Math.round(o.top),
        w: Math.round((rect ? rect.width : 44) * (o.scaleX || 1)),
        h: Math.round((rect ? rect.height : 44) * (o.scaleY || 1)),
        rotation: Math.round(o.angle || 0),
        status: data.status ?? 0
      });
    } else if (o.dataType === 'text') {
      const data = getObjData(o);
      texts.push({
        id: data.id, content: o.text || '', x: Math.round(o.left), y: Math.round(o.top),
        fontSize: o.fontSize, color: o.fill
      });
    }
  });
  return {
    canvas: { width: sceneW, height: sceneH, bgImage: bgImageSrc },
    areas: areasArr, walls, paths, seats, texts
  };
};

const clearCanvas = async () => {
  loadingLock = true;
  canvas.clear();
  applySceneSize();
  fitAfterLoad();
  bgImageSrc = '';
  historyStack = [];
  historyIndex = -1;
  selected.value = null;
  canvas.requestRenderAll();
  loadingLock = false;
};

const loadBusinessJson = async (jsonStr) => {
  loadingLock = true;
  try {
    const json = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr;
    canvas.clear();
    bgImageSrc = '';

    const c = json.canvas || {};
    if (c.width) sceneW = Number(c.width);
    if (c.height) sceneH = Number(c.height);
    if (c.bgImage) {
      try {
        const img = await FabricImage.fromURL(c.bgImage);
        img.set({ left: 0, top: 0, originX: 'left', originY: 'top', selectable: false, evented: false, dataType: 'bg', data: { src: c.bgImage } });
        canvas.add(img);
        img.sendToBack();
        bgImageSrc = c.bgImage;
      } catch (e) {
        console.warn('底图加载失败', e);
      }
    }

    (json.areas || []).forEach(a => {
      const rect = new Rect({
        left: a.x, top: a.y, width: a.w, height: a.h,
        fill: a.fill || 'rgba(102,126,234,0.38)', stroke: a.stroke || '#667eea',
        originX: 'left', originY: 'top',
        strokeWidth: 2, rx: 6, ry: 6,
        dataType: 'area', data: { id: a.id || uid('a'), name: a.name || '' }
      });
      canvas.add(rect);
    });

    (json.walls || []).forEach(w => {
      canvas.add(new Line([w.x1, w.y1, w.x2, w.y2], {
        stroke: w.color || '#8a8a96', strokeWidth: w.thickness || 8,
        originX: 'left', originY: 'top',
        dataType: 'wall', data: { id: w.id || uid('w') }
      }));
    });

    (json.paths || []).forEach(p => {
      if (!p.points || !p.points.length) return;
      const pathStr = p.points.map((pt, i) => (i === 0 ? 'M ' : 'L ') + pt[0] + ' ' + pt[1]).join(' ');
      canvas.add(new Path(pathStr, {
        stroke: p.color || '#999999', strokeWidth: p.thickness || 5, fill: '',
        dataType: 'path', data: { id: p.id || uid('p') }
      }));
    });

    (json.seats || []).forEach(s => {
      const size = s.w || 44;
      const rect = new Rect({
        left: 0, top: 0, width: size, height: size,
        fill: seatColor(s.seatType || 1), stroke: '#d8d8e0', strokeWidth: 2, rx: 6, ry: 6
      });
      const label = new Textbox(s.seatNo || '', {
        left: -size / 2, top: -size / 2 + 5, width: size,
        fontSize: 11, fill: '#ffffff', textAlign: 'center',
        selectable: false, evented: false
      });
      const group = new Group([rect, label], {
        left: s.x, top: s.y, angle: s.rotation || 0,
        originX: 'left', originY: 'top',
        dataType: 'seat',
        data: { id: s.id || uid('s'), seatId: s.seatId ?? null, seatNo: s.seatNo || '', seatType: s.seatType || 1, areaId: s.areaId ?? null, status: s.status ?? 0 }
      });
      if (s.status === 3) refreshSeatFill(group);
      canvas.add(group);
    });

    (json.texts || []).forEach(t => {
      canvas.add(new Textbox(t.content || '', {
        left: t.x, top: t.y, fontSize: t.fontSize || 24, fill: t.color || '#e8e8ec', width: 200,
        originX: 'left', originY: 'top',
        dataType: 'text', data: { id: t.id || uid('t'), content: t.content || '' }
      }));
    });

    applySceneSize();
    fitAfterLoad();
    canvas.requestRenderAll();

    historyStack = [json];
    historyIndex = 0;
  } catch (e) {
    console.error('加载结构图失败', e);
    toast('结构图加载失败', 'error');
  } finally {
    loadingLock = false;
  }
};
// ---------- 保存 / 发布 ----------
const handleSaveDraft = async () => {
  if (!selectedFloorId.value) { toast('请先选择楼层', 'warning'); return; }
  saving.value = true;
  try {
    await saveDraftLayout(selectedFloorId.value, JSON.stringify(toBusinessJson()));
    toast('草稿已保存', 'success');
  } catch (e) {
    toast('保存失败: ' + (e.response?.data?.message || e.message || ''), 'error');
  } finally {
    saving.value = false;
  }
};

const handlePublish = async () => {
  if (!selectedFloorId.value) { toast('请先选择楼层', 'warning'); return; }
  if (!confirm('发布后普通用户即可看到该楼层的结构图，座位数据将同步到数据库，确定发布吗？')) return;
  saving.value = true;
  try {
    const res = await publishLayout(selectedFloorId.value, JSON.stringify(toBusinessJson()));
    let data = res;
    if (data?.code === 200) data = data.data;
    const warnings = data?.warnings || [];
    if (warnings && warnings.length) {
      toast('发布成功，但有警告：' + warnings.join('；'), 'warning');
    } else {
      toast('发布成功！', 'success');
    }
    if (data?.layoutJson) {
      await loadBusinessJson(data.layoutJson);
    }
  } catch (e) {
    toast('发布失败: ' + (e.response?.data?.message || e.message || ''), 'error');
  } finally {
    saving.value = false;
  }
};

// ---------- 键盘 ----------
const onKeyDown = (e) => {
  if (!canvas) return;
  const tag = document.activeElement?.tagName;
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;
  if (e.code === 'Space') {
    isSpaceDown = true;
    canvas.defaultCursor = 'grab';
    canvas.skipTargetFind = true;
  }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z') {
    e.preventDefault();
    if (e.shiftKey) redo(); else undo();
  }
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'd') {
    e.preventDefault();
    duplicateSelected();
  }
  if (e.key === 'Delete' || e.key === 'Backspace') deleteSelected();
  const k = e.key.toLowerCase();
  if (k === 'v') setMode('select');
  if (k === 'h') setMode('pan');
  if (k === 'p') setMode('path');
  if (k === 't') setMode('text');
};

const onKeyUp = (e) => {
  if (e.code === 'Space') {
    isSpaceDown = false;
    if (canvas) { canvas.defaultCursor = 'default'; canvas.skipTargetFind = false; }
  }
};

const setMode = (m) => {
  mode.value = m;
  if (!canvas) return;
  if (m === 'path') {
    canvas.isDrawingMode = true;
    canvas.selection = false;
    canvas.skipTargetFind = true;
    selected.value = null;
  } else if (m === 'pan') {
    canvas.isDrawingMode = false;
    canvas.selection = false;
    canvas.skipTargetFind = true;
    canvas.defaultCursor = 'grab';
    canvas.discardActiveObject();
    selected.value = null;
  } else {
    canvas.isDrawingMode = false;
    canvas.selection = true;
    canvas.skipTargetFind = false;
    canvas.defaultCursor = 'default';
  }
};

const onObjectModified = () => {
  if (selected.value) syncPropPanel(selected.value.obj);
  recordHistory();
};
const onObjectAdded = (opt) => {
  const obj = opt.target;
  if (obj.type === 'path' && !obj.dataType && !tmpDrawObj) {
    obj.set({ dataType: 'path', data: { id: uid('p') } });
    recordHistory();
  }
};
const onObjectRemoved = () => recordHistory();

// ---------- 生命周期 ----------
onMounted(async () => {
  initCanvas();
  await loadFloors();
});

onBeforeUnmount(() => {
  disposeCanvas();
});
</script>
<style scoped>
.layout-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 73px);
  background: var(--bg-primary, #0d0d12);
  color: var(--text-primary, #e8e8ec);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
.editor-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-color, rgba(255,255,255,0.12));
  background: var(--sidebar-bg, #16161c);
  flex-shrink: 0;
  gap: 12px;
  flex-wrap: wrap;
}
.topbar-left { display: flex; align-items: center; gap: 10px; }
.topbar-label { font-size: 13px; color: var(--text-secondary, #9a9aa5); }
.floor-select {
  background: var(--sidebar-hover, rgba(255,255,255,0.08));
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  color: var(--text-primary, #e8e8ec);
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 14px;
}
.topbar-right { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.tb-btn {
  background: var(--sidebar-hover, rgba(255,255,255,0.08));
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  color: var(--text-primary, #e8e8ec);
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}
.tb-btn:hover:not(:disabled) { background: var(--sidebar-active, rgba(102,126,234,0.25)); }
.tb-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.tb-btn.primary { background: linear-gradient(135deg, #667eea, #764ba2); border: none; color: #fff; }
.tb-btn.publish { background: linear-gradient(135deg, #2ecc71, #27ae60); border: none; color: #fff; }
.tb-sep { width: 1px; height: 22px; background: var(--border-color, rgba(255,255,255,0.12)); margin: 0 4px; }
.tb-zoom { font-size: 13px; color: var(--text-secondary, #9a9aa5); min-width: 46px; text-align: center; }

.editor-body { display: flex; flex: 1; min-height: 0; }
.tool-panel {
  width: 92px;
  background: var(--sidebar-bg, #16161c);
  border-right: 1px solid var(--border-color, rgba(255,255,255,0.12));
  padding: 12px 8px;
  overflow-y: auto;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.tool-group { display: flex; flex-direction: column; gap: 4px; }
.tool-divider { height: 1px; background: var(--border-color, rgba(255,255,255,0.12)); margin: 4px 0; }
.tool-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 8px 4px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: none;
  color: var(--text-secondary, #9a9aa5);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 12px;
}
.tool-btn:hover { background: var(--sidebar-hover, rgba(255,255,255,0.08)); color: var(--text-primary, #e8e8ec); }
.tool-btn.active { background: var(--sidebar-active, rgba(102,126,234,0.25)); color: #fff; border-color: rgba(102,126,234,0.5); }
.tool-btn.danger:hover { background: rgba(244,67,54,0.2); color: #f44336; }
.tool-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.tool-icon { font-size: 20px; }
.tool-name { font-size: 11px; }
.upload-btn { position: relative; overflow: hidden; }
.hidden-file { display: none; }
.seat-type-row { padding: 2px 0 6px; }
.mini-select {
  width: 100%;
  background: var(--sidebar-hover, rgba(255,255,255,0.08));
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  color: var(--text-primary, #e8e8ec);
  border-radius: 6px;
  padding: 3px 2px;
  font-size: 11px;
}
.tool-hint {
  margin-top: auto;
  font-size: 11px;
  color: var(--text-secondary, #9a9aa5);
  line-height: 1.7;
  padding: 8px 4px;
  border-top: 1px solid var(--border-color, rgba(255,255,255,0.12));
}
.tool-hint p { margin: 0; }

.canvas-wrap {
  flex: 1;
  overflow: auto;
  padding: 16px;
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  min-width: 0;
}
.canvas-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary, #9a9aa5);
  font-size: 16px;
}
.canvas-wrap :deep(canvas) {
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  border-radius: 4px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.4);
}
/* 网格与底色只应用于 lower-canvas（真正的内容渲染层），避免遮挡 */
.canvas-wrap :deep(canvas.lower-canvas) {
  background-image:
    linear-gradient(rgba(255,255,255,0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.04) 1px, transparent 1px);
  background-size: 24px 24px;
  background-color: #12121a;
}
/* upper-canvas 覆盖在内容层之上，必须保持透明，否则会挡住所有图形 */
.canvas-wrap :deep(canvas.upper-canvas) {
  background: transparent;
}

.prop-panel {
  width: 240px;
  background: var(--sidebar-bg, #16161c);
  border-left: 1px solid var(--border-color, rgba(255,255,255,0.12));
  padding: 14px;
  overflow-y: auto;
  flex-shrink: 0;
}
.prop-empty { color: var(--text-secondary, #9a9aa5); font-size: 13px; text-align: center; margin-top: 40px; }
.prop-empty p { margin: 4px 0; }
.prop-sub { font-size: 12px; opacity: 0.8; }
.prop-title { margin: 0 0 12px; font-size: 14px; color: var(--text-primary, #e8e8ec); }
.prop-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; gap: 8px; }
.prop-row label { font-size: 12px; color: var(--text-secondary, #9a9aa5); flex-shrink: 0; min-width: 44px; }
.prop-row input[type='text'],
.prop-row input[type='number'],
.prop-row select {
  flex: 1;
  min-width: 0;
  background: var(--sidebar-hover, rgba(255,255,255,0.08));
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  color: var(--text-primary, #e8e8ec);
  padding: 5px 8px;
  border-radius: 6px;
  font-size: 13px;
}
.prop-row input[type='color'] {
  width: 44px; height: 28px; padding: 2px;
  border: 1px solid var(--border-color, rgba(255,255,255,0.12));
  border-radius: 6px; background: none; cursor: pointer;
}
.prop-sep { height: 1px; background: var(--border-color, rgba(255,255,255,0.12)); margin: 10px 0; }
.prop-note { font-size: 11px; color: #f39c12; margin-top: 8px; line-height: 1.5; }
</style>
