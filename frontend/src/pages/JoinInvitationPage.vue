<template>
  <main class="join-page">
    <section class="join-page__panel">
      <div class="join-page__brand">
        <span class="join-page__mark">协</span>
        <div>
          <h1>加入协作</h1>
          <p>你收到了一份小组项目邀请。</p>
        </div>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="join-page__loading">
        <el-skeleton :rows="4" animated />
      </div>

      <!-- 邀请无效或已过期 -->
      <el-alert
        v-else-if="errorMessage"
        :title="errorMessage"
        type="error"
        show-icon
        :closable="false"
        class="join-page__alert"
      />

      <!-- 邀请详情 -->
      <template v-else-if="invitation">
        <div class="join-page__info">
          <div class="join-page__info-row">
            <span class="join-page__info-label">小组</span>
            <span class="join-page__info-value">{{ invitation.groupName }}</span>
          </div>
          <div class="join-page__info-row">
            <span class="join-page__info-label">项目</span>
            <span class="join-page__info-value">{{ invitation.projectName }}</span>
          </div>
          <div class="join-page__info-row">
            <span class="join-page__info-label">加入方式</span>
            <el-tag :type="invitation.mode === 'direct' ? 'success' : 'warning'" size="small">
              {{ invitation.mode === 'direct' ? '直接加入' : '需审核' }}
            </el-tag>
          </div>
        </div>

        <!-- 未登录 -->
        <template v-if="!authStore.isAuthenticated">
          <el-alert
            title="请先登录或注册后再加入"
            type="info"
            show-icon
            :closable="false"
            class="join-page__alert"
          />
          <div class="join-page__actions">
            <el-button type="primary" @click="goLogin">去登录</el-button>
            <el-button @click="goRegister">去注册</el-button>
          </div>
        </template>

        <!-- 已登录 -->
        <template v-else>
          <el-alert
            v-if="joinResultMessage"
            :title="joinResultMessage"
            :type="joinResultType"
            show-icon
            :closable="false"
            class="join-page__alert"
          />

          <div class="join-page__actions">
            <el-button
              v-if="!joined"
              type="primary"
              :loading="joining"
              @click="handleJoin"
            >
              加入项目
            </el-button>
            <el-button v-if="joined" type="primary" @click="goToProject">
              进入项目
            </el-button>
          </div>
        </template>
      </template>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';

import { getInvitation, joinInvitation } from '@/services/memberPermissionApi';
import { useAuthStore } from '@/stores/auth';
import type { InvitationDetail } from '@/types/project';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(true);
const joining = ref(false);
const joined = ref(false);
const errorMessage = ref('');
const joinResultMessage = ref('');
const joinResultType = ref<'success' | 'info' | 'warning'>('success');
const invitation = ref<InvitationDetail | null>(null);

/**
 * 加载邀请详情。
 */
async function loadInvitation(): Promise<void> {
  const code = route.params.code as string;
  if (!code) {
    errorMessage.value = '邀请链接无效';
    loading.value = false;
    return;
  }

  loading.value = true;
  try {
    invitation.value = await getInvitation(code);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '邀请不存在或已过期';
  } finally {
    loading.value = false;
  }
}

/**
 * 加入邀请项目。
 */
async function handleJoin(): Promise<void> {
  if (!invitation.value) return;

  joining.value = true;
  joinResultMessage.value = '';

  try {
    const result = await joinInvitation({
      code: (route.params.code as string),
      userId: authStore.currentUser?.id,
    });

    if (result.status === 'joined') {
      joined.value = true;
      joinResultType.value = 'success';
      joinResultMessage.value = '已成功加入项目！';
      ElMessage.success('已加入项目');
    } else if (result.status === 'pending_review') {
      joinResultType.value = 'info';
      joinResultMessage.value = '加入申请已提交，等待管理员审核。';
      ElMessage.info('申请已提交，等待审核');
    }
  } catch (error) {
    joinResultType.value = 'warning';
    joinResultMessage.value = error instanceof Error ? error.message : '加入失败，请稍后重试';
  } finally {
    joining.value = false;
  }
}

function goLogin(): void {
  router.push({ path: '/login', query: { redirect: route.fullPath } });
}

function goRegister(): void {
  router.push({ path: '/register', query: { redirect: route.fullPath } });
}

function goToProject(): void {
  if (invitation.value) {
    router.push(`/projects/${invitation.value.projectId}`);
  } else {
    router.push('/');
  }
}

onMounted(loadInvitation);
</script>

<style scoped>
.join-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  color: #20242c;
  background:
    linear-gradient(135deg, rgba(29, 79, 145, 0.08), transparent 38%),
    #f6f7f9;
}

.join-page__panel {
  width: min(100%, 440px);
  padding: 30px;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 18px 42px rgba(32, 36, 44, 0.08);
}

.join-page__brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 24px;
}

.join-page__mark {
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

.join-page__brand h1 {
  margin: 0;
  font-size: 22px;
  line-height: 1.3;
}

.join-page__brand p {
  margin: 4px 0 0;
  color: #687386;
  line-height: 1.5;
}

.join-page__loading {
  padding: 16px 0;
}

.join-page__alert {
  margin-bottom: 16px;
}

.join-page__info {
  display: grid;
  gap: 12px;
  padding: 16px;
  margin-bottom: 20px;
  background: #f8f9fb;
  border-radius: 8px;
  border: 1px solid #e8ebf0;
}

.join-page__info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.join-page__info-label {
  font-size: 13px;
  color: #687386;
}

.join-page__info-value {
  font-size: 14px;
  font-weight: 500;
  color: #20242c;
}

.join-page__actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}
</style>
