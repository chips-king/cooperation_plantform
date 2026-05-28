<template>
  <div data-sidebar="group-label" class="sidebar-group-label">
    <slot />
  </div>
</template>

<style>
.sidebar-group-label {
  padding: 0 12px 6px;
  color: var(--sidebar-fg-muted);
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}
</style>
