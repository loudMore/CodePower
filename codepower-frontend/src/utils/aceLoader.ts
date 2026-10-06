/** Ace Editor 语言模式与主题的懒加载器 */
const loadedAceModes = new Set<string>()
const loadedAceThemes = new Set<string>()
let aceCorePromise: Promise<any> | null = null
let aceExtPromise: Promise<void> | null = null

const aceModeLoaders: Record<string, () => Promise<unknown>> = {
  c_cpp: () => import('ace-builds/src-noconflict/mode-c_cpp'),
  java: () => import('ace-builds/src-noconflict/mode-java'),
  python: () => import('ace-builds/src-noconflict/mode-python'),
  javascript: () => import('ace-builds/src-noconflict/mode-javascript'),
  typescript: () => import('ace-builds/src-noconflict/mode-typescript'),
  assembly_x86: () => import('ace-builds/src-noconflict/mode-assembly_x86'),
  sh: () => import('ace-builds/src-noconflict/mode-sh'),
  csharp: () => import('ace-builds/src-noconflict/mode-csharp'),
  golang: () => import('ace-builds/src-noconflict/mode-golang'),
  rust: () => import('ace-builds/src-noconflict/mode-rust')
}

const aceThemeLoaders: Record<string, () => Promise<unknown>> = {
  'ace/theme/monokai': () => import('ace-builds/src-noconflict/theme-monokai'),
  'ace/theme/tomorrow_night': () => import('ace-builds/src-noconflict/theme-tomorrow_night'),
  'ace/theme/textmate': () => import('ace-builds/src-noconflict/theme-textmate'),
  'ace/theme/dracula': () => import('ace-builds/src-noconflict/theme-dracula'),
  'ace/theme/one_dark': () => import('ace-builds/src-noconflict/theme-one_dark'),
  'ace/theme/nord_dark': () => import('ace-builds/src-noconflict/theme-nord_dark'),
  'ace/theme/cobalt': () => import('ace-builds/src-noconflict/theme-cobalt'),
  'ace/theme/terminal': () => import('ace-builds/src-noconflict/theme-terminal'),
  'ace/theme/twilight': () => import('ace-builds/src-noconflict/theme-twilight'),
  'ace/theme/github': () => import('ace-builds/src-noconflict/theme-github'),
  'ace/theme/chrome': () => import('ace-builds/src-noconflict/theme-chrome'),
  'ace/theme/eclipse': () => import('ace-builds/src-noconflict/theme-eclipse'),
  'ace/theme/xcode': () => import('ace-builds/src-noconflict/theme-xcode'),
  'ace/theme/solarized_light': () => import('ace-builds/src-noconflict/theme-solarized_light'),
  'ace/theme/clouds': () => import('ace-builds/src-noconflict/theme-clouds')
}

/** 加载 Ace Editor 核心包，多个页面调用时共用同一个 Promise，避免重复下载。 */
export async function loadAceCore() {
  if (!aceCorePromise) {
    aceCorePromise = import('ace-builds')
  }
  const ace = await aceCorePromise
  return ace.default || ace
}

/** 加载 Ace 的语言工具扩展，提供基础补全和代码片段能力。 */
export async function loadAceExt() {
  if (!aceExtPromise) {
    aceExtPromise = import('ace-builds/src-noconflict/ext-language_tools').then(() => undefined)
  }
  await aceExtPromise
}

/** 按需加载指定语言模式，例如 java、python、c_cpp。 */
export async function loadAceMode(mode: string) {
  if (!mode || loadedAceModes.has(mode)) return
  const loader = aceModeLoaders[mode]
  if (!loader) return
  await loader()
  loadedAceModes.add(mode)
}

/** 按需加载指定主题，例如 xcode、monokai、dracula。 */
export async function loadAceTheme(theme: string) {
  if (!theme || loadedAceThemes.has(theme)) return
  const loader = aceThemeLoaders[theme]
  if (!loader) return
  await loader()
  loadedAceThemes.add(theme)
}
