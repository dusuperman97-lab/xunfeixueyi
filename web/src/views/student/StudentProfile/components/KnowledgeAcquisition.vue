NEW_FILE_CODE
<template>
  <div class="student-profile-info-block">
    <div class="student-profile-info-block-header">
      <div class="student-profile-info-block-header-inner">
        <span class="student-profile-info-block-header-icon">🧠</span>
        <h3>知 识 获 取 偏 好</h3>
      </div>
    </div>
    <div class="student-profile-info-block-body">
      <div class="information-processing-preferences">
        <div
            v-for="(preference, index) in processingPreferences"
            :key="index"
            class="processing-preference-item"
        >
          <div class="preference-header">
            <span class="preference-type">{{ preference.type }}</span>
            <span class="preference-value">{{ preference.value }}%</span>
          </div>
          <div class="preference-bar">
            <div
                class="preference-fill"
                :style="{
                width: preference.value + '%',
                background: preference.color,
                borderRadius: '4px'
              }"
            ></div>
          </div>
          <div class="preference-description">{{ preference.description }}</div>
        </div>

        <div class="preference-summary-chart">
          <div class="chart-container">
            <svg viewBox="0 0 120 120" class="pie-chart">
              <circle
                  cx="60"
                  cy="60"
                  r="50"
                  fill="none"
                  stroke="#f0f0f0"
                  stroke-width="10"
              />
              <path
                  v-for="(preference, index) in processingPreferencesWithStartAngle"
                  :key="index"
                  :d="getArcPath(60, 60, 50, preference.startAngle, preference.endAngle)"
                  :fill="preference.color"
                  stroke="#fff"
                  stroke-width="1"
              />
              <text x="60" y="55" text-anchor="middle" font-size="12" fill="#333" font-weight="bold">{{ dominantPreference.type }}</text>
              <text x="60" y="70" text-anchor="middle" font-size="10" fill="#666">{{ dominantPreference.value }}%</text>
              <text x="60" y="85" text-anchor="middle" font-size="9" fill="#888">主导偏好</text>
            </svg>
          </div>

          <div class="chart-legend">
            <div
                v-for="(preference, index) in processingPreferences"
                :key="index"
                class="legend-item"
            >
              <div
                  class="legend-color"
                  :style="{ backgroundColor: preference.color }"
              ></div>
              <span class="legend-text">{{ preference.type }} ({{ preference.value }}%)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { processingPreferences } from '../mock/data.js'

const processingPreferencesWithStartAngle = computed(() => {
  let currentAngle = 0;
  const totalValue = processingPreferences.reduce((sum, pref) => sum + pref.value, 0);

  return processingPreferences.map(preference => {
    const startAngle = currentAngle;
    const angle = (preference.value / totalValue) * 360;
    currentAngle += angle;

    return {
      ...preference,
      startAngle,
      endAngle: currentAngle
    };
  });
});

const getArcPath = (cx, cy, r, startAngle, endAngle) => {
  const startRad = (startAngle - 90) * Math.PI / 180;
  const endRad = (endAngle - 90) * Math.PI / 180;

  const x1 = cx + r * Math.cos(startRad);
  const y1 = cy + r * Math.sin(startRad);
  const x2 = cx + r * Math.cos(endRad);
  const y2 = cy + r * Math.sin(endRad);

  const largeArcFlag = endAngle - startAngle <= 180 ? 0 : 1;

  return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`;
};

const dominantPreference = computed(() => {
  return processingPreferences.reduce((dominant, current) => {
    return current.value > dominant.value ? current : dominant;
  });
});
</script>

<style scoped>
.student-profile-info-block {
  background: linear-gradient(145deg, #ffffff 0%, #f5f8ff 100%);
  border-radius: 16px;
  border: 1px solid rgba(98, 114, 216, 0.18);
  margin-bottom: 14px;
  overflow: hidden;
  box-shadow:
      0 2px 12px rgba(91, 111, 212, 0.10),
      0 1px 3px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: box-shadow 0.25s ease, transform 0.25s ease;
}

.student-profile-info-block:hover {
  box-shadow:
      0 6px 24px rgba(91, 111, 212, 0.16),
      0 2px 6px rgba(0, 0, 0, 0.06),
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transform: translateY(-1px);
}

.student-profile-info-block-header {
  background: linear-gradient(135deg, #eef1fd 0%, #e6ebfa 100%);
  padding: 0;
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid rgba(98, 114, 216, 0.15);
  position: relative;
}

.student-profile-info-block-header::before {
  content: '';
  position: absolute;
  left: 0; top: 0; bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #6272d8 0%, #7b8fe8 100%);
  border-radius: 0 2px 2px 0;
}

.student-profile-info-block-header-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 16px 9px 20px;
}

.student-profile-info-block-header h3 {
  color: #3a4fa8;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
}

.student-profile-info-block-header-icon {
  font-size: 16px;
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #6272d8, #7b8fe8);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(98, 114, 216, 0.35);
  flex-shrink: 0;
}

.student-profile-info-block-body {
  padding: 14px 18px;
}

.information-processing-preferences { display: flex; flex-direction: column; gap: 12px; }

.processing-preference-item {
  padding: 10px 0;
  border-bottom: 1px solid rgba(91,111,212,0.1);
}

.processing-preference-item:last-child {
  border-bottom: none;
}

.preference-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.preference-type {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.preference-value {
  font-size: 12px;
  font-weight: 700;
  color: #3f51b5;
}

.preference-bar {
  height: 8px;
  background: rgba(91,111,212,0.1);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 6px;
}

.preference-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.preference-description {
  font-size: 11px;
  color: #64748b;
  line-height: 1.4;
  padding-left: 2px;
}

.preference-summary-chart {
  display: flex;
  gap: 16px;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid rgba(91,111,212,0.1);
  margin-top: 8px;
}

.chart-container {
  flex-shrink: 0;
}

.pie-chart {
  width: 120px;
  height: 120px;
}

.chart-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-text {
  font-size: 11px;
  color: #334155;
}
</style>
