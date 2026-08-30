/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,js}'],
  darkMode: ['selector', '[data-theme="dark"]'],
  corePlugins: {
    preflight: false
  },
  theme: {
    extend: {}
  },
  plugins: []
}
