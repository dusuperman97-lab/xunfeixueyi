<template>
  <div class="universe-wrapper">
    <!-- 3D WebGL Canvas 容器 -->
    <div ref="canvasContainer" class="universe-canvas"></div>

    <!-- 悬浮 Hover 提示 Tooltip -->
    <div
        ref="tooltipRef"
        class="universe-tooltip"
        :style="tooltipStyle"
    >
      {{ tooltipText }}
    </div>

    <!-- UI 图层：顶部 Header -->
    <div class="ui-layer header-panel">
      <div class="title-box">
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <div class="glass-card metrics-strip interactive">
        <div class="metric-item">
          <strong>{{ stages.length }}</strong>
          <span>阶段星球</span>
        </div>
        <div class="metric-item">
          <strong style="color: #a855f7">{{ totalTasks }}</strong>
          <span>核心任务</span>
        </div>
        <div class="metric-item">
          <strong style="color: #ef4444">{{ highPriorityCount }}</strong>
          <span>高优先</span>
        </div>
      </div>
    </div>

    <!-- UI 图层：左侧阶段导航列表 -->
    <div class="ui-layer left-nav glass-card interactive">
      <div class="nav-title">星系阶段导航</div>
      <div
          v-for="(st, idx) in stages"
          :key="st.id || idx"
          class="stage-btn"
          :class="{ active: activeStageIndex === idx }"
          @click="selectPlanet(idx)"
      >
        <span>{{ getShortTitle(st.title) }}</span>
        <span class="badge" :style="{ borderColor: st.color }">
          {{ st.tasks ? st.tasks.length : 0 }}任务
        </span>
      </div>
    </div>

    <!-- UI 图层：右侧毛玻璃行动抽屉 -->
    <div
        class="ui-layer right-drawer glass-card interactive"
        :class="{ open: isDrawerOpen }"
    >
      <!-- 阶段总览模式 -->
      <template v-if="activeStage && !selectedTaskData">
        <div class="drawer-kicker">STAGE 0{{ activeStageIndex + 1 }}</div>
        <div class="drawer-title">{{ activeStage.title }}</div>
        <div class="drawer-desc">{{ activeStage.desc }}</div>

        <div class="nav-title">学习目标</div>
        <div class="stage-goals">
          <div v-for="(task, gi) in activeStage.tasks" :key="gi" class="goal-item">
            <span class="goal-dot" :style="{ background: task.priority === 'high' ? '#ef4444' : '#38bdf8' }"></span>
            <span>{{ task.name }}</span>
          </div>
        </div>

        <div class="nav-title">课程任务卫星</div>
        <div class="drawer-tasks">
          <div
              v-for="(task, tIdx) in activeStage.tasks"
              :key="tIdx"
              class="task-card-mini"
              :class="{ expanded: expandedTaskIdx === tIdx }"
              @click="toggleTaskExpand(tIdx)"
          >
            <div class="head">
              <span class="title">{{ task.name }}</span>
              <span
                  class="priority-tag"
                  :class="task.priority === 'high' ? 'pri-high' : 'pri-mid'"
              >
                {{ task.priority === 'high' ? '高优先' : '中优先' }}
              </span>
            </div>
            <div class="task-summary">{{ task.summary }}</div>
            <transition name="task-detail">
              <div v-if="expandedTaskIdx === tIdx" class="task-detail-panel">
                <div v-if="task.tags && task.tags.length" class="task-tags">
                  <span v-for="tag in task.tags" :key="tag" class="task-tag">{{ tag }}</span>
                </div>
                <div v-if="task.reason" class="task-reason">
                  <span class="reason-label">学习建议</span>
                  <p>{{ task.reason }}</p>
                </div>
                <div class="task-plan">
                  <span class="reason-label">学习计划</span>
                  <ol>
                    <li v-for="(step, si) in (task.learningPlan || [])" :key="si">{{ step }}</li>
                  </ol>
                </div>
                <div class="task-nav-actions">
                  <button
                      v-for="link in (task.navLinks || [])"
                      :key="link.key"
                      class="nav-action-btn"
                      @click.stop="navigateTo(link.route)"
                  >
                    {{ link.label }}
                  </button>
                  <button v-if="task.videoUrl" class="nav-action-btn nav-video" @click.stop="openVideo(task.videoUrl)">
                    观看视频
                  </button>
                </div>
              </div>
            </transition>
          </div>
        </div>
        <button class="control-btn interactive reset-btn" @click="closeDrawer">
          返回全景宇宙
        </button>
      </template>

      <!-- 任务详情模式（点击卫星后） -->
      <template v-if="selectedTaskData">
        <button class="drawer-back-btn" @click="backToStageView">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          返回阶段总览
        </button>
        <div class="drawer-kicker">{{ selectedTaskData.stageTitle }}</div>
        <div class="drawer-title">{{ selectedTaskData.name }}</div>
        <div class="drawer-meta">
          <span class="priority-tag" :class="selectedTaskData.priority === 'high' ? 'pri-high' : 'pri-mid'">
            {{ selectedTaskData.priority === 'high' ? '高优先' : '中优先' }}
          </span>
          <span v-if="selectedTaskData.videoUrl" class="has-video-badge">含视频</span>
        </div>
        <div class="drawer-desc">{{ selectedTaskData.summary }}</div>

        <div v-if="selectedTaskData.tags && selectedTaskData.tags.length" class="nav-title">知识标签</div>
        <div v-if="selectedTaskData.tags && selectedTaskData.tags.length" class="task-tags">
          <span v-for="tag in selectedTaskData.tags" :key="tag" class="task-tag">{{ tag }}</span>
        </div>

        <div v-if="selectedTaskData.reason" class="nav-title">学习建议</div>
        <div v-if="selectedTaskData.reason" class="task-reason">
          <p>{{ selectedTaskData.reason }}</p>
        </div>

        <div class="nav-title">学习计划</div>
        <div class="task-plan">
          <ol>
            <li v-for="(step, si) in (selectedTaskData.learningPlan || [])" :key="si">{{ step }}</li>
          </ol>
        </div>

        <div class="nav-title">下一步行动</div>
        <div class="next-action-box">{{ selectedTaskData.nextAction || '按阶段顺序学习，并用知识库补齐概念连接。' }}</div>

        <div class="nav-title">导航到学习任务</div>
        <div class="task-nav-grid">
          <button
              v-for="link in (selectedTaskData.navLinks || [])"
              :key="link.key"
              class="nav-grid-btn"
              @click="navigateTo(link.route)"
          >
            <strong>{{ link.label }}</strong>
            <small>{{ link.hint }}</small>
          </button>
          <button v-if="selectedTaskData.videoUrl" class="nav-grid-btn nav-grid-video" @click="openVideo(selectedTaskData.videoUrl)">
            <strong>观看视频</strong>
            <small>先看视频建立直觉</small>
          </button>
        </div>

        <button class="control-btn interactive reset-btn" @click="closeDrawer">
          返回全景宇宙
        </button>
      </template>
    </div>

    <!-- UI 图层：底部悬浮控制工具栏 -->
    <div class="ui-layer bottom-bar glass-card interactive">
      <button class="control-btn" @click="resetCamera">重置视角</button>
      <button class="control-btn" @click="toggleRotation">
        {{ isAutoRotating ? '暂停自转' : '开启自转' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, reactive, watch, nextTick } from 'vue'
import * as THREE from 'three'

// --- Props & Emits ---
const props = defineProps({
  title: {
    type: String,
    default: '知识宇宙 · 个性化学习路线'
  },
  subtitle: {
    type: String,
    default: '基于诊断驱动与关联算法生成的 3D 粒子星系图谱'
  },
  // 注入的数据路线 (默认 fallback 为源码 1 的结构)
  stagesData: {
    type: Array,
    default: () => [
      {
        id: 'stage-1',
        title: '01. 基础构建与语言内核',
        desc: '掌握 Java Web 核心机制、Spring Boot 原理及 Linux 容器化部署。',
        color: '#38bdf8',
        pos: { x: -60, y: 10, z: 20 },
        radius: 12,
        tasks: [
          { name: 'Java Web 高级编程', priority: 'high', summary: '深入 Servlet、JVM 调优与并发模型' },
          { name: 'Linux 与 Docker 运维实战', priority: 'medium', summary: '掌握命令行、Shell 脚本及容器镜像搭建' }
        ]
      },
      {
        id: 'stage-2',
        title: '02. AI Agent 与 RAG 架构',
        desc: '围绕大模型 Agent 智能体设计、向量数据库与 RAG 检索增强架构。',
        color: '#a855f7',
        pos: { x: 20, y: -15, z: -40 },
        radius: 16,
        hasRing: true,
        tasks: [
          { name: 'RAG 检索增强生成原理', priority: 'high', summary: '向量切片、Embedding 索引与召回优化' },
          { name: 'LangChain & Agent 工作流', priority: 'high', summary: 'ReAct 范式、Tool Calling 与记忆链实现' },
          { name: '大模型微调与 Prompt 适配', priority: 'medium', summary: '针对特定领域的逻辑强化训练' }
        ]
      },
      {
        id: 'stage-3',
        title: '03. 知识图谱与复杂推理',
        desc: '使用 Neo4j 图数据库构建结构化知识关联，支持多跳推理。',
        color: '#f59e0b',
        pos: { x: 80, y: 25, z: 30 },
        radius: 14,
        tasks: [
          { name: 'Neo4j 图数据库建模', priority: 'high', summary: '节点、关系属性定义与 Cypher 语法' },
          { name: '知识图谱实体抽取', priority: 'medium', summary: '非结构化文本三元组自动解析' }
        ]
      },
      {
        id: 'stage-4',
        title: '04. 综合项目与诊断评测',
        desc: '全流程实战项目落地，配合 AI 智能体进行效果测评与迭代。',
        color: '#10b981',
        pos: { x: -20, y: -30, z: 80 },
        radius: 15,
        tasks: [
          { name: '诊断驱动学习中枢项目', priority: 'high', summary: '完整打通智能推荐与知识宇宙可视化' },
          { name: '全真模拟题库与闭环评测', priority: 'medium', summary: '基于能力画像的弱项专项突破' }
        ]
      }
    ]
  }
})

const emit = defineEmits(['select-stage', 'select-task', 'drawer-close', 'navigate'])

// --- 响应式数据 ---
const stages = computed(() => props.stagesData)
const activeStageIndex = ref(-1)
const isDrawerOpen = ref(false)
const isAutoRotating = ref(true)
const selectedTaskData = ref(null)
const expandedTaskIdx = ref(-1)

const totalTasks = computed(() => {
  return stages.value.reduce((acc, st) => acc + (st.tasks ? st.tasks.length : 0), 0)
})

const highPriorityCount = computed(() => {
  return stages.value.reduce((acc, st) => {
    return acc + (st.tasks ? st.tasks.filter(t => t.priority === 'high').length : 0)
  }, 0)
})

const activeStage = computed(() => {
  if (activeStageIndex.value >= 0 && activeStageIndex.value < stages.value.length) {
    return stages.value[activeStageIndex.value]
  }
  return null
})

// --- Tooltip 状态 ---
const tooltipText = ref('')
const tooltipStyle = reactive({
  opacity: 0,
  left: '0px',
  top: '0px'
})

// --- Three.js & DOM Refs ---
const canvasContainer = ref(null)
const tooltipRef = ref(null)

let scene, camera, renderer, animFrameId
let planetObjects = []
let raycaster, mouse
let isMouseDown = false
let mouseX = 0, mouseY = 0

// 摄像机控制变量 (FlyTo 插值)
const targetCameraPos = new THREE.Vector3(0, 40, 180)
const targetLookAt = new THREE.Vector3(0, 0, 0)
const currentLookAt = new THREE.Vector3(0, 0, 0)

// --- GLSL 着色器定义 ---
const vertexShader = `
  attribute float size;
  attribute vec3 customColor;
  attribute float orbitSpeed;
  attribute float isRing;

  varying vec3 vColor;
  varying float vDist;
  varying float vIsRing;

  uniform float uTime;
  uniform float uScale;

  mat2 rotate2d(float angle) {
      return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
  }

  void main() {
      vec3 pos = position;

      if (isRing > 0.5) {
          float angle = uTime * orbitSpeed * 0.3;
          vec2 rotated = rotate2d(angle) * pos.xz;
          pos.x = rotated.x; pos.z = rotated.y;
      } else {
          float angle = uTime * 0.05;
          vec2 rotated = rotate2d(angle) * pos.xz;
          pos.x = rotated.x; pos.z = rotated.y;
      }

      vec4 mvPosition = modelViewMatrix * vec4(pos * uScale, 1.0);
      vDist = -mvPosition.z;
      gl_Position = projectionMatrix * mvPosition;

      float pSize = size * (320.0 / vDist);
      gl_PointSize = clamp(pSize, 1.0, 150.0);

      vColor = customColor;
      vIsRing = isRing;
  }
`

const fragmentShader = `
  varying vec3 vColor;
  varying float vDist;
  varying float vIsRing;

  void main() {
      vec2 cxy = 2.0 * gl_PointCoord - 1.0;
      float r = dot(cxy, cxy);
      if (r > 1.0) discard;

      float glow = smoothstep(1.0, 0.2, r);
      vec3 finalCol = vColor * 1.3;

      gl_FragColor = vec4(finalCol, glow * 0.85);
  }
`

// --- Three.js 初始化流程 ---
const initThree = () => {
  if (!canvasContainer.value) return

  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight

  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x030308, 0.001)

  camera = new THREE.PerspectiveCamera(60, width / height, 0.1, 2000)
  camera.position.copy(targetCameraPos)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' })
  renderer.setSize(width, height)
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  canvasContainer.value.appendChild(renderer.domElement)

  raycaster = new THREE.Raycaster()
  mouse = new THREE.Vector2()

  // 构建星盘与场景
  createStarfield()
  createPlanets()
  createOrbitConnections()

  // 绑定事件
  window.addEventListener('resize', onWindowResize)
  const dom = renderer.domElement
  dom.addEventListener('mousedown', onMouseDown)
  dom.addEventListener('mousemove', onMouseMove)
  dom.addEventListener('mouseup', () => (isMouseDown = false))
  dom.addEventListener('wheel', onMouseWheel)
  dom.addEventListener('click', onClick)

  animate(0)
}

// 1. 创建背景星空粒子
const createStarfield = () => {
  const count = 12000
  const geo = new THREE.BufferGeometry()
  const positions = new Float32Array(count * 3)
  const colors = new Float32Array(count * 3)

  for (let i = 0; i < count; i++) {
    positions[i * 3] = (Math.random() - 0.5) * 1200
    positions[i * 3 + 1] = (Math.random() - 0.5) * 1200
    positions[i * 3 + 2] = (Math.random() - 0.5) * 1200

    const c = new THREE.Color().setHSL(Math.random(), 0.5, 0.8)
    colors[i * 3] = c.r
    colors[i * 3 + 1] = c.g
    colors[i * 3 + 2] = c.b
  }

  geo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geo.setAttribute('color', new THREE.BufferAttribute(colors, 3))

  const mat = new THREE.PointsMaterial({
    size: 1.5,
    vertexColors: true,
    transparent: true,
    opacity: 0.65
  })

  scene.add(new THREE.Points(geo, mat))
}

// 2. 创建粒子星球与公转卫星
const createPlanets = () => {
  planetObjects = []

  stages.value.forEach((data, index) => {
    const planetGroup = new THREE.Group()
    planetGroup.position.set(data.pos.x, data.pos.y, data.pos.z)

    const particleCount = 20000
    const geo = new THREE.BufferGeometry()
    const pos = new Float32Array(particleCount * 3)
    const cols = new Float32Array(particleCount * 3)
    const sizes = new Float32Array(particleCount)
    const speeds = new Float32Array(particleCount)
    const isRings = new Float32Array(particleCount)

    const baseColor = new THREE.Color(data.color)

    for (let i = 0; i < particleCount; i++) {
      let x, y, z, isRing = 0

      if (data.hasRing && i > particleCount * 0.7) {
        isRing = 1
        const r = data.radius * (1.5 + Math.random() * 0.8)
        const theta = Math.random() * Math.PI * 2
        x = r * Math.cos(theta)
        z = r * Math.sin(theta)
        y = (Math.random() - 0.5) * 1.5
      } else {
        const u = Math.random(), v = Math.random()
        const theta = 2 * Math.PI * u
        const phi = Math.acos(2 * v - 1)
        const r = data.radius * (0.95 + Math.random() * 0.1)

        x = r * Math.sin(phi) * Math.cos(theta)
        y = r * Math.cos(phi)
        z = r * Math.sin(phi) * Math.sin(theta)
      }

      pos[i * 3] = x
      pos[i * 3 + 1] = y
      pos[i * 3 + 2] = z

      const c = baseColor.clone().offsetHSL(0, 0, (Math.random() - 0.5) * 0.2)
      cols[i * 3] = c.r
      cols[i * 3 + 1] = c.g
      cols[i * 3 + 2] = c.b

      sizes[i] = isRing ? 0.8 + Math.random() * 0.5 : 1.2 + Math.random() * 0.8
      speeds[i] = isRing ? 5.0 / Math.sqrt(x * x + z * z) : 0
      isRings[i] = isRing
    }

    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    geo.setAttribute('customColor', new THREE.BufferAttribute(cols, 3))
    geo.setAttribute('size', new THREE.BufferAttribute(sizes, 1))
    geo.setAttribute('orbitSpeed', new THREE.BufferAttribute(speeds, 1))
    geo.setAttribute('isRing', new THREE.BufferAttribute(isRings, 1))

    const uniforms = {
      uTime: { value: 0 },
      uScale: { value: 1.0 }
    }

    const mat = new THREE.ShaderMaterial({
      depthWrite: false,
      blending: THREE.AdditiveBlending,
      uniforms: uniforms,
      vertexShader: vertexShader,
      fragmentShader: fragmentShader,
      transparent: true
    })

    const particles = new THREE.Points(geo, mat)
    planetGroup.add(particles)

    // Raycast 点击碰撞检测隐形 Mesh
    const hitGeo = new THREE.SphereGeometry(data.radius * 1.2, 16, 16)
    const hitMat = new THREE.MeshBasicMaterial({ visible: false })
    const hitMesh = new THREE.Mesh(hitGeo, hitMat)
    hitMesh.userData = { id: data.id, index: index }
    planetGroup.add(hitMesh)

    // 绘制环绕此星球的课程任务卫星
    const satMeshes = []
    if (data.tasks) {
      data.tasks.forEach((task, tIdx) => {
        const satAngle = (tIdx / data.tasks.length) * Math.PI * 2
        const satDist = data.radius * 2.2
        const satGeo = new THREE.SphereGeometry(Math.max(1.5, data.radius * 0.12), 16, 16)
        const satMat = new THREE.MeshBasicMaterial({
          color: task.priority === 'high' ? 0xef4444 : 0x38bdf8,
          transparent: true,
          opacity: 0.85
        })
        const satMesh = new THREE.Mesh(satGeo, satMat)
        satMesh.position.set(satDist * Math.cos(satAngle), (tIdx - 1) * 3, satDist * Math.sin(satAngle))
        satMesh.userData = { type: 'satellite', stageIndex: index, taskIndex: tIdx, task }
        planetGroup.add(satMesh)
        satMeshes.push(satMesh)

        // 卫星光晕
        const glowGeo = new THREE.SphereGeometry(Math.max(2.5, data.radius * 0.2), 16, 16)
        const glowMat = new THREE.MeshBasicMaterial({
          color: task.priority === 'high' ? 0xef4444 : 0x38bdf8,
          transparent: true,
          opacity: 0.15
        })
        const glowMesh = new THREE.Mesh(glowGeo, glowMat)
        glowMesh.position.copy(satMesh.position)
        planetGroup.add(glowMesh)
      })
    }

    scene.add(planetGroup)
    planetObjects.push({ group: planetGroup, uniforms: uniforms, data: data, hitMesh: hitMesh, satMeshes })
  })
}

// 3. 路线轨迹能量虚线
const createOrbitConnections = () => {
  if (stages.value.length < 2) return
  const points = stages.value.map(d => new THREE.Vector3(d.pos.x, d.pos.y, d.pos.z))
  const curve = new THREE.CatmullRomCurve3(points)

  const curvePoints = curve.getPoints(100)
  const geo = new THREE.BufferGeometry().setFromPoints(curvePoints)
  const mat = new THREE.LineDashedMaterial({
    color: 0x38bdf8,
    dashSize: 3,
    gapSize: 2,
    opacity: 0.4,
    transparent: true
  })

  const line = new THREE.Line(geo, mat)
  line.computeLineDistances()
  scene.add(line)
}

// --- 动画主循环 ---
const animate = time => {
  animFrameId = requestAnimationFrame(animate)
  const sec = time * 0.001

  planetObjects.forEach(obj => {
    obj.uniforms.uTime.value = sec
    if (isAutoRotating.value) {
      obj.group.rotation.y = sec * 0.1
    }
  })

  // 视角 Smooth Lerp 飞跃
  camera.position.lerp(targetCameraPos, 0.05)
  currentLookAt.lerp(targetLookAt, 0.05)
  camera.lookAt(currentLookAt)

  renderer.render(scene, camera)
}

// --- 交互与点击控制 ---
const selectPlanet = index => {
  activeStageIndex.value = index
  selectedTaskData.value = null
  expandedTaskIdx.value = -1
  const obj = planetObjects[index]
  if (!obj) return

  const pPos = obj.group.position
  targetLookAt.copy(pPos)
  targetCameraPos.set(pPos.x + 20, pPos.y + 15, pPos.z + obj.data.radius * 3.5)

  isDrawerOpen.value = true
  emit('select-stage', { stage: obj.data, index })
}

const closeDrawer = () => {
  isDrawerOpen.value = false
  selectedTaskData.value = null
  expandedTaskIdx.value = -1
  resetCamera()
  emit('drawer-close')
}

const resetCamera = () => {
  activeStageIndex.value = -1
  targetCameraPos.set(0, 40, 180)
  targetLookAt.set(0, 0, 0)
}

const toggleRotation = () => {
  isAutoRotating.value = !isAutoRotating.value
}

const toggleTaskExpand = idx => {
  expandedTaskIdx.value = expandedTaskIdx.value === idx ? -1 : idx
}

const backToStageView = () => {
  selectedTaskData.value = null
}

const navigateTo = route => {
  emit('navigate', { route })
}

const openVideo = url => {
  if (url) window.open(url, '_blank')
}

const onTaskClick = (task, stage) => {
  // 点击卫星后切换到任务详情模式
  selectedTaskData.value = task
  emit('select-task', { task, stage })
}

const getShortTitle = fullTitle => {
  if (!fullTitle) return ''
  const parts = fullTitle.split(' ')
  return parts.length > 1 ? `${parts[0]} ${parts[1]}` : fullTitle
}

// --- 鼠标监听 ---
const onMouseDown = e => {
  isMouseDown = true
  mouseX = e.clientX
  mouseY = e.clientY
}

const onMouseMove = e => {
  if (isMouseDown) {
    const deltaX = e.clientX - mouseX
    const deltaY = e.clientY - mouseY

    const offset = targetCameraPos.clone().sub(targetLookAt)
    const radius = offset.length()

    let theta = Math.atan2(offset.x, offset.z) - deltaX * 0.005
    let phi = Math.acos(Math.min(Math.max(offset.y / radius, -1), 1)) - deltaY * 0.005
    phi = Math.min(Math.max(phi, 0.1), Math.PI - 0.1)

    targetCameraPos.x = targetLookAt.x + radius * Math.sin(phi) * Math.sin(theta)
    targetCameraPos.y = targetLookAt.y + radius * Math.cos(phi)
    targetCameraPos.z = targetLookAt.z + radius * Math.sin(phi) * Math.cos(theta)

    mouseX = e.clientX
    mouseY = e.clientY
  }

  // Raycaster Hover 效果
  const rect = renderer.domElement.getBoundingClientRect()
  mouse.x = ((e.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((e.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  // 先检测卫星
  const allSatMeshes = planetObjects.flatMap(p => p.satMeshes || [])
  const satIntersects = raycaster.intersectObjects(allSatMeshes)
  if (satIntersects.length > 0) {
    const ud = satIntersects[0].object.userData
    if (ud.type === 'satellite') {
      tooltipText.value = ud.task ? ud.task.name : '任务卫星'
      tooltipStyle.opacity = 1
      tooltipStyle.left = `${e.clientX + 12}px`
      tooltipStyle.top = `${e.clientY + 12}px`
      document.body.style.cursor = 'pointer'
      return
    }
  }
  // 再检测星球
  const hitMeshes = planetObjects.map(p => p.hitMesh)
  const intersects = raycaster.intersectObjects(hitMeshes)

  if (intersects.length > 0) {
    const idx = intersects[0].object.userData.index
    tooltipText.value = stages.value[idx].title
    tooltipStyle.opacity = 1
    tooltipStyle.left = `${e.clientX + 12}px`
    tooltipStyle.top = `${e.clientY + 12}px`
    document.body.style.cursor = 'pointer'
  } else {
    tooltipStyle.opacity = 0
    document.body.style.cursor = 'default'
  }
}

const onClick = () => {
  raycaster.setFromCamera(mouse, camera)
  // 先检测卫星
  const allSatMeshes = planetObjects.flatMap(p => p.satMeshes || [])
  const satIntersects = raycaster.intersectObjects(allSatMeshes)
  if (satIntersects.length > 0) {
    const ud = satIntersects[0].object.userData
    if (ud.type === 'satellite') {
      // 确保对应星球已选中
      if (activeStageIndex.value !== ud.stageIndex) {
        selectPlanet(ud.stageIndex)
      }
      onTaskClick(ud.task, planetObjects[ud.stageIndex].data)
      return
    }
  }
  // 再检测星球
  const hitMeshes = planetObjects.map(p => p.hitMesh)
  const intersects = raycaster.intersectObjects(hitMeshes)

  if (intersects.length > 0) {
    const idx = intersects[0].object.userData.index
    selectPlanet(idx)
  }
}

const onMouseWheel = e => {
  const dir = targetCameraPos.clone().sub(targetLookAt).normalize()
  targetCameraPos.addScaledVector(dir, e.deltaY * 0.1)
}

const onWindowResize = () => {
  if (!canvasContainer.value || !renderer || !camera) return
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

// 生命周期
onMounted(() => {
  nextTick(() => {
    initThree()
  })
})

onUnmounted(() => {
  if (animFrameId) cancelAnimationFrame(animFrameId)
  window.removeEventListener('resize', onWindowResize)
  if (renderer && renderer.domElement) {
    renderer.domElement.remove()
  }
})
</script>

<style scoped>
.universe-wrapper {
  position: relative;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  background-color: #030308;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", sans-serif;
  color: #fff;
}

.universe-canvas {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

/* Tooltip 提示 */
.universe-tooltip {
  position: absolute;
  z-index: 100;
  pointer-events: none;
  transition: opacity 0.2s ease;
  padding: 6px 12px;
  font-size: 12px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.85);
  border: 1px solid #38bdf8;
  color: #fff;
  white-space: nowrap;
}

/* UI 图层基础类 */
.ui-layer {
  position: absolute;
  z-index: 10;
  pointer-events: none;
}

.interactive {
  pointer-events: auto;
}

/* 毛玻璃统一卡片 */
.glass-card {
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
  border-radius: 12px;
}

/* Header 面板 */
.header-panel {
  top: 24px;
  left: 32px;
  right: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-box h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(90deg, #ffffff, #a5b4fc);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.title-box p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.metrics-strip {
  display: flex;
  gap: 16px;
  padding: 8px 16px;
}

.metric-item {
  display: flex;
  flex-direction: column;
}

.metric-item strong {
  font-size: 18px;
  color: #38bdf8;
  font-family: monospace;
}

.metric-item span {
  font-size: 11px;
  color: #64748b;
}

/* 左侧导航栏 */
.left-nav {
  top: 120px;
  left: 32px;
  width: 220px;
  padding: 16px;
}

.nav-title {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.stage-btn {
  width: 100%;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  color: #e2e8f0;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stage-btn:hover,
.stage-btn.active {
  background: rgba(56, 189, 248, 0.15);
  border-color: #38bdf8;
  color: #fff;
  transform: translateX(4px);
}

.stage-btn .badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid transparent;
}

/* 右侧抽屉 */
.right-drawer {
  top: 120px;
  right: 32px;
  width: 340px;
  padding: 20px;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  transform: translateX(400px);
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.right-drawer.open {
  transform: translateX(0);
}

/* 阶段学习目标列表 */
.stage-goals {
  margin-bottom: 16px;
}

.goal-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #cbd5e1;
  padding: 4px 0;
}

.goal-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 任务卡片展开状态 */
.task-card-mini.expanded {
  border-color: #38bdf8;
  background: rgba(56, 189, 248, 0.08);
}

/* 任务详情面板 */
.task-detail-panel {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

/* 任务标签 */
.task-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.task-tag {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(168, 85, 247, 0.15);
  color: #c4b5fd;
  border: 1px solid rgba(168, 85, 247, 0.25);
}

/* 学习建议 */
.task-reason {
  margin-bottom: 10px;
}

.reason-label {
  font-size: 11px;
  color: #38bdf8;
  font-weight: 600;
  display: block;
  margin-bottom: 4px;
}

.task-reason p {
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.5;
  margin: 0;
}

/* 学习计划 */
.task-plan {
  margin-bottom: 10px;
}

.task-plan ol {
  margin: 0;
  padding-left: 18px;
}

.task-plan li {
  font-size: 12px;
  color: #cbd5e1;
  line-height: 1.8;
}

/* 任务导航按钮行 */
.task-nav-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.nav-action-btn {
  font-size: 11px;
  padding: 5px 10px;
  border-radius: 4px;
  border: 1px solid rgba(56, 189, 248, 0.35);
  background: rgba(56, 189, 248, 0.1);
  color: #7dd3fc;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-action-btn:hover {
  background: rgba(56, 189, 248, 0.25);
}

.nav-action-btn.nav-video {
  border-color: rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.1);
  color: #fca5a5;
}

.nav-action-btn.nav-video:hover {
  background: rgba(239, 68, 68, 0.25);
}

/* 任务详情模式的返回按钮 */
.drawer-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  padding: 0 0 12px;
  transition: color 0.2s;
}

.drawer-back-btn:hover {
  color: #38bdf8;
}

/* 任务详情 meta 行 */
.drawer-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.has-video-badge {
  font-size: 10px;
  padding: 2px 8px;
  border-radius: 4px;
  background: rgba(239, 68, 68, 0.15);
  color: #fca5a5;
  border: 1px solid rgba(239, 68, 68, 0.25);
}

/* 下一步行动 */
.next-action-box {
  font-size: 12px;
  color: #e2e8f0;
  line-height: 1.5;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(16, 185, 129, 0.08);
  border: 1px solid rgba(16, 185, 129, 0.2);
  margin-bottom: 16px;
}

/* 导航网格 */
.task-nav-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 16px;
}

.nav-grid-btn {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.3);
  background: rgba(56, 189, 248, 0.08);
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.nav-grid-btn strong {
  font-size: 13px;
  color: #7dd3fc;
}

.nav-grid-btn small {
  font-size: 10px;
  color: #64748b;
}

.nav-grid-btn:hover {
  background: rgba(56, 189, 248, 0.2);
  border-color: #38bdf8;
}

.nav-grid-video {
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.08);
}

.nav-grid-video strong {
  color: #fca5a5;
}

.nav-grid-video:hover {
  background: rgba(239, 68, 68, 0.2);
  border-color: #ef4444;
}

/* 展开动画 */
.task-detail-enter-active,
.task-detail-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.task-detail-enter-from,
.task-detail-leave-to {
  opacity: 0;
  max-height: 0;
}

.task-detail-enter-to,
.task-detail-leave-from {
  opacity: 1;
  max-height: 500px;
}

.drawer-kicker {
  font-size: 11px;
  color: #38bdf8;
  font-weight: 700;
}

.drawer-title {
  font-size: 18px;
  font-weight: 600;
  margin: 6px 0 8px;
  color: #f8fafc;
}

.drawer-desc {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.6;
  margin-bottom: 16px;
}

.task-card-mini {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: border-color 0.2s ease;
}

.task-card-mini:hover {
  border-color: #38bdf8;
}

.task-card-mini .head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.task-card-mini .title {
  font-size: 14px;
  font-weight: 500;
  color: #e2e8f0;
}

.task-summary {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

.priority-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
}

.pri-high {
  background: rgba(239, 68, 68, 0.2);
  color: #fca5a5;
}

.pri-mid {
  background: rgba(245, 158, 11, 0.2);
  color: #fde047;
}

.pri-low {
  background: rgba(16, 185, 129, 0.2);
  color: #6ee7b7;
}

/* 底部控制栏 */
.bottom-bar {
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 8px 16px;
}

.control-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: #e2e8f0;
  padding: 8px 16px;
  font-size: 12px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.control-btn:hover {
  background: #38bdf8;
  color: #000;
}

.reset-btn {
  width: 100%;
  margin-top: 12px;
}
</style>