<!-- 整個網站的外框：導覽列＋目前頁面＋Toast -->
<script setup>
import AppNavbar from '@/components/AppNavbar.vue'
import AppToast from '@/components/AppToast.vue'
</script>

<template>
  <AppNavbar />
  <main>
    <!-- 依網址顯示對應的頁面（router/index.js 決定） -->
    <RouterView />
  </main>
  <AppToast />
</template>
