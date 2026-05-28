import { inject } from 'vue';
import { SidebarKey, type SidebarContext } from './context';

export function useSidebar(): SidebarContext {
  const context = inject(SidebarKey);
  if (!context) {
    throw new Error('useSidebar must be used within a SidebarProvider');
  }
  return context;
}
