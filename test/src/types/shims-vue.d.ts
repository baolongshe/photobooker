declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

/*shims-vue.d.ts 的作用：
✅ 让 TypeScript 认识 .vue 文件 ✅ 允许在 .ts 文件中导入 .vue 组件 ✅ 提供基本的类型定义 ✅ 编译时必需，运行时不需要*/