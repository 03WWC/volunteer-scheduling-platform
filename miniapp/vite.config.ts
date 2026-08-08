import { defineConfig } from 'vite'
import uniPluginModule from '@dcloudio/vite-plugin-uni'

type UniPlugin = typeof uniPluginModule
const uni = (
  typeof uniPluginModule === 'function'
    ? uniPluginModule
    : (uniPluginModule as unknown as { default: UniPlugin }).default
)

export default defineConfig({
  plugins: [uni()],
})
