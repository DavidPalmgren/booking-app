<script setup lang="ts">
import { ref } from 'vue'
import { api, errorMessage, type Room } from '../api'

defineProps<{ rooms: Room[] }>()
const emit = defineEmits<{ created: [Room] }>()

const name = ref('')
const error = ref('')
const submitting = ref(false)

async function create() {
  error.value = ''
  submitting.value = true
  try {
    const room = await api.createRoom(name.value)
    emit('created', room)
    name.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="card">
    <h2>Rooms</h2>

    <form class="inline-form" @submit.prevent="create">
      <input v-model="name" placeholder="New room name" required />
      <button class="primary" type="submit" :disabled="submitting">Add room</button>
    </form>
    <p v-if="error" class="alert error" role="alert">{{ error }}</p>

    <p v-if="rooms.length === 0" class="muted">No rooms yet.</p>
    <ul v-else class="list">
      <li v-for="room in rooms" :key="room.id" class="list-item">
        <strong>{{ room.name }}</strong>
        <span class="muted">#{{ room.id }}</span>
      </li>
    </ul>
  </section>
</template>
