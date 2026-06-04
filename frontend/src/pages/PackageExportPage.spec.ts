import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { defineComponent, h, inject, provide, type PropType } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import PackageExportPage from './PackageExportPage.vue';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';

import type { FormRules } from 'element-plus';

const packageApiMock = vi.hoisted(() => ({
  createPackage: vi.fn(),
  downloadLatestPackage: vi.fn(),
  getLatestPackage: vi.fn(),
}));

const elementPlusMock = vi.hoisted(() => ({
  confirm: vi.fn(),
  error: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
}));

vi.mock('@/services/packageApi', () => packageApiMock);

vi.mock('@/services/activityApi', () => ({
  listNotifications: vi.fn().mockResolvedValue({ notifications: [] }),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :href="String(to)"><slot /></a>',
  },
  useRoute: () => ({ params: { projectId: 'p1' } }),
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock('element-plus', () => ({
  ElMessage: {
    error: elementPlusMock.error,
    success: elementPlusMock.success,
    warning: elementPlusMock.warning,
  },
  ElMessageBox: {
    confirm: elementPlusMock.confirm,
  },
}));

const radioGroupKey = Symbol('radioGroup');

interface RadioGroupContext {
  value: () => string;
  update: (value: string) => void;
}

/**
 * 创建页面测试用 Element Plus 替身，保留 v-model、点击和表单校验行为。
 *
 * @returns Vue Test Utils 全局组件替身配置
 */
function createElementStubs(): Record<string, ReturnType<typeof defineComponent>> {
  const ElForm = defineComponent({
    props: {
      model: { type: Object as PropType<Record<string, unknown>>, required: true },
      rules: { type: Object as PropType<FormRules>, default: () => ({}) },
    },
    setup(props, { expose, slots }) {
      /**
       * 按页面声明的 required 与 pattern 规则执行最小校验。
       *
       * @returns 校验通过时返回 true，否则返回 false
       */
      async function validate(): Promise<boolean> {
        return Object.entries(props.rules).every(([field, rules]) => {
          const value = props.model[field];
          const ruleList = Array.isArray(rules) ? rules : [rules];

          return ruleList.every((rule) => {
            if (rule.required && !String(value ?? '').trim()) {
              return false;
            }

            if (rule.pattern && !rule.pattern.test(String(value ?? ''))) {
              return false;
            }

            return true;
          });
        });
      }

      expose({ validate });

      return () => h('form', slots.default?.());
    },
  });

  const ElInput = defineComponent({
    props: {
      modelValue: { type: String, default: '' },
      placeholder: { type: String, default: '' },
      type: { type: String, default: 'text' },
    },
    emits: ['update:modelValue'],
    template: `
      <textarea
        v-if="type === 'textarea'"
        :placeholder="placeholder"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
      />
      <input
        v-else
        :placeholder="placeholder"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
      />
    `,
  });

  const ElRadioGroup = defineComponent({
    props: { modelValue: { type: String, default: '' } },
    emits: ['update:modelValue'],
    setup(props, { emit, slots }) {
      provide<RadioGroupContext>(radioGroupKey, {
        value: () => props.modelValue,
        update: (value: string) => emit('update:modelValue', value),
      });

      return () => h('div', slots.default?.());
    },
  });

  const ElRadioButton = defineComponent({
    props: { label: { type: String, required: true } },
    setup(props, { slots }) {
      const group = inject<RadioGroupContext>(radioGroupKey);

      return () => h(
        'button',
        {
          type: 'button',
          'data-selected': group?.value() === props.label ? 'true' : 'false',
          'data-radio-label': props.label,
          onClick: () => group?.update(props.label),
        },
        slots.default?.(),
      );
    },
  });

  const passthrough = defineComponent({
    template: '<div><slot name="header" /><slot /></div>',
  });

  return {
    ElButton: defineComponent({
      props: {
        disabled: { type: Boolean, default: false },
        loading: { type: Boolean, default: false },
      },
      emits: ['click'],
      template: '<button :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>',
    }),
    ElBadge: passthrough,
    ElCard: passthrough,
    ElAside: passthrough,
    ElContainer: passthrough,
    ElDescriptions: passthrough,
    ElDescriptionsItem: defineComponent({
      template: '<div><slot /></div>',
    }),
    ElEmpty: defineComponent({
      props: { description: { type: String, default: '' } },
      template: '<div>{{ description }}</div>',
    }),
    ElDropdown: defineComponent({
      emits: ['command'],
      template: '<div><slot /><slot name="dropdown" /></div>',
    }),
    ElDropdownItem: defineComponent({
      props: {
        command: { type: String, default: '' },
        divided: { type: Boolean, default: false },
      },
      template: '<button type="button"><slot /></button>',
    }),
    ElDropdownMenu: passthrough,
    ElForm,
    ElFormItem: passthrough,
    ElHeader: passthrough,
    ElIcon: passthrough,
    ElInput,
    ElMain: passthrough,
    ElRadioButton,
    ElRadioGroup,
    ElSwitch: defineComponent({
      props: { modelValue: { type: Boolean, default: false } },
      emits: ['update:modelValue'],
      template: '<input type="checkbox" :checked="modelValue" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
    }),
    ElTag: defineComponent({
      template: '<span><slot /></span>',
    }),
  };
}

/**
 * 挂载打包导出页面，并写入当前用户与项目上下文。
 *
 * @returns 页面包装器
 */
function mountPage() {
  const pinia = createPinia();
  setActivePinia(pinia);

  useAuthStore().setSession({ id: 7, displayName: '负责人', email: 'owner@example.com' }, 'token');
  useProjectStore().setCurrentProject({
    id: 1,
    groupId: 2,
    name: '协作项目',
    ownerId: 7,
    status: 'active',
    endedAt: null,
    reopenedAt: null,
  });

  return mount(PackageExportPage, {
    global: {
      plugins: [pinia],
      stubs: {
        RouterLink: RouterLinkStub,
        ...createElementStubs(),
      },
    },
  });
}

/**
 * 按按钮文字触发点击，避免测试依赖按钮排列顺序。
 *
 * @param wrapper 页面包装器
 * @param text 按钮文字
 */
async function clickButton(wrapper: ReturnType<typeof mount>, text: string): Promise<void> {
  const button = wrapper.findAll('button').find((item) => item.text().includes(text));
  expect(button, `未找到按钮：${text}`).toBeTruthy();
  await button!.trigger('click');
}

describe('PackageExportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    packageApiMock.getLatestPackage.mockRejectedValue(new Error('暂无压缩包'));
    packageApiMock.createPackage.mockResolvedValue({
      packageId: 'pkg-new',
      filename: '课程项目成果.7z',
      format: '7z',
      size: 2048,
      snapshotCreatedAt: '2026-05-25 10:00:00',
    });
    elementPlusMock.confirm.mockResolvedValue('confirm');
  });

  it('选择 7z 格式后按所选格式生成压缩包', async () => {
    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('input[placeholder="例如：课程项目成果"]').setValue('课程项目成果');
    await wrapper.find('[data-radio-label="7z"]').trigger('click');
    await clickButton(wrapper, '生成压缩包');
    await flushPromises();

    expect(packageApiMock.createPackage).toHaveBeenCalledWith(expect.objectContaining({
      baseName: '课程项目成果',
      format: '7z',
      projectId: '1',
      userId: 7,
    }));
  });

  it('文件名包含路径分隔符时阻止提交', async () => {
    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('input[placeholder="例如：课程项目成果"]').setValue('../课程项目成果');
    await clickButton(wrapper, '生成压缩包');
    await flushPromises();

    expect(packageApiMock.createPackage).not.toHaveBeenCalled();
  });

  it('存在最近压缩包时先展示覆盖确认', async () => {
    packageApiMock.getLatestPackage.mockResolvedValue({
      packageId: 'pkg-old',
      filename: '旧成果.zip',
      format: 'zip',
      size: 1024,
      snapshotCreatedAt: '2026-05-24 10:00:00',
    });

    const wrapper = mountPage();
    await flushPromises();

    await wrapper.find('input[placeholder="例如：课程项目成果"]').setValue('课程项目成果');
    await clickButton(wrapper, '生成压缩包');
    await flushPromises();

    expect(elementPlusMock.confirm).toHaveBeenCalledWith(
      expect.stringContaining('旧成果.zip'),
      '确认覆盖',
      expect.objectContaining({ confirmButtonText: '继续打包' }),
    );
    expect(packageApiMock.createPackage).toHaveBeenCalled();
  });
});
