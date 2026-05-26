import { flushPromises, mount, RouterLinkStub } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { defineComponent, h, inject, provide, type PropType } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import MailDraftPage from './MailDraftPage.vue';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';

import type { FormRules } from 'element-plus';

const mailApiMock = vi.hoisted(() => ({
  createMailDraft: vi.fn(),
  sendMailDraft: vi.fn(),
  updateMailDraft: vi.fn(),
}));

const packageApiMock = vi.hoisted(() => ({
  getLatestPackage: vi.fn(),
}));

const elementPlusMock = vi.hoisted(() => ({
  confirm: vi.fn(),
  error: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
}));

vi.mock('@/services/mailApi', () => mailApiMock);
vi.mock('@/services/packageApi', () => packageApiMock);

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { projectId: 'p1' } }),
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
 * 创建邮件页面测试用 Element Plus 替身，覆盖输入、单选、按钮和表单校验。
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
       * 校验必填字段，模拟页面依赖的 Element Plus validate 行为。
       *
       * @returns 校验通过时返回 true，否则返回 false
       */
      async function validate(): Promise<boolean> {
        return Object.entries(props.rules).every(([field, rules]) => {
          const value = props.model[field];
          const ruleList = Array.isArray(rules) ? rules : [rules];

          return ruleList.every((rule) => !rule.required || Boolean(String(value ?? '').trim()));
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
    ElAlert: defineComponent({
      props: { title: { type: String, default: '' } },
      template: '<div role="alert">{{ title }}</div>',
    }),
    ElButton: defineComponent({
      props: {
        disabled: { type: Boolean, default: false },
        loading: { type: Boolean, default: false },
      },
      emits: ['click'],
      template: '<button :disabled="disabled || loading" @click="$emit(\'click\', $event)"><slot /></button>',
    }),
    ElCard: passthrough,
    ElAside: passthrough,
    ElContainer: passthrough,
    ElDescriptions: passthrough,
    ElDescriptionsItem: defineComponent({
      template: '<div><slot /></div>',
    }),
    ElForm,
    ElFormItem: passthrough,
    ElHeader: passthrough,
    ElInput,
    ElMain: passthrough,
    ElRadioButton,
    ElRadioGroup,
    ElTag: defineComponent({
      template: '<span><slot /></span>',
    }),
  };
}

/**
 * 挂载邮件草稿页面，并写入当前用户与项目上下文。
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

  return mount(MailDraftPage, {
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
 * 按按钮文字触发点击，确保测试表达用户动作。
 *
 * @param wrapper 页面包装器
 * @param text 按钮文字
 */
async function clickButton(wrapper: ReturnType<typeof mount>, text: string): Promise<void> {
  const button = wrapper.findAll('button').find((item) => item.text().includes(text));
  expect(button, `未找到按钮：${text}`).toBeTruthy();
  await button!.trigger('click');
}

/**
 * 填写创建草稿所需字段。
 *
 * @param wrapper 页面包装器
 */
async function fillDraftForm(wrapper: ReturnType<typeof mount>): Promise<void> {
  const textareas = wrapper.findAll('textarea');
  await textareas[0].setValue('a@example.com, b@example.com');
  await wrapper.find('input').setValue('项目提交');
  await textareas[1].setValue('请查收附件。');
}

describe('MailDraftPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    packageApiMock.getLatestPackage.mockResolvedValue({
      packageId: 'pkg-zip',
      filename: '课程项目成果.zip',
      format: 'zip',
      size: 2048,
      snapshotCreatedAt: '2026-05-25 10:00:00',
    });
    mailApiMock.createMailDraft.mockResolvedValue({
      draftId: 'draft-1',
      projectId: '1',
      recipients: ['a@example.com', 'b@example.com'],
      subject: '项目提交',
      body: '请查收附件。',
      packageId: 'pkg-zip',
      attachmentFilename: '课程项目成果.zip',
      status: 'draft',
      createdAt: null,
      sentAt: null,
    });
    mailApiMock.updateMailDraft.mockResolvedValue({
      draftId: 'draft-1',
      projectId: '1',
      recipients: ['c@example.com'],
      subject: '项目提交更新',
      body: '正文已更新。',
      packageId: 'pkg-zip',
      attachmentFilename: '课程项目成果.zip',
      status: 'draft',
      createdAt: null,
      sentAt: null,
    });
    mailApiMock.sendMailDraft.mockResolvedValue({
      draftId: 'draft-1',
      projectId: '1',
      recipients: ['a@example.com', 'b@example.com'],
      subject: '项目提交',
      body: '请查收附件。',
      packageId: 'pkg-zip',
      attachmentFilename: '课程项目成果.zip',
      status: 'sent',
      createdAt: null,
      sentAt: '2026-05-25 10:20:00',
      message: '邮件已发送',
    });
    elementPlusMock.confirm.mockResolvedValue('confirm');
  });

  it('展示 zip 附件格式推荐提示', async () => {
    const wrapper = mountPage();
    await flushPromises();

    expect(wrapper.text()).toContain('推荐使用 .zip 作为邮件附件格式');
  });

  it('草稿生成后可编辑并保存收件人、主题和正文', async () => {
    const wrapper = mountPage();
    await flushPromises();

    await fillDraftForm(wrapper);
    await clickButton(wrapper, '生成草稿');
    await flushPromises();

    const textareas = wrapper.findAll('textarea');
    await textareas[0].setValue('c@example.com');
    await wrapper.find('input').setValue('项目提交更新');
    await textareas[1].setValue('正文已更新。');
    await clickButton(wrapper, '保存修改');
    await flushPromises();

    expect(mailApiMock.updateMailDraft).toHaveBeenCalledWith({
      draftId: 'draft-1',
      userId: 7,
      recipients: ['c@example.com'],
      subject: '项目提交更新',
      body: '正文已更新。',
      packageId: 'pkg-zip',
    });
  });

  it('发送草稿前要求确认并携带 confirmed 标识', async () => {
    const wrapper = mountPage();
    await flushPromises();

    await fillDraftForm(wrapper);
    await clickButton(wrapper, '生成草稿');
    await flushPromises();
    await clickButton(wrapper, '确认发送');
    await flushPromises();

    expect(elementPlusMock.confirm).toHaveBeenCalledWith(
      expect.stringContaining('请确认收件人、正文和附件均已核实'),
      '发送确认',
      expect.objectContaining({ confirmButtonText: '确认发送' }),
    );
    expect(mailApiMock.sendMailDraft).toHaveBeenCalledWith({
      draftId: 'draft-1',
      userId: 7,
      confirmed: true,
    });
  });
});
