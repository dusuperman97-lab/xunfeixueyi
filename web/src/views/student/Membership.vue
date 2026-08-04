<template>
  <div class="membership-page" ref="pageRoot" :style="{ '--shimmer-state': shimmerState }">

    <!-- ============================================================
         HEADER
         ============================================================ -->
    <header class="page-hero">
      <div class="hero-badge">
        <span class="badge-dot"></span>
        积分中心
      </div>
      <h1 class="hero-title">为你的 AI 学习之旅<span class="hero-highlight">持续充能</span></h1>
      <p class="hero-desc">积分是平台AI服务的通用"燃料"，按需补给，灵活使用</p>
    </header>

    <!-- ============================================================
         MEMBER LEVEL CARD  (新增：仿图片钻石会员卡)
         ============================================================ -->
    <section class="member-card-section">
      <div class="member-card">
        <!-- Subtle geometric texture via CSS -->
        <div class="member-card-texture"></div>
        <div class="member-card-shine"></div>

        <div class="member-card-content">
          <div class="member-info">
            <div class="member-header">
              <span class="member-crown">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M2 4l3 12h14l3-12-6 4-3-6-3 6-6-4z"/>
                </svg>
              </span>
              <h2 class="member-title">{{ memberLevel }}</h2>
              <span class="member-tag">当前等级</span>
            </div>

            <div class="member-progress-area">
              <div class="member-progress-label">
                <span>成长值需达到 {{ targetGrowth }}</span>
                <span class="member-progress-current">{{ currentGrowth }} / {{ targetGrowth }}</span>
              </div>
              <div class="member-progress-track">
                <div class="member-progress-fill" :style="{ width: growthPercent + '%' }">
                  <div class="progress-glow"></div>
                </div>
              </div>
              <p class="member-progress-hint">
                还需 <strong>{{ remainingGrowth }}</strong> 成长值可升级
              </p>
            </div>

            <div class="member-benefits">
              <span class="benefit-pill" v-for="b in memberBenefits" :key="b">{{ b }}</span>
            </div>
          </div>

          <!-- 3D 钻石 SVG -->
          <div class="member-diamond">
            <svg class="diamond-svg" viewBox="0 0 140 140" fill="none">
              <defs>
                <linearGradient id="dTop" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#e9d5ff"/>
                  <stop offset="100%" stop-color="#c4b5fd"/>
                </linearGradient>
                <linearGradient id="dLeft" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#a78bfa"/>
                  <stop offset="100%" stop-color="#8b5cf6"/>
                </linearGradient>
                <linearGradient id="dRight" x1="1" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#7c3aed"/>
                  <stop offset="100%" stop-color="#6d28d9"/>
                </linearGradient>
                <linearGradient id="dCenter" x1="0.5" y1="0" x2="0.5" y2="1">
                  <stop offset="0%" stop-color="#ddd6fe"/>
                  <stop offset="50%" stop-color="#a78bfa"/>
                  <stop offset="100%" stop-color="#5b21b6"/>
                </linearGradient>
                <filter id="dGlow" x="-20%" y="-20%" width="140%" height="140%">
                  <feGaussianBlur stdDeviation="3" result="blur"/>
                  <feComposite in="SourceGraphic" in2="blur" operator="over"/>
                </filter>
              </defs>
              <!-- 钻石主体 -->
              <g filter="url(#dGlow)">
                <!-- 上半部分 -->
                <path d="M70 18 L105 48 L70 70 L35 48 Z" fill="url(#dTop)"/>
                <!-- 左切面 -->
                <path d="M35 48 L70 70 L70 118 L20 58 Z" fill="url(#dLeft)"/>
                <!-- 右切面 -->
                <path d="M105 48 L70 70 L70 118 L120 58 Z" fill="url(#dRight)"/>
                <!-- 中心高光面 -->
                <path d="M70 70 L70 118 L45 95 L35 48 Z" fill="url(#dCenter)" opacity="0.9"/>
                <path d="M70 70 L70 118 L95 95 L105 48 Z" fill="url(#dCenter)" opacity="0.7"/>
                <!-- 顶部高光 -->
                <path d="M70 18 L85 32 L70 45 L55 32 Z" fill="#ffffff" opacity="0.6"/>
                <path d="M70 45 L85 32 L105 48 L70 70 Z" fill="#ffffff" opacity="0.25"/>
                <!-- 底部反光 -->
                <path d="M45 95 L70 118 L70 70 Z" fill="#ffffff" opacity="0.15"/>
              </g>
              <!-- 星星 sparkle -->
              <circle cx="45" cy="35" r="2" fill="#fff" opacity="0.9">
                <animate attributeName="opacity" values="0.9;0.3;0.9" dur="2s" repeatCount="indefinite"/>
              </circle>
              <circle cx="95" cy="55" r="1.5" fill="#fff" opacity="0.7">
                <animate attributeName="opacity" values="0.7;0.2;0.7" dur="2.5s" repeatCount="indefinite"/>
              </circle>
              <circle cx="60" cy="90" r="1" fill="#fff" opacity="0.8">
                <animate attributeName="opacity" values="0.8;0.3;0.8" dur="1.8s" repeatCount="indefinite"/>
              </circle>
            </svg>
            <div class="diamond-shadow"></div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============================================================
         BALANCE + QUICK STATS ROW
         ============================================================ -->
    <section class="overview-section">
      <!-- Main balance card with 3D tilt -->
      <div class="balance-card-stage" @mouseleave="onTiltLeave">
        <div class="balance-hero-card" :style="tiltStyle">
          <!-- Physical card elements -->
          <div class="balance-card-chip"></div>
          <div class="balance-contactless">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.3">
              <path d="M4 12c0-4.4 3.6-8 8-8s8 3.6 8 8"/>
              <path d="M7 12c0-2.8 2.2-5 5-5s5 2.2 5 5"/>
              <path d="M10 12c0-1.1.9-2 2-2s2 .9 2 2"/>
            </svg>
          </div>
          <div class="balance-card-edge"></div>
          <div class="balance-shimmer"></div>
          <!-- Logo watermark -->
          <div class="balance-card-watermark">
            <img src="@/assets/almightLogo_card.png" alt="" />
          </div>

          <div class="balance-content">
            <!-- Issuer logo area — top -->
            <div class="balance-card-top">
              <div class="balance-issuer-logo">
                <svg class="issuer-emblem" viewBox="0 0 32 32" fill="none">
                  <circle cx="16" cy="16" r="15" stroke="currentColor" stroke-width="1.5" opacity="0.6"/>
                  <path d="M10 22V10l6 9 6-9v12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span class="issuer-name">讯飞学弈</span>
              </div>
            </div>

            <!-- Points number — the "card number" -->
            <p class="balance-number">
              <template v-if="loadingBalance">
                <span class="balance-skeleton">----</span>
              </template>
              <template v-else>
                <span class="bal-animated">{{ formattedBalance }}</span>
              </template>
            </p>

            <!-- Energy gauge -->
            <div class="energy-gauge">
              <div class="gauge-track">
                <div class="gauge-fill" :style="{ width: energyLevel + '%' }"></div>
              </div>
              <span class="gauge-label">{{ energyLabel }}</span>
            </div>

            <!-- Card bottom row: label + network badge -->
            <div class="balance-card-bottom">
              <span class="balance-card-label">可用积分</span>
              <div class="balance-network-badge">
                <svg class="network-icon" viewBox="0 0 28 20" fill="none">
                  <circle cx="10" cy="10" r="9" fill="rgba(234,88,12,0.85)"/>
                  <circle cx="18" cy="10" r="9" fill="rgba(212,168,83,0.75)"/>
                  <circle cx="14" cy="10" r="5.5" fill="none" stroke="rgba(255,255,255,0.4)" stroke-width="1"/>
                </svg>
                <span class="network-label">PLATINUM</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 5×5 tilt tracker grid -->
        <div class="tilt-grid">
          <div
              v-for="n in 25"
              :key="n"
              class="tilt-cell"
              @mouseenter="onTiltEnter(n)"
          ></div>
        </div>
      </div>

      <!-- Stat cards column -->
      <div class="stats-column">
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-out">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/>
            </svg>
          </div>
          <div class="stat-info">
            <p class="stat-value">{{ monthlyStats.spent.toLocaleString() }}</p>
            <p class="stat-label">本月消耗</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-in">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/>
            </svg>
          </div>
          <div class="stat-info">
            <p class="stat-value">{{ monthlyStats.earned.toLocaleString() }}</p>
            <p class="stat-label">本月获得</p>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon-wrap stat-icon-usage">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>
            </svg>
          </div>
          <div class="stat-info">
            <p class="stat-value">{{ monthlyStats.usageCount }}</p>
            <p class="stat-label">本月服务次数</p>
          </div>
        </div>
      </div>
    </section>

    <!-- ============================================================
         TRANSACTION HISTORY TRIGGER
         ============================================================ -->
    <section class="history-trigger-section">
      <div class="history-trigger-card" @click="toggleHistory">
        <div class="history-trigger-left">
          <div class="history-trigger-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <path d="M3 9h18M9 21V9" />
            </svg>
          </div>
          <div>
            <h3 class="history-trigger-title">积分流水查询</h3>
            <p class="history-trigger-desc">查看积分收支明细，掌握每一笔变动</p>
          </div>
        </div>
        <div class="history-trigger-right">
          <span class="history-trigger-badge" v-if="monthlyStats.usageCount > 0">本月 {{ monthlyStats.usageCount }} 笔</span>
          <svg class="history-trigger-chevron" :class="{ 'is-open': showHistory }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 12 15 18 9" />
          </svg>
        </div>
      </div>
    </section>

    <!-- ============================================================
         SERVICE VALUE — What points unlock
         ============================================================ -->
    <section class="value-section">
      <div class="section-header">
        <h2 class="section-title">积分能做什么</h2>
        <p class="section-sub">每一分都用在刀刃上，解锁平台全部 AI 能力</p>
      </div>
      <div class="value-grid">
        <article class="value-card">
          <div class="value-icon-wrap value-icon-digital">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-7 8-7s8 3 8 7"/>
            </svg>
          </div>
          <h3 class="value-name">数字人对话</h3>
          <p class="value-desc">与 AI 数字人进行实时语音或文字对话，获取智能辅导与答疑</p>
          <div class="value-meta">
            <span class="value-cost">约 10 积分/次</span>
            <span class="value-tag">高频使用</span>
          </div>
        </article>
        <article class="value-card">
          <div class="value-icon-wrap value-icon-voice">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/><path d="M12 19v4M8 23h8"/>
            </svg>
          </div>
          <h3 class="value-name">语音合成</h3>
          <p class="value-desc">将文字转为自然流畅的语音，支持多种高品质音色与语速调节</p>
          <div class="value-meta">
            <span class="value-cost">约 20 积分/千字</span>
            <span class="value-tag">高品质</span>
          </div>
        </article>
        <article class="value-card">
          <div class="value-icon-wrap value-icon-model">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="2" y="2" width="20" height="8" rx="2"/><rect x="2" y="14" width="20" height="8" rx="2"/>
              <circle cx="8" cy="6" r="1" fill="currentColor"/><circle cx="8" cy="18" r="1" fill="currentColor"/>
            </svg>
          </div>
          <h3 class="value-name">高级模型</h3>
          <p class="value-desc">解锁 GPT-4 级别深度推理能力，处理复杂问题与知识分析</p>
          <div class="value-meta">
            <span class="value-cost">约 50 积分/次</span>
            <span class="value-tag">深度推理</span>
          </div>
        </article>
        <article class="value-card">
          <div class="value-icon-wrap value-icon-course">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/>
              <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/>
            </svg>
          </div>
          <h3 class="value-name">智能课程</h3>
          <p class="value-desc">AI 驱动个性化课程生成、知识图谱构建与自适应学习路径</p>
          <div class="value-meta">
            <span class="value-cost">约 100 积分/课</span>
            <span class="value-tag">个性化</span>
          </div>
        </article>
      </div>
    </section>

    <!-- ============================================================
         PACKAGES
         ============================================================ -->
    <section class="packages-section" ref="packagesRef">
      <div class="section-header">
        <h2 class="section-title">选择充值方案</h2>
        <p class="section-sub">灵活套餐，满足不同学习阶段的需求</p>
      </div>

      <div class="packages-grid">
        <article
            v-for="pkg in packages"
            :key="pkg.id"
            class="package-card"
            :class="{ 'is-featured': pkg.popular }"
        >
          <span v-if="pkg.badge" class="pkg-badge">{{ pkg.badge }}</span>

          <div class="pkg-top">
            <h3 class="pkg-name">{{ pkg.name }}</h3>
            <div class="pkg-price">
              <span class="pkg-currency">¥</span>
              <span class="pkg-amount">{{ pkg.price }}</span>
            </div>
            <p class="pkg-points">
              <span class="pkg-points-num">{{ pkg.points.toLocaleString() }}</span> 积分
              <span v-if="pkg.bonus" class="pkg-bonus-tag">赠 {{ pkg.bonus }}</span>
            </p>
          </div>

          <ul class="pkg-features">
            <li v-for="f in pkg.features" :key="f">
              <svg class="pkg-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
              {{ f }}
            </li>
          </ul>

          <button
              class="pkg-cta"
              :class="{ 'pkg-cta-featured': pkg.popular }"
              :disabled="recharging"
              @click="openConfirm(pkg)"
          >
            {{ pkg.cta }}
          </button>
        </article>
      </div>

      <!-- Custom recharge (inline, below packages) -->
      <div class="custom-recharge-bar">
        <div class="custom-recharge-left">
          <h3 class="custom-recharge-title">自定义金额</h3>
          <p class="custom-recharge-desc">按需输入积分数量，10 积分 = ¥1，最低 10 积分起</p>
        </div>
        <div class="custom-recharge-right">
          <div class="custom-input-group">
            <span class="custom-input-label">积分</span>
            <input
                v-model.number="customPoints"
                type="number"
                min="10"
                step="10"
                placeholder="输入数量"
                class="custom-input"
            />
          </div>
          <span class="custom-price-tag" v-if="customPoints >= 10">= ¥{{ customPrice }}</span>
          <button
              class="custom-cta"
              :disabled="!isCustomValid || recharging"
              @click="openConfirm(customPackage)"
          >
            立即充值
          </button>
        </div>
      </div>
    </section>

    <!-- ============================================================
         TRANSACTION HISTORY
         ============================================================ -->
    <transition name="section-expand">
      <section v-if="showHistory" class="history-section">
        <div class="section-header history-section-header">
          <h2 class="section-title">积分明细</h2>
          <div class="history-header-right">
            <div class="history-filter-row">
              <select v-model="txFilter.type" class="tx-filter-select" @change="onFilterChange">
                <option value="">全部类型</option>
                <option value="RECHARGE">充值</option>
                <option value="DEDUCTION">消费扣减</option>
                <option value="REFUND">退款</option>
                <option value="GIFT">赠送</option>
                <option value="ADMIN_ADJUST">管理员调整</option>
              </select>
              <input
                v-model="txFilter.startDate"
                type="date"
                class="tx-filter-date"
                @change="onFilterChange"
                placeholder="开始日期"
              />
              <span class="tx-filter-sep">至</span>
              <input
                v-model="txFilter.endDate"
                type="date"
                class="tx-filter-date"
                @change="onFilterChange"
                placeholder="结束日期"
              />
              <button v-if="hasActiveFilter" class="tx-filter-clear" @click="clearFilter">清除</button>
            </div>
            <button class="refresh-btn" @click="fetchTransactions">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
                <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
              </svg>
              刷新
            </button>
          </div>
        </div>

        <div v-if="loadingTx" class="tx-empty">加载中…</div>

        <template v-else>
          <div class="tx-table-wrap">
            <table v-if="transactions.length" class="tx-table">
              <thead>
              <tr>
                <th>时间</th>
                <th>类型</th>
                <th>积分变动</th>
                <th>余额</th>
                <th>说明</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="(tx, idx) in transactions" :key="tx.id ?? idx">
                <td class="tx-cell-time">{{ formatDate(tx.createTime || tx.createdAt) }}</td>
                <td>
                  <span class="tx-type-badge" :class="isTxPositive(tx) ? 'tx-in' : 'tx-out'">
                    {{ txTypeLabel(tx) }}
                  </span>
                </td>
                <td>
                  <span class="tx-amount" :class="getTxAmount(tx) >= 0 ? 'tx-positive' : 'tx-negative'">
                    {{ getTxAmount(tx) > 0 ? '+' : '' }}{{ getTxAmount(tx) }}
                  </span>
                </td>
                <td class="tx-cell-balance">{{ tx.balanceAfter ?? tx.afterBalance ?? '--' }}</td>
                <td class="tx-cell-desc">{{ tx.description || '--' }}</td>
              </tr>
              </tbody>
            </table>
            <div v-else class="tx-empty">暂无积分变动记录</div>
          </div>

          <div class="tx-pagination">
            <button class="tx-page-btn" :disabled="pageNum <= 1" @click="changePage(-1)">上一页</button>
            <span class="tx-page-num">第 {{ pageNum }} 页</span>
            <button class="tx-page-btn" :disabled="!hasMore" @click="changePage(1)">下一页</button>
          </div>
        </template>
      </section>
    </transition>

    <!-- ============================================================
         CONFIRM MODAL
         ============================================================ -->
    <transition name="modal-fade">
      <div v-if="confirmTarget" class="modal-backdrop" @click.self="closeConfirm">
        <div class="modal-dialog">
          <div class="modal-icon-wrap">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28">
              <rect x="2" y="3" width="20" height="14" rx="2"/>
              <path d="M8 21h8M12 17v4"/>
            </svg>
          </div>
          <h3 class="modal-title">确认充值</h3>
          <div class="modal-pkg-info">
            <span class="modal-pkg-name">{{ confirmTarget.name }}</span>
            <span class="modal-pkg-points">
              {{ confirmTarget.points.toLocaleString() }}
              <template v-if="confirmTarget.bonus">+ {{ confirmTarget.bonus }}</template>
              积分
            </span>
          </div>
          <p class="modal-price">¥{{ confirmTarget.price }}</p>
          <div class="modal-buttons">
            <button class="modal-btn-cancel" :disabled="recharging" @click="closeConfirm">取消</button>
            <button class="modal-btn-confirm" :disabled="recharging" @click="confirmRecharge">
              {{ recharging ? '处理中…' : '确认支付' }}
            </button>
          </div>
        </div>
      </div>
    </transition>

    <!-- ============================================================
         PAYMENT OVERLAY
         ============================================================ -->
    <transition name="modal-fade">
      <div v-if="paymentOverlay.show" class="payment-overlay">
        <div class="pay-dialog">
          <template v-if="paymentOverlay.phase === 'loading'">
            <div class="pay-spinner-wrap">
              <div class="pay-spinner-ring"></div>
              <svg class="pay-spinner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="3" width="20" height="14" rx="2"/>
                <path d="M8 21h8M12 17v4"/>
              </svg>
            </div>
            <h2 class="pay-title">正在连接支付宝</h2>
            <p class="pay-desc">为您生成支付订单，请稍候…</p>
          </template>
          <template v-else-if="paymentOverlay.phase === 'redirecting'">
            <div class="pay-check-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M8 12l3 3 5-5"/>
              </svg>
            </div>
            <h2 class="pay-title">正在跳转支付宝</h2>
            <p class="pay-desc">请在支付宝页面完成扫码支付</p>
            <p class="pay-amount">¥{{ paymentOverlay.amount }}</p>
            <div class="pay-status-hint">
              <span class="hint-dot"></span>
              支付完成后将自动返回本页面
            </div>
          </template>
        </div>
      </div>
    </transition>

    <!-- ============================================================
         TOAST
         ============================================================ -->
    <transition name="toast-slide">
      <div v-if="toast.show" class="global-toast" :class="toast.type">{{ toast.message }}</div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBalance, createAlipayPayment, queryPaymentOrder, getTransactions } from '@/api/member.js'

const route = useRoute()
const router = useRouter()

// ── Synced shimmer state — both cards start on the same frame ────
const pageRoot = ref(null)
const shimmerState = ref('paused')

// ── Member Level (新增) ──────────────────────────────────────────
const memberLevel = ref('钻石会员')
const currentGrowth = ref(0)
const targetGrowth = ref(3200)
const growthPercent = computed(() => {
  if (!targetGrowth.value) return 0
  return Math.min(100, Math.round((currentGrowth.value / targetGrowth.value) * 100))
})
const remainingGrowth = computed(() => Math.max(0, targetGrowth.value - currentGrowth.value))
const memberBenefits = ['专属客服', '优先排队', '双倍积分']

// ── Package definitions ──────────────────────────────────────────
const PRESET_PACKAGES = [
  {
    id: 'starter',
    name: '体验包',
    points: 100,
    bonus: 0,
    price: 9,
    cta: '立即充值',
    badge: '',
    popular: false,
    features: ['适合短期体验数字人对话', '标准合成音色', '不限设备登录']
  },
  {
    id: 'standard',
    name: '标准包',
    points: 500,
    bonus: 50,
    price: 45,
    cta: '立即充值',
    badge: '多数人选择',
    popular: false,
    features: ['加赠 10% 积分', '解锁高清数字人形象', '优先客服响应']
  },
  {
    id: 'premium',
    name: '尊享包',
    points: 1200,
    bonus: 240,
    price: 98,
    cta: '立即充值',
    badge: '限时加赠 20%',
    popular: true,
    features: ['加赠最多积分', '解锁全部高级数字人形象', '语音克隆功能', '专属客服通道']
  },
  {
    id: 'enterprise',
    name: '企业包',
    points: 5000,
    bonus: 1000,
    price: 398,
    cta: '联系开通',
    badge: '',
    popular: false,
    features: ['团队成员共享额度', 'API 接口调用权限', '专属技术支持', '定制数字人形象']
  }
]

const packages = PRESET_PACKAGES
const packagesRef = ref(null)

function scrollToPackages() {
  packagesRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// ── Balance ──────────────────────────────────────────────────────
const balance = ref(0)
const loadingBalance = ref(false)

const displayBalance = ref(0)
const formattedBalance = computed(() => {
  if (loadingBalance.value && !displayBalance.value) return '----'
  return displayBalance.value.toLocaleString()
})

function countUp(target) {
  const start = displayBalance.value
  const diff = target - start
  if (diff === 0) return
  const duration = 800
  const startTime = performance.now()
  function tick(now) {
    const elapsed = now - startTime
    const progress = Math.min(elapsed / duration, 1)
    // easeOutExpo
    const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress)
    displayBalance.value = Math.round(start + diff * eased)
    if (progress < 1) {
      requestAnimationFrame(tick)
    }
  }
  requestAnimationFrame(tick)
}

// Star particle positions (pre-computed, fixed per mount)
const starCount = 18
const starStyles = Array.from({ length: starCount }, () => {
  const top = Math.random() * 100
  const left = Math.random() * 100
  const size = 1 + Math.random() * 2.5
  const delay = Math.random() * 3
  const duration = 2 + Math.random() * 3
  return {
    top: top + '%',
    left: left + '%',
    width: size + 'px',
    height: size + 'px',
    animationDelay: delay + 's',
    animationDuration: duration + 's'
  }
})

// Energy gauge: maps balance to a 0–100 visual level
const energyLevel = computed(() => {
  if (!balance.value) return 0
  if (balance.value >= 5000) return 100
  if (balance.value >= 2000) return 75
  if (balance.value >= 500) return 50
  if (balance.value >= 100) return 25
  return Math.max(5, Math.floor((balance.value / 100) * 25))
})

const energyLabel = computed(() => {
  if (!balance.value) return '余额为空，快去充值吧'
  if (balance.value >= 5000) return '能量充沛，尽情使用'
  if (balance.value >= 2000) return '能量充足'
  if (balance.value >= 500) return '能量适中'
  if (balance.value >= 100) return '能量偏低'
  return '能量不足，建议充值'
})

// ── 3D tilt effect ───────────────────────────────────────────────
const tiltX = ref(0)
const tiltY = ref(0)
const tiltTransition = ref(false)

const tiltStyle = computed(() => ({
  transform: `rotateX(${tiltY.value}deg) rotateY(${tiltX.value}deg)`,
  transition: tiltTransition.value
      ? 'transform 125ms ease-in-out'
      : 'transform 400ms cubic-bezier(0.34, 1.56, 0.64, 1)'
}))

function onTiltEnter(n) {
  const row = Math.ceil(n / 5)       // 1..5
  const col = ((n - 1) % 5) + 1      // 1..5
  tiltY.value = (3 - row) * 10       // 20 10 0 -10 -20
  tiltX.value = (col - 3) * 5         // -10 -5 0 5 10
  tiltTransition.value = true
}

function onTiltLeave() {
  tiltX.value = 0
  tiltY.value = 0
  tiltTransition.value = false
}

// ── Monthly stats (computed from all transactions) ───────────────
const monthlyStats = reactive({
  spent: 0,
  earned: 0,
  usageCount: 0
})

const allTransactions = ref([])

async function fetchMonthlyStats() {
  try {
    const res = await getTransactions({}, 1, 50)
    if (!res) return
    const data = unwrap(res)
    if (!data) return
    const list = data.list || data.records || data.rows || []
    allTransactions.value = list

    const now = new Date()
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)

    let spent = 0, earned = 0, usage = 0
    for (const tx of list) {
      const txDate = new Date(tx.createTime || tx.createdAt || 0)
      if (txDate >= monthStart) {
        const amount = getTxAmountRaw(tx)
        if (isTxPositive(tx)) {
          earned += amount
        } else {
          spent += Math.abs(amount)
          usage++
        }
      }
    }
    monthlyStats.spent = spent
    monthlyStats.earned = earned
    monthlyStats.usageCount = usage
  } catch (err) {
    console.error('fetchMonthlyStats 失败:', err)
  }
}

// ── Transaction history ──────────────────────────────────────────
const showHistory = ref(false)
const transactions = ref([])
const loadingTx = ref(false)
const pageNum = ref(1)
const pageSize = 10
const hasMore = ref(false)
const txFilter = reactive({
  type: '',
  startDate: '',
  endDate: '',
})
const hasActiveFilter = computed(() => txFilter.type || txFilter.startDate || txFilter.endDate)

// ── Custom recharge ──────────────────────────────────────────────
const customPoints = ref(null)
const customPrice = computed(() => {
  if (!customPoints.value || customPoints.value <= 0) return '0.00'
  return (customPoints.value / 10).toFixed(2)
})
const isCustomValid = computed(() => Number(customPoints.value) >= 10)
const customPackage = computed(() => ({
  id: 'custom',
  name: '自定义充值',
  points: customPoints.value,
  bonus: 0,
  price: customPrice.value,
  description: '自定义充值'
}))

// ── Confirm modal ────────────────────────────────────────────────
const confirmTarget = ref(null)
const recharging = ref(false)

// ── Payment overlay ──────────────────────────────────────────────
const paymentOverlay = reactive({
  show: false,
  phase: 'loading',
  amount: '0.00',
  description: '',
  outTradeNo: '',
  qrCodeUrl: ''
})

// ── Toast ────────────────────────────────────────────────────────
const toast = reactive({ show: false, type: 'success', message: '' })
let toastTimer = null
function showToast(type, message) {
  toast.type = type
  toast.message = message
  toast.show = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.show = false }, 2600)
}

// ── Helpers ──────────────────────────────────────────────────────
function unwrap(res) {
  if (res && typeof res === 'object' && 'code' in res) {
    if (res.code === 200 || res.code === 0) return res.data
    throw new Error(res.message || '请求失败')
  }
  return res
}

function getPointsValue(data) {
  if (!data) return 0
  return data.totalPoints ?? data.points ?? data.balance ?? data.pointsBalance ?? data.currentPoints ?? 0
}

const TX_TYPE_MAP = {
  DEDUCTION: '消费扣减',
  RECHARGE: '充值',
  REFUND: '退款',
  GIFT: '赠送',
  ADMIN_ADJUST: '管理员调整'
}

function txTypeLabel(tx) {
  if (tx.transactionType) return TX_TYPE_MAP[tx.transactionType] || tx.transactionType
  return '其他'
}

function isTxPositive(tx) {
  const type = tx.transactionType
  if (!type) return true
  return type !== 'DEDUCTION'
}

function getTxAmountRaw(tx) {
  return tx.points ?? tx.amount ?? tx.changeAmount ?? tx.changePoints ?? 0
}

function getTxAmount(tx) {
  const raw = getTxAmountRaw(tx)
  return isTxPositive(tx) ? Math.abs(raw) : -Math.abs(raw)
}

function formatDate(value) {
  if (!value) return '--'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// ── API calls ────────────────────────────────────────────────────
async function fetchBalance() {
  loadingBalance.value = true
  try {
    const res = await getBalance()
    const data = unwrap(res)
    balance.value = getPointsValue(data)
    countUp(balance.value)
  } catch (err) {
    showToast('error', err.message || '获取积分余额失败')
  } finally {
    loadingBalance.value = false
  }
}

async function fetchTransactions() {
  loadingTx.value = true
  try {
    const query = {}
    if (txFilter.type) query.transactionType = txFilter.type
    if (txFilter.startDate) query.startDate = txFilter.startDate
    if (txFilter.endDate) query.endDate = txFilter.endDate
    const res = await getTransactions(query, pageNum.value, pageSize)
    const data = unwrap(res) || {}
    const list = data.list || data.records || data.rows || []
    const total = data.total ?? data.totalCount ?? list.length
    transactions.value = list
    hasMore.value = pageNum.value * pageSize < total
  } catch (err) {
    showToast('error', err.message || '获取积分明细失败')
  } finally {
    loadingTx.value = false
  }
}

function toggleHistory() {
  showHistory.value = !showHistory.value
  if (showHistory.value && transactions.value.length === 0) {
    fetchTransactions()
  }
}

function onFilterChange() {
  pageNum.value = 1
  fetchTransactions()
}

function clearFilter() {
  txFilter.type = ''
  txFilter.startDate = ''
  txFilter.endDate = ''
  pageNum.value = 1
  fetchTransactions()
}

function changePage(delta) {
  const next = pageNum.value + delta
  if (next < 1) return
  pageNum.value = next
  fetchTransactions()
}

function openConfirm(pkg) {
  if (pkg.id === 'enterprise') {
    showToast('success', '请联系企业服务团队为你开通')
    return
  }
  confirmTarget.value = pkg
}

function closeConfirm() {
  if (recharging.value) return
  confirmTarget.value = null
}

async function confirmRecharge() {
  if (!confirmTarget.value) return
  const pkg = confirmTarget.value
  const totalPoints = Number(pkg.points) + Number(pkg.bonus || 0)
  if (!totalPoints || totalPoints <= 0) {
    showToast('error', '充值积分数量不合法')
    return
  }

  recharging.value = true
  confirmTarget.value = null

  paymentOverlay.show = true
  paymentOverlay.phase = 'loading'
  paymentOverlay.amount = String(pkg.price)

  try {
    const res = await createAlipayPayment(totalPoints, String(pkg.price), pkg.description || `购买${pkg.name}`)
    const data = unwrap(res)
    const payForm = data.payForm || data.payUrl

    if (!payForm) {
      paymentOverlay.show = false
      showToast('error', '创建支付订单失败')
      return
    }

    paymentOverlay.phase = 'redirecting'

    setTimeout(() => {
      if (payForm.startsWith('<form')) {
        const div = document.createElement('div')
        div.innerHTML = payForm
        document.body.appendChild(div)
        const f = div.querySelector('form')
        if (f) f.submit()
      } else {
        window.location.href = payForm
      }
    }, 1200)

    customPoints.value = null
  } catch (err) {
    paymentOverlay.show = false
    showToast('error', err.message || '充值失败，请稍后再试')
  } finally {
    recharging.value = false
  }
}

// ── Payment return handling ──────────────────────────────────────
async function handlePaymentReturn() {
  const outTradeNo = route.query.out_trade_no
  if (!outTradeNo) return

  const cleanQuery = { ...route.query }
  const alipayParams = ['out_trade_no', 'trade_no', 'total_amount', 'seller_id', 'app_id', 'charset', 'sign', 'sign_type', 'timestamp', 'version', 'method']
  alipayParams.forEach(k => delete cleanQuery[k])
  router.replace({ query: Object.keys(cleanQuery).length > 0 ? cleanQuery : undefined })

  const paid = await checkAndShowPaymentResult(outTradeNo)
  if (!paid) {
    await pollPaymentStatus(outTradeNo)
  }
}

async function checkAndShowPaymentResult(outTradeNo) {
  try {
    const res = await queryPaymentOrder(outTradeNo)
    const data = unwrap(res)
    if (data && (data.status === 'TRADE_SUCCESS' || data.status === 'TRADE_FINISHED')) {
      showToast('success', '支付成功！积分已到账')
      await fetchBalance()
      return true
    }
  } catch (err) {
    console.error('查询支付状态失败：', err)
  }
  return false
}

async function pollPaymentStatus(outTradeNo) {
  for (let i = 0; i < 8; i++) {
    await new Promise(r => setTimeout(r, 2500))
    const paid = await checkAndShowPaymentResult(outTradeNo)
    if (paid) return
  }
  showToast('success', '支付结果确认中，请稍后刷新页面查看积分')
}

// ── Init ─────────────────────────────────────────────────────────
onMounted(async () => {
  fetchBalance().catch(err => console.error('fetchBalance 失败:', err))
  fetchMonthlyStats()
  handlePaymentReturn().catch(err => console.error('handlePaymentReturn 失败:', err))

  // Start both card shimmers on the same animation frame
  await nextTick()
  requestAnimationFrame(() => {
    shimmerState.value = 'running'
  })
})
</script>

<style scoped>
/* =================================================================
   DESIGN TOKENS
   ================================================================= */
.membership-page {
  /* ── Refined palette: indigo-forward, warm gold accent ── */
  --color-bg: #f7f8fa;
  --color-surface: #ffffff;
  --color-border: #e8ecf1;
  --color-border-light: #f1f4f8;
  --color-text: #0f172a;
  --color-text-secondary: #5b6a7c;
  --color-text-muted: #94a3b8;
  --color-accent: #6366f1;
  --color-accent-light: #eef2ff;
  --color-accent-gradient: linear-gradient(135deg, #6366f1 0%, #8b5cf6 50%, #a855f7 100%);
  --color-accent-deep: #4338ca;
  --color-amber: #f59e0b;
  --color-amber-light: #fffbeb;
  --color-emerald: #10b981;
  --color-emerald-light: #ecfdf5;
  --color-rose: #f43f5e;
  --color-rose-light: #fff1f2;
  --color-sky: #0ea5e9;
  --color-sky-light: #f0f9ff;
  --color-premium: #0f172a;
  --color-premium-surface: #1e1b4b;
  --color-success: #10b981;
  --color-success-light: #ecfdf5;
  --color-warning: #f59e0b;
  --color-danger: #ef4444;
  --color-danger-light: #fef2f2;
  --radius-sm: 8px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 20px;
  --shadow-xs: 0 1px 2px rgba(0,0,0,0.03);
  --shadow-sm: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.03);
  --shadow-md: 0 4px 16px rgba(0,0,0,0.06), 0 1px 3px rgba(0,0,0,0.04);
  --shadow-lg: 0 12px 32px rgba(0,0,0,0.08), 0 2px 6px rgba(0,0,0,0.04);
  --shadow-xl: 0 24px 56px rgba(0,0,0,0.12);
  --font-stack: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;

  min-height: 100%;
  background: var(--color-bg);
  padding: 48px 24px 80px;
  font-family: var(--font-stack);
  color: var(--color-text);
  -webkit-font-smoothing: antialiased;
}

/* =================================================================
   HERO HEADER
   ================================================================= */
.page-hero {
  text-align: center;
  margin-bottom: 32px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: var(--color-accent-light);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 600;
  padding: 5px 14px;
  border-radius: 999px;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-accent);
  animation: badge-pulse 2s ease-in-out infinite;
}
@keyframes badge-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.hero-title {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -0.6px;
  line-height: 1.25;
  margin: 0 0 10px;
  color: var(--color-text);
}
.hero-highlight {
  background: var(--color-accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 15px;
  color: var(--color-text-secondary);
  margin: 0;
  font-weight: 400;
  line-height: 1.5;
}

/* =================================================================
   MEMBER CARD — Premium dark card, the page's signature element
   ================================================================= */
.member-card-section {
  max-width: 1040px;
  margin: 0 auto 32px;
}

.member-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, #1e1b4b 0%, #0f172a 40%, #1e1b4b 100%);
  border: 1px solid rgba(255,255,255,0.08);
  padding: 40px 44px;
  min-height: 200px;
  display: flex;
  align-items: center;
  box-shadow:
      0 1px 0 rgba(255,255,255,0.05) inset,
      0 20px 60px rgba(15,23,42,0.35),
      0 4px 12px rgba(0,0,0,0.15);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.member-card:hover {
  transform: translateY(-2px);
  box-shadow:
      0 1px 0 rgba(255,255,255,0.06) inset,
      0 28px 72px rgba(15,23,42,0.42),
      0 6px 16px rgba(0,0,0,0.18);
}

/* ── Subtle geometric dot texture ──────────────────────────── */
.member-card-texture {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  opacity: 0.06;
  background-image: radial-gradient(circle, #ffffff 1px, transparent 1px);
  background-size: 24px 24px;
}

/* ── Shimmer sweep ─────────────────────────────────────────── */
.member-card-shine {
  position: absolute;
  inset: 0;
  background: linear-gradient(
      110deg,
      transparent 25%,
      rgba(255,255,255,0.04) 40%,
      rgba(255,255,255,0.10) 48%,
      rgba(255,255,255,0.04) 56%,
      transparent 70%
  );
  background-size: 250% 100%;
  animation: card-shimmer 6s ease-in-out infinite;
  animation-play-state: var(--shimmer-state, paused);
  pointer-events: none;
}

/* ── Shared shimmer keyframe (syncs both cards) ─────────────── */
@keyframes card-shimmer {
  0% { background-position: 250% 0; }
  100% { background-position: -250% 0; }
}

/* ── Subtle ambient glow (top-right) ───────────────────────── */
.member-card::after {
  content: '';
  position: absolute;
  top: -30%;
  right: -5%;
  width: 320px;
  height: 320px;
  background: radial-gradient(circle, rgba(99,102,241,0.18) 0%, transparent 65%);
  border-radius: 50%;
  pointer-events: none;
  z-index: 0;
}

/* ── Content ───────────────────────────────────────────────── */
.member-card-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 32px;
}

.member-info {
  flex: 1;
  min-width: 0;
  color: #e2e8f0;
}

.member-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}
.member-crown {
  width: 28px;
  height: 28px;
  color: var(--color-amber);
  display: flex;
  align-items: center;
  justify-content: center;
}
.member-crown svg {
  width: 100%;
  height: 100%;
  fill: currentColor;
  stroke: none;
}
.member-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0;
  color: #f1f5f9;
}
.member-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.10);
  color: #cbd5e1;
  margin-left: 4px;
}

/* ── Progress ─────────────────────────────────────────────── */
.member-progress-area {
  margin-bottom: 18px;
}
.member-progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  margin-bottom: 8px;
}
.member-progress-current {
  font-weight: 700;
  color: #cbd5e1;
  font-variant-numeric: tabular-nums;
}
.member-progress-track {
  height: 6px;
  background: rgba(255,255,255,0.10);
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 8px;
}
.member-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #fbbf24 0%, #f59e0b 60%, #f97316 100%);
  border-radius: 999px;
  transition: width 0.8s cubic-bezier(0.22, 0.61, 0.36, 1);
  position: relative;
  box-shadow: 0 0 10px rgba(251, 191, 36, 0.3);
}
.progress-glow {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 16px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.5));
  filter: blur(3px);
}
.member-progress-hint {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}
.member-progress-hint strong {
  font-weight: 700;
  color: var(--color-amber);
}

/* ── Benefit pills ────────────────────────────────────────── */
.member-benefits {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.benefit-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 999px;
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.10);
  color: #cbd5e1;
}

/* ── Diamond SVG ──────────────────────────────────────────── */
.member-diamond {
  flex-shrink: 0;
  width: 140px;
  height: 140px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.diamond-svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 8px 24px rgba(99, 102, 241, 0.4));
  animation: diamond-float 3s ease-in-out infinite;
}
@keyframes diamond-float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-6px) rotate(2deg); }
}
.diamond-shadow {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 8px;
  background: rgba(0,0,0,0.25);
  border-radius: 50%;
  filter: blur(6px);
  animation: shadow-pulse 3s ease-in-out infinite;
}
@keyframes shadow-pulse {
  0%, 100% { transform: translateX(-50%) scale(1); opacity: 0.25; }
  50% { transform: translateX(-50%) scale(0.75); opacity: 0.12; }
}

/* =================================================================
   OVERVIEW SECTION — Metal card (balance) + stat cards
   ================================================================= */
.overview-section {
  max-width: 1040px;
  margin: 0 auto 48px;
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 20px;
  align-items: stretch;
}

/* ── Stage & perspective ───────────────────────────────────── */
.balance-card-stage {
  position: relative;
  perspective: 900px;
  min-height: 240px;
}

/* ═══════════════════════════════════════════════════════════════
   THE METAL CARD — Dark Sapphire, brushed texture, physical feel
   ═══════════════════════════════════════════════════════════════ */
.balance-hero-card {
  position: relative;
  overflow: hidden;
  /* Deep navy base with subtle warmth */
  background:
    /* Brushed metal diagonal micro-strokes */
    repeating-linear-gradient(
      -35deg,
      transparent,
      transparent 3px,
      rgba(255,255,255,0.012) 3px,
      rgba(255,255,255,0.012) 6px
    ),
    /* Depth layer */
    linear-gradient(165deg, #1a1f2e 0%, #111827 30%, #0f172a 60%, #1a1f2e 100%);
  /* Asymmetric card-edge bevel — top/left catch light, bottom/right recede */
  border-top: 1px solid rgba(255,255,255,0.10);
  border-left: 1px solid rgba(255,255,255,0.06);
  border-right: 1px solid rgba(255,255,255,0.04);
  border-bottom: 1px solid rgba(0,0,0,0.35);
  border-radius: 20px;
  padding: 38px 42px;
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  transform-style: preserve-3d;
  will-change: transform;
  z-index: 0;
  height: 100%;
  min-height: 240px;
  box-shadow:
    /* Soft ambient */
    0 1px 2px rgba(0,0,0,0.20),
    /* Mid-depth */
    0 8px 24px rgba(0,0,0,0.30),
    /* Far depth — subtle navy tint */
    0 24px 56px rgba(15,23,42,0.45);
  transition: box-shadow 0.4s ease, transform 0.4s ease;
}

/* ── Specular highlight — static "studio light" on card surface ── */
.balance-hero-card::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -15%;
  width: 55%;
  height: 90%;
  background: radial-gradient(ellipse at center,
      rgba(255,255,255,0.06) 0%,
      rgba(255,255,255,0.03) 25%,
      transparent 70%);
  transform: rotate(-15deg);
  pointer-events: none;
  z-index: 0;
  border-radius: 50%;
}

/* ── Subtle top-edge highlight ──────────────────────────────── */
.balance-card-edge {
  position: absolute;
  top: 0;
  left: 3px;
  right: 3px;
  height: 1px;
  background: linear-gradient(90deg,
      transparent 0%,
      rgba(255,255,255,0.15) 15%,
      rgba(255,255,255,0.25) 50%,
      rgba(255,255,255,0.15) 85%,
      transparent 100%);
  z-index: 3;
  pointer-events: none;
  border-radius: 1px;
}

/* ── Chip — metallic accent, like a credit card chip ───────── */
.balance-card-chip {
  position: absolute;
  top: 34px;
  right: 42px;
  width: 42px;
  height: 30px;
  border-radius: 6px;
  background:
    linear-gradient(160deg, #d4a853 0%, #c9952e 25%, #f0d78c 45%, #a67c20 65%, #d4a853 100%);
  box-shadow:
    0 1px 3px rgba(0,0,0,0.3),
    inset 0 1px 0 rgba(255,255,255,0.3);
  z-index: 1;
  pointer-events: none;
}
/* Chip horizontal grooves */
.balance-card-chip::before {
  content: '';
  position: absolute;
  inset: 3px 5px;
  border-radius: 2px;
  background: repeating-linear-gradient(
    0deg,
    transparent,
    transparent 1px,
    rgba(0,0,0,0.15) 1px,
    rgba(0,0,0,0.15) 2px
  );
}
.balance-card-chip::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 5px;
  right: 5px;
  height: 1px;
  background: rgba(255,255,255,0.3);
  transform: translateY(-50%);
}

/* ── 5×5 tilt tracker grid ─────────────────────────────────── */
.tilt-grid {
  position: absolute;
  inset: 0;
  z-index: 200;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  grid-template-rows: repeat(5, 1fr);
  gap: 0;
}
.tilt-cell {
  cursor: default;
}

/* ── Holographic shimmer sweep ─────────────────────────────── */
.balance-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
      115deg,
      transparent 15%,
      transparent 38%,
      rgba(255,255,255,0.04) 43%,
      rgba(255,255,255,0.10) 46%,
      rgba(255,255,255,0.04) 49%,
      transparent 54%,
      transparent 100%
  );
  background-size: 250% 100%;
  animation: card-shimmer 6s ease-in-out infinite;
  animation-play-state: var(--shimmer-state, paused);
  pointer-events: none;
  z-index: 0;
}

/* ── Hover — card lifts, shadow deepens ────────────────────── */
.balance-card-stage:hover .balance-hero-card {
  box-shadow:
    0 2px 4px rgba(0,0,0,0.25),
    0 12px 32px rgba(0,0,0,0.35),
    0 32px 72px rgba(15,23,42,0.55);
  transform: translateY(-3px);
}
/* Specular highlight shifts on hover */
.balance-card-stage:hover .balance-hero-card::before {
  top: -35%;
  right: -10%;
  opacity: 1.3;
  transition: all 0.4s ease;
}

/* ── Content layer ─────────────────────────────────────────── */
.balance-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* ── Logo watermark — subtle, behind content ───────────────── */
.balance-card-watermark {
  position: absolute;
  right: -30px;
  bottom: -40px;
  width: 260px;
  height: 260px;
  z-index: 0;
  pointer-events: none;
  opacity: 0.06;
  filter: brightness(0) invert(1);
}
.balance-card-watermark img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* ── Contactless NFC icon ───────────────────────────────────── */
.balance-contactless {
  position: absolute;
  top: 38px;
  right: 82px;
  width: 22px;
  height: 22px;
  z-index: 1;
  pointer-events: none;
  color: rgba(212,168,83,0.7);
}
.balance-contactless svg {
  width: 100%;
  height: 100%;
}

/* ── Top row: issuer logo ──────────────────────────────────── */
.balance-card-top {
  margin-bottom: 20px;
}
.balance-issuer-logo {
  display: flex;
  align-items: center;
  gap: 8px;
}
.issuer-emblem {
  width: 26px;
  height: 26px;
  color: rgba(255,255,255,0.7);
}
.issuer-name {
  font-size: 14px;
  font-weight: 700;
  color: rgba(255,255,255,0.55);
  letter-spacing: 1.2px;
}

/* ── The Number — embossed, dominant ───────────────────────── */
.balance-number {
  font-size: 64px;
  font-weight: 800;
  letter-spacing: -3px;
  line-height: 1;
  margin: 0 0 18px;
  font-variant-numeric: tabular-nums;
  color: #fff;
  text-shadow:
      0 1px 0 rgba(255,255,255,0.15),
      0 -1px 0 rgba(0,0,0,0.3),
      0 4px 16px rgba(0,0,0,0.20);
}
.balance-skeleton {
  opacity: 0.25;
  letter-spacing: 0;
}

/* ── Energy gauge — gold, refined ──────────────────────────── */
.energy-gauge {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 22px;
}
.gauge-track {
  flex: 1;
  height: 3px;
  background: rgba(255,255,255,0.08);
  border-radius: 999px;
  overflow: hidden;
}
.gauge-fill {
  height: 100%;
  background: linear-gradient(90deg, #c9952e, #f0d78c, #d4a853);
  border-radius: 999px;
  transition: width 0.8s cubic-bezier(0.22, 0.61, 0.36, 1);
  box-shadow: 0 0 6px rgba(212,168,83,0.35);
}
.gauge-label {
  font-size: 11px;
  font-weight: 500;
  color: rgba(255,255,255,0.45);
  white-space: nowrap;
}

/* ── Bottom row: label left, network badge right ───────────── */
.balance-card-bottom {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-top: auto;
}
.balance-card-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255,255,255,0.40);
  letter-spacing: 0.6px;
  text-transform: uppercase;
}
.balance-network-badge {
  display: flex;
  align-items: center;
  gap: 7px;
}
.network-icon {
  width: 32px;
  height: 22px;
  flex-shrink: 0;
}
.network-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: rgba(255,255,255,0.35);
}

/* ── Stats column ──────────────────────────────────────────── */
.stats-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  flex: 1;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 22px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow 0.2s, transform 0.15s;
}
.stat-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon-wrap svg {
  width: 22px;
  height: 22px;
}
.stat-icon-out {
  background: var(--color-rose-light);
  color: var(--color-rose);
}
.stat-icon-in {
  background: var(--color-emerald-light);
  color: var(--color-emerald);
}
.stat-icon-usage {
  background: var(--color-sky-light);
  color: var(--color-sky);
}

.stat-info {
  min-width: 0;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0 0 2px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
  color: var(--color-text);
}
.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1;
}

/* =================================================================
   TRANSACTION HISTORY TRIGGER
   ================================================================= */
.history-trigger-section {
  max-width: 1040px;
  margin: 0 auto 32px;
}
.history-trigger-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.15s, border-color 0.2s;
}
.history-trigger-card:hover {
  box-shadow: var(--shadow-md);
  border-color: #c7d2fe;
  transform: translateY(-1px);
}
.history-trigger-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.history-trigger-icon {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  background: var(--color-accent-light);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
}
.history-trigger-icon svg {
  width: 22px;
  height: 22px;
}
.history-trigger-title {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 2px;
  color: var(--color-text);
}
.history-trigger-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin: 0;
}
.history-trigger-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.history-trigger-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--color-accent-light);
  color: var(--color-accent);
}
.history-trigger-chevron {
  width: 20px;
  height: 20px;
  color: var(--color-text-muted);
  transition: transform 0.25s ease;
}
.history-trigger-chevron.is-open {
  transform: rotate(180deg);
}

/* =================================================================
   SERVICE VALUE SECTION
   ================================================================= */
.value-section {
  max-width: 1040px;
  margin: 0 auto 48px;
}

.section-header {
  margin-bottom: 24px;
}
.section-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0 0 6px;
  color: var(--color-text);
}
.section-sub {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

.value-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.value-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px 20px;
  transition: box-shadow 0.2s, transform 0.15s, border-color 0.2s;
  position: relative;
  overflow: hidden;
}
.value-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  opacity: 0;
  transition: opacity 0.2s;
}
.value-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: transparent;
}
.value-card:hover::before {
  opacity: 1;
}
.value-card:nth-child(1):before { background: linear-gradient(90deg, #818cf8, #6366f1); }
.value-card:nth-child(2):before { background: linear-gradient(90deg, #f472b6, #db2777); }
.value-card:nth-child(3):before { background: linear-gradient(90deg, #60a5fa, #2563eb); }
.value-card:nth-child(4):before { background: linear-gradient(90deg, #34d399, #059669); }

.value-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}
.value-icon-wrap svg {
  width: 22px;
  height: 22px;
}
.value-icon-digital {
  background: #eef2ff;
  color: #6366f1;
}
.value-icon-voice {
  background: #fdf2f8;
  color: #db2777;
}
.value-icon-model {
  background: #eff6ff;
  color: #2563eb;
}
.value-icon-course {
  background: #ecfdf5;
  color: #059669;
}

.value-name {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--color-text);
}
.value-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0 0 14px;
  line-height: 1.55;
}
.value-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.value-cost {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-accent);
  background: var(--color-accent-light);
  padding: 3px 10px;
  border-radius: 6px;
}
.value-tag {
  font-size: 11px;
  color: var(--color-text-muted);
  background: var(--color-border-light);
  padding: 3px 8px;
  border-radius: 6px;
  font-weight: 500;
}

/* =================================================================
   PACKAGES SECTION
   ================================================================= */
.packages-section {
  max-width: 1040px;
  margin: 0 auto 48px;
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

/* ── Package card ──────────────────────────────────────────── */
.package-card {
  position: relative;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 28px 22px 24px;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s, transform 0.2s, border-color 0.2s;
}
.package-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.package-card.is-featured {
  border: 2px solid var(--color-accent);
  box-shadow: 0 4px 24px rgba(99,102,241,0.12);
  background: linear-gradient(180deg, #fafafe 0%, var(--color-surface) 8%);
}
.package-card.is-featured:hover {
  box-shadow: 0 8px 32px rgba(99,102,241,0.18);
}

.pkg-badge {
  position: absolute;
  top: -1px;
  right: 20px;
  background: var(--color-accent-gradient);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 0 0 8px 8px;
  letter-spacing: 0.2px;
  box-shadow: 0 2px 8px rgba(99,102,241,0.25);
}

.pkg-top {
  margin-bottom: 16px;
}
.pkg-name {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 10px;
  color: var(--color-text);
}
.pkg-price {
  display: flex;
  align-items: baseline;
  gap: 1px;
  margin-bottom: 4px;
}
.pkg-currency {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}
.pkg-amount {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1.2px;
  line-height: 1;
  color: var(--color-text);
}
.pkg-points {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
}
.pkg-points-num {
  font-weight: 700;
  color: var(--color-text);
}
.pkg-bonus-tag {
  display: inline-block;
  background: var(--color-accent-light);
  color: var(--color-accent);
  font-size: 11px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 5px;
  margin-left: 4px;
}

.pkg-features {
  list-style: none;
  margin: 0 0 20px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
  flex: 1;
}
.pkg-features li {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.4;
}
.pkg-check {
  width: 15px;
  height: 15px;
  color: var(--color-accent);
  flex-shrink: 0;
  margin-top: 1px;
}

.pkg-cta {
  width: 100%;
  border: none;
  border-radius: var(--radius-sm);
  padding: 11px 0;
  font-size: 13px;
  font-weight: 600;
  background: var(--color-text);
  color: #fff;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.1s, background 0.15s;
}
.pkg-cta:hover:not(:disabled) { opacity: 0.9; }
.pkg-cta:active:not(:disabled) { transform: scale(0.98); }
.pkg-cta:disabled { opacity: 0.4; cursor: not-allowed; }
.pkg-cta-featured {
  background: var(--color-accent-gradient);
  box-shadow: 0 2px 12px rgba(99,102,241,0.25);
}
.pkg-cta-featured:hover:not(:disabled) {
  box-shadow: 0 4px 16px rgba(99,102,241,0.35);
}

/* ── Custom recharge bar ───────────────────────────────────── */
.custom-recharge-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  flex-wrap: wrap;
}
.custom-recharge-left {
  min-width: 0;
}
.custom-recharge-title {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--color-text);
}
.custom-recharge-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 0;
  line-height: 1.4;
}
.custom-recharge-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.custom-input-group {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  overflow: hidden;
  transition: border-color 0.2s, box-shadow 0.2s;
}
.custom-input-group:focus-within {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(99,102,241,0.08);
}
.custom-input-label {
  padding: 9px 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-bg);
  border-right: 1px solid var(--color-border);
}
.custom-input {
  width: 110px;
  border: none;
  padding: 9px 12px;
  font-size: 14px;
  outline: none;
  background: transparent;
  color: var(--color-text);
  font-weight: 500;
}
.custom-input::placeholder { color: var(--color-text-muted); }
.custom-price-tag {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-accent);
  white-space: nowrap;
}
.custom-cta {
  padding: 10px 22px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: 13px;
  font-weight: 600;
  background: var(--color-accent-gradient);
  color: #fff;
  cursor: pointer;
  transition: opacity 0.15s, transform 0.1s, box-shadow 0.15s;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(99,102,241,0.2);
}
.custom-cta:hover:not(:disabled) {
  opacity: 0.92;
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
}
.custom-cta:active:not(:disabled) { transform: scale(0.97); }
.custom-cta:disabled { opacity: 0.4; cursor: not-allowed; }

/* =================================================================
   TRANSACTION HISTORY
   ================================================================= */
.history-section {
  max-width: 1040px;
  margin: 0 auto 48px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
}

.history-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.history-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.history-filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.tx-filter-select {
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text);
  background: var(--color-surface);
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
}
.tx-filter-select:focus {
  border-color: var(--color-accent);
}
.tx-filter-date {
  height: 34px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text);
  background: var(--color-surface);
  outline: none;
  transition: border-color 0.15s;
}
.tx-filter-date:focus {
  border-color: var(--color-accent);
}
.tx-filter-sep {
  font-size: 12px;
  color: var(--color-text-muted);
}
.tx-filter-clear {
  height: 34px;
  padding: 0 12px;
  border: 0;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 500;
  color: var(--color-accent);
  background: var(--color-accent-light);
  cursor: pointer;
  transition: background 0.15s;
}
.tx-filter-clear:hover {
  background: #dde4ff;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: background 0.15s;
}
.refresh-btn:hover {
  background: var(--color-bg);
}

.tx-table-wrap {
  margin-top: 4px;
}

.tx-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tx-table th {
  text-align: left;
  color: var(--color-text-muted);
  font-weight: 500;
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.6px;
}
.tx-table td {
  padding: 11px 12px;
  border-bottom: 1px solid var(--color-border-light);
}
.tx-table tbody tr:hover td {
  background: rgba(99,102,241,0.03);
}

.tx-type-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  border-radius: 999px;
}
.tx-type-badge.tx-in {
  background: var(--color-success-light);
  color: var(--color-success);
}
.tx-type-badge.tx-out {
  background: var(--color-danger-light);
  color: var(--color-danger);
}

.tx-amount.tx-positive {
  color: var(--color-success);
  font-weight: 600;
}
.tx-amount.tx-negative {
  color: var(--color-danger);
  font-weight: 600;
}
.tx-cell-time { color: var(--color-text-secondary); font-size: 12px; white-space: nowrap; }
.tx-cell-balance { font-variant-numeric: tabular-nums; }
.tx-cell-desc { color: var(--color-text-secondary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.tx-empty {
  text-align: center;
  color: var(--color-text-muted);
  font-size: 13px;
  padding: 32px 0;
}

.tx-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}
.tx-page-btn {
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: var(--radius-sm);
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
}
.tx-page-btn:hover:not(:disabled) { background: var(--color-bg); }
.tx-page-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.tx-page-num {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

/* =================================================================
   MODAL
   ================================================================= */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.5);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-dialog {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: 40px 36px;
  width: 380px;
  max-width: 90vw;
  text-align: center;
  box-shadow: var(--shadow-xl);
}
.modal-icon-wrap {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--color-accent-light);
  color: var(--color-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.modal-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 14px;
}
.modal-pkg-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 12px;
}
.modal-pkg-name {
  font-size: 14px;
  color: var(--color-text-secondary);
}
.modal-pkg-points {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}
.modal-price {
  font-size: 36px;
  font-weight: 800;
  letter-spacing: -1px;
  color: var(--color-accent);
  margin: 0 0 28px;
}
.modal-buttons {
  display: flex;
  gap: 12px;
}
.modal-btn-cancel,
.modal-btn-confirm {
  flex: 1;
  border: none;
  border-radius: var(--radius-sm);
  padding: 12px 0;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}
.modal-btn-cancel {
  background: var(--color-bg);
  color: var(--color-text);
}
.modal-btn-cancel:hover { background: var(--color-border); }
.modal-btn-confirm {
  background: var(--color-accent-gradient);
  color: #fff;
}
.modal-btn-confirm:hover:not(:disabled) { opacity: 0.9; transform: translateY(-1px); }
.modal-btn-cancel:disabled,
.modal-btn-confirm:disabled { opacity: 0.4; cursor: not-allowed; }

/* =================================================================
   PAYMENT OVERLAY
   ================================================================= */
.payment-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,0.6);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.pay-dialog {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: 48px 44px;
  width: 400px;
  max-width: 90vw;
  text-align: center;
  box-shadow: var(--shadow-xl);
}
.pay-spinner-wrap {
  position: relative;
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
}
.pay-spinner-ring {
  width: 72px;
  height: 72px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.pay-spinner-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 28px;
  height: 28px;
  color: var(--color-accent);
}
.pay-check-wrap {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  color: var(--color-success);
}
.pay-check-wrap svg { width: 100%; height: 100%; }
.pay-title {
  font-size: 19px;
  font-weight: 700;
  margin: 0 0 8px;
}
.pay-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0 0 16px;
}
.pay-amount {
  font-size: 32px;
  font-weight: 800;
  letter-spacing: -1px;
  color: var(--color-accent);
  margin: 0 0 20px;
}
.pay-status-hint {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: var(--color-emerald-light);
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  padding: 10px 18px;
  font-size: 13px;
  color: #059669;
}
.hint-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-emerald);
  animation: pulse-dot 1.2s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.3; transform: scale(0.7); }
}

/* =================================================================
   TOAST
   ================================================================= */
.global-toast {
  position: fixed;
  bottom: 36px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--color-premium);
  color: #fff;
  padding: 12px 28px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 500;
  z-index: 1100;
  box-shadow: var(--shadow-lg);
  pointer-events: none;
}
.global-toast.error {
  background: var(--color-danger);
}

/* =================================================================
   TRANSITIONS
   ================================================================= */
.modal-fade-enter-active,
.modal-fade-leave-active { transition: opacity 0.2s ease; }
.modal-fade-enter-from,
.modal-fade-leave-to { opacity: 0; }

.toast-slide-enter-active,
.toast-slide-leave-active { transition: opacity 0.2s ease, transform 0.2s ease; }
.toast-slide-enter-from,
.toast-slide-leave-to { opacity: 0; transform: translate(-50%, 8px); }

.section-expand-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; overflow: hidden; }
.section-expand-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; overflow: hidden; }
.section-expand-enter-from { opacity: 0; transform: translateY(-6px); }
.section-expand-leave-to { opacity: 0; transform: translateY(-4px); }

/* =================================================================
   RESPONSIVE
   ================================================================= */
@media (max-width: 960px) {
  .overview-section {
    grid-template-columns: 1fr;
  }
  .value-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .packages-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .custom-recharge-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .custom-recharge-right {
    flex-wrap: wrap;
  }
  .member-card-content {
    flex-direction: column;
    text-align: center;
  }
  .member-diamond {
    order: -1;
    width: 100px;
    height: 100px;
  }
  .member-benefits {
    justify-content: center;
  }
}

@media (max-width: 560px) {
  .membership-page {
    padding: 28px 14px 60px;
  }
  .hero-title {
    font-size: 24px;
  }
  .balance-hero-card {
    padding: 24px;
  }
  .balance-number {
    font-size: 38px;
  }
  .balance-card-top {
    margin-bottom: 12px;
  }
  .balance-number {
    font-size: 42px;
  }
  .value-grid,
  .packages-grid {
    grid-template-columns: 1fr;
  }
  .overview-section {
    gap: 12px;
  }
  .member-card {
    padding: 24px;
  }
  .member-title {
    font-size: 20px;
  }
}
</style>
