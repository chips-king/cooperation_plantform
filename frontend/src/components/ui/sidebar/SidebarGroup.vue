<template>
  <div data-sidebar="group" class="sidebar-group">
    <slot />
  </div>
</template>

<style>
.sidebar-group {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 0 8px;
}

.sidebar-group + .sidebar-group {
  border-top: 1px solid var(--sidebar-border);
  padding-top: 8px;
}

/* 让 group header 支持 flex 对齐 */
.sidebar-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 6px;
}
</style>
