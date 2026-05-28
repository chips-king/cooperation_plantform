<template>
  <div data-sidebar="header" class="sidebar-header">
    <slot />
  </div>
</template>

<style>
.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 60px;
  padding: 0 16px;
  border-bottom: 1px solid var(--sidebar-border);
  flex-shrink: 0;
  overflow: hidden;
  white-space: nowrap;
}
</style>
