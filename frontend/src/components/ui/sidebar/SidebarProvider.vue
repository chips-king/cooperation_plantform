<template>
  <div
    class="sidebar-provider"
    :style="providerStyle"
    :data-sidebar-variant="variant"
    :data-sidebar-side="side"
  >
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ref, provide, computed, onMounted, onUnmounted, watch } from 'vue';
import { SidebarKey, type SidebarContext, type SidebarVariant, type SidebarCollapsible, type SidebarSide } from './context';

const props = withDefaults(defineProps<{
  defaultOpen?: boolean;
  open?: boolean;
  collapsible?: SidebarCollapsible;
  variant?: SidebarVariant;
  side?: SidebarSide;
}>(), {
  defaultOpen: true,
  open: undefined,
  collapsible: 'offcanvas',
  variant: 'sidebar',
  side: 'left',
});

const emit = defineEmits<{
  'update:open': [value: boolean];
}>();

const isMobile = ref(false);
const mobileOpen = ref(false);
const collapsed = ref(false);
const hasHydrated = ref(false);

// 优先使用 v-model:open，否则内部管理
const isControlled = computed(() => props.open !== undefined);

function checkMobile(): void {
  const wasMobile = isMobile.value;
  isMobile.value = window.innerWidth < 768;
  if (wasMobile && !isMobile.value) {
    mobileOpen.value = false;
  }
}

function getLocalStorageKey(): string {
  return 'sidebar:state';
}

function readPersistedState(): boolean | null {
  try {
    const raw = localStorage.getItem(getLocalStorageKey());
    if (raw === null) return null;
    return raw === 'true';
  } catch {
    return null;
  }
}

function writePersistedState(value: boolean): void {
  try {
    localStorage.setItem(getLocalStorageKey(), String(value));
  } catch {
    // ignore
  }
}

onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);

  const persisted = readPersistedState();
  if (persisted !== null) {
    collapsed.value = !persisted;
  } else {
    collapsed.value = !props.defaultOpen;
  }
  hasHydrated.value = true;
});

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile);
});

const state = computed<'expanded' | 'collapsed'>(() => {
  if (isMobile.value) return 'expanded';
  return collapsed.value ? 'collapsed' : 'expanded';
});

const open = computed<boolean>(() => {
  if (isControlled.value) return props.open!;
  if (isMobile.value) return mobileOpen.value;
  return !collapsed.value;
});

function setOpen(value: boolean): void {
  if (isControlled.value) {
    emit('update:open', value);
    return;
  }
  if (isMobile.value) {
    mobileOpen.value = value;
  } else {
    collapsed.value = !value;
    if (hasHydrated.value) {
      writePersistedState(value);
    }
  }
}

function toggleSidebar(): void {
  setOpen(!open.value);
}

// 键盘快捷键 Cmd/Ctrl + B
function onKeyDown(e: KeyboardEvent): void {
  if (e.key === 'b' && (e.metaKey || e.ctrlKey)) {
    e.preventDefault();
    toggleSidebar();
  }
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown);
});

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown);
});

provide<SidebarContext>(SidebarKey, {
  state,
  open,
  setOpen,
  toggleSidebar,
  isMobile,
  collapsible: props.collapsible,
  variant: props.variant,
  side: props.side,
});

const providerStyle = computed(() => {
  return {
    '--sidebar-width': '16rem',
    '--sidebar-width-icon': '3rem',
  } as Record<string, string>;
});
</script>

<style>
.sidebar-provider {
  display: flex;
  min-height: 100vh;
  width: 100%;
}
</style>
