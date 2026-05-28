<template>
  <div data-sidebar="footer" class="sidebar-footer">
    <slot />
  </div>
</template>

<style>
.sidebar-footer {
  flex-shrink: 0;
  padding: 12px 12px;
  border-top: 1px solid var(--sidebar-border);
}
</style>
