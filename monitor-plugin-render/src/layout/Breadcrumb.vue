<template>
  <el-breadcrumb separator="/">
    <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
    <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script>
export default {
  name: 'Breadcrumb',
  computed: {
    currentTitle() {
      const meta = this.$route.meta
      return meta && meta.title ? meta.title : this.$route.name
    }
  }
}
</script>

<style lang="scss" scoped>
.el-breadcrumb {
  font-size: 14px;
  line-height: 56px;
}
</style>
