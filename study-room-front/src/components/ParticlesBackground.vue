<!-- src/components/ParticlesBackground.vue 星空粒子背景（复刻自 iindex.html） -->
<template>
  <canvas ref="canvasRef" class="particles-canvas" aria-hidden="true"></canvas>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)

let particles: Particle[] = []
let width = 0
let height = 0
let mouseX = -999
let mouseY = -999
let animationId = 0

const PARTICLE_COUNT = 100
const CONNECTION_DIST = 140
const MOUSE_RADIUS = 180

class Particle {
  x = 0
  y = 0
  vx = 0
  vy = 0
  size = 0
  opacity = 0
  twinkleSpeed = 0
  twinkleOffset = 0

  constructor() {
    this.reset()
    this.y = Math.random() * height
  }

  reset() {
    this.x = Math.random() * width
    this.y = Math.random() * height
    this.vx = (Math.random() - 0.5) * 0.6
    this.vy = (Math.random() - 0.5) * 0.6
    this.size = Math.random() * 2.2 + 0.6
    this.opacity = Math.random() * 0.7 + 0.3
    this.twinkleSpeed = Math.random() * 0.02 + 0.005
    this.twinkleOffset = Math.random() * Math.PI * 2
  }

  update() {
    this.x += this.vx
    this.y += this.vy
    // 边界回绕
    if (this.x < -20) this.x = width + 20
    if (this.x > width + 20) this.x = -20
    if (this.y < -20) this.y = height + 20
    if (this.y > height + 20) this.y = -20
    // 靠近鼠标时轻微偏移
    const dx = mouseX - this.x
    const dy = mouseY - this.y
    const dist = Math.sqrt(dx * dx + dy * dy)
    if (dist < MOUSE_RADIUS && dist > 0) {
      const force = (MOUSE_RADIUS - dist) / MOUSE_RADIUS
      this.x -= (dx / dist) * force * 1.2
      this.y -= (dy / dist) * force * 1.2
    }
  }

  draw(ctx: CanvasRenderingContext2D, time: number) {
    const twinkle = Math.sin(time * this.twinkleSpeed + this.twinkleOffset) * 0.35 + 0.65
    const alpha = this.opacity * twinkle
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(180,200,255,${alpha})`
    ctx.fill()
    // 光晕
    if (this.size > 1.5) {
      ctx.beginPath()
      ctx.arc(this.x, this.y, this.size * 3, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(140,160,240,${alpha * 0.15})`
      ctx.fill()
    }
  }
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = width
  canvas.height = height
}

function animate(time: number) {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)
  // 更新并绘制粒子
  particles.forEach((p) => {
    p.update()
    p.draw(ctx, time)
  })
  // 绘制连线
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < CONNECTION_DIST) {
        const alpha = (1 - dist / CONNECTION_DIST) * 0.28
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.strokeStyle = `rgba(160,180,230,${alpha})`
        ctx.lineWidth = 0.7
        ctx.stroke()
      }
    }
  }
  // 鼠标连线
  if (mouseX > -100 && mouseY > -100) {
    particles.forEach((p) => {
      const dx = mouseX - p.x
      const dy = mouseY - p.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < MOUSE_RADIUS) {
        const alpha = (1 - dist / MOUSE_RADIUS) * 0.5
        ctx.beginPath()
        ctx.moveTo(mouseX, mouseY)
        ctx.lineTo(p.x, p.y)
        ctx.strokeStyle = `rgba(180,210,255,${alpha})`
        ctx.lineWidth = 0.8
        ctx.stroke()
      }
    })
  }
  animationId = requestAnimationFrame(animate)
}

function handleMouseMove(e: MouseEvent) {
  mouseX = e.clientX
  mouseY = e.clientY
}
function handleMouseLeave() {
  mouseX = -999
  mouseY = -999
}
function handleTouchMove(e: TouchEvent) {
  if (e.touches.length) {
    mouseX = e.touches[0].clientX
    mouseY = e.touches[0].clientY
  }
}
function handleTouchEnd() {
  mouseX = -999
  mouseY = -999
}

onMounted(() => {
  resize()
  window.addEventListener('resize', resize)
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseleave', handleMouseLeave)
  document.addEventListener('touchmove', handleTouchMove, { passive: true })
  document.addEventListener('touchend', handleTouchEnd)
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push(new Particle())
  }
  animationId = requestAnimationFrame(animate)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseleave', handleMouseLeave)
  document.removeEventListener('touchmove', handleTouchMove)
  document.removeEventListener('touchend', handleTouchEnd)
  cancelAnimationFrame(animationId)
  particles = []
})
</script>

<style scoped>
.particles-canvas {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
  opacity: var(--particles-opacity, 1);
  transition: opacity 0.4s ease;
}
</style>
