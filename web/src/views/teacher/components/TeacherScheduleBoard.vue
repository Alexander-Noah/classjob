<script setup>
import { computed } from 'vue'

defineOptions({
  name: 'TeacherScheduleBoard',
})

const props = defineProps({
  activeId: {
    type: String,
    default: '',
  },
  courses: {
    type: Array,
    default: () => [],
  },
  weekDays: {
    type: Array,
    default: () => [],
  },
})

defineEmits(['select'])

const periods = [
  { label: '01-02节', time: '08:30-10:00', start: 1 },
  { label: '03-04节', time: '10:20-11:50', start: 3 },
  { label: '05-06节', time: '14:00-15:30', start: 5 },
  { label: '07-08节', time: '15:50-17:20', start: 7 },
  { label: '09-10节', time: '18:40-20:10', start: 9 },
  { label: '11-12节', time: '20:20-21:50', start: 11 },
]

const getCellCourses = (dayValue, periodStart) =>
  props.courses.filter((course) => course.day === dayValue && Number(course.periodStart || 1) === periodStart)

const hasCourses = computed(() => props.courses.length > 0)
</script>

<template>
  <div class="schedule-table-wrap">
    <div class="schedule-table">
      <div class="table-cell table-corner">节次</div>
      <div v-for="day in weekDays" :key="day.value" class="table-cell day-head">
        <strong>{{ day.label }}</strong>
        <span>{{ day.date }}</span>
      </div>

      <template v-for="period in periods" :key="period.start">
        <div class="table-cell period-head">
          <strong>{{ period.label }}</strong>
          <span>{{ period.time }}</span>
        </div>

        <div v-for="day in weekDays" :key="`${day.value}-${period.start}`" class="table-cell course-cell">
          <button
            v-for="course in getCellCourses(day.value, period.start)"
            :key="course.id"
            class="course-card"
            :class="{ active: course.id === props.activeId }"
            type="button"
            @click="$emit('select', course)"
          >
            <div class="course-top">
              <strong>{{ course.title }}</strong>
            </div>
            <p>{{ course.time }}</p>
            <small>{{ course.lab }} · {{ course.className }}</small>
          </button>
        </div>
      </template>
    </div>

    <el-empty v-if="!hasCourses" description="本周暂无课程" :image-size="72" />
  </div>
</template>

<style scoped>
.schedule-table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: #ffffff;
}

.schedule-table {
  display: grid;
  min-width: 960px;
  grid-template-columns: 96px repeat(7, minmax(118px, 1fr));
}

.table-cell {
  min-width: 0;
  border-right: 1px solid var(--border-light);
  border-bottom: 1px solid var(--border-light);
}

.table-cell:nth-child(8n) {
  border-right: 0;
}

.table-corner,
.day-head,
.period-head {
  background: #f8fafc;
}

.table-corner,
.day-head {
  padding: 12px;
  text-align: center;
}

.table-corner,
.day-head strong,
.period-head strong {
  display: block;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 700;
}

.day-head span,
.period-head span {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

.period-head {
  display: grid;
  align-content: center;
  min-height: 118px;
  padding: 12px;
  text-align: center;
}

.course-cell {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 118px;
  padding: 10px;
  background: #ffffff;
}

.course-card {
  width: 100%;
  padding: 10px;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: #eff6ff;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.course-card:hover,
.course-card.active {
  border-color: #60a5fa;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
  transform: translateY(-1px);
}

.course-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.course-top strong {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.5;
}

.course-card p,
.course-card small {
  display: block;
  margin: 8px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 760px) {
  .schedule-table {
    min-width: 820px;
    grid-template-columns: 82px repeat(7, minmax(105px, 1fr));
  }

  .period-head,
  .course-cell {
    min-height: 108px;
  }
}
</style>
