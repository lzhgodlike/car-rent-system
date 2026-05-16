<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const cars = ref([])
const carTypes = ref([])
const brands = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentId = ref(null)
const formRef = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const summary = ref({})
const query = ref({ brand: '', typeId: '', status: '', sort: '', keyword: '' })
const detailVisible = ref(false)
const detailRow = ref(null)
const detailZoomVisible = ref(false)
const detailActiveImageIndex = ref(0)
const uploadLoading = ref(false)
const importLoading = ref(false)
const imageUrlInput = ref('')

const provinceCityOptions = [
  { name: '北京市', cities: ['北京市'] },
  { name: '天津市', cities: ['天津市'] },
  { name: '上海市', cities: ['上海市'] },
  { name: '重庆市', cities: ['重庆市'] },
  { name: '河北省', cities: ['石家庄市', '唐山市', '秦皇岛市', '邯郸市', '邢台市', '保定市', '张家口市', '承德市', '沧州市', '廊坊市', '衡水市'] },
  { name: '山西省', cities: ['太原市', '大同市', '阳泉市', '长治市', '晋城市', '朔州市', '晋中市', '运城市', '忻州市', '临汾市', '吕梁市'] },
  { name: '辽宁省', cities: ['沈阳市', '大连市', '鞍山市', '抚顺市', '本溪市', '丹东市', '锦州市', '营口市', '阜新市', '辽阳市', '盘锦市', '铁岭市', '朝阳市', '葫芦岛市'] },
  { name: '吉林省', cities: ['长春市', '吉林市', '四平市', '辽源市', '通化市', '白山市', '松原市', '白城市', '延边朝鲜族自治州'] },
  { name: '黑龙江省', cities: ['哈尔滨市', '齐齐哈尔市', '鸡西市', '鹤岗市', '双鸭山市', '大庆市', '伊春市', '佳木斯市', '七台河市', '牡丹江市', '黑河市', '绥化市', '大兴安岭地区'] },
  { name: '江苏省', cities: ['南京市', '无锡市', '徐州市', '常州市', '苏州市', '南通市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市'] },
  { name: '浙江省', cities: ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市'] },
  { name: '安徽省', cities: ['合肥市', '芜湖市', '蚌埠市', '淮南市', '马鞍山市', '淮北市', '铜陵市', '安庆市', '黄山市', '滁州市', '阜阳市', '宿州市', '六安市', '亳州市', '池州市', '宣城市'] },
  { name: '福建省', cities: ['福州市', '厦门市', '莆田市', '三明市', '泉州市', '漳州市', '南平市', '龙岩市', '宁德市'] },
  { name: '江西省', cities: ['南昌市', '景德镇市', '萍乡市', '九江市', '新余市', '鹰潭市', '赣州市', '吉安市', '宜春市', '抚州市', '上饶市'] },
  { name: '山东省', cities: ['济南市', '青岛市', '淄博市', '枣庄市', '东营市', '烟台市', '潍坊市', '济宁市', '泰安市', '威海市', '日照市', '临沂市', '德州市', '聊城市', '滨州市', '菏泽市'] },
  { name: '河南省', cities: ['郑州市', '开封市', '洛阳市', '平顶山市', '安阳市', '鹤壁市', '新乡市', '焦作市', '濮阳市', '许昌市', '漯河市', '三门峡市', '南阳市', '商丘市', '信阳市', '周口市', '驻马店市', '济源市'] },
  { name: '湖北省', cities: ['武汉市', '黄石市', '十堰市', '宜昌市', '襄阳市', '鄂州市', '荆门市', '孝感市', '荆州市', '黄冈市', '咸宁市', '随州市', '恩施土家族苗族自治州', '仙桃市', '潜江市', '天门市', '神农架林区'] },
  { name: '湖南省', cities: ['长沙市', '株洲市', '湘潭市', '衡阳市', '邵阳市', '岳阳市', '常德市', '张家界市', '益阳市', '郴州市', '永州市', '怀化市', '娄底市', '湘西土家族苗族自治州'] },
  { name: '广东省', cities: ['广州市', '韶关市', '深圳市', '珠海市', '汕头市', '佛山市', '江门市', '湛江市', '茂名市', '肇庆市', '惠州市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '东莞市', '中山市', '潮州市', '揭阳市', '云浮市'] },
  { name: '广西壮族自治区', cities: ['南宁市', '柳州市', '桂林市', '梧州市', '北海市', '防城港市', '钦州市', '贵港市', '玉林市', '百色市', '贺州市', '河池市', '来宾市', '崇左市'] },
  { name: '海南省', cities: ['海口市', '三亚市', '三沙市', '儋州市', '五指山市', '琼海市', '文昌市', '万宁市', '东方市', '定安县', '屯昌县', '澄迈县', '临高县', '白沙黎族自治县', '昌江黎族自治县', '乐东黎族自治县', '陵水黎族自治县', '保亭黎族苗族自治县', '琼中黎族苗族自治县'] },
  { name: '四川省', cities: ['成都市', '自贡市', '攀枝花市', '泸州市', '德阳市', '绵阳市', '广元市', '遂宁市', '内江市', '乐山市', '南充市', '眉山市', '宜宾市', '广安市', '达州市', '雅安市', '巴中市', '资阳市', '阿坝藏族羌族自治州', '甘孜藏族自治州', '凉山彝族自治州'] },
  { name: '贵州省', cities: ['贵阳市', '六盘水市', '遵义市', '安顺市', '毕节市', '铜仁市', '黔西南布依族苗族自治州', '黔东南苗族侗族自治州', '黔南布依族苗族自治州'] },
  { name: '云南省', cities: ['昆明市', '曲靖市', '玉溪市', '保山市', '昭通市', '丽江市', '普洱市', '临沧市', '楚雄彝族自治州', '红河哈尼族彝族自治州', '文山壮族苗族自治州', '西双版纳傣族自治州', '大理白族自治州', '德宏傣族景颇族自治州', '怒江傈僳族自治州', '迪庆藏族自治州'] },
  { name: '西藏自治区', cities: ['拉萨市', '日喀则市', '昌都市', '林芝市', '山南市', '那曲市', '阿里地区'] },
  { name: '陕西省', cities: ['西安市', '铜川市', '宝鸡市', '咸阳市', '渭南市', '延安市', '汉中市', '榆林市', '安康市', '商洛市'] },
  { name: '甘肃省', cities: ['兰州市', '嘉峪关市', '金昌市', '白银市', '天水市', '武威市', '张掖市', '平凉市', '酒泉市', '庆阳市', '定西市', '陇南市', '临夏回族自治州', '甘南藏族自治州'] },
  { name: '青海省', cities: ['西宁市', '海东市', '海北藏族自治州', '黄南藏族自治州', '海南藏族自治州', '果洛藏族自治州', '玉树藏族自治州', '海西蒙古族藏族自治州'] },
  { name: '宁夏回族自治区', cities: ['银川市', '石嘴山市', '吴忠市', '固原市', '中卫市'] },
  { name: '新疆维吾尔自治区', cities: ['乌鲁木齐市', '克拉玛依市', '吐鲁番市', '哈密市', '昌吉回族自治州', '博尔塔拉蒙古自治州', '巴音郭楞蒙古自治州', '阿克苏地区', '克孜勒苏柯尔克孜自治州', '喀什地区', '和田地区', '伊犁哈萨克自治州', '塔城地区', '阿勒泰地区', '石河子市', '阿拉尔市', '图木舒克市', '五家渠市', '北屯市', '铁门关市', '双河市', '可克达拉市', '昆玉市', '胡杨河市', '新星市'] },
  { name: '内蒙古自治区', cities: ['呼和浩特市', '包头市', '乌海市', '赤峰市', '通辽市', '鄂尔多斯市', '呼伦贝尔市', '巴彦淖尔市', '乌兰察布市', '兴安盟', '锡林郭勒盟', '阿拉善盟'] },
  { name: '香港特别行政区', cities: ['香港岛', '九龙', '新界'] },
  { name: '澳门特别行政区', cities: ['澳门半岛', '氹仔', '路环'] },
  { name: '台湾省', cities: ['台北市', '新北市', '桃园市', '台中市', '台南市', '高雄市', '基隆市', '新竹市', '嘉义市', '新竹县', '苗栗县', '彰化县', '南投县', '云林县', '嘉义县', '屏东县', '宜兰县', '花莲县', '台东县', '澎湖县', '金门县', '连江县'] }
]

const createEmptyForm = () => ({
  carNo: '',
  typeId: '',
  brand: '',
  model: '',
  plateNumber: '',
  dayPrice: 0,
  mileage: 0,
  province: '',
  city: '',
  detailAddress: '',
  images: [],
})

const form = ref(createEmptyForm())

const provinceOptions = provinceCityOptions.map(item => item.name)
const cityOptions = computed(() => provinceCityOptions.find(item => item.name === form.value.province)?.cities || [])

const statusMap = { AVAILABLE: '空闲', RESERVED: '已预订', RENTED: '租赁中', AWAITING_REPAIR: '待维修', REPAIRING: '维修中', DISABLED: '停用' }
const statusClass = { AVAILABLE: 'status-available', RESERVED: 'status-reserved', RENTED: 'status-rented', AWAITING_REPAIR: 'status-awating-repair', REPAIRING: 'status-repairing', DISABLED: 'status-disabled' }

const loadData = async () => {
  loading.value = true
  try {
    const page = await request.get('/cars', { params: { ...query.value, pageNum: currentPage.value, pageSize: pageSize.value } })
    cars.value = page.records
    total.value = page.total
    summary.value = page.summary || {}
  } finally { loading.value = false }
}
const loadTypes = async () => { carTypes.value = await request.get('/car-types') }
const loadBrands = async () => { brands.value = await request.get('/cars/brands') }

watch(query, () => { currentPage.value = 1; loadData() }, { deep: true })
watch(() => form.value.province, (province) => {
  if (!province) {
    form.value.city = ''
    return
  }
  if (!cityOptions.value.includes(form.value.city)) {
    form.value.city = cityOptions.value[0] || ''
  }
})

onMounted(async () => { await Promise.all([loadTypes(), loadBrands()]); loadData() })

const onSearch = () => { currentPage.value = 1; loadData() }
const onClear = () => { query.value = { brand: '', typeId: '', status: '', sort: '', keyword: '' }; currentPage.value = 1; loadData() }

const normalizeImages = (images = [], fallbackImage = '') => {
  const normalized = Array.isArray(images) && images.length
    ? images.map((item, index) => ({
        id: item.id || null,
        imageUrl: item.imageUrl || item.url || '',
        sortOrder: Number.isFinite(item.sortOrder) ? item.sortOrder : index,
      })).filter(item => item.imageUrl)
    : []
  if (!normalized.length && fallbackImage) {
    normalized.push({ id: null, imageUrl: fallbackImage, sortOrder: 0 })
  }
  return normalized.map((item, index) => ({ ...item, sortOrder: index }))
}

const openAdd = () => {
  currentId.value = null
  imageUrlInput.value = ''
  form.value = createEmptyForm()
  dialogVisible.value = true
}

const openEdit = async (row) => {
  currentId.value = row.id
  imageUrlInput.value = ''
  const detail = await request.get(`/cars/${row.id}`)
  const { rentCount, totalIncome, currentRenterName, pickupAddress, carImage, carImages, ...rest } = detail
  form.value = {
    ...createEmptyForm(),
    ...rest,
    province: detail.province || '',
    city: detail.city || '',
    detailAddress: detail.detailAddress || '',
    images: normalizeImages(carImages, carImage),
  }
  dialogVisible.value = true
}

const openDetail = async (row) => {
  detailRow.value = await request.get(`/cars/${row.id}`)
  detailActiveImageIndex.value = 0
  detailVisible.value = true
}

const validateImages = () => {
  if (!form.value.images.length) {
    ElMessage.warning('请至少添加一张车辆图片')
    return false
  }
  return true
}

const saveCar = async () => {
  await formRef.value?.validate()
  if (!validateImages()) return
  const payload = {
    typeId: form.value.typeId,
    brand: form.value.brand,
    model: form.value.model,
    plateNumber: form.value.plateNumber,
    dayPrice: form.value.dayPrice,
    mileage: form.value.mileage,
    province: form.value.province,
    city: form.value.city,
    detailAddress: form.value.detailAddress,
    images: form.value.images.map((item, index) => ({ imageUrl: item.imageUrl, sortOrder: index })),
  }
  if (currentId.value) {
    await request.put(`/cars/${currentId.value}`, payload)
    ElMessage.success('修改成功')
  } else {
    await request.post('/cars', payload)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  loadData()
}

const removeCar = async (id) => {
  await ElMessageBox.confirm('确定要删除这辆车吗？', '删除确认', { type: 'warning' })
  await request.delete(`/cars/${id}`)
  ElMessage.success('删除成功')
  loadData()
}
const disableCar = async (id) => { await request.put(`/cars/${id}/disable`); ElMessage.success('已停用'); loadData() }
const enableCar = async (id) => { await request.put(`/cars/${id}/enable`); ElMessage.success('已启用'); loadData() }
const carTypeName = (typeId) => carTypes.value.find(t => t.id === typeId)?.typeName || '-'

const onUploadFile = async (event) => {
  const [file] = event.target.files || []
  event.target.value = ''
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  uploadLoading.value = true
  try {
    const result = await request.post('/admin/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      params: { carNo: form.value.carNo || '' }
    })
    form.value.images.push({ id: null, imageUrl: result.url, sortOrder: form.value.images.length })
    ElMessage.success('图片上传成功')
  } finally {
    uploadLoading.value = false
  }
}

const importImageByUrl = async () => {
  if (!imageUrlInput.value.trim()) {
    ElMessage.warning('请输入图片链接')
    return
  }
  importLoading.value = true
  try {
    const result = await request.post('/admin/media/import-by-url', {
      url: imageUrlInput.value.trim(),
      carNo: form.value.carNo || '',
    })
    form.value.images.push({ id: null, imageUrl: result.url, sortOrder: form.value.images.length })
    imageUrlInput.value = ''
    ElMessage.success('图片导入成功')
  } finally {
    importLoading.value = false
  }
}

const moveImage = (index, direction) => {
  const targetIndex = index + direction
  if (targetIndex < 0 || targetIndex >= form.value.images.length) return
  const list = [...form.value.images]
  ;[list[index], list[targetIndex]] = [list[targetIndex], list[index]]
  form.value.images = list.map((item, idx) => ({ ...item, sortOrder: idx }))
}

const removeImage = async (index) => {
  const target = form.value.images[index]
  if (!target) return
  if (!target.id) {
    await request.delete('/admin/media', { params: { url: target.imageUrl } })
  }
  form.value.images.splice(index, 1)
  form.value.images = form.value.images.map((item, idx) => ({ ...item, sortOrder: idx }))
}

const fullAddress = (row) => row.pickupAddress || [row.province, row.city, row.detailAddress].filter(Boolean).join('') || '-'
const detailImages = computed(() => {
  const images = detailRow.value?.carImages || []
  return images.length ? images : (detailRow.value?.carImage ? [{ imageUrl: detailRow.value.carImage }] : [])
})
const activeDetailImage = computed(() => {
  const current = detailImages.value[detailActiveImageIndex.value]
  return current?.imageUrl || current || ''
})
const hasMultipleDetailImages = computed(() => detailImages.value.length > 1)
const selectDetailImage = (index) => {
  detailActiveImageIndex.value = index
}
const prevDetailImage = () => {
  if (!hasMultipleDetailImages.value) return
  detailActiveImageIndex.value = (detailActiveImageIndex.value - 1 + detailImages.value.length) % detailImages.value.length
}
const nextDetailImage = () => {
  if (!hasMultipleDetailImages.value) return
  detailActiveImageIndex.value = (detailActiveImageIndex.value + 1) % detailImages.value.length
}
const openDetailZoom = () => {
  if (activeDetailImage.value) detailZoomVisible.value = true
}
</script>

<template>
  <div>
    <div class="toolbar">
      <div class="summary-strip">
        <span class="sum-item">全部 <strong>{{ summary.totalCount ?? 0 }}</strong></span>
        <span class="sum-item">空闲 <strong>{{ summary.available ?? 0 }}</strong></span>
        <span class="sum-item">已预订 <strong>{{ summary.reserved ?? 0 }}</strong></span>
        <span class="sum-item">租赁中 <strong>{{ summary.rented ?? 0 }}</strong></span>
        <span class="sum-item">待维修 <strong>{{ summary.awaitingRepair ?? 0 }}</strong></span>
        <span class="sum-item">维修中 <strong>{{ summary.repairing ?? 0 }}</strong></span>
        <span class="sum-item">停用 <strong>{{ summary.disabled ?? 0 }}</strong></span>
      </div>
      <div style="display:flex;gap:8px;align-items:center;">
        <el-input v-model="query.keyword" placeholder="搜索品牌、型号、车牌号…" clearable size="small" style="width:200px;" @keyup.enter="onSearch" @clear="onClear" />
        <el-select v-model="query.brand" placeholder="品牌" clearable size="small" style="width:120px;">
          <el-option v-for="b in brands" :key="b" :label="b" :value="b" />
        </el-select>
        <el-select v-model="query.typeId" placeholder="类型" clearable size="small" style="width:100px;">
          <el-option v-for="t in carTypes" :key="t.id" :label="t.typeName" :value="t.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable size="small" style="width:100px;">
          <el-option v-for="(label, key) in statusMap" :key="key" :label="label" :value="key" />
        </el-select>
        <button class="btn-sm btn-sm-ghost" @click="onClear"><el-icon><RefreshLeft /></el-icon></button>
        <button class="btn-sm btn-sm-primary" @click="openAdd"><el-icon><Plus /></el-icon> 添加车辆</button>
      </div>
    </div>

    <div class="card">
      <el-table :data="cars" v-loading="loading">
        <el-table-column label="车辆信息" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px;">
              <div class="car-thumb">
                <img v-if="row.carImage" :src="row.carImage" :alt="`${row.brand} ${row.model}`" @error="(e) => e.target.style.display='none'" />
                <el-icon v-else size="20"><Van /></el-icon>
              </div>
              <div>
                <div style="font-weight:500;">{{ row.brand }} {{ row.model }}</div>
                <div style="font-size:11px;color:var(--muted);">{{ row.carNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="plateNumber" label="车牌号" width="120">
          <template #default="{ row }"><span class="font-mono">{{ row.plateNumber }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="90"><template #default="{ row }">{{ carTypeName(row.typeId) }}</template></el-table-column>
        <el-table-column label="取车城市" width="120"><template #default="{ row }">{{ row.city || '-' }}</template></el-table-column>
        <el-table-column label="日租金" width="90"><template #default="{ row }"><span class="font-mono text-accent">¥ {{ row.dayPrice }}/天</span></template></el-table-column>
        <el-table-column label="里程" width="90"><template #default="{ row }"><span class="font-mono">{{ row.mileage }}km</span></template></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><span class="status-badge" :class="statusClass[row.status]">{{ statusMap[row.status] || row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <div style="display:flex;gap:4px;">
              <button class="btn-sm btn-sm-ghost btn-icon" title="查看详情" @click="openDetail(row)"><el-icon><View /></el-icon></button>
              <button class="btn-sm btn-sm-ghost btn-icon" title="编辑" @click="openEdit(row)"><el-icon><Edit /></el-icon></button>
              <button
                :class="['btn-sm', 'btn-icon', row.status === 'DISABLED' ? 'btn-sm-success' : 'btn-sm-warning']"
                :disabled="['RENTED', 'RESERVED', 'AWAITING_REPAIR', 'REPAIRING'].includes(row.status)"
                :title="row.status === 'DISABLED' ? '启用' : '停用'"
                @click="row.status === 'DISABLED' ? enableCar(row.id) : disableCar(row.id)"
              ><el-icon><component :is="row.status === 'DISABLED' ? 'Unlock' : 'Lock'" /></el-icon></button>
              <button class="btn-sm btn-sm-danger btn-icon" title="删除" :disabled="['RENTED', 'RESERVED'].includes(row.status)" @click="removeCar(row.id)"><el-icon><Delete /></el-icon></button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="total > pageSize" class="pagination-wrap">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @current-change="loadData" @size-change="currentPage=1;loadData()" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="currentId ? '编辑车辆' : '新增车辆'" width="760px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item v-if="currentId" label="车辆编号">
          <el-input :model-value="form.carNo" disabled />
        </el-form-item>
        <el-form-item label="类型" prop="typeId" :rules="[{ required: true, message: '必填' }]">
          <el-select v-model="form.typeId" style="width:100%"><el-option v-for="t in carTypes" :key="t.id" :label="t.typeName" :value="t.id" /></el-select>
        </el-form-item>
        <el-form-item label="品牌" prop="brand" :rules="[{ required: true, message: '必填' }]"><el-input v-model="form.brand" /></el-form-item>
        <el-form-item label="型号" prop="model" :rules="[{ required: true, message: '必填' }]"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="车牌号" prop="plateNumber" :rules="[{ required: true, message: '必填' }]"><el-input v-model="form.plateNumber" /></el-form-item>
        <el-form-item label="日租金" prop="dayPrice" :rules="[{ required: true, message: '必填' }]"><el-input-number v-model="form.dayPrice" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="里程" prop="mileage" :rules="[{ required: true, message: '必填' }]"><el-input-number v-model="form.mileage" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="省份" prop="province" :rules="[{ required: true, message: '请选择省份' }]">
          <el-select v-model="form.province" style="width:100%" filterable>
            <el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市" prop="city" :rules="[{ required: true, message: '请选择城市' }]">
          <el-select v-model="form.city" style="width:100%" filterable :disabled="!form.province">
            <el-option v-for="city in cityOptions" :key="city" :label="city" :value="city" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress" :rules="[{ required: true, message: '请输入详细地址' }]">
          <el-input v-model="form.detailAddress" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="上传图片">
          <div class="image-tools">
            <label class="btn-sm btn-sm-ghost upload-trigger" :class="{ disabled: uploadLoading }">
              <input type="file" accept="image/png,image/jpeg,image/webp" hidden @change="onUploadFile" />
              {{ uploadLoading ? '上传中...' : '上传到服务器' }}
            </label>
            <el-input v-model="imageUrlInput" placeholder="粘贴图片链接，服务器会自动下载保存" />
            <button class="btn-sm btn-sm-primary" type="button" :disabled="importLoading" @click="importImageByUrl">
              {{ importLoading ? '导入中...' : '导入图片' }}
            </button>
          </div>
        </el-form-item>
        <el-form-item label="图片列表" required>
          <div class="image-list-wrap">
            <div v-if="!form.images.length" class="image-empty">请至少添加一张车辆图片</div>
            <div v-for="(image, index) in form.images" :key="`${image.imageUrl}-${index}`" class="image-item">
              <img :src="image.imageUrl" :alt="`车辆图片${index + 1}`" />
              <div class="image-item-meta">
                <div class="image-item-title">第 {{ index + 1 }} 张{{ index === 0 ? '（封面）' : '' }}</div>
                <div class="image-item-url">{{ image.imageUrl }}</div>
              </div>
              <div class="image-item-actions">
                <button class="btn-sm btn-sm-ghost btn-icon" type="button" title="上移" :disabled="index === 0" @click="moveImage(index, -1)"><el-icon><Top /></el-icon></button>
                <button class="btn-sm btn-sm-ghost btn-icon" type="button" title="下移" :disabled="index === form.images.length - 1" @click="moveImage(index, 1)"><el-icon><Bottom /></el-icon></button>
                <button class="btn-sm btn-sm-danger btn-icon" type="button" title="删除" @click="removeImage(index)"><el-icon><Delete /></el-icon></button>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="btn-sm btn-sm-ghost" @click="dialogVisible = false">取消</button>
        <button class="btn-sm btn-sm-primary" @click="saveCar">保存</button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="车辆详情" width="720px">
      <div v-if="detailRow">
        <div v-if="detailImages.length" class="detail-gallery-block">
          <div class="detail-gallery-main" @click="openDetailZoom">
            <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-left" @click.stop="prevDetailImage"><el-icon><ArrowLeft /></el-icon></button>
            <img :src="activeDetailImage" :alt="`${detailRow.brand} ${detailRow.model}`" class="detail-gallery-image" />
            <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-right" @click.stop="nextDetailImage"><el-icon><ArrowRight /></el-icon></button>
          </div>
          <div v-if="detailImages.length > 1" class="detail-thumbs">
            <button
              v-for="(image, index) in detailImages"
              :key="`${image.imageUrl || image}-${index}`"
              class="detail-thumb"
              :class="{ active: index === detailActiveImageIndex }"
              @click.stop="selectDetailImage(index)"
            >
              <img :src="image.imageUrl || image" :alt="`${detailRow.brand} ${detailRow.model}`" />
            </button>
          </div>
        </div>
        <div class="detail-grid">
          <div class="detail-item"><span class="label">车辆编号</span><span class="value">{{ detailRow.carNo }}</span></div>
          <div class="detail-item"><span class="label">品牌型号</span><span class="value">{{ detailRow.brand }} {{ detailRow.model }}</span></div>
          <div class="detail-item"><span class="label">车牌号</span><span class="value font-mono">{{ detailRow.plateNumber }}</span></div>
          <div class="detail-item"><span class="label">类型</span><span class="value">{{ carTypeName(detailRow.typeId) }}</span></div>
          <div class="detail-item"><span class="label">日租金</span><span class="value text-accent font-mono">¥ {{ detailRow.dayPrice }}/天</span></div>
          <div class="detail-item"><span class="label">里程</span><span class="value font-mono">{{ detailRow.mileage }} km</span></div>
          <div class="detail-item"><span class="label">省份</span><span class="value">{{ detailRow.province || '-' }}</span></div>
          <div class="detail-item"><span class="label">城市</span><span class="value">{{ detailRow.city || '-' }}</span></div>
          <div class="detail-item full-width"><span class="label">取车地址</span><span class="value">{{ fullAddress(detailRow) }}</span></div>
          <div class="detail-item"><span class="label">状态</span><span class="value"><span class="status-badge" :class="statusClass[detailRow.status]">{{ statusMap[detailRow.status] || detailRow.status }}</span></span></div>
          <div v-if="detailRow.status === 'RENTED'" class="detail-item full-width">
            <span class="label">当前使用者</span>
            <span class="value">{{ detailRow.currentRenterName || '暂无数据' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <div v-if="detailZoomVisible" class="zoom-overlay" @click="detailZoomVisible = false">
      <div class="zoom-panel" @click.stop>
        <div class="zoom-main">
          <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-left zoom-arrow" @click.stop="prevDetailImage"><el-icon><ArrowLeft /></el-icon></button>
          <img :src="activeDetailImage" class="zoom-img" :alt="`${detailRow?.brand || ''} ${detailRow?.model || ''}`" />
          <button v-if="hasMultipleDetailImages" class="gallery-arrow gallery-arrow-right zoom-arrow" @click.stop="nextDetailImage"><el-icon><ArrowRight /></el-icon></button>
        </div>
        <div v-if="detailImages.length > 1" class="zoom-thumbs">
          <button
            v-for="(image, index) in detailImages"
            :key="`zoom-${image.imageUrl || image}-${index}`"
            class="detail-thumb zoom-thumb"
            :class="{ active: index === detailActiveImageIndex }"
            @click.stop="selectDetailImage(index)"
          >
            <img :src="image.imageUrl || image" :alt="`${detailRow?.brand || ''} ${detailRow?.model || ''}`" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.toolbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.summary-strip { display: flex; gap: 16px; flex-wrap: wrap; }
.sum-item { font-size: 12px; color: var(--muted); }
.sum-item strong { color: var(--text); margin-left: 4px; }
.card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; overflow: hidden; }
.car-thumb { width: 40px; height: 40px; border-radius: 8px; background: var(--surface2); border: 1px solid var(--border); display: flex; align-items: center; justify-content: center; color: var(--muted); overflow: hidden; }
.car-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; border-radius: inherit; }
.pagination-wrap { display: flex; justify-content: flex-end; padding: 16px; }
.detail-gallery-block { margin-bottom: 16px; }
.detail-gallery-main {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface2);
  cursor: pointer;
}
.detail-gallery-image { width: 100%; height: 280px; object-fit: cover; display: block; }
.detail-thumbs { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
.detail-thumb {
  width: 88px;
  height: 60px;
  padding: 0;
  border: 2px solid var(--border);
  border-radius: 10px;
  overflow: hidden;
  background: var(--surface);
  cursor: pointer;
}
.detail-thumb.active { border-color: var(--accent); }
.detail-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.gallery-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: rgba(17, 24, 39, 0.58);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 2;
}
.gallery-arrow-left { left: 12px; }
.gallery-arrow-right { right: 12px; }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; }
.detail-item.full-width { grid-column: 1 / -1; }
.detail-item .label { font-size: 12px; color: var(--muted); }
.detail-item .value { font-size: 14px; }
.image-tools { display: grid; grid-template-columns: 120px 1fr 100px; gap: 10px; width: 100%; align-items: center; }
.upload-trigger { justify-content: center; }
.upload-trigger.disabled { opacity: 0.6; pointer-events: none; }
.image-list-wrap { width: 100%; display: grid; gap: 12px; }
.image-empty { padding: 14px; border: 1px dashed var(--border); border-radius: 10px; color: var(--muted); background: var(--surface2); }
.image-item { display: grid; grid-template-columns: 120px 1fr auto; gap: 12px; align-items: center; padding: 12px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface2); }
.image-item img { width: 120px; height: 78px; object-fit: cover; border-radius: 8px; background: var(--surface); }
.image-item-meta { min-width: 0; }
.image-item-title { font-size: 13px; font-weight: 500; margin-bottom: 6px; }
.image-item-url { font-size: 12px; color: var(--muted); word-break: break-all; }
.image-item-actions { display: flex; gap: 6px; }
:deep(.el-input--small) { --el-input-bg-color: var(--surface2); --el-input-border-color: var(--border); --el-input-hover-border-color: var(--accent); --el-input-focus-border-color: var(--accent); }
:deep(.el-select--small) { --el-select-input-bg-color: var(--surface2); --el-select-border-color: var(--border); }
.zoom-overlay {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(0,0,0,0.88);
  display: flex;
  align-items: center;
  justify-content: center;
}
.zoom-panel {
  width: min(92vw, 1120px);
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.zoom-main {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: min(92vw, 1120px);
  height: min(72vh, 760px);
  padding: 24px;
  background: rgba(255,255,255,0.04);
  border-radius: 16px;
  box-sizing: border-box;
}
.zoom-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  border-radius: 10px;
}
.zoom-thumbs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}
.zoom-thumb {
  background: rgba(255,255,255,0.08);
}
.zoom-arrow {
  background: rgba(255,255,255,0.18);
  backdrop-filter: blur(8px);
}
</style>
