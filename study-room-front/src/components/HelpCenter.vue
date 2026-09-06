<!-- src/components/HelpCenter.vue -->
<template>
  <div class="help-page">
    <!-- 页头 -->
    <header class="help-header">
      <div class="help-header-text">
        <h2 class="help-title">❓ 帮助中心</h2>
        <p class="help-subtitle">使用指南、预约规则与常见问题，帮助你快速上手自习室预约</p>
      </div>
      <div class="help-time-badge">
        <span class="help-time-dot"></span>
        服务时间 08:00 - 21:30
      </div>
    </header>

    <!-- 分类页签 -->
    <nav class="help-tabs" role="tablist" aria-label="帮助分类">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        class="help-tab"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        <span class="help-tab-icon">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
      </button>
    </nav>

    <main class="help-main">
      <!-- 使用指南 -->
      <section v-show="activeTab === 'guide'" class="help-pane">
        <ol class="guide-list panel">
          <li v-for="(step, i) in guideSteps" :key="i" class="guide-step">
            <span class="step-num">{{ i + 1 }}</span>
            <div class="guide-step-body">
              <div class="guide-step-title">{{ step.title }}</div>
              <div class="guide-step-desc">{{ step.desc }}</div>
            </div>
          </li>
        </ol>
      </section>

      <!-- 预约规则 -->
      <section v-show="activeTab === 'rules'" class="help-pane">
        <div class="pane-intro">📏 座位预约需要遵守的基本规则</div>
        <ul class="line-list panel">
          <li v-for="(rule, i) in rules" :key="i">
            <span class="line-icon">{{ rule.icon }}</span>
            <span class="line-text">{{ rule.text }}</span>
          </li>
        </ul>
      </section>

      <!-- 注意事项 -->
      <section v-show="activeTab === 'notice'" class="help-pane">
        <div class="notice-head">
          <span class="notice-head-icon">⚠️</span>
          <div>
            <div class="notice-head-title">违约判定与提醒</div>
            <div class="notice-head-desc">请仔细阅读，避免产生违约记录</div>
          </div>
        </div>
        <ul class="line-list notice-list">
          <li v-for="(n, i) in notices" :key="i">
            <span class="line-icon">{{ n.icon }}</span>
            <span class="line-text">{{ n.text }}</span>
          </li>
        </ul>
      </section>

      <!-- 常见问题 -->
      <section v-show="activeTab === 'faq'" class="help-pane">
        <div class="faq-search">
          <span class="faq-search-icon">🔍</span>
          <input
            v-model="keyword"
            class="faq-search-input"
            type="text"
            placeholder="搜索常见问题，如：取消 / 违约 / 签到"
          />
        </div>
        <div v-if="filteredFaqs.length === 0" class="faq-empty">没有找到相关的问题，可联系管理员咨询</div>
        <div v-else class="faq-list">
          <div v-for="(faq, idx) in filteredFaqs" :key="faq.q" class="faq-item" :class="{ open: openIndex === idx }">
            <button class="faq-question" @click="toggleFaq(idx)">
              <span class="faq-q">{{ faq.q }}</span>
              <span class="faq-arrow">{{ openIndex === idx ? '▾' : '▸' }}</span>
            </button>
            <div v-if="openIndex === idx" class="faq-answer">
              <p v-for="(line, li) in faq.a" :key="li">{{ line }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 联系我们 -->
      <section v-show="activeTab === 'contact'" class="help-pane">
        <div class="pane-intro">📞 有问题可通过以下方式联系我们</div>
        <div class="contact-grid">
          <div v-for="c in contacts" :key="c.label" class="contact-card">
            <span class="contact-icon">{{ c.icon }}</span>
            <div class="contact-body">
              <div class="contact-label">{{ c.label }}</div>
              <div class="contact-value">{{ c.value }}</div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

const activeTab = ref('guide')
const keyword = ref('')
const openIndex = ref(-1)

const tabs = [
  { id: 'guide', icon: '📖', label: '使用指南' },
  { id: 'faq', icon: '💬', label: '常见问题' },
  { id: 'rules', icon: '📏', label: '预约规则' },
  { id: 'notice', icon: '⚠️', label: '注意事项' },
  { id: 'contact', icon: '📞', label: '联系我们' }
]

const guideSteps = [
  {
    title: '登录系统',
    desc: '点击左侧「点击登录」或进入「个人中心」，使用学号和密码登录。初始密码为身份证号后 6 位，首次登录请及时修改。'
  },
  { title: '选择座位', desc: '在「座位地图」中点击绿色（空闲）座位即可预约；有楼层结构图时，也可直接点击图中座位。' },
  {
    title: '选择日期与时间',
    desc: '开放时间 08:00 - 21:30，按 10 分钟一档选择开始/结束时间。当天只能约当前时间之后的时段，提前一天预约可约全天。'
  },
  { title: '确认预约', desc: '核对日期、座位和时间段后点击「确认预约」，成功后座位会显示为已占用或部分占用状态。' },
  {
    title: '到馆签到使用',
    desc: '预约开始前 30 分钟即可签到，最晚需在预约结束前完成签到；到结束仍未签到将被记为违约。'
  }
]

const rules = [
  { icon: '⏰', text: '开放时间：每日 08:00 - 21:30' },
  { icon: '🪑', text: '一人一座，同一时间段每位用户仅可预约一个座位' },
  { icon: '⏳', text: '预约按 10 分钟为粒度，最短 10 分钟，最晚可约到当日 21:30' },
  { icon: '📅', text: '当天预约需至少提前 10 分钟；提前一天预约可约全天' },
  { icon: '⚠️', text: '到预约结束仍未签到将记为违约，请及时取消不用的预约' },
  { icon: '🛠️', text: '维修中的座位不可预约' }
]

const notices = [
  { icon: '⏰', text: '开放时间 08:00 - 21:30，按 10 分钟一档；当天只能约当前时间之后的时段，提前一天预约可约全天。' },
  { icon: '🕐', text: '预约成功后请按时到馆签到：可在预约开始前 30 分钟至结束时间之间完成签到。' },
  { icon: '⚠️', text: '到预约结束仍未签到，将被记为「已违约」，该时段座位会自动释放。' },
  { icon: '🚫', text: '违约记录会保留在「预约记录」中；如无法到馆，请提前取消预约，避免被记为违约。' },
  { icon: '✅', text: '使用中超过结束时间会自动「已完成」并释放座位，不算违约；离开时请及时签退。' }
]

const faqs = [
  {
    q: '如何预约座位？',
    a: [
      '进入「座位地图」，点击绿色（空闲）座位，选择日期和预约时间段后点击「确认预约」即可。',
      '预约成功后，该座位将显示为占用状态。'
    ]
  },
  {
    q: '如何取消预约？',
    a: ['进入「预约记录」，找到进行中的预约记录，点击「取消预约」按钮即可。', '取消后座位将被释放，可再次预约。']
  },
  {
    q: '一次可以预约多久？',
    a: ['单次预约最短10分钟，可预约至当日 21:30 闭馆（提前一天预约可约全天）。', '预约时段以 10 分钟为单位。']
  },
  {
    q: '预约时段有什么限制？',
    a: [
      '自习室开放时间为每日 08:00 - 21:30，预约时间必须在此范围内。',
      '当天预约的开始时间不能早于当前时间，请至少提前10分钟；提前一天预约可约全天。'
    ]
  },
  {
    q: '为什么提示「已有正在进行的预约」？',
    a: ['同一时间段内，每位用户只能有一个进行中的预约。', '如需预约其他座位，请先取消当前预约。']
  },
  {
    q: '什么是违约？什么情况会被记违约？',
    a: [
      '预约后若始终未签到，直到预约结束时间仍未签到，将被系统记为违约。',
      '只要在预约结束前完成签到（可提前 30 分钟签到）就不会违约；如不能到馆，请提前在「预约记录」中取消预约。'
    ]
  },
  {
    q: '忘记密码怎么办？',
    a: ['请联系管理员重置密码。', '重置后初始密码为身份证号后6位，登录后请及时修改。']
  },
  {
    q: '如何修改密码？',
    a: [
      '进入「个人中心」，在「修改密码」区域输入当前密码和新密码即可。',
      '密码需包含大小写字母、数字和特殊字符，长度不少于8位。'
    ]
  }
]

const contacts = [
  { icon: '📍', label: '自习室位置', value: '图书馆三楼 / 四楼' },
  { icon: '🕐', label: '服务时间', value: '每日 08:00 - 21:30' },
  { icon: '📧', label: '意见反馈', value: 'service@studyroom.example.com' },
  { icon: '☎️', label: '联系电话', value: '010-8888-6666' }
]

const filteredFaqs = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return faqs
  return faqs.filter((f) => f.q.toLowerCase().includes(kw) || f.a.some((line) => line.toLowerCase().includes(kw)))
})

const toggleFaq = (idx: number) => {
  openIndex.value = openIndex.value === idx ? -1 : idx
}
</script>

<style scoped>
.help-page {
  width: 100%;
  max-width: 980px;
  margin: 0 auto;
  padding: 20px 24px 48px;
  box-sizing: border-box;
}

/* ---- 页头 ---- */
.help-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin: 2px 2px 18px;
}
.help-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  margin: 0 0 6px;
  letter-spacing: 0.5px;
}
.help-subtitle {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}
.help-time-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-secondary);
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 8px 14px;
  white-space: nowrap;
}
.help-time-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4caf50;
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.18);
}

/* ---- 分类页签 ---- */
.help-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 6px;
  margin-bottom: 20px;
}
.help-tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
  padding: 9px 16px;
  border-radius: 11px;
  cursor: pointer;
  transition: all 0.2s;
}
.help-tab:hover {
  color: var(--text-primary);
  background: var(--sidebar-hover);
}
.help-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);
}

/* ---- 通用卡片 ---- */
.panel {
  list-style: none;
  margin: 0;
  padding: 6px 22px;
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}
.pane-intro {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 2px 12px;
}

/* ---- 使用指南 ---- */
.guide-list {
  padding: 8px 24px;
}
.guide-step {
  position: relative;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  column-gap: 16px;
  align-items: start;
  padding: 16px 0;
  border-bottom: 1px dashed var(--border-color);
}
.guide-step:last-child {
  border-bottom: none;
}
/* 连接序号圆点的竖向引导线，让各步骤处于同一条竖直线上 */
.guide-step:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 21px;
  top: 48px;
  bottom: -16px;
  width: 2px;
  background: linear-gradient(to bottom, rgba(102, 126, 234, 0.45), rgba(102, 126, 234, 0.08));
  border-radius: 2px;
}
.step-num {
  justify-self: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(102, 126, 234, 0.3);
}
.guide-step-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 32px;
}
.guide-step-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
  line-height: 1.7;
}

/* ---- 规则 / 注意事项列表 ---- */
.line-list {
  padding: 6px 22px;
}
.line-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 0;
  border-bottom: 1px dashed var(--border-color);
}
.line-list li:last-child {
  border-bottom: none;
}
.line-icon {
  flex-shrink: 0;
  font-size: 17px;
  line-height: 1.5;
}
.line-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.65;
}

/* 注意事项 - 强调样式 */
.notice-head {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 152, 0, 0.08);
  border: 1px solid rgba(255, 152, 0, 0.3);
  border-radius: 14px;
  padding: 12px 18px;
  margin-bottom: 14px;
}
.notice-head-icon {
  font-size: 26px;
}
.notice-head-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.notice-head-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}
.notice-list {
  background: rgba(255, 152, 0, 0.04);
  border: 1px solid rgba(255, 152, 0, 0.22);
}

/* ---- FAQ ---- */
.faq-search {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 0 14px;
  margin-bottom: 14px;
}
.faq-search:focus-within {
  border-color: rgba(102, 126, 234, 0.7);
}
.faq-search-icon {
  color: var(--text-secondary);
  font-size: 14px;
}
.faq-search-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  color: var(--text-primary);
  font-size: 14px;
  padding: 11px 0;
}
.faq-empty {
  text-align: center;
  color: var(--text-secondary);
  font-size: 13px;
  padding: 30px 0;
}
.faq-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.faq-item {
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.faq-item.open {
  border-color: rgba(102, 126, 234, 0.55);
}
.faq-question {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-primary);
  font-size: 14px;
  text-align: left;
}
.faq-q {
  font-weight: 500;
}
.faq-arrow {
  color: var(--text-secondary);
  font-size: 13px;
  flex-shrink: 0;
  transition: color 0.2s;
}
.faq-item.open .faq-arrow {
  color: #667eea;
}
.faq-answer {
  padding: 0 18px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.faq-answer p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.75;
  margin: 0;
}

/* ---- 联系我们 ---- */
.contact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.contact-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--sidebar-bg);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 16px 18px;
}
.contact-icon {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--sidebar-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}
.contact-label {
  font-size: 12px;
  color: var(--text-secondary);
}
.contact-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 3px;
  word-break: break-all;
}

/* ---- 响应式 ---- */
@media (max-width: 720px) {
  .help-page {
    padding: 14px 14px 40px;
  }
  .help-header {
    align-items: flex-start;
  }
  .help-tab {
    padding: 8px 12px;
  }
  .contact-grid {
    grid-template-columns: 1fr;
  }
  .panel {
    padding: 4px 16px;
  }
}
</style>
