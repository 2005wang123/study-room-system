<template>
  <div class="fam-mask" @click.self="emit('close')">
    <div class="fam">
      <header class="fam-head">
        <span class="fam-title">楼层 / 区域管理</span>
        <button type="button" class="fam-close" aria-label="关闭" @click="emit('close')">&times;</button>
      </header>

      <div class="fam-body">
        <!-- 左：楼层 -->
        <section class="fam-col">
          <h4 class="fam-col-title">楼层</h4>
          <div class="fam-add">
            <input
              v-model="newFloorName"
              class="fam-input"
              placeholder="新楼层名称，如：三楼"
              maxlength="30"
              @keydown.enter="addFloor"
            />
            <input
              v-model.number="newFloorNum"
              class="fam-input fam-num"
              type="number"
              min="1"
              placeholder="排序"
              title="排序号（数字，越小越靠前）"
            />
            <button type="button" class="fam-btn primary" :disabled="busy" @click="addFloor">新增</button>
          </div>

          <div class="fam-list">
            <div
              v-for="fl in floors"
              :key="fl.id"
              class="fam-item"
              :class="{ active: selectedFloorId === fl.id }"
              @click="selectFloor(fl)"
            >
              <div class="fam-item-inputs">
                <input class="fam-input" :value="fl.floorName" maxlength="30" @input="onFloorName(fl, $event)" />
                <input
                  class="fam-input fam-num"
                  type="number"
                  min="1"
                  :value="fl.floorNumber ?? ''"
                  title="排序号"
                  @change="onFloorNum(fl, $event)"
                />
              </div>
              <div class="fam-item-actions">
                <button type="button" class="fam-btn small" :disabled="busy" @click.stop="saveFloor(fl)">保存</button>
                <button
                  type="button"
                  class="fam-btn small danger"
                  :disabled="busy"
                  title="删除（需先清空其下区域/座位/结构图）"
                  @click.stop="removeFloor(fl)"
                >
                  删除
                </button>
              </div>
            </div>
            <p v-if="!floors.length" class="fam-empty">还没有楼层，先在上方新增一个吧</p>
          </div>
        </section>

        <!-- 右：区域 -->
        <section class="fam-col">
          <h4 class="fam-col-title">区域 · {{ selectedFloorName }}</h4>
          <div class="fam-add">
            <input
              v-model="newAreaName"
              class="fam-input"
              :disabled="!selectedFloorId"
              placeholder="新区域名称，如：101自习室"
              maxlength="30"
              @keydown.enter="addArea"
            />
            <button type="button" class="fam-btn primary" :disabled="busy || !selectedFloorId" @click="addArea">
              新增
            </button>
          </div>

          <div class="fam-list">
            <div v-for="ar in areas" :key="ar.id" class="fam-item">
              <input class="fam-input" :value="ar.areaName" maxlength="30" @input="onAreaName(ar, $event)" />
              <div class="fam-item-actions">
                <button type="button" class="fam-btn small" :disabled="busy" @click="saveArea(ar)">保存</button>
                <button type="button" class="fam-btn small danger" :disabled="busy" @click="removeArea(ar)">
                  删除
                </button>
              </div>
            </div>
            <p v-if="selectedFloorId && !areas.length" class="fam-empty">该楼层暂无区域</p>
            <p v-if="!selectedFloorId" class="fam-empty">请先选择左侧楼层</p>
          </div>
        </section>
      </div>

      <footer class="fam-foot">
        <span>提示：删除楼层前需清空其下区域 / 座位 / 结构图；删除区域会自动解绑其下座位，学生端实时生效。</span>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getAreas, getFloors } from '@/api/seat'
import { createArea, createFloor, deleteArea, deleteFloor, updateArea, updateFloor } from '@/api/floor'
import type { ApiErrorShape, AreaInfo, FloorInfo } from '@/types/api'

type ToastType = 'success' | 'error' | 'info' | 'warning'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'toast', payload: { message: string; type: ToastType }): void
  (e: 'changed'): void
}>()

const busy = ref(false)
const floors = ref<FloorInfo[]>([])
const areas = ref<AreaInfo[]>([])
const selectedFloorId = ref<number | null>(null)

const newFloorName = ref('')
const newFloorNum = ref<number | null>(null)
const newAreaName = ref('')

const selectedFloorName = computed(() => {
  const f = floors.value.find((x) => x.id === selectedFloorId.value)
  return f ? f.floorName || f.floor_name || '未命名' : '未选择'
})

const toast = (message: string, type: ToastType = 'info') => emit('toast', { message, type })
const errMsg = (err: unknown, fallback: string): string => {
  const e = err as ApiErrorShape
  return e?.response?.data?.message || e?.message || fallback
}

function unwrapArr<T>(res: unknown): T[] {
  if (Array.isArray(res)) return res as T[]
  const r = res as { code?: number; data?: unknown } | null
  if (r && r.code === 200) {
    const d = r.data
    if (Array.isArray(d)) return d as T[]
  }
  return []
}

const loadFloors = async () => {
  try {
    const res = await getFloors()
    floors.value = unwrapArr<FloorInfo>(res)
    if (!floors.value.some((x) => x.id === selectedFloorId.value)) {
      selectedFloorId.value = floors.value.length ? floors.value[0].id : null
    }
    await loadAreas()
  } catch (err) {
    toast('楼层加载失败：' + errMsg(err, '请重试'), 'error')
  }
}

const loadAreas = async () => {
  if (selectedFloorId.value == null) {
    areas.value = []
    return
  }
  try {
    const res = await getAreas(selectedFloorId.value)
    areas.value = unwrapArr<AreaInfo>(res)
  } catch (err) {
    areas.value = []
  }
}

const selectFloor = async (fl: FloorInfo) => {
  if (selectedFloorId.value === fl.id) return
  selectedFloorId.value = fl.id
  await loadAreas()
}

// ---- 楼层操作 ----
const onFloorName = (fl: FloorInfo, e: Event) => {
  fl.floorName = (e.target as HTMLInputElement).value
}
const onFloorNum = (fl: FloorInfo, e: Event) => {
  const v = Number((e.target as HTMLInputElement).value)
  fl.floorNumber = Number.isFinite(v) ? v : fl.floorNumber
}

const addFloor = async () => {
  const name = newFloorName.value.trim()
  if (!name) {
    toast('请输入楼层名称', 'warning')
    return
  }
  busy.value = true
  try {
    await createFloor({
      floorName: name,
      floorNumber: newFloorNum.value ?? undefined
    })
    newFloorName.value = ''
    newFloorNum.value = null
    toast('✅ 楼层创建成功', 'success')
    emit('changed')
    await loadFloors()
  } catch (err) {
    toast('创建失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    busy.value = false
  }
}

const saveFloor = async (fl: FloorInfo) => {
  const name = String(fl.floorName || '').trim()
  if (!name) {
    toast('楼层名称不能为空', 'warning')
    return
  }
  busy.value = true
  try {
    await updateFloor(fl.id, { floorName: name, floorNumber: fl.floorNumber })
    toast('✅ 楼层已保存', 'success')
    emit('changed')
    await loadFloors()
  } catch (err) {
    toast('保存失败：' + errMsg(err, '请重试'), 'error')
    await loadFloors()
  } finally {
    busy.value = false
  }
}

const removeFloor = async (fl: FloorInfo) => {
  if (!window.confirm(`确定删除楼层「${fl.floorName || fl.floor_name}」吗？`)) return
  busy.value = true
  try {
    await deleteFloor(fl.id)
    toast('✅ 楼层已删除', 'success')
    emit('changed')
    await loadFloors()
  } catch (err) {
    toast('删除失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    busy.value = false
  }
}

// ---- 区域操作 ----
const onAreaName = (ar: AreaInfo, e: Event) => {
  ar.areaName = (e.target as HTMLInputElement).value
}

const addArea = async () => {
  if (selectedFloorId.value == null) {
    toast('请先选择楼层', 'warning')
    return
  }
  const name = newAreaName.value.trim()
  if (!name) {
    toast('请输入区域名称', 'warning')
    return
  }
  busy.value = true
  try {
    await createArea({ floorId: selectedFloorId.value, areaName: name })
    newAreaName.value = ''
    toast('✅ 区域创建成功', 'success')
    emit('changed')
    await loadAreas()
  } catch (err) {
    toast('创建失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    busy.value = false
  }
}

const saveArea = async (ar: AreaInfo) => {
  const name = String(ar.areaName || '').trim()
  if (!name) {
    toast('区域名称不能为空', 'warning')
    return
  }
  busy.value = true
  try {
    await updateArea(ar.id, { floorId: ar.floorId, areaName: name })
    toast('✅ 区域已保存', 'success')
    emit('changed')
    await loadAreas()
  } catch (err) {
    toast('保存失败：' + errMsg(err, '请重试'), 'error')
    await loadAreas()
  } finally {
    busy.value = false
  }
}

const removeArea = async (ar: AreaInfo) => {
  if (!window.confirm(`确定删除区域「${ar.areaName}」吗？其下座位会自动解绑。`)) return
  busy.value = true
  try {
    await deleteArea(ar.id)
    toast('✅ 区域已删除', 'success')
    emit('changed')
    await loadAreas()
  } catch (err) {
    toast('删除失败：' + errMsg(err, '请重试'), 'error')
  } finally {
    busy.value = false
  }
}

onMounted(loadFloors)
</script>

<style scoped>
.fam-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
}
.fam {
  width: min(860px, 94vw);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  background: #16161d;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55);
  overflow: hidden;
}
.fam-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.fam-title {
  font-size: 15px;
  font-weight: 700;
  color: #eef0f6;
}
.fam-close {
  border: none;
  background: transparent;
  color: #9a9aa5;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
}
.fam-close:hover {
  color: #fff;
}
.fam-body {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 14px;
  min-height: 0;
}
.fam-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 10px;
}
.fam-col-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 700;
  color: #cdd2e0;
}
.fam-add {
  display: flex;
  gap: 6px;
  margin-bottom: 10px;
}
.fam-list {
  flex: 1;
  overflow: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.fam-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}
.fam-item:hover {
  background: rgba(255, 255, 255, 0.05);
}
.fam-item.active {
  border-color: rgba(102, 126, 234, 0.7);
  background: rgba(102, 126, 234, 0.14);
}
.fam-item-inputs {
  flex: 1;
  display: flex;
  gap: 6px;
  min-width: 0;
}
.fam-input {
  flex: 1;
  min-width: 0;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 7px;
  color: #e8e8ec;
  font-size: 13px;
  padding: 6px 8px;
  box-sizing: border-box;
}
.fam-input.fam-num {
  flex: 0 0 64px;
}
.fam-item-actions {
  display: flex;
  gap: 4px;
  flex: 0 0 auto;
}
.fam-btn {
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.07);
  color: #e8e8ec;
  border-radius: 7px;
  font-size: 13px;
  padding: 6px 10px;
  cursor: pointer;
  white-space: nowrap;
}
.fam-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.14);
}
.fam-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.fam-btn.primary {
  background: linear-gradient(135deg, #5b6cff, #7a4dff);
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}
.fam-btn.small {
  padding: 4px 8px;
  font-size: 12px;
}
.fam-btn.danger {
  color: #f2554a;
}
.fam-btn.danger:hover:not(:disabled) {
  background: rgba(244, 67, 54, 0.2);
}
.fam-empty {
  color: #7c7c88;
  font-size: 12px;
  text-align: center;
  padding: 14px 0;
}
.fam-foot {
  padding: 10px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  color: #8b8b98;
  font-size: 12px;
}
</style>
