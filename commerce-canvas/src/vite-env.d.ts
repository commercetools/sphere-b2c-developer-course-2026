/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_STOREFRONT_URL?: string;
  readonly VITE_BFF_ORIGIN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
