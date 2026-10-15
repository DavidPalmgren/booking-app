<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  api,
  errorMessage,
  formatDate,
  formatTime,
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

const showAll = ref(false)
const bookings = ref<Booking[]>([])
const loadedAt = ref('')
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  loading.value = true
  try {
    bookings.value = await api.getBookings(showAll.value ? {} : { userId: props.me.id })
    loadedAt.value = toLocalIso(new Date())
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

watch(showAll, load, { immediate: true })

const upcoming = computed(() => bookings.value.filter((b) => b.tend > loadedAt.value))
const past = computed(() => bookings.value.filter((b) => b.tend <= loadedAt.value).reverse())

function roomName(id: number) {
  return props.rooms.find((r) => r.id === id)?.name ?? `Room #${id}`
}

function userName(id: number) {
  if (id === props.me.id) return 'You'
  return props.users.find((u) => u.id === id)?.name ?? `User #${id}`
}

async function cancel(b: Booking) {
  if (!confirm(`Cancel booking of ${roomName(b.roomId)} on ${formatDate(b.tstart)}?`)) return
  error.value = ''
  try {
    await api.cancelBooking(b.id)
    bookings.value = bookings.value.filter((x) => x.id !== b.id)
  } catch (e) {
    error.value = errorMessage(e)
  }
}
</script>

<template>
  <section class="card">
    <div class="section-header">
      <h2>{{ showAll ? 'All bookings' : 'My bookings' }}</h2>
      <label class="toggle">
        <input v-model="showAll" type="checkbox" />
        Show everyone's bookings
      </label>
    </div>

    <p v-if="error" class="alert error" role="alert">{{ error }}</p>
    <p v-if="loading" class="muted">Loading…</p>

    <template v-else>
      <h3>Upcoming</h3>
      <p v-if="upcoming.length === 0" class="muted">No upcoming bookings.</p>
      <ul v-else class="list">
        <li v-for="b in upcoming" :key="b.id" class="list-item">
          <div>
            <strong>{{ roomName(b.roomId) }}</strong>
            <div class="muted">
              {{ formatDate(b.tstart) }} · {{ formatTime(b.tstart) }}–{{ formatTime(b.tend) }}
              <template v-if="showAll"> · {{ userName(b.userId) }}</template>
            </div>
          </div>
          <button v-if="b.userId === me.id" class="danger" @click="cancel(b)">Cancel</button>
        </li>
      </ul>

      <template v-if="past.length > 0">
        <h3>Past</h3>
        <ul class="list past">
          <li v-for="b in past" :key="b.id" class="list-item">
            <div>
              <strong>{{ roomName(b.roomId) }}</strong>
              <div class="muted">
                {{ formatDate(b.tstart) }} · {{ formatTime(b.tstart) }}–{{ formatTime(b.tend) }}
                <template v-if="showAll"> · {{ userName(b.userId) }}</template>
              </div>
            </div>
          </li>
        </ul>
      </template>
    </template>
  </section>
</template>

<style scoped>
.past {
  opacity: 0.6;
}

.toggle {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9rem;
}
</style>
