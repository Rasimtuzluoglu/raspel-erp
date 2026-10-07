/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{vue,js}'],
  darkMode: ['selector', '[data-theme="dark"]'],
  corePlugins: {
    preflight: false
  },
  theme: {
    extend: {
      // Faz 4.4: kurumsal tasarım token'ları. PrimeVue primary paleti ve
      // app.css `--accent` ile AYNI: teal (#14b8a6).
      colors: {
        brand: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          200: '#99f6e4',
          300: '#5eead4',
          400: '#2dd4bf',
          500: '#14b8a6',
          600: '#0d9488',
          700: '#0f766e',
          800: '#115e59',
          900: '#134e4a',
          950: '#042f2e'
        }
      },
      borderRadius: {
        brand: '0.625rem',
        'brand-lg': '0.875rem'
      }
    }
  },
  plugins: []
}
