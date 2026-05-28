<template>
  <main data-sidebar="inset" class="sidebar-inset">
    <slot />
  </main>
</template>

<style>
.sidebar-inset {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--cb-bg-page, #f5f5f5);
  transition: margin-left 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>
