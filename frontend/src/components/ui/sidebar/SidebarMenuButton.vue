<template>
  <component
    :is="asChild ? 'div' : 'button'"
    ref="buttonRef"
    data-sidebar="menu-button"
    :data-active="isActive"
    :data-size="size"
    :class="buttonClass"
    @click="handleClick"
    @mouseenter="showTooltip = true"
    @mouseleave="showTooltip = false"
  >
    <slot />
    <span
      v-if="isTooltipVisible"
      class="sidebar-menu-tooltip"
      :style="tooltipStyle"
    >
      {{ tooltip }}
    </span>
  </component>
</template>

<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue';
import { useSidebar } from './useSidebar';

const props = withDefaults(defineProps<{
  isActive?: boolean;
  asChild?: boolean;
  variant?: 'default' | 'outline';
  size?: 'default' | 'sm' | 'lg';
  tooltip?: string;
}>(), {
  isActive: false,
  asChild: false,
  variant: 'default',
  size: 'default',
});

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

const { state, collapsible, isMobile } = useSidebar();
const showTooltip = ref(false);
const buttonRef = ref<HTMLElement | null>(null);
const tooltipTop = ref(0);

const isTooltipVisible = computed(() => {
  return showTooltip.value
    && props.tooltip
    && !isMobile.value
    && state.value === 'collapsed'
    && collapsible === 'icon';
});

watch(isTooltipVisible, async (visible) => {
  if (visible && buttonRef.value) {
    await nextTick();
    const rect = buttonRef.value.getBoundingClientRect();
    tooltipTop.value = rect.top + rect.height / 2;
  }
});

const tooltipStyle = computed(() => ({
  top: `${tooltipTop.value}px`,
}));

const buttonClass = computed(() => [
  'sidebar-menu-button',
  `sidebar-menu-button--${props.variant}`,
  `sidebar-menu-button--${props.size}`,
  { 'sidebar-menu-button--active': props.isActive },
]);

function handleClick(event: MouseEvent): void {
  emit('click', event);
}
</script>

<style>
.sidebar-menu-button {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 36px;
  padding: 0 12px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: var(--sidebar-fg);
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.15s ease;
  text-align: left;
  overflow: visible;
  position: relative;
  line-height: 1.4;
}

.sidebar-menu-button:hover {
  color: var(--sidebar-fg-active);
  background: var(--sidebar-menu-hover-bg);
}

.sidebar-menu-button--active {
  color: var(--sidebar-fg-active);
  background: var(--sidebar-menu-active-bg);
}

/* 左侧激活指示器 */
.sidebar-menu-button--active::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: var(--sidebar-accent);
  transition: height 0.2s ease;
}

.sidebar-menu-button .sidebar-icon {
  width: 18px;
  height: 18px;
  opacity: 0.7;
  flex-shrink: 0;
  transition: opacity 0.15s;
}

.sidebar-menu-button:hover .sidebar-icon {
  opacity: 0.9;
}

.sidebar-menu-button--active .sidebar-icon {
  opacity: 1;
  color: var(--sidebar-accent);
}

/* 尺寸变体 */
.sidebar-menu-button--sm {
  min-height: 30px;
  font-size: 13px;
  padding: 0 10px;
}

.sidebar-menu-button--lg {
  min-height: 42px;
  font-size: 15px;
  padding: 0 14px;
}

/* outline 变体 */
.sidebar-menu-button--outline {
  border: 1px solid var(--sidebar-border);
}

.sidebar-menu-button--outline:hover {
  border-color: var(--sidebar-fg-muted);
}

/* tooltip */
.sidebar-menu-tooltip {
  position: fixed;
  left: calc(var(--sidebar-width-icon) + 8px);
  transform: translateY(-50%);
  z-index: 100;
  padding: 6px 10px;
  border-radius: 6px;
  background: var(--sidebar-bg-secondary);
  color: var(--sidebar-fg-active);
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 1px solid var(--sidebar-border);
}
</style>
