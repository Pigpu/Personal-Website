// 声明 .vue 文件模块，解决 "no default export" 报错
declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

// 告诉 TypeScript：凡是 .png 结尾的文件，导入后都是一个字符串（路径）
declare module "*.png" {
  const value: string;
  export default value;
}

// 顺便把其他格式也加上，省得以后报错
declare module "*.jpg";
declare module "*.jpeg";
declare module "*.svg";
declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}