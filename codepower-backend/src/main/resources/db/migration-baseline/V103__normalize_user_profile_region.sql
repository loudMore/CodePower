-- Normalize legacy free-text regions to province-level names for map statistics.
UPDATE user_profiles
SET region = CASE TRIM(region)
    WHEN '北京' THEN '北京市'
    WHEN '北京市' THEN '北京市'
    WHEN '上海' THEN '上海市'
    WHEN '上海市' THEN '上海市'
    WHEN '天津' THEN '天津市'
    WHEN '天津市' THEN '天津市'
    WHEN '重庆' THEN '重庆市'
    WHEN '重庆市' THEN '重庆市'
    WHEN '南京' THEN '江苏省'
    WHEN '苏州' THEN '江苏省'
    WHEN '无锡' THEN '江苏省'
    WHEN '广州' THEN '广东省'
    WHEN '深圳' THEN '广东省'
    WHEN '东莞' THEN '广东省'
    WHEN '佛山' THEN '广东省'
    WHEN '成都' THEN '四川省'
    WHEN '杭州' THEN '浙江省'
    WHEN '宁波' THEN '浙江省'
    WHEN '武汉' THEN '湖北省'
    WHEN '西安' THEN '陕西省'
    WHEN '长沙' THEN '湖南省'
    WHEN '郑州' THEN '河南省'
    WHEN '济南' THEN '山东省'
    WHEN '青岛' THEN '山东省'
    WHEN '福州' THEN '福建省'
    WHEN '厦门' THEN '福建省'
    WHEN '南昌' THEN '江西省'
    WHEN '合肥' THEN '安徽省'
    WHEN '太原' THEN '山西省'
    WHEN '石家庄' THEN '河北省'
    WHEN '沈阳' THEN '辽宁省'
    WHEN '大连' THEN '辽宁省'
    WHEN '长春' THEN '吉林省'
    WHEN '哈尔滨' THEN '黑龙江省'
    WHEN '南宁' THEN '广西壮族自治区'
    WHEN '海口' THEN '海南省'
    WHEN '贵阳' THEN '贵州省'
    WHEN '昆明' THEN '云南省'
    WHEN '拉萨' THEN '西藏自治区'
    WHEN '兰州' THEN '甘肃省'
    WHEN '西宁' THEN '青海省'
    WHEN '银川' THEN '宁夏回族自治区'
    WHEN '乌鲁木齐' THEN '新疆维吾尔自治区'
    WHEN '呼和浩特' THEN '内蒙古自治区'
    WHEN '香港' THEN '香港特别行政区'
    WHEN '澳门' THEN '澳门特别行政区'
    WHEN '台北' THEN '台湾省'
    WHEN '台湾' THEN '台湾省'
    ELSE region
END
WHERE region IS NOT NULL
  AND TRIM(region) IN (
      '北京', '北京市', '上海', '上海市', '天津', '天津市', '重庆', '重庆市',
      '南京', '苏州', '无锡', '广州', '深圳', '东莞', '佛山', '成都', '杭州', '宁波',
      '武汉', '西安', '长沙', '郑州', '济南', '青岛', '福州', '厦门', '南昌', '合肥',
      '太原', '石家庄', '沈阳', '大连', '长春', '哈尔滨', '南宁', '海口', '贵阳',
      '昆明', '拉萨', '兰州', '西宁', '银川', '乌鲁木齐', '呼和浩特', '香港', '澳门',
      '台北', '台湾'
  );
