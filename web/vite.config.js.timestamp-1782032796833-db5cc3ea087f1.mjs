// vite.config.js
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "file:///G:/%E7%AC%AC%E5%8D%81%E4%BA%94%E5%B1%8A%E8%BD%AF%E4%BB%B6%E6%9D%AF/%E5%88%9D%E5%A7%8B%E9%A1%B9%E7%9B%AE/template-v3-s/web/node_modules/vite/dist/node/index.js";
import vue from "file:///G:/%E7%AC%AC%E5%8D%81%E4%BA%94%E5%B1%8A%E8%BD%AF%E4%BB%B6%E6%9D%AF/%E5%88%9D%E5%A7%8B%E9%A1%B9%E7%9B%AE/template-v3-s/web/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import vueJsx from "file:///G:/%E7%AC%AC%E5%8D%81%E4%BA%94%E5%B1%8A%E8%BD%AF%E4%BB%B6%E6%9D%AF/%E5%88%9D%E5%A7%8B%E9%A1%B9%E7%9B%AE/template-v3-s/web/node_modules/@vitejs/plugin-vue-jsx/dist/index.mjs";
var __vite_injected_original_import_meta_url = "file:///G:/%E7%AC%AC%E5%8D%81%E4%BA%94%E5%B1%8A%E8%BD%AF%E4%BB%B6%E6%9D%AF/%E5%88%9D%E5%A7%8B%E9%A1%B9%E7%9B%AE/template-v3-s/web/vite.config.js";
var vite_config_default = defineConfig({
  plugins: [
    vue(),
    vueJsx()
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        // 解决 legacy-js-api 弃用警告（推荐方案）
        api: "modern"
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJHOlxcXFxcdTdCMkNcdTUzNDFcdTRFOTRcdTVDNEFcdThGNkZcdTRFRjZcdTY3NkZcXFxcXHU1MjFEXHU1OUNCXHU5ODc5XHU3NkVFXFxcXHRlbXBsYXRlLXYzLXNcXFxcd2ViXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCJHOlxcXFxcdTdCMkNcdTUzNDFcdTRFOTRcdTVDNEFcdThGNkZcdTRFRjZcdTY3NkZcXFxcXHU1MjFEXHU1OUNCXHU5ODc5XHU3NkVFXFxcXHRlbXBsYXRlLXYzLXNcXFxcd2ViXFxcXHZpdGUuY29uZmlnLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9HOi8lRTclQUMlQUMlRTUlOEQlODElRTQlQkElOTQlRTUlQjElOEElRTglQkQlQUYlRTQlQkIlQjYlRTYlOUQlQUYvJUU1JTg4JTlEJUU1JUE3JThCJUU5JUExJUI5JUU3JTlCJUFFL3RlbXBsYXRlLXYzLXMvd2ViL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZmlsZVVSTFRvUGF0aCwgVVJMIH0gZnJvbSAnbm9kZTp1cmwnXHJcblxyXG5pbXBvcnQgeyBkZWZpbmVDb25maWcgfSBmcm9tICd2aXRlJ1xyXG5pbXBvcnQgdnVlIGZyb20gJ0B2aXRlanMvcGx1Z2luLXZ1ZSdcclxuaW1wb3J0IHZ1ZUpzeCBmcm9tICdAdml0ZWpzL3BsdWdpbi12dWUtanN4J1xyXG5cclxuLy8gaHR0cHM6Ly92aXRlanMuZGV2L2NvbmZpZy9cclxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKHtcclxuICBwbHVnaW5zOiBbXHJcbiAgICB2dWUoKSxcclxuICAgIHZ1ZUpzeCgpLFxyXG4gIF0sXHJcbiAgcmVzb2x2ZToge1xyXG4gICAgYWxpYXM6IHtcclxuICAgICAgJ0AnOiBmaWxlVVJMVG9QYXRoKG5ldyBVUkwoJy4vc3JjJywgaW1wb3J0Lm1ldGEudXJsKSlcclxuICAgIH1cclxuICB9LFxyXG4gIGNzczoge1xyXG4gICAgcHJlcHJvY2Vzc29yT3B0aW9uczoge1xyXG4gICAgICBzY3NzOiB7XHJcbiAgICAgICAgLy8gXHU4OUUzXHU1MUIzIGxlZ2FjeS1qcy1hcGkgXHU1RjAzXHU3NTI4XHU4QjY2XHU1NDRBXHVGRjA4XHU2M0E4XHU4MzUwXHU2NUI5XHU2ODQ4XHVGRjA5XHJcbiAgICAgICAgYXBpOiAnbW9kZXJuJyxcclxuICAgICAgfVxyXG4gICAgfVxyXG4gIH1cclxufSkiXSwKICAibWFwcGluZ3MiOiAiO0FBQXVYLFNBQVMsZUFBZSxXQUFXO0FBRTFaLFNBQVMsb0JBQW9CO0FBQzdCLE9BQU8sU0FBUztBQUNoQixPQUFPLFlBQVk7QUFKZ0ssSUFBTSwyQ0FBMkM7QUFPcE8sSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUztBQUFBLElBQ1AsSUFBSTtBQUFBLElBQ0osT0FBTztBQUFBLEVBQ1Q7QUFBQSxFQUNBLFNBQVM7QUFBQSxJQUNQLE9BQU87QUFBQSxNQUNMLEtBQUssY0FBYyxJQUFJLElBQUksU0FBUyx3Q0FBZSxDQUFDO0FBQUEsSUFDdEQ7QUFBQSxFQUNGO0FBQUEsRUFDQSxLQUFLO0FBQUEsSUFDSCxxQkFBcUI7QUFBQSxNQUNuQixNQUFNO0FBQUE7QUFBQSxRQUVKLEtBQUs7QUFBQSxNQUNQO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFDRixDQUFDOyIsCiAgIm5hbWVzIjogW10KfQo=
