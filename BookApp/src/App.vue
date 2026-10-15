<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, errorMessage, LOGIN_URL, unauthenticated, type Room, type User } from './api'
import BookView from './components/BookView.vue'
import BookingsView from './components/BookingsView.vue'
import RoomsView from './components/RoomsView.vue'

type Tab = 'book' | 'bookings' | 'rooms'

const tabs: { id: Tab; label: string }[] = [
  { id: 'book', label: 'Book a room' },
  { id: 'bookings', label: 'My bookings' },
  { id: 'rooms', label: 'Rooms' },
]

const activeTab = ref<Tab>('book')
const me = ref<User | null>(null)
const rooms = ref<Room[]>([])
const users = ref<User[]>([])
const loading = ref(true)
const loadError = ref('')
const loginFailed = new URLSearchParams(location.search).has('loginError')

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    me.value = await api.getMe()
    unauthenticated.value = false
    ;[rooms.value, users.value] = await Promise.all([api.getRooms(), api.getUsers()])
  } catch (e) {
    if (!unauthenticated.value) loadError.value = `Could not reach the backend: ${errorMessage(e)}`
  } finally {
    loading.value = false
  }
}

async function logout() {
  try {
    await api.logout()
  } finally {
    me.value = null
    unauthenticated.value = true
  }
}

onMounted(load)
</script>

<template>
  <header class="topbar">
    <div class="brand">📅 Room Booking</div>
    <div v-if="me && !unauthenticated" class="account">
      <img v-if="me.avatarUrl" :src="me.avatarUrl" alt="" class="avatar" />
      <span class="account-name">{{ me.name }}</span>
      <button class="ghost" @click="logout">Sign out</button>
    </div>
  </header>

  <main v-if="loading" class="muted center">Loading…</main>

  <main v-else-if="unauthenticated" class="card login">
    <h1>Welcome</h1>
    <p>Sign in to see room availability and make bookings.</p>
    <p v-if="loginFailed" class="alert error">GitHub sign-in failed. Please try again.</p>
    <a class="button primary" :href="LOGIN_URL">Sign in with GitHub</a>
  </main>

  <main v-else-if="loadError">
    <p class="alert error">{{ loadError }}</p>
    <button @click="load">Retry</button>
  </main>

  <template v-else-if="me">
    <nav class="tabs">
      <button
        v-for="t in tabs"
        :key="t.id"
        :class="{ active: activeTab === t.id }"
        :aria-current="activeTab === t.id ? 'page' : undefined"
        @click="activeTab = t.id"
      >
        {{ t.label }}
      </button>
    </nav>

    <main>
      <BookView
        v-if="activeTab === 'book'"
        :rooms="rooms"
        :users="users"
        :me="me"
        @go-to-rooms="activeTab = 'rooms'"
      />
      <BookingsView v-else-if="activeTab === 'bookings'" :rooms="rooms" :users="users" :me="me" />
      <RoomsView v-else :rooms="rooms" @created="rooms.push($event)" />
    </main>
  </template>
</template>
