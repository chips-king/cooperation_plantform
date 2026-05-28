import { type InjectionKey, type Ref } from 'vue';

export type SidebarVariant = 'sidebar' | 'floating' | 'inset';
export type SidebarCollapsible = 'offcanvas' | 'icon';
export type SidebarSide = 'left' | 'right';

export interface SidebarContext {
  state: Ref<'expanded' | 'collapsed'>;
  open: Ref<boolean>;
  setOpen: (value: boolean) => void;
  toggleSidebar: () => void;
  isMobile: Ref<boolean>;
  collapsible: SidebarCollapsible;
  variant: SidebarVariant;
  side: SidebarSide;
}

export const SidebarKey: InjectionKey<SidebarContext> = Symbol('sidebar');
