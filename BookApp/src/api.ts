import { ref } from 'vue'

export interface Room {
  id: number
  name: string
}

export interface User {
  id: number
  name: string
  email: string
  avatarUrl: string | null
}

export interface Booking {
  id: number
  roomId: number
  userId: number
  tstart: string
  tend: string
}

const BASE = '/api'

export const LOGIN_URL = '/oauth2/authorization/github'

/** Set to true whenever the backend answers 401, i.e. the session is missing or expired. */
export const unauthenticated = ref(false)

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

function csrfToken(): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith('XSRF-TOKEN='))
    ?.slice('XSRF-TOKEN='.length)
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body) headers.set('Content-Type', 'application/json')

  const method = (init.method ?? 'GET').toUpperCase()
  if (method !== 'GET') {
    const token = csrfToken()
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }

  const res = await fetch(BASE + path, { ...init, headers })

  if (res.status === 401) {
    unauthenticated.value = true
    throw new ApiError('Please sign in again.', 401)
  }

  if (!res.ok) {
    let message = `HTTP error ${res.status}`
    try {
      const body = await res.json()
      if (body?.message) message = body.message
    } catch {
      // response had no JSON body
    }
    throw new ApiError(message, res.status)
  }

  if (res.status === 204 || res.headers.get('Content-Length') === '0') return undefined as T
  return res.json()
}

export const api = {
  getMe: () => request<User>('/users/me'),
  logout: () => request<void>('/logout', { method: 'POST' }),

  getRooms: () => request<Room[]>('/rooms'),
  createRoom: (name: string) =>
    request<Room>('/rooms', { method: 'POST', body: JSON.stringify({ name }) }),

  getUsers: () => request<User[]>('/users'),

  getBookings: (filter: { roomId?: number; userId?: number } = {}) => {
    const params = new URLSearchParams()
    if (filter.roomId != null) params.set('roomId', String(filter.roomId))
    if (filter.userId != null) params.set('userId', String(filter.userId))
    const query = params.toString()
    return request<Booking[]>('/bookings' + (query ? `?${query}` : ''))
  },
  createBooking: (booking: { roomId: number; start: string; end: string }) =>
    request<Booking>('/bookings', { method: 'POST', body: JSON.stringify(booking) }),
  cancelBooking: (id: number) => request<void>(`/bookings/${id}`, { method: 'DELETE' }),
}

export function errorMessage(e: unknown): string {
  return e instanceof Error ? e.message : String(e)
}

const pad = (n: number) => String(n).padStart(2, '0')

/** Formats a Date as a local "YYYY-MM-DD" string. */
export function toDateString(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** Formats a Date as a local "YYYY-MM-DDTHH:mm:ss" string, matching the backend's LocalDateTime. */
export function toLocalIso(d: Date): string {
  return `${toDateString(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

export function formatTime(iso: string): string {
  return iso.slice(11, 16)
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}
