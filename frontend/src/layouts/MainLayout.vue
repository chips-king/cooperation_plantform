<template>
  <el-container class="main-layout">
    <el-aside class="main-layout__aside" width="240px">
      <section class="main-layout__brand">
        <span class="main-layout__mark">协</span>
        <div>
          <strong>分工协作系统</strong>
          <small>项目成果工作台</small>
        </div>
      </section>

      <nav class="main-layout__nav" aria-label="主导航">
        <RouterLink class="main-layout__nav-item" to="/">
          <FolderOpened class="main-layout__nav-icon" />
          <span>项目首页</span>
        </RouterLink>
        <RouterLink class="main-layout__nav-item" to="/notifications">
          <Bell class="main-layout__nav-icon" />
          <span>通知中心</span>
        </RouterLink>
      </nav>

      <section class="main-layout__side-slot">
        <slot name="aside" />
      </section>
    </el-aside>

    <el-container>
      <el-header class="main-layout__header">
        <div class="main-layout__title">
          <slot name="title">
            <span>工作台</span>
          </slot>
        </div>

        <div class="main-layout__actions">
          <slot name="actions" />
          <RouterLink class="main-layout__notice" to="/notifications" aria-label="打开通知中心">
            <Bell class="main-layout__notice-icon" />
          </RouterLink>
        </div>
      </el-header>

      <el-main class="main-layout__content">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { Bell, FolderOpened } from '@element-plus/icons-vue';

// 主布局只负责框架结构和插槽承载，不直接发起业务请求。
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  color: #20242c;
  background: #f6f7f9;
}

.main-layout__aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #d9dee7;
  background: #ffffff;
}

.main-layout__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 0 18px;
  border-bottom: 1px solid #e4e7ed;
}

.main-layout__mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #ffffff;
  background: #1d4f91;
  font-weight: 700;
}

.main-layout__brand strong,
.main-layout__brand small {
  display: block;
  line-height: 1.35;
}

.main-layout__brand small {
  margin-top: 2px;
  color: #687386;
  font-size: 12px;
}

.main-layout__nav {
  display: grid;
  gap: 4px;
  padding: 14px 10px;
}

.main-layout__nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  color: #3b4351;
  text-decoration: none;
}

.main-layout__nav-item.router-link-active,
.main-layout__nav-item:hover {
  color: #173b70;
  background: #eef3f8;
}

.main-layout__nav-icon {
  width: 18px;
  height: 18px;
}

.main-layout__side-slot {
  flex: 1;
  padding: 10px 18px 18px;
  overflow: auto;
}

.main-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 28px;
  border-bottom: 1px solid #d9dee7;
  background: #ffffff;
}

.main-layout__title {
  min-width: 0;
  color: #20242c;
  font-size: 18px;
  font-weight: 700;
}

.main-layout__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.main-layout__notice {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  color: #3b4351;
  background: #ffffff;
}

.main-layout__notice:hover {
  color: #173b70;
  border-color: #9ab2d3;
  background: #f3f7fb;
}

.main-layout__notice-icon {
  width: 18px;
  height: 18px;
}

.main-layout__content {
  min-width: 0;
  padding: 28px;
  overflow: auto;
}

@media (max-width: 768px) {
  .main-layout {
    display: block;
  }

  .main-layout__aside {
    width: 100% !important;
    border-right: 0;
    border-bottom: 1px solid #d9dee7;
  }

  .main-layout__nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .main-layout__side-slot {
    display: none;
  }

  .main-layout__header {
    height: auto;
    min-height: 64px;
    padding: 14px 18px;
  }

  .main-layout__content {
    padding: 18px;
  }
}
</style>
