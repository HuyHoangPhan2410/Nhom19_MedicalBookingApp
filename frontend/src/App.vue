<template>
  <div id="app">
    <Navbar />
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import Navbar from './components/Navbar.vue';
</script>

<style>
.app-main {
  min-height: calc(100vh - 70px);
}
</style>
