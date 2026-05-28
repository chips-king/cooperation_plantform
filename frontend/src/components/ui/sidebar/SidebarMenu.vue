<template>
  <ul data-sidebar="menu" class="sidebar-menu">
    <slot />
  </ul>
</template>

<style>
.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 2px;
  list-style: none;
  margin: 0;
  padding: 0;
}
</style>
