<template>
  <button
    class="sidebar-trigger"
    title="切换侧边栏 (Ctrl+B)"
    @click="toggleSidebar"
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
    >
      <rect width="18" height="18" x="3" y="3" rx="2" />
      <path d="M9 3v18" />
      <path d="m14 9 3 3-3 3" />
    </svg>
  </button>
</template>

<script setup lang="ts">
import { useSidebar } from './useSidebar';

const { toggleSidebar } = useSidebar();
</script>

<style>
.sidebar-trigger {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  color: var(--cb-text-secondary, #666);
  background: transparent;
  cursor: pointer;
  transition: all 0.15s ease;
}

.sidebar-trigger:hover {
  background: var(--cb-border-light, #eee);
  color: var(--cb-text-primary, #333);
  transform: scale(1.05);
}

.sidebar-trigger:active {
  transform: scale(0.95);
}
</style>
