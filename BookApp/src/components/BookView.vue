<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  api,
  errorMessage,
  formatTime,
  toDateString,
  toLocalIso,
  type Booking,
  type Room,
  type User,
} from '../api'

const props = defineProps<{
  rooms: Room[]
  users: User[]
  me: User
}>()

const emit = defineEmits<{ goToRooms: [] }>()

const DAY_START_HOUR = 7
const DAY_END_HOUR = 21
const HOUR_HEIGHT = 48
const hours = Array.from({ length: DAY_END_HOUR - DAY_START_HOUR }, (_, i) => DAY_START_HOUR + i)

const today = toDateString(new Date())
const date = ref(today)
const roomId = ref<number | null>(props.rooms[0]?.id ?? null)
const startTime = ref('09:00')
const endTime = ref('10:00')

const roomBookings = ref<Booking[]>([])
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')

// Select the first room once rooms exist (e.g. after the first one is created)
watch(
  () => props.rooms.length,
  () => {
    if (roomId.value == null && props.rooms.length > 0) roomId.value = props.rooms[0]!.id
  },
)

watch(roomId, loadBookings, { immediate: true })

async function loadBookings() {
  if (roomId.value == null) return
  loading.value = true
  error.value = ''
  try {
    roomBookings.value = await api.getBookings({ roomId: roomId.value })
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

const dayStart = computed(() => `${date.value}T00:00:00`)
const dayEnd = computed(() => `${date.value}T23:59:59`)

const dateHeading = computed(() =>
  new Date(`${date.value}T12:00:00`).toLocaleDateString(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }),
)

const bookingsForDay = computed(() =>
  roomBookings.value.filter((b) => b.tstart < dayEnd.value && b.tend > dayStart.value),
)

function userName(id: number) {
  if (id === props.me.id) return 'You'
  return props.users.find((u) => u.id === id)?.name ?? `User #${id}`
}

/** Minutes since midnight on the selected date, clamped to that day. */
function minutesOnDay(iso: string) {
  if (iso < dayStart.value) return 0
  if (iso > dayEnd.value) return 24 * 60
  return Number(iso.slice(11, 13)) * 60 + Number(iso.slice(14, 16))
}

function blockStyle(b: Booking) {
  const visibleStart = DAY_START_HOUR * 60
  const visibleEnd = DAY_END_HOUR * 60
  const start = Math.max(minutesOnDay(b.tstart), visibleStart)
  const end = Math.min(minutesOnDay(b.tend), visibleEnd)
  if (end <= start) return { display: 'none' }
  return {
    top: `${((start - visibleStart) / 60) * HOUR_HEIGHT}px`,
    height: `${((end - start) / 60) * HOUR_HEIGHT}px`,
  }
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function pickHour(hour: number) {
  startTime.value = `${pad(hour)}:00`
  endTime.value = `${pad(hour + 1)}:00`
  success.value = ''
  error.value = ''
}

function shiftDate(days: number) {
  const d = new Date(`${date.value}T12:00:00`)
  d.setDate(d.getDate() + days)
  date.value = toDateString(d)
}

function isPastHour(hour: number) {
  return `${date.value}T${pad(hour)}:59:59` < toLocalIso(new Date())
}

async function book() {
  error.value = ''
  success.value = ''

  if (roomId.value == null) {
    error.value = 'Select a room.'
    return
  }
  if (endTime.value <= startTime.value) {
    error.value = 'End time must be after start time.'
    return
  }

  submitting.value = true
  try {
    await api.createBooking({
      roomId: roomId.value,
      start: `${date.value}T${startTime.value}:00`,
      end: `${date.value}T${endTime.value}:00`,
    })
    const room = props.rooms.find((r) => r.id === roomId.value)
    success.value = `Booked ${room?.name} on ${dateHeading.value}, ${startTime.value}–${endTime.value}.`
    await loadBookings()
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section v-if="rooms.length === 0" class="card empty">
    <p>There are no rooms yet.</p>
    <button class="primary" @click="emit('goToRooms')">Add a room</button>
  </section>

  <div v-else class="book-layout">
    <section class="card">
      <h2>New booking</h2>

      <form class="form" @submit.prevent="book">
        <label>
          Room
          <select v-model="roomId">
            <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.name }}</option>
          </select>
        </label>

        <label>
          Date
          <input v-model="date" type="date" :min="today" required />
        </label>

        <div class="row">
          <label>
            From
            <input v-model="startTime" type="time" step="900" required />
          </label>
          <label>
            To
            <input v-model="endTime" type="time" step="900" required />
          </label>
        </div>

        <button class="primary" type="submit" :disabled="submitting">
          {{ submitting ? 'Booking…' : 'Book room' }}
        </button>

        <p v-if="error" class="alert error" role="alert">{{ error }}</p>
        <p v-if="success" class="alert success" role="status">{{ success }}</p>
      </form>
    </section>

    <section class="card">
      <div class="schedule-header">
        <button class="ghost" aria-label="Previous day" @click="shiftDate(-1)">‹</button>
        <h2>
          {{ dateHeading }}
          <span v-if="date === today" class="badge">Today</span>
        </h2>
        <button class="ghost" aria-label="Next day" @click="shiftDate(1)">›</button>
      </div>
      <p class="muted">Click a free hour to fill in the form.</p>

      <div class="schedule" :class="{ loading }" :aria-busy="loading">
        <button
          v-for="h in hours"
          :key="h"
          type="button"
          class="hour"
          :style="{ height: `${HOUR_HEIGHT}px` }"
          :disabled="isPastHour(h)"
          :aria-label="`Select ${pad(h)}:00 to ${pad(h + 1)}:00`"
          @click="pickHour(h)"
        >
          <span class="hour-label">{{ pad(h) }}:00</span>
        </button>

        <div
          v-for="b in bookingsForDay"
          :key="b.id"
          class="block"
          :class="{ mine: b.userId === me.id }"
          :style="blockStyle(b)"
        >
          <strong>{{ userName(b.userId) }}</strong>
          <span>{{ formatTime(b.tstart) }}–{{ formatTime(b.tend) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.book-layout {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 2fr;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 760px) {
  .book-layout {
    grid-template-columns: 1fr;
  }
}

.schedule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.schedule-header h2 {
  margin: 0;
  text-align: center;
}

.schedule {
  position: relative;
  border-top: 1px solid var(--border);
}

.schedule.loading {
  opacity: 0.5;
}

.hour {
  display: block;
  width: 100%;
  position: relative;
  border: none;
  border-bottom: 1px solid var(--border);
  border-radius: 0;
  background: transparent;
  padding: 0;
}

.hour:hover:not(:disabled) {
  background: var(--accent-bg);
}

.hour:disabled {
  opacity: 1;
  cursor: default;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 6px,
    var(--stripe) 6px,
    var(--stripe) 12px
  );
}

.hour-label {
  position: absolute;
  left: 0.5rem;
  top: 0.25rem;
  font-size: 0.8rem;
  color: var(--text-muted);
  font-variant-numeric: tabular-nums;
}

.block {
  position: absolute;
  left: 4rem;
  right: 0.5rem;
  background: var(--block-bg);
  color: var(--text-h);
  border-left: 3px solid var(--text-muted);
  border-radius: 6px;
  padding: 0.25rem 0.6rem;
  font-size: 0.85rem;
  display: flex;
  flex-wrap: wrap;
  column-gap: 0.75rem;
  align-items: flex-start;
  overflow: hidden;
  pointer-events: none;
}

.block.mine {
  background: var(--accent);
  color: var(--on-accent);
  border-left-color: transparent;
}

.badge {
  font-size: 0.7rem;
  background: var(--accent-bg);
  color: var(--accent);
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  vertical-align: middle;
  margin-left: 0.4rem;
}
</style>
