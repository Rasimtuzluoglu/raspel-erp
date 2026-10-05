module.exports = {
  root: true,
  env: {
    node: true,
    browser: true,
    es2021: true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended'
  ],
  parserOptions: {
    ecmaVersion: 2021,
    sourceType: 'module'
  },
  globals: {
    // Vite define ile build-time gomulen surum sabiti.
    __APP_VERSION__: 'readonly'
  },
  rules: {
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',

    // REDTEAM/Faz3.3: PrimeVue 4'te `p-input-icon-left/right` KALDIRILDI ve
    // artik uretilmiyor. Bir CSS kurali olarak yazilsalar bile yerlestirme
    // kurali UYGULANMAZ -> ikon input'un uzerine biner, hic uyari cikmaz.
    // Dogrusu: <IconField><InputIcon class="pi pi-search" /><InputText/></IconField>
    // (Not: hazir `no-restricted-syntax` kurali `templateBody`'yi GECMEZ;
    //  bu yuzden `vue/no-restricted-static-attribute` kullanilir.)
    'vue/no-restricted-static-attribute': ['error',
      {
        key: 'class',
        value: '/(^|[\\s"\'`])p-input-icon-(left|right)([\\s"\'`]|$)/',
        message:
          'p-input-icon-left/right PrimeVue 4\'te KALDIRILDI ve artik uretilmiyor, ' +
          'bu yuzden yerlestirme kurali uygulanmaz (ikon yazinin uzerine biner). ' +
          'Dogrusu: <IconField><InputIcon class="pi pi-search" /><InputText/></IconField>'
      }
    ]
  }
}
