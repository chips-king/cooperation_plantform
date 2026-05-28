<template>
  <button
    data-sidebar="group-action"
    class="sidebar-group-action"
    :title="tooltip"
    @click="$emit('click', $event)"
  >
    <slot />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  tooltip?: string;
}>();

defineEmits<{
  click: [event: MouseEvent];
}>();
</script>

<style>
.sidebar-group-action {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--sidebar-fg-muted);
  cursor: pointer;
  transition: all 0.15s ease;
  opacity: 0;
}

.sidebar-group:hover .sidebar-group-action {
  opacity: 1;
}

.sidebar-group-action:hover {
  background: var(--sidebar-menu-hover-bg);
  color: var(--sidebar-fg-active);
}

.sidebar-group-action svg {
  width: 14px;
  height: 14px;
}
</style>
