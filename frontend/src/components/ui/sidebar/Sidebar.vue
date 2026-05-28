<template>
  <!-- 移动端遮罩 -->
  <div
    v-if="isMobile && open"
    class="sidebar-overlay"
    @click="setOpen(false)"
  />
  <aside
    ref="sidebarRef"
    class="sidebar"
    :data-state="state"
    :data-collapsible="collapsible"
    :data-variant="variant"
    :data-side="side"
    :data-mobile="isMobile ? 'true' : undefined"
  >
    <div class="sidebar-inner">
      <slot />
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useSidebar } from './useSidebar';

const { state, open, setOpen, isMobile, collapsible, variant, side } = useSidebar();

const sidebarRef = ref<HTMLElement | null>(null);
</script>

<style>
.sidebar {
  position: relative;
  width: var(--sidebar-width);
  height: 100vh;
  flex-shrink: 0;
  background: var(--sidebar-bg);
  color: var(--sidebar-fg);
  transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  z-index: 10;
  border-right: 1px solid var(--sidebar-border);
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: var(--sidebar-width);
  overflow: hidden;
}

/* ---- collapsible=offcanvas 折叠 ---- */
.sidebar[data-collapsible="offcanvas"][data-state="collapsed"] {
  width: 0;
}

.sidebar[data-collapsible="offcanvas"][data-state="collapsed"] .sidebar-inner {
  width: 0;
}

/* ---- collapsible=icon 折叠为图标栏 ---- */
.sidebar[data-collapsible="icon"][data-state="collapsed"] {
  width: var(--sidebar-width-icon);
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-inner {
  width: var(--sidebar-width-icon);
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-group-label,
.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-menu-button span:not(.sidebar-icon),
.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-group-action {
  display: none;
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-menu-button {
  justify-content: center;
  padding: 0;
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-header {
  justify-content: center;
  padding: 0;
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-header .sidebar-brand-name {
  display: none;
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-footer {
  padding: 8px 4px;
}

.sidebar[data-collapsible="icon"][data-state="collapsed"] .sidebar-footer > * {
  justify-content: center;
}

/* ---- 移动端 ---- */
.sidebar[data-mobile="true"] {
  position: fixed;
  left: 0;
  top: 0;
  z-index: 50;
  transform: translateX(-100%);
  width: var(--sidebar-width);
}

.sidebar[data-mobile="true"] .sidebar-inner {
  width: var(--sidebar-width);
}

.sidebar[data-mobile="true"][data-state="expanded"] {
  transform: translateX(0);
}

/* 遮罩 */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 40;
  backdrop-filter: blur(2px);
}

/* variant: floating */
.sidebar[data-variant="floating"] {
  margin: 8px;
  height: calc(100vh - 16px);
  border-radius: 12px;
}

/* variant: inset */
.sidebar[data-variant="inset"] {
  background: transparent;
  color: var(--cb-text-primary);
}

.sidebar[data-variant="inset"] .sidebar-inner {
  margin: 8px;
  height: calc(100vh - 16px);
  border-radius: 12px;
  background: var(--sidebar-bg);
  color: var(--sidebar-fg);
}

/* side: right */
.sidebar[data-side="right"] {
  order: 1;
}
</style>
