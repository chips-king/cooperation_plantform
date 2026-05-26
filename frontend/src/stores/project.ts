import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import type { Group, Project } from '@/types/project';

/**
 * 项目筛选条件。
 */
export interface ProjectFilters {
  groupId: number | null;
  keyword: string;
  status: string | null;
}

/**
 * 小组与项目状态 Store。
 *
 * @returns 当前小组、当前项目、项目列表、筛选条件和状态变更方法
 */
export const useProjectStore = defineStore('project', () => {
  const currentGroup = ref<Group | null>(null);
  const currentProject = ref<Project | null>(null);
  const groups = ref<Group[]>([]);
  const projects = ref<Project[]>([]);
  const filters = ref<ProjectFilters>({
    groupId: null,
    keyword: '',
    status: null,
  });

  const activeProjects = computed(() => projects.value.filter((project) => project.status === 'active'));

  /**
   * 更新小组列表。
   *
   * @param nextGroups 最新小组列表
   */
  function setGroups(nextGroups: Group[]): void {
    groups.value = nextGroups;
  }

  /**
   * 更新当前小组。
   *
   * @param group 当前小组，传入 null 表示清空
   */
  function setCurrentGroup(group: Group | null): void {
    currentGroup.value = group;
    filters.value.groupId = group?.id ?? null;
  }

  /**
   * 更新项目列表。
   *
   * @param nextProjects 最新项目列表
   */
  function setProjects(nextProjects: Project[]): void {
    projects.value = nextProjects;
  }

  /**
   * 更新当前项目。
   *
   * @param project 当前项目，传入 null 表示清空
   */
  function setCurrentProject(project: Project | null): void {
    currentProject.value = project;
  }

  /**
   * 合并更新项目筛选条件。
   *
   * @param nextFilters 待更新的筛选字段
   */
  function setFilters(nextFilters: Partial<ProjectFilters>): void {
    filters.value = {
      ...filters.value,
      ...nextFilters,
    };
  }

  /**
   * 清空项目相关状态。
   */
  function clearProjectState(): void {
    currentGroup.value = null;
    currentProject.value = null;
    groups.value = [];
    projects.value = [];
    filters.value = {
      groupId: null,
      keyword: '',
      status: null,
    };
  }

  return {
    currentGroup,
    currentProject,
    groups,
    projects,
    filters,
    activeProjects,
    setGroups,
    setCurrentGroup,
    setProjects,
    setCurrentProject,
    setFilters,
    clearProjectState,
  };
});
