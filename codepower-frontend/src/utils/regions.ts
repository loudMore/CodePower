/** 文件说明：地区工具与省份选项，负责用户资料地区名称归一化。 */
export type RegionOption = {
  label: string
  value: string
}

export const REGION_NAMES = [
  '北京市',
  '天津市',
  '河北省',
  '山西省',
  '内蒙古自治区',
  '辽宁省',
  '吉林省',
  '黑龙江省',
  '上海市',
  '江苏省',
  '浙江省',
  '安徽省',
  '福建省',
  '江西省',
  '山东省',
  '河南省',
  '湖北省',
  '湖南省',
  '广东省',
  '广西壮族自治区',
  '海南省',
  '重庆市',
  '四川省',
  '贵州省',
  '云南省',
  '西藏自治区',
  '陕西省',
  '甘肃省',
  '青海省',
  '宁夏回族自治区',
  '新疆维吾尔自治区',
  '香港特别行政区',
  '澳门特别行政区',
  '台湾省'
] as const

export const REGION_OPTIONS: RegionOption[] = REGION_NAMES.map(region => ({ label: region, value: region }))

const SHORT_NAME_MAP = new Map<string, string>(REGION_NAMES.map(region => [toShortRegionName(region), region]))

// 常见城市到省份的兜底映射，用户只填“兰州/成都”时也能归一到省级地区。
const CITY_TO_REGION: Record<string, string> = {
  北京: '北京市',
  天津: '天津市',
  上海: '上海市',
  重庆: '重庆市',
  石家庄: '河北省',
  唐山: '河北省',
  秦皇岛: '河北省',
  太原: '山西省',
  大同: '山西省',
  呼和浩特: '内蒙古自治区',
  包头: '内蒙古自治区',
  沈阳: '辽宁省',
  大连: '辽宁省',
  长春: '吉林省',
  吉林市: '吉林省',
  哈尔滨: '黑龙江省',
  南京: '江苏省',
  苏州: '江苏省',
  无锡: '江苏省',
  杭州: '浙江省',
  宁波: '浙江省',
  合肥: '安徽省',
  芜湖: '安徽省',
  福州: '福建省',
  厦门: '福建省',
  南昌: '江西省',
  济南: '山东省',
  青岛: '山东省',
  郑州: '河南省',
  洛阳: '河南省',
  武汉: '湖北省',
  长沙: '湖南省',
  广州: '广东省',
  深圳: '广东省',
  东莞: '广东省',
  佛山: '广东省',
  南宁: '广西壮族自治区',
  桂林: '广西壮族自治区',
  海口: '海南省',
  三亚: '海南省',
  成都: '四川省',
  绵阳: '四川省',
  贵阳: '贵州省',
  昆明: '云南省',
  拉萨: '西藏自治区',
  西安: '陕西省',
  兰州: '甘肃省',
  西宁: '青海省',
  银川: '宁夏回族自治区',
  乌鲁木齐: '新疆维吾尔自治区',
  香港: '香港特别行政区',
  澳门: '澳门特别行政区',
  台北: '台湾省',
  台湾: '台湾省'
}

/** 规范地区名称：把简称、城市名或带“中国”的写法归一成省级标准名称。 */
export function normalizeRegionLabel(name?: string | null) {
  const normalized = String(name || '')
    .trim()
    .replace(/\s+/g, '')
    .replace(/^中华人民共和国/, '')
    .replace(/^中国/, '')

  if (!normalized) return ''

  if ((REGION_NAMES as readonly string[]).includes(normalized)) return normalized

  const directShortName = SHORT_NAME_MAP.get(normalized)
  if (directShortName) return directShortName

  for (const region of REGION_NAMES) {
    const shortName = toShortRegionName(region)
    if (normalized.includes(region) || normalized.includes(shortName)) {
      return region
    }
  }

  for (const [city, region] of Object.entries(CITY_TO_REGION)) {
    if (normalized.includes(city)) return region
  }

  return ''
}

// 获取省份简称，用于“甘肃省 -> 甘肃”这类匹配。
function toShortRegionName(region: string) {
  if (['北京市', '天津市', '上海市', '重庆市'].includes(region)) return region.slice(0, 2)
  if (['香港特别行政区', '澳门特别行政区'].includes(region)) return region.slice(0, 2)
  if (region === '内蒙古自治区') return '内蒙古'
  if (region === '广西壮族自治区') return '广西'
  if (region === '西藏自治区') return '西藏'
  if (region === '宁夏回族自治区') return '宁夏'
  if (region === '新疆维吾尔自治区') return '新疆'
  return region.replace(/省|市/g, '')
}
