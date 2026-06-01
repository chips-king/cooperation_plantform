<template>
  <main class="register-page">
    <section class="register-page__panel" aria-label="注册">
      <div class="register-page__brand">
        <span class="register-page__mark">协</span>
        <div>
          <h1>创建新账户</h1>
          <p>注册后即可加入小组协作，管理项目与文件。</p>
        </div>
      </div>

      <el-form
        ref="registerFormRef"
        class="register-page__form"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        @submit.prevent="submitRegister"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model.trim="registerForm.username"
            placeholder="2-50 个字符，用于登录"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="显示名称" prop="displayName">
          <el-input
            v-model.trim="registerForm.displayName"
            placeholder="你在系统中的展示名称"
            maxlength="100"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model.trim="registerForm.email"
            placeholder="用于接收通知"
            autocomplete="email"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="registerForm.password"
            placeholder="至少 6 位"
            type="password"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            placeholder="再次输入密码"
            type="password"
            autocomplete="new-password"
            show-password
          />
        </el-form-item>

        <el-alert
          v-if="errorMessage"
          class="register-page__alert"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
        />

        <el-button class="register-page__submit" type="primary" native-type="submit" :loading="submitting">
          注册
        </el-button>
      </el-form>

      <p class="register-page__footer">
        已有账户？<router-link to="/login">立即登录</router-link>
      </p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';

import { request } from '@/services/http';
import { useAuthStore } from '@/stores/auth';
import type { CurrentUser, PermissionCode } from '@/types/project';

interface RegisterForm {
  username: string;
  displayName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

interface RegisterResponse {
  user: CurrentUser;
  token: string;
  permissions?: PermissionCode[];
}

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const registerFormRef = ref<FormInstance>();
const submitting = ref(false);
const errorMessage = ref('');
const registerForm = reactive<RegisterForm>({
  username: '',
  displayName: '',
  email: '',
  password: '',
  confirmPassword: '',
});

/**
 * 校验确认密码是否与密码一致。
 */
function validateConfirmPassword(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请再次输入密码'));
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
}

const registerRules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度需在 2 到 50 个字符之间', trigger: 'blur' },
  ],
  displayName: [
    { required: true, message: '请输入显示名称', trigger: 'blur' },
    { max: 100, message: '显示名称不能超过 100 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.(com|cn|net|org|edu|gov|io|cc|vip|info|top|club|xyz|me|com\.cn|net\.cn|org\.cn|co\.cn)$/i, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' },
  ],
};

/**
 * 调用注册接口。
 */
async function registerUser(): Promise<RegisterResponse> {
  return request<RegisterResponse>({
    url: '/auth/register',
    method: 'POST',
    data: {
      username: registerForm.username,
      password: registerForm.password,
      displayName: registerForm.displayName,
      email: registerForm.email,
    },
  });
}

/**
 * 校验并提交注册表单。
 */
async function submitRegister(): Promise<void> {
  errorMessage.value = '';

  if (!registerFormRef.value) {
    return;
  }

  const valid = await registerFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  submitting.value = true;

  try {
    const response = await registerUser();
    authStore.setSession(response.user, response.token, response.permissions ?? []);
    ElMessage.success('注册成功');
    await router.push((route.query.redirect as string) || '/');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.register-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  color: #20242c;
  background:
    linear-gradient(135deg, rgba(29, 79, 145, 0.08), transparent 38%),
    #f6f7f9;
}

.register-page__panel {
  width: min(100%, 440px);
  padding: 30px;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(32, 36, 44, 0.08);
}

.register-page__brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 24px;
}

.register-page__mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  color: #ffffff;
  background: #1d4f91;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.register-page__brand h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
}

.register-page__brand p {
  margin: 4px 0 0;
  color: #687386;
  line-height: 1.5;
}

.register-page__form {
  display: grid;
  gap: 4px;
}

.register-page__alert {
  margin-bottom: 8px;
}

.register-page__submit {
  width: 100%;
  margin-top: 8px;
}

.register-page__footer {
  margin: 16px 0 0;
  text-align: center;
  color: #687386;
  font-size: 14px;
}

.register-page__footer a {
  color: #1d4f91;
  text-decoration: none;
  font-weight: 500;
}

.register-page__footer a:hover {
  text-decoration: underline;
}
</style>
