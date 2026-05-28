<template>
  <button
    data-sidebar="menu-action"
    class="sidebar-menu-action"
    :title="tooltip"
    @click.stop="$emit('click', $event)"
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
.sidebar-menu-action {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  margin-left: auto;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--sidebar-fg-muted);
  cursor: pointer;
  transition: all 0.15s ease;
  opacity: 0;
  flex-shrink: 0;
}

.sidebar-menu-item:hover .sidebar-menu-action {
  opacity: 1;
}

.sidebar-menu-action:hover {
  background: var(--sidebar-menu-hover-bg);
  color: var(--sidebar-fg-active);
}

.sidebar-menu-action svg {
  width: 14px;
  height: 14px;
}
</style>
