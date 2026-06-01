<template>
  <main class="login-page">
    <section class="login-page__panel" aria-label="登录">
      <div class="login-page__brand">
        <span class="login-page__mark">协</span>
        <div>
          <h1>分工协作系统</h1>
          <p>登录后继续管理小组项目、文件与最终提交。</p>
        </div>
      </div>

      <el-form
        ref="loginFormRef"
        class="login-page__form"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        @submit.prevent="submitLogin"
      >
        <el-form-item label="账号" prop="account">
          <el-input v-model.trim="loginForm.account" placeholder="请输入账号或邮箱" autocomplete="username" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            placeholder="请输入密码"
            type="password"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>

        <el-alert
          v-if="errorMessage"
          class="login-page__alert"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
        />

        <el-button class="login-page__submit" type="primary" native-type="submit" :loading="submitting">
          登录
        </el-button>
      </el-form>

      <p class="login-page__footer">
        还没有账户？<router-link to="/register">立即注册</router-link>
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

interface LoginForm {
  account: string;
  password: string;
}

interface LoginResponse {
  user: CurrentUser;
  token: string;
  permissions?: PermissionCode[];
}

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const loginFormRef = ref<FormInstance>();
const submitting = ref(false);
const errorMessage = ref('');
const loginForm = reactive<LoginForm>({
  account: '',
  password: '',
});

const loginRules: FormRules<LoginForm> = {
  account: [
    { required: true, message: '请输入账号或邮箱', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
};

/**
 * 调用登录接口并写入会话信息。
 *
 * @returns 登录成功后的用户、令牌和权限摘要
 */
async function loginWithPassword(): Promise<LoginResponse> {
  return request<LoginResponse>({
    url: '/auth/login',
    method: 'POST',
    data: {
      account: loginForm.account,
      password: loginForm.password,
    },
  });
}

/**
 * 校验并提交登录表单，密码仅用于本次请求，不写入本地持久化。
 */
async function submitLogin(): Promise<void> {
  errorMessage.value = '';

  if (!loginFormRef.value) {
    return;
  }

  const valid = await loginFormRef.value.validate().catch(() => false);

  // 表单未通过基础校验时停止请求，避免把明显无效数据交给后端。
  if (!valid) {
    return;
  }

  submitting.value = true;

  try {
    const response = await loginWithPassword();
    authStore.setSession(response.user, response.token, response.permissions ?? []);
    loginForm.password = '';
    ElMessage.success('登录成功');
    await router.push((route.query.redirect as string) || '/');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  color: #20242c;
  background:
    linear-gradient(135deg, rgba(29, 79, 145, 0.08), transparent 38%),
    #f6f7f9;
}

.login-page__panel {
  width: min(100%, 420px);
  padding: 30px;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(32, 36, 44, 0.08);
}

.login-page__brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 28px;
}

.login-page__mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  color: #ffffff;
  background: #1d4f91;
  font-size: 20px;
  font-weight: 700;
}

.login-page__brand h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
}

.login-page__brand p {
  margin: 4px 0 0;
  color: #687386;
  line-height: 1.5;
}

.login-page__form {
  display: grid;
  gap: 4px;
}

.login-page__alert {
  margin-bottom: 8px;
}

.login-page__submit {
  width: 100%;
  margin-top: 8px;
}

.login-page__footer {
  margin: 16px 0 0;
  text-align: center;
  color: #687386;
  font-size: 14px;
}

.login-page__footer a {
  color: #1d4f91;
  text-decoration: none;
  font-weight: 500;
}

.login-page__footer a:hover {
  text-decoration: underline;
}
</style>
