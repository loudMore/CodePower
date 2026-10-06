-- Keep user profile regions on the same province-level vocabulary used by the UI selector.
UPDATE user_profiles
SET region = CASE
    WHEN TRIM(region) IN ('北京', '北京市') OR TRIM(region) LIKE '%北京市%' THEN '北京市'
    WHEN TRIM(region) IN ('天津', '天津市') OR TRIM(region) LIKE '%天津市%' THEN '天津市'
    WHEN TRIM(region) IN ('上海', '上海市') OR TRIM(region) LIKE '%上海市%' THEN '上海市'
    WHEN TRIM(region) IN ('重庆', '重庆市') OR TRIM(region) LIKE '%重庆市%' THEN '重庆市'

    WHEN TRIM(region) LIKE '%河北%' OR TRIM(region) LIKE '%石家庄%' OR TRIM(region) LIKE '%唐山%' OR TRIM(region) LIKE '%秦皇岛%' THEN '河北省'
    WHEN TRIM(region) LIKE '%山西%' OR TRIM(region) LIKE '%太原%' OR TRIM(region) LIKE '%大同%' THEN '山西省'
    WHEN TRIM(region) LIKE '%内蒙古%' OR TRIM(region) LIKE '%呼和浩特%' OR TRIM(region) LIKE '%包头%' THEN '内蒙古自治区'
    WHEN TRIM(region) LIKE '%辽宁%' OR TRIM(region) LIKE '%沈阳%' OR TRIM(region) LIKE '%大连%' THEN '辽宁省'
    WHEN TRIM(region) LIKE '%吉林省%' OR TRIM(region) = '吉林' OR TRIM(region) LIKE '%吉林市%' OR TRIM(region) LIKE '%长春%' THEN '吉林省'
    WHEN TRIM(region) LIKE '%黑龙江%' OR TRIM(region) LIKE '%哈尔滨%' THEN '黑龙江省'
    WHEN TRIM(region) LIKE '%江苏%' OR TRIM(region) LIKE '%南京%' OR TRIM(region) LIKE '%苏州%' OR TRIM(region) LIKE '%无锡%' THEN '江苏省'
    WHEN TRIM(region) LIKE '%浙江%' OR TRIM(region) LIKE '%杭州%' OR TRIM(region) LIKE '%宁波%' THEN '浙江省'
    WHEN TRIM(region) LIKE '%安徽%' OR TRIM(region) LIKE '%合肥%' OR TRIM(region) LIKE '%芜湖%' THEN '安徽省'
    WHEN TRIM(region) LIKE '%福建%' OR TRIM(region) LIKE '%福州%' OR TRIM(region) LIKE '%厦门%' THEN '福建省'
    WHEN TRIM(region) LIKE '%江西%' OR TRIM(region) LIKE '%南昌%' THEN '江西省'
    WHEN TRIM(region) LIKE '%山东%' OR TRIM(region) LIKE '%济南%' OR TRIM(region) LIKE '%青岛%' THEN '山东省'
    WHEN TRIM(region) LIKE '%河南%' OR TRIM(region) LIKE '%郑州%' OR TRIM(region) LIKE '%洛阳%' THEN '河南省'
    WHEN TRIM(region) LIKE '%湖北%' OR TRIM(region) LIKE '%武汉%' THEN '湖北省'
    WHEN TRIM(region) LIKE '%湖南%' OR TRIM(region) LIKE '%长沙%' THEN '湖南省'
    WHEN TRIM(region) LIKE '%广东%' OR TRIM(region) LIKE '%广州%' OR TRIM(region) LIKE '%深圳%' OR TRIM(region) LIKE '%东莞%' OR TRIM(region) LIKE '%佛山%' THEN '广东省'
    WHEN TRIM(region) LIKE '%广西%' OR TRIM(region) LIKE '%南宁%' OR TRIM(region) LIKE '%桂林%' THEN '广西壮族自治区'
    WHEN TRIM(region) LIKE '%海南%' OR TRIM(region) LIKE '%海口%' OR TRIM(region) LIKE '%三亚%' THEN '海南省'
    WHEN TRIM(region) LIKE '%四川%' OR TRIM(region) LIKE '%成都%' OR TRIM(region) LIKE '%绵阳%' THEN '四川省'
    WHEN TRIM(region) LIKE '%贵州%' OR TRIM(region) LIKE '%贵阳%' THEN '贵州省'
    WHEN TRIM(region) LIKE '%云南%' OR TRIM(region) LIKE '%昆明%' THEN '云南省'
    WHEN TRIM(region) LIKE '%西藏%' OR TRIM(region) LIKE '%拉萨%' THEN '西藏自治区'
    WHEN TRIM(region) LIKE '%陕西%' OR TRIM(region) LIKE '%西安%' THEN '陕西省'
    WHEN TRIM(region) LIKE '%甘肃%' OR TRIM(region) LIKE '%兰州%' THEN '甘肃省'
    WHEN TRIM(region) LIKE '%青海%' OR TRIM(region) LIKE '%西宁%' THEN '青海省'
    WHEN TRIM(region) LIKE '%宁夏%' OR TRIM(region) LIKE '%银川%' THEN '宁夏回族自治区'
    WHEN TRIM(region) LIKE '%新疆%' OR TRIM(region) LIKE '%乌鲁木齐%' THEN '新疆维吾尔自治区'
    WHEN TRIM(region) LIKE '%香港%' THEN '香港特别行政区'
    WHEN TRIM(region) LIKE '%澳门%' THEN '澳门特别行政区'
    WHEN TRIM(region) LIKE '%台湾%' OR TRIM(region) LIKE '%台北%' THEN '台湾省'
    ELSE region
END
WHERE region IS NOT NULL
  AND TRIM(region) <> '';
