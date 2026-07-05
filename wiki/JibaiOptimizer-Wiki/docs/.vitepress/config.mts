import { defineConfig } from 'vitepress'

export default defineConfig({
  title: 'JibaiOptimizer Wiki',
  description: '即白开发的 Minecraft 服务器优化与性能监控插件文档',
  lang: 'zh-CN',
  cleanUrls: false,
  themeConfig: {
    logo: '/logo.svg',
    nav: [
      { text: '首页', link: '/' },
      { text: '安装', link: '/guide/install' },
      { text: '指令', link: '/guide/commands' },
      { text: '配置', link: '/guide/config' },
      { text: 'FAQ', link: '/advanced/faq' }
    ],
    sidebar: [
      {
        text: '入门指南',
        items: [
          { text: '项目介绍', link: '/' },
          { text: '安装教程', link: '/guide/install' },
          { text: '指令说明', link: '/guide/commands' },
          { text: '权限说明', link: '/guide/permissions' },
          { text: '配置说明', link: '/guide/config' }
        ]
      },
      {
        text: '功能说明',
        items: [
          { text: '功能总览', link: '/features/overview' },
          { text: '性能监控', link: '/features/monitor' },
          { text: '自动优化', link: '/features/auto-optimizer' },
          { text: '掉落物与经验球', link: '/features/item-clean' },
          { text: '红石高频检测', link: '/features/redstone' },
          { text: '漏斗密集检测', link: '/features/hopper' },
          { text: '刷怪限制', link: '/features/spawn-limit' }
        ]
      },
      {
        text: '高级说明',
        items: [
          { text: '兼容性', link: '/advanced/compatibility' },
          { text: '性能风险说明', link: '/advanced/performance-risk' },
          { text: '常见问题', link: '/advanced/faq' },
          { text: '更新日志', link: '/changelog' }
        ]
      }
    ],
    search: {
      provider: 'local'
    },
    footer: {
      message: 'Made by 即白 · jibai0517@gamil.com',
      copyright: 'Copyright © 2026 即白'
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/A2225211273/Jibai-Minecraft-Plugins' }
    ]
  },
  head: [
    ['meta', { name: 'theme-color', content: '#16a34a' }]
  ]
})
