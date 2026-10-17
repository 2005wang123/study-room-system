<!--
  ============================================================================
  PetCompanion.vue —— 自习室桌面宠物（独立组件，可直接复制到其他项目使用）
  ----------------------------------------------------------------------------
  两个角色：夜读精灵「小夜」 / 晨读精灵「小晨」，可随时切换。
  能力：待机呼吸、定点散步、拖拽、点击互动、双击休眠、悬停搭话、
        右键菜单、键盘操作、状态本地记忆、外部指令 API。

  ■ 想改台词 / 立绘 / 主题色 → 改下方「1. 角色配置」区域的 PERSONAS。
    每个角色有 `sprite`（睁眼）与 `sleepSprite`（睡眠闭眼）两张立绘，直接替换图片即可。
  ■ 想调柔光强弱 → 改 PERSONAS 里的 `halo`；想彻底去掉用右键菜单「角色柔光」，
    或给组件传 `:glow="false"`。
  ■ 想加新动作 → 在「3. 状态」加字段，模板里加 class，样式区写 @keyframes。
  ■ 想从别处控制桌宠 → 见「8. 对外 API」：
        window.dispatchEvent(new CustomEvent('pet:say', { detail: '你好' }))
        window.dispatchEvent(new CustomEvent('pet:switch', { detail: 'dawn' }))
        window.__studyPet?.say('被其他组件调用')
  ============================================================================
-->
<template>
  <Teleport to="body">
    <div v-show="!hidden" class="pet-layer" :style="layerStyle">
      <div
        ref="rootRef"
        class="pet-root"
        :class="rootClass"
        :style="rootStyle"
        tabindex="0"
        role="button"
        :aria-label="`${persona.name}，${persona.title}。回车互动，S 键切换角色，双击休眠`"
        @keydown.enter.prevent="react"
        @keydown.space.prevent="react"
        @keydown.s.prevent="toggleCharacter"
        @keydown.esc="closeMenu"
      >
        <div
          class="pet-stage"
          @pointerdown="onPointerDown"
          @pointermove="onPointerMove"
          @pointerup="onPointerUp"
          @pointercancel="onPointerUp"
          @pointerenter="onPointerEnter"
          @pointerleave="onPointerLeave"
          @dblclick="onDoubleClick"
          @contextmenu.prevent="openMenuAt"
        >
          <span class="pet-aura" aria-hidden="true"></span>
          <span class="pet-shadow" aria-hidden="true"></span>

          <!-- 角色主题微粒：夜=星芒，晨=光点 -->
          <span class="pet-motif" aria-hidden="true"><i></i><i></i><i></i></span>

          <div class="pet-facing" :style="{ transform: `scaleX(${facing})` }">
            <div class="pet-sprite-wrap" :class="{ 'is-react': reacting }">
              <img
                class="pet-sprite"
                :class="{ active: character === 'night' && !sleeping }"
                :src="nightSprite"
                alt=""
                aria-hidden="true"
                draggable="false"
              />
              <img
                class="pet-sprite"
                :class="{ active: character === 'dawn' && !sleeping }"
                :src="dawnSprite"
                alt=""
                aria-hidden="true"
                draggable="false"
              />
              <!-- 睡眠时换成闭眼立绘 -->
              <img
                class="pet-sprite"
                :class="{ active: character === 'night' && sleeping }"
                :src="nightSleepSprite"
                alt=""
                aria-hidden="true"
                draggable="false"
              />
              <img
                class="pet-sprite"
                :class="{ active: character === 'dawn' && sleeping }"
                :src="dawnSleepSprite"
                alt=""
                aria-hidden="true"
                draggable="false"
              />
            </div>
          </div>

          <div class="pet-zzz" aria-hidden="true"><span>z</span><span>z</span><span>z</span></div>
        </div>

        <!-- 角色切换（悬停 / 聚焦时出现，触屏常显） -->
        <div
          class="pet-switch"
          role="group"
          aria-label="切换桌宠角色"
          @pointerenter="onPointerEnter"
          @pointerleave="onPointerLeave"
        >
          <button
            type="button"
            :class="{ on: character === 'night' }"
            title="换成夜读精灵"
            aria-label="换成夜读精灵"
            @click.stop="switchTo('night')"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="1.7">
              <path d="M20 14.4A8.6 8.6 0 1 1 9.6 4a6.9 6.9 0 0 0 10.4 10.4Z" stroke-linejoin="round" />
            </svg>
          </button>
          <button
            type="button"
            :class="{ on: character === 'dawn' }"
            title="换成晨读精灵"
            aria-label="换成晨读精灵"
            @click.stop="switchTo('dawn')"
          >
            <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="1.7">
              <circle cx="12" cy="12" r="4" />
              <path
                d="M12 2.5v2.2M12 19.3v2.2M2.5 12h2.2M19.3 12h2.2M5.2 5.2l1.6 1.6M17.2 17.2l1.6 1.6M18.8 5.2l-1.6 1.6M6.8 17.2l-1.6 1.6"
                stroke-linecap="round"
              />
            </svg>
          </button>
        </div>
      </div>

      <!-- 对话气泡（放在移动容器之外，避免被 transform 影响定位） -->
      <div ref="bubbleRef" class="pet-bubble" :class="{ show: bubble.visible }" aria-live="polite">
        {{ bubble.text }}
      </div>

      <!-- 右键 / 长按菜单 -->
      <div v-if="menu.open" ref="menuRef" class="pet-menu" :style="menuStyle" @pointerdown.stop @contextmenu.prevent>
        <div class="pet-menu-title">桌宠 · {{ persona.name }}</div>
        <button
          type="button"
          class="pet-menu-item"
          :class="{ on: character === 'night' }"
          @click="switchTo('night')"
        >
          <span class="pet-menu-glyph">☾</span> 夜读 · 小夜
          <small v-if="character === 'night'">当前</small>
        </button>
        <button
          type="button"
          class="pet-menu-item"
          :class="{ on: character === 'dawn' }"
          @click="switchTo('dawn')"
        >
          <span class="pet-menu-glyph">☀</span> 晨读 · 小晨
          <small v-if="character === 'dawn'">当前</small>
        </button>
        <div class="pet-menu-sep"></div>
        <button type="button" class="pet-menu-item" @click="toggleRoam">
          <span class="pet-menu-glyph">↝</span> 自动散步
          <small>{{ roam ? '开' : '关' }}</small>
        </button>
        <button type="button" class="pet-menu-item" @click="toggleGlow">
          <span class="pet-menu-glyph">✦</span> 角色柔光
          <small>{{ glowOn ? '开' : '关' }}</small>
        </button>
        <button type="button" class="pet-menu-item" @click="toggleSleep">
          <span class="pet-menu-glyph">☾</span> {{ sleeping ? '叫醒他' : '让他歇会儿' }}
        </button>
        <button type="button" class="pet-menu-item" @click="recenter">
          <span class="pet-menu-glyph">⌂</span> 回到右下角
        </button>
        <div class="pet-menu-sep"></div>
        <button type="button" class="pet-menu-item" @click="hidePet">
          <span class="pet-menu-glyph">–</span> 隐藏桌宠 <small>Shift+P</small>
        </button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import nightSprite from '@/assets/pet/night.png'
import dawnSprite from '@/assets/pet/dawn.png'
import nightSleepSprite from '@/assets/pet/night_sleep.png'
import dawnSleepSprite from '@/assets/pet/dawn_sleep.png'

/* ==========================================================================
   1. 角色配置：改台词、换立绘、调主题色都集中在这里
   ========================================================================== */
type PetId = 'night' | 'dawn'

interface Persona {
  id: PetId
  name: string
  title: string
  sprite: string
  /** 睡眠时的闭眼立绘 */
  sleepSprite: string
  accent: string
  soft: string
  glow: string
  /** 角色外围柔光，越淡越不明显 */
  halo: string
  motif: 'moon' | 'sun'
  greet: string
  idle: string[]
  click: string[]
  drag: string[]
  sleep: string[]
  wake: string[]
  swap: string[]
}

const PERSONAS: Record<PetId, Persona> = {
  night: {
    id: 'night',
    name: '小夜',
    title: '守夜的书灵',
    sprite: nightSprite,
    sleepSprite: nightSleepSprite,
    accent: '#9db4ff',
    soft: 'rgba(157, 180, 255, 0.32)',
    glow: 'rgba(96, 124, 255, 0.24)',
    halo: 'rgba(150, 175, 255, 0.16)',
    motif: 'moon',
    greet: '晚上好，我是小夜。右键可以换人，拖着我走也行。',
    idle: [
      '夜里的自习室最安静了。',
      '这本书第三章讲专注，值得一读。',
      '别忘了签到，不然会被记成占座哦。',
      '嘘……有人在背书。',
      '靠窗的位置，今晚还有空。',
      '看累了就闭眼三十秒吧。'
    ],
    click: ['唔？我在看书呢。', '嘿嘿，被你发现啦。', '戳我也不会让座位变多哦。', '今晚也要好好自习。', '这本书会发光，真的。'],
    drag: ['等等，书要掉了！', '飞起来啦——', '轻一点放我下来。'],
    sleep: ['那我看一页就睡……', '呼……书还开着呢。'],
    wake: ['我醒了我醒了。', '唔…刚才看到第几页了？'],
    swap: ['换我陪你熬夜吧。', '夜读模式，启动。']
  },
  dawn: {
    id: 'dawn',
    name: '小晨',
    title: '晨光的书灵',
    sprite: dawnSprite,
    sleepSprite: dawnSleepSprite,
    accent: '#f6c877',
    soft: 'rgba(246, 200, 119, 0.34)',
    glow: 'rgba(255, 183, 77, 0.20)',
    halo: 'rgba(255, 205, 140, 0.15)',
    motif: 'sun',
    greet: '早上好，我是小晨。右键可以换人，也可以拖我走。',
    idle: [
      '早上的光线最适合读书。',
      '今天占到靠窗的位置了吗？',
      '签到之后才算真正开始哦。',
      '坐直一点，眼睛会感谢你。',
      '要不要休息五分钟再继续？',
      '书页上有阳光的味道。'
    ],
    click: ['早安呀！', '书页上有阳光的味道。', '加油，你已经很棒了。', '一起自习吗？', '戳戳戳，我可不是闹钟。'],
    drag: ['哇，飞起来了！', '我的蝴蝶结要乱了。', '放我下来，我有点晕。'],
    sleep: ['那我在书页上趴一会儿……', '呼……'],
    wake: ['我醒啦，继续加油。', '唔…阳光好亮。'],
    swap: ['换我陪你到天亮吧。', '晨读模式，启动。']
  }
}

/* ==========================================================================
   2. 组件参数与对外事件
   ========================================================================== */
const props = withDefaults(
  defineProps<{
    /** 初始角色，本地记忆优先 */
    defaultCharacter?: PetId
    /** 精灵高度（px），窄屏会自动缩小 */
    size?: number
    /** 是否允许自动散步 */
    autoRoam?: boolean
    /** 首次出现时是否打招呼 */
    greeting?: boolean
    /** 自动散步时避开的元素选择器（例如左侧导航栏 '.sidebar'） */
    avoidSelector?: string
    /** 是否默认开启角色柔光（右键菜单里也能随时关掉） */
    glow?: boolean
    /** 层级，默认在内容之上、弹窗之下 */
    zIndex?: number
  }>(),
  {
    defaultCharacter: 'night',
    size: 176,
    autoRoam: true,
    greeting: true,
    avoidSelector: '',
    glow: true,
    zIndex: 1500
  }
)

const emit = defineEmits<{
  (e: 'ready'): void
  (e: 'character-change', id: PetId): void
  (e: 'interact', type: 'click' | 'drag' | 'switch' | 'sleep' | 'wake'): void
}>()

/* ==========================================================================
   3. 状态
   ========================================================================== */
const STORE_KEY = 'studyroom.pet.v1'
const WALK_SPEED = 76 // px/s
const WANDER_INTERVAL = 4200

const character = ref<PetId>(props.defaultCharacter)
const sleeping = ref(false)
const hidden = ref(false)
const roam = ref(props.autoRoam)
const glowOn = ref(props.glow)
const dragging = ref(false)
const walking = ref(false)
const hovering = ref(false)
const reacting = ref(false)
// 立绘本身略朝画面左侧：向左走不用镜像，向右走才镜像
const facing = ref<1 | -1>(1)
// 行走时的前倾方向（世界坐标）：右=1，左=-1
const leanDir = ref<1 | -1>(1)
const bubble = reactive({ visible: false, text: '' })
const menu = reactive({ open: false, x: 0, y: 0 })

const rootRef = ref<HTMLElement | null>(null)
const bubbleRef = ref<HTMLElement | null>(null)
const menuRef = ref<HTMLElement | null>(null)
const bubbleSize = reactive({ w: 0, h: 0 })

const persona = computed(() => PERSONAS[character.value])

const layerStyle = computed<Record<string, string>>(() => ({
  '--pet-size': `${props.size}px`,
  '--pet-accent': persona.value.accent,
  '--pet-soft': persona.value.soft,
  '--pet-glow': persona.value.glow,
  '--pet-halo': persona.value.halo,
  zIndex: String(props.zIndex)
}))

const rootStyle = computed(() => ({ '--lean-dir': String(leanDir.value) }))

const menuStyle = computed(() => ({ left: `${menu.x}px`, top: `${menu.y}px` }))

const rootClass = computed(() => ({
  'is-walking': walking.value && !dragging.value && !sleeping.value,
  'is-dragging': dragging.value,
  'is-sleeping': sleeping.value,
  'is-hover': hovering.value,
  'no-glow': !glowOn.value,
  [`motif-${persona.value.motif}`]: true
}))

/* ==========================================================================
   4. 位置与渲染
   ========================================================================== */
const pos = { x: 0, y: 0 } // 精灵左上角（非响应式，逐帧直接写 style）
const target = { x: 0, y: 0 }
const petW = ref(0)
const petH = ref(0)

const clamp = (v: number, min: number, max: number) => Math.min(Math.max(v, min), Math.max(min, max))
const pick = (arr: string[]) => arr[Math.floor(Math.random() * arr.length)] ?? ''

function apply() {
  const el = rootRef.value
  if (el) el.style.transform = `translate3d(${pos.x}px, ${pos.y}px, 0)`
}

function measure() {
  const el = rootRef.value
  if (!el) return
  petW.value = el.offsetWidth
  petH.value = el.offsetHeight
}

function roamZone() {
  const vw = window.innerWidth
  const vh = window.innerHeight
  let left = 20
  // 避开固定侧栏之类的遮挡物，别让桌宠在菜单上走来走去
  if (props.avoidSelector) {
    const el = document.querySelector(props.avoidSelector)
    if (el) {
      const r = el.getBoundingClientRect()
      if (r.width > 0 && r.right > 40 && r.right < vw * 0.6) left = r.right + 16
    }
  }
  // 沿着屏幕底部“地面”活动，上下幅度收窄，避免看起来在空气里飘
  const bottom = vh - petH.value - 18
  const band = Math.min(130, vh * 0.18)
  const top = Math.max(bottom - band, 72)
  return {
    x1: left,
    x2: Math.max(left, vw - petW.value - 20),
    y1: Math.min(top, bottom),
    y2: Math.max(top, bottom)
  }
}

function placeBubble() {
  const el = bubbleRef.value
  if (!el || !bubble.visible) return
  const bw = bubbleSize.w || el.offsetWidth
  const bh = bubbleSize.h || el.offsetHeight
  const cx = pos.x + petW.value / 2
  const left = clamp(cx - bw / 2, 8, window.innerWidth - bw - 8)
  const top = clamp(pos.y - bh - 14, 8, window.innerHeight - bh - 8)
  el.style.left = `${left}px`
  el.style.top = `${top}px`
  el.style.setProperty('--tail-x', `${clamp(cx - left, 18, bw - 18)}px`)
}

/* ==========================================================================
   5. 主循环：匀速走向目标点
   ========================================================================== */
let raf = 0
let lastTs = 0

function loop(ts: number) {
  const dt = Math.min((ts - lastTs) / 1000 || 0.016, 0.06)
  lastTs = ts

  if (!dragging.value && !sleeping.value) {
    const dx = target.x - pos.x
    const dy = target.y - pos.y
    const dist = Math.hypot(dx, dy)
    if (dist > 1.5) {
      const step = Math.min(dist, WALK_SPEED * dt)
      pos.x += (dx / dist) * step
      pos.y += (dy / dist) * step
      if (Math.abs(dx) > 0.6) {
        // 立绘本身略朝左：向右走才镜像，否则会看着像倒着走
        facing.value = dx < 0 ? 1 : -1
        leanDir.value = dx < 0 ? -1 : 1
      }
      walking.value = true
      apply()
    } else if (walking.value) {
      walking.value = false
    }
  } else if (walking.value) {
    walking.value = false
  }

  if (bubble.visible) placeBubble()
  raf = requestAnimationFrame(loop)
}

/* ==========================================================================
   6. 行为：说话 / 互动 / 切换 / 休眠 / 散步
   ========================================================================== */
let bubbleTimer: ReturnType<typeof setTimeout> | null = null

function say(text: string, duration = 2600) {
  if (!text) return
  bubble.text = text
  bubble.visible = true
  nextTick(() => {
    const el = bubbleRef.value
    if (el) {
      bubbleSize.w = el.offsetWidth
      bubbleSize.h = el.offsetHeight
    }
    placeBubble()
  })
  if (bubbleTimer) clearTimeout(bubbleTimer)
  bubbleTimer = setTimeout(() => {
    bubble.visible = false
  }, duration)
}

let reactTimer: ReturnType<typeof setTimeout> | null = null
let lastWakeAt = 0

function react() {
  if (hidden.value) return
  if (menu.open) {
    closeMenu()
    return
  }
  if (sleeping.value) {
    wake()
    return
  }
  reacting.value = false
  requestAnimationFrame(() => {
    reacting.value = true
    if (reactTimer) clearTimeout(reactTimer)
    reactTimer = setTimeout(() => (reacting.value = false), 620)
  })
  say(pick(persona.value.click))
  emit('interact', 'click')
}

function wake() {
  if (!sleeping.value) return
  sleeping.value = false
  lastWakeAt = Date.now()
  say(pick(persona.value.wake), 2200)
  emit('interact', 'wake')
}

function sleep() {
  if (sleeping.value) return
  sleeping.value = true
  bubble.visible = false
  say(pick(persona.value.sleep), 2600)
  emit('interact', 'sleep')
}

function toggleSleep() {
  if (sleeping.value) wake()
  else sleep()
}

function onDoubleClick() {
  // 单击已经把它叫醒了，双击事件不要再让它睡回去
  if (Date.now() - lastWakeAt < 500) return
  toggleSleep()
}

function switchTo(id: PetId) {
  closeMenu()
  if (id === character.value) {
    say(pick(PERSONAS[id].swap), 1800)
    return
  }
  character.value = id
  // 睡着的时候换人只换立绘，不要跳一下
  if (!sleeping.value) {
    reacting.value = false
    requestAnimationFrame(() => {
      reacting.value = true
      if (reactTimer) clearTimeout(reactTimer)
      reactTimer = setTimeout(() => (reacting.value = false), 620)
    })
  }
  say(pick(PERSONAS[id].swap), 2600)
  emit('interact', 'switch')
  emit('character-change', id)
  persist()
}

function toggleCharacter() {
  switchTo(character.value === 'night' ? 'dawn' : 'night')
}

function walkTo(x: number, y: number) {
  target.x = clamp(x, 0, window.innerWidth - petW.value)
  target.y = clamp(y, 0, window.innerHeight - petH.value)
  if (sleeping.value) wake()
}

function recenter() {
  closeMenu()
  if (sleeping.value) wake()
  walkTo(window.innerWidth - petW.value - 28, window.innerHeight - petH.value - 18)
  say('我回老位置啦。', 1800)
}

function toggleRoam() {
  roam.value = !roam.value
  closeMenu()
  say(roam.value ? '那我四处逛逛。' : '我就在这儿待着。', 1800)
  persist()
}

function toggleGlow() {
  glowOn.value = !glowOn.value
  closeMenu()
  persist()
}

let wanderTimer: ReturnType<typeof setInterval> | null = null

function maybeWander() {
  if (!roam.value || sleeping.value || dragging.value || hidden.value || hovering.value || menu.open) return
  const zone = roamZone()
  if (Math.random() < 0.4) {
    facing.value = facing.value === 1 ? -1 : 1 // 只转身看看
    return
  }
  target.x = zone.x1 + Math.random() * (zone.x2 - zone.x1)
  target.y = zone.y1 + Math.random() * (zone.y2 - zone.y1)
  if (Math.random() < 0.18) say(pick(persona.value.idle), 2800)
}

/* ==========================================================================
   7. 指针 / 键盘 / 菜单
   ========================================================================== */
let pressTimer: ReturnType<typeof setTimeout> | null = null
let hoverTimer: ReturnType<typeof setTimeout> | null = null
let pressStart = { x: 0, y: 0 }
let grabOffset = { x: 0, y: 0 }
let moved = false
let pressing = false

function onPointerDown(e: PointerEvent) {
  if (e.button !== 0) return
  closeMenu()
  pressing = true
  moved = false
  pressStart = { x: e.clientX, y: e.clientY }
  grabOffset = { x: e.clientX - pos.x, y: e.clientY - pos.y }
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  pressTimer = setTimeout(() => {
    if (!moved) openMenu() // 触屏长按
  }, 620)
}

function onPointerMove(e: PointerEvent) {
  if (!pressing) return
  if (!moved && Math.hypot(e.clientX - pressStart.x, e.clientY - pressStart.y) > 5) {
    moved = true
    dragging.value = true
    if (pressTimer) clearTimeout(pressTimer)
  }
  if (dragging.value) {
    pos.x = clamp(e.clientX - grabOffset.x, 4, window.innerWidth - petW.value - 4)
    pos.y = clamp(e.clientY - grabOffset.y, 4, window.innerHeight - petH.value - 6)
    apply()
  }
}

function onPointerUp(e: PointerEvent) {
  if (pressTimer) clearTimeout(pressTimer)
  const wasPressing = pressing
  pressing = false
  try {
    ;(e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId)
  } catch {
    /* 指针已释放，忽略 */
  }
  if (dragging.value) {
    dragging.value = false
    target.x = pos.x
    target.y = pos.y
    persist()
    say(pick(persona.value.drag), 1800)
    emit('interact', 'drag')
    return
  }
  if (wasPressing && !moved) react()
}

function onPointerEnter() {
  hovering.value = true
  if (leaveTimer) clearTimeout(leaveTimer)
  // 鼠标靠近就停下，不要从光标底下走开
  target.x = pos.x
  target.y = pos.y
  if (hoverTimer) clearTimeout(hoverTimer)
  hoverTimer = setTimeout(() => {
    if (!sleeping.value && !menu.open && !bubble.visible) say(pick(persona.value.idle), 3000)
  }, 1600)
}

let leaveTimer: ReturnType<typeof setTimeout> | null = null

function onPointerLeave() {
  if (leaveTimer) clearTimeout(leaveTimer)
  if (hoverTimer) clearTimeout(hoverTimer)
  // 留一点缓冲，避免从身体移到切换按钮时被判定为离开
  leaveTimer = setTimeout(() => {
    hovering.value = false
  }, 420)
}

function openMenuAt() {
  openMenu()
}

function openMenu() {
  bubble.visible = false
  menu.open = true
  nextTick(() => {
    const el = menuRef.value
    if (!el) return
    const w = el.offsetWidth
    const h = el.offsetHeight
    // 优先贴在角色左侧，放不下再换到右侧，避免菜单盖住桌宠
    let x = pos.x - w - 12
    if (x < 8) x = pos.x + petW.value + 12
    const y = pos.y + petH.value * 0.18
    menu.x = clamp(x, 8, window.innerWidth - w - 8)
    menu.y = clamp(y, 8, window.innerHeight - h - 8)
  })
}

function closeMenu() {
  menu.open = false
}

function hidePet() {
  closeMenu()
  bubble.visible = false
  hidden.value = true
}

function showPet() {
  hidden.value = false
  nextTick(() => {
    measure()
    apply()
    say('我在这儿。', 1800)
  })
}

function toggleHidden() {
  if (hidden.value) showPet()
  else hidePet()
}

/* ==========================================================================
   8. 对外 API：defineExpose + window 事件 / 全局对象
   ========================================================================== */
const api = {
  say,
  switchTo,
  toggleCharacter,
  sleep,
  wake,
  walkTo,
  toggleGlow,
  hide: hidePet,
  show: showPet,
  get state() {
    return {
      character: character.value,
      sleeping: sleeping.value,
      hidden: hidden.value,
      roam: roam.value,
      glow: glowOn.value
    }
  }
}

defineExpose(api)

function onCommand(e: Event) {
  const { type, detail } = e as CustomEvent<unknown>
  switch (type) {
    case 'pet:say':
      if (typeof detail === 'string') say(detail)
      break
    case 'pet:switch':
      if (detail === 'night' || detail === 'dawn') switchTo(detail)
      break
    case 'pet:toggle':
      toggleCharacter()
      break
    case 'pet:sleep':
      sleep()
      break
    case 'pet:wake':
      wake()
      break
    case 'pet:hide':
      hidePet()
      break
    case 'pet:show':
      showPet()
      break
  }
}

/* ==========================================================================
   9. 本地记忆
   ========================================================================== */
interface SavedState {
  id?: PetId
  xr?: number
  yr?: number
  roam?: boolean
  glow?: boolean
}

function load(): SavedState | null {
  try {
    const raw = localStorage.getItem(STORE_KEY)
    return raw ? (JSON.parse(raw) as SavedState) : null
  } catch {
    return null
  }
}

function persist() {
  try {
    const vw = window.innerWidth - petW.value
    const vh = window.innerHeight - petH.value
    localStorage.setItem(
      STORE_KEY,
      JSON.stringify({
        id: character.value,
        xr: vw > 0 ? pos.x / vw : 0,
        yr: vh > 0 ? pos.y / vh : 0,
        roam: roam.value,
        glow: glowOn.value
      })
    )
  } catch {
    /* localStorage 不可用时静默降级 */
  }
}

/* ==========================================================================
   10. 生命周期
   ========================================================================== */
function onResize() {
  measure()
  pos.x = clamp(pos.x, 4, window.innerWidth - petW.value - 4)
  pos.y = clamp(pos.y, 4, window.innerHeight - petH.value - 6)
  target.x = clamp(target.x, 0, window.innerWidth - petW.value)
  target.y = clamp(target.y, 0, window.innerHeight - petH.value)
  apply()
  placeBubble()
}

function onGlobalKey(e: KeyboardEvent) {
  if (e.shiftKey && (e.key === 'P' || e.key === 'p')) {
    e.preventDefault()
    toggleHidden()
  }
  if (e.key === 'Escape') closeMenu()
}

function onDocPointerDown(e: PointerEvent) {
  if (menu.open && menuRef.value && !menuRef.value.contains(e.target as Node)) {
    closeMenu()
  }
}

onMounted(() => {
  const saved = load()
  if (saved?.id === 'night' || saved?.id === 'dawn') character.value = saved.id
  if (typeof saved?.roam === 'boolean') roam.value = saved.roam
  if (typeof saved?.glow === 'boolean') glowOn.value = saved.glow

  measure()
  const vw = window.innerWidth
  const vh = window.innerHeight
  const hasPos = typeof saved?.xr === 'number' && typeof saved?.yr === 'number'
  pos.x = hasPos ? saved!.xr! * (vw - petW.value) : vw - petW.value - 28
  pos.y = hasPos ? saved!.yr! * (vh - petH.value) : vh - petH.value - 18
  pos.x = clamp(pos.x, 4, vw - petW.value - 4)
  pos.y = clamp(pos.y, 4, vh - petH.value - 6)
  target.x = pos.x
  target.y = pos.y
  apply()

  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', onGlobalKey)
  window.addEventListener('pointerdown', onDocPointerDown, true)
  for (const type of ['pet:say', 'pet:switch', 'pet:toggle', 'pet:sleep', 'pet:wake', 'pet:hide', 'pet:show']) {
    window.addEventListener(type, onCommand as EventListener)
  }
  ;(window as unknown as Record<string, unknown>).__studyPet = api

  raf = requestAnimationFrame((ts) => {
    lastTs = ts
    loop(ts)
  })
  wanderTimer = setInterval(maybeWander, WANDER_INTERVAL)

  emit('ready')

  if (props.greeting) {
    setTimeout(() => say(persona.value.greet, 4200), 1000)
  }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  if (wanderTimer) clearInterval(wanderTimer)
  if (bubbleTimer) clearTimeout(bubbleTimer)
  if (reactTimer) clearTimeout(reactTimer)
  if (pressTimer) clearTimeout(pressTimer)
  if (hoverTimer) clearTimeout(hoverTimer)
  if (leaveTimer) clearTimeout(leaveTimer)
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onGlobalKey)
  window.removeEventListener('pointerdown', onDocPointerDown, true)
  for (const type of ['pet:say', 'pet:switch', 'pet:toggle', 'pet:sleep', 'pet:wake', 'pet:hide', 'pet:show']) {
    window.removeEventListener(type, onCommand as EventListener)
  }
  delete (window as unknown as Record<string, unknown>).__studyPet
})
</script>

<style scoped>
/* --------------------------------------------------------------------------
   容器：整层不拦截鼠标，只有桌宠本体与菜单可交互
   -------------------------------------------------------------------------- */
.pet-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  font-family: var(--sans, system-ui, -apple-system, 'Segoe UI', sans-serif);
  -webkit-tap-highlight-color: transparent;
}

.pet-root {
  --pet-h: clamp(112px, 24vh, var(--pet-size, 168px));
  --pet-w: calc(var(--pet-h) * 0.62);
  position: absolute;
  left: 0;
  top: 0;
  width: var(--pet-w);
  height: var(--pet-h);
  pointer-events: none;
  outline: none;
  will-change: transform;
}

/* --------------------------------------------------------------------------
   舞台与角色立绘
   -------------------------------------------------------------------------- */
.pet-stage {
  position: relative;
  width: 100%;
  height: 100%;
  pointer-events: auto;
  cursor: grab;
  touch-action: none;
  transform-origin: 50% 100%;
  animation: pet-bob 5.4s ease-in-out infinite;
}
.pet-stage:active {
  cursor: grabbing;
}

.pet-facing {
  position: absolute;
  inset: 0;
  transform-origin: 50% 90%;
  /* 立绘接近正面，镜像要瞬时，避免出现“翻个身”的过渡错觉 */
  transition: none;
}

.pet-sprite-wrap {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  transform-origin: 50% 100%;
}

.pet-sprite {
  position: absolute;
  bottom: 0;
  left: 50%;
  height: 100%;
  width: auto;
  transform: translateX(-50%);
  opacity: 0;
  filter: drop-shadow(0 8px 14px rgba(0, 0, 0, 0.38)) drop-shadow(0 0 4px var(--pet-halo, transparent));
  transition: opacity 0.34s ease, filter 0.34s ease;
  user-select: none;
  -webkit-user-drag: none;
}
.pet-sprite.active {
  opacity: 1;
}

/* 呼吸光晕（很淡，只做一点点环境氛围） */
.pet-aura {
  position: absolute;
  left: 50%;
  bottom: -4%;
  width: 126%;
  height: 62%;
  transform: translateX(-50%);
  background: radial-gradient(closest-side, var(--pet-halo, transparent), transparent 72%);
  filter: blur(12px);
  opacity: 0.35;
  pointer-events: none;
  animation: pet-glow 6.4s ease-in-out infinite;
}

/* 落地投影 */
.pet-shadow {
  position: absolute;
  left: 50%;
  bottom: -3px;
  width: 54%;
  height: 9px;
  transform: translateX(-50%);
  background: radial-gradient(closest-side, rgba(0, 0, 0, 0.55), transparent 78%);
  pointer-events: none;
  animation: pet-shadow 4.6s ease-in-out infinite;
}

/* 主题微粒 */
.pet-motif {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.pet-motif i {
  position: absolute;
  display: block;
  width: 7px;
  height: 7px;
  background: var(--pet-accent);
  opacity: 0;
  animation: pet-float 3.6s ease-in-out infinite;
}
.pet-motif i:nth-child(1) {
  left: 4%;
  top: 26%;
  animation-delay: 0.2s;
}
.pet-motif i:nth-child(2) {
  right: 2%;
  top: 42%;
  animation-delay: 1.4s;
  width: 5px;
  height: 5px;
}
.pet-motif i:nth-child(3) {
  right: 16%;
  top: 12%;
  animation-delay: 2.4s;
  width: 4px;
  height: 4px;
}
.motif-moon .pet-motif i {
  clip-path: polygon(50% 0, 62% 38%, 100% 50%, 62% 62%, 50% 100%, 38% 62%, 0 50%, 38% 38%);
  box-shadow: 0 0 6px var(--pet-accent);
}
.motif-sun .pet-motif i {
  border-radius: 50%;
  filter: blur(0.4px);
  box-shadow: 0 0 7px var(--pet-accent);
}

/* 休眠 Zzz */
.pet-zzz {
  position: absolute;
  right: 0;
  top: -2%;
  width: 28px;
  height: 44px;
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s;
}
.is-sleeping .pet-zzz {
  opacity: 1;
}
.pet-zzz span {
  position: absolute;
  bottom: 0;
  left: 6px;
  font-size: 14px;
  font-weight: 700;
  color: var(--pet-accent);
  text-shadow: 0 0 6px var(--pet-glow), 0 1px 2px rgba(0, 0, 0, 0.5);
  opacity: 0;
}
.is-sleeping .pet-zzz span {
  animation: pet-zzz 2.8s ease-out infinite;
}
.pet-zzz span:nth-child(2) {
  animation-delay: 0.7s !important;
  font-size: 16px;
}
.pet-zzz span:nth-child(3) {
  animation-delay: 1.4s !important;
  font-size: 18px;
}

/* 状态修饰 */
.is-walking .pet-stage {
  animation: pet-step 0.46s ease-in-out infinite;
}
.is-dragging .pet-stage {
  animation: none;
  transform: scale(1.04) rotate(2deg);
  transition: transform 0.18s ease;
}
.is-sleeping .pet-stage {
  animation: none;
  transform: rotate(-8deg) translateY(4px);
  transition: transform 0.34s ease;
}
.is-sleeping .pet-sprite.active {
  filter: drop-shadow(0 8px 14px rgba(0, 0, 0, 0.38)) drop-shadow(0 0 4px var(--pet-halo, transparent))
    brightness(0.95) saturate(0.95);
}
.is-sleeping .pet-aura {
  opacity: 0.18;
}
.is-react {
  animation: pet-hop 0.6s cubic-bezier(0.3, 0.9, 0.4, 1);
}

/* 关闭柔光：去掉光晕、外发光、微粒与 Zzz 的辉光 */
.no-glow .pet-aura {
  display: none;
}
.no-glow .pet-sprite,
.no-glow .pet-sprite.active {
  filter: drop-shadow(0 8px 14px rgba(0, 0, 0, 0.38));
}
.no-glow .pet-motif i {
  box-shadow: none;
}
.no-glow .pet-zzz span {
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}

/* --------------------------------------------------------------------------
   角色切换按钮
   -------------------------------------------------------------------------- */
.pet-switch {
  position: absolute;
  left: -34px;
  top: 32%;
  display: flex;
  flex-direction: column;
  gap: 6px;
  opacity: 0;
  transform: translateX(6px);
  pointer-events: none;
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.pet-root:hover .pet-switch,
.pet-root:focus-within .pet-switch {
  opacity: 1;
  transform: none;
  pointer-events: auto;
}
.pet-switch button {
  width: 26px;
  height: 26px;
  padding: 0;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(18, 20, 30, 0.72);
  color: #cbd3e8;
  cursor: pointer;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  transition: color 0.2s, border-color 0.2s, transform 0.2s, box-shadow 0.2s;
}
.pet-switch button:hover {
  color: #fff;
  transform: translateY(-1px);
  border-color: var(--pet-accent);
}
.pet-switch button.on {
  color: var(--pet-accent);
  border-color: var(--pet-accent);
  box-shadow: 0 0 0 3px var(--pet-soft);
}
.pet-switch button:focus-visible {
  outline: 2px solid var(--pet-accent);
  outline-offset: 2px;
}

/* --------------------------------------------------------------------------
   对话气泡
   -------------------------------------------------------------------------- */
.pet-bubble {
  position: absolute;
  left: 0;
  top: 0;
  max-width: 232px;
  padding: 9px 13px;
  border-radius: 14px;
  background: rgba(17, 19, 28, 0.84);
  border: 1px solid var(--pet-soft);
  color: #eef1fa;
  font-size: 12.5px;
  line-height: 1.6;
  letter-spacing: 0.2px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  pointer-events: none;
  opacity: 0;
  transform: translateY(6px) scale(0.97);
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.pet-bubble.show {
  opacity: 1;
  transform: translateY(0) scale(1);
}
.pet-bubble::after {
  content: '';
  position: absolute;
  left: var(--tail-x, 50%);
  bottom: -5px;
  width: 9px;
  height: 9px;
  background: rgba(17, 19, 28, 0.84);
  border-right: 1px solid var(--pet-soft);
  border-bottom: 1px solid var(--pet-soft);
  border-radius: 1px;
  transform: translateX(-50%) rotate(45deg);
}

/* --------------------------------------------------------------------------
   右键菜单
   -------------------------------------------------------------------------- */
.pet-menu {
  position: absolute;
  width: 198px;
  padding: 6px;
  border-radius: 14px;
  background: rgba(17, 19, 28, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 18px 44px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  pointer-events: auto;
  color: #e8ecf7;
  animation: pet-menu-in 0.16s ease-out;
}
.pet-menu-title {
  padding: 8px 10px 5px;
  font-size: 10.5px;
  letter-spacing: 0.08em;
  color: #8990a6;
  text-transform: uppercase;
}
.pet-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: inherit;
  font-family: inherit;
  font-size: 12.5px;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s;
}
.pet-menu-item:hover {
  background: rgba(255, 255, 255, 0.08);
}
.pet-menu-item.on {
  color: var(--pet-accent);
}
.pet-menu-item small {
  margin-left: auto;
  font-size: 10.5px;
  color: #8990a6;
}
.pet-menu-glyph {
  width: 14px;
  text-align: center;
  color: var(--pet-accent);
  opacity: 0.9;
}
.pet-menu-sep {
  height: 1px;
  margin: 4px 8px;
  background: rgba(255, 255, 255, 0.08);
}

/* --------------------------------------------------------------------------
   动画
   -------------------------------------------------------------------------- */
@keyframes pet-bob {
  0%,
  100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-3px) scale(1.004, 0.996);
  }
}
/* 走路：两步一循环，前倾 + 起落，读起来像在迈步而不是平移 */
@keyframes pet-step {
  0% {
    transform: translateY(0) rotate(0deg);
  }
  20% {
    transform: translateY(-7px) rotate(calc(var(--lean-dir, 1) * 2.8deg));
  }
  48% {
    transform: translateY(0) rotate(0deg);
  }
  70% {
    transform: translateY(-3.5px) rotate(calc(var(--lean-dir, 1) * -1.8deg));
  }
  100% {
    transform: translateY(0) rotate(0deg);
  }
}
@keyframes pet-hop {
  0% {
    transform: none;
  }
  18% {
    transform: scale(1.07, 0.93);
  }
  45% {
    transform: translateY(-15px) scale(0.97, 1.04);
  }
  72% {
    transform: translateY(0) scale(1.03, 0.97);
  }
  100% {
    transform: none;
  }
}
@keyframes pet-glow {
  0%,
  100% {
    opacity: 0.22;
    transform: translateX(-50%) scale(0.97);
  }
  50% {
    opacity: 0.42;
    transform: translateX(-50%) scale(1.04);
  }
}
@keyframes pet-shadow {
  0%,
  100% {
    opacity: 0.5;
    transform: translateX(-50%) scaleX(1);
  }
  50% {
    opacity: 0.3;
    transform: translateX(-50%) scaleX(0.86);
  }
}
@keyframes pet-float {
  0% {
    opacity: 0;
    transform: translateY(6px) scale(0.7);
  }
  35% {
    opacity: 0.9;
  }
  100% {
    opacity: 0;
    transform: translateY(-16px) scale(1.05);
  }
}
@keyframes pet-zzz {
  0% {
    opacity: 0;
    transform: translate(0, 0) scale(0.7);
  }
  30% {
    opacity: 0.95;
  }
  100% {
    opacity: 0;
    transform: translate(12px, -26px) scale(1.1);
  }
}
@keyframes pet-menu-in {
  from {
    opacity: 0;
    transform: translateY(-4px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

/* --------------------------------------------------------------------------
   响应式与无障碍
   -------------------------------------------------------------------------- */
@media (hover: none) {
  .pet-switch {
    opacity: 1;
    transform: none;
    pointer-events: auto;
    left: -30px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .pet-stage,
  .pet-aura,
  .pet-shadow,
  .pet-motif i,
  .pet-sprite-wrap {
    animation: none !important;
  }
  .pet-sprite,
  .pet-bubble {
    transition-duration: 0.01ms;
  }
}
</style>
