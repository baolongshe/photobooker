<template>
  <div id="mapContainer" class="map-fullscreen"></div>
  <div v-if="infoWindow.visible" :style="infoWindowStyle" class="custom-info-window">
    <div><strong>{{ infoWindow.data.name }}</strong></div>
    <div>电话：{{ infoWindow.data.phone || '无' }}</div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const infoWindow = reactive({
  visible: false,
  data: {},
  x: 0,
  y: 0
})
const infoWindowStyle = ref({})

onMounted(async () => {
  // 获取摄影师分布数据
  const { data } = await axios.get('/photo/photographer/map')
  // eslint-disable-next-line no-undef
  const map = new TMap.Map('mapContainer', {
    center: new TMap.LatLng(22, 113), // 默认定位到经纬度22, 113
    zoom: 10
  })
  // 添加Marker并绑定事件
  data.data.forEach(item => {
    if (item.latitude && item.longitude) {
      const multiMarker = new TMap.MultiMarker({
        map,
        geometries: [{
          id: item.id,
          position: new TMap.LatLng(item.latitude, item.longitude),
          properties: { title: item.name }
        }]
      })
      // 点击跳转
      multiMarker.on('click', (evt) => {
        if (evt && evt.geometry && evt.geometry.id) {
          const photographerId = evt.geometry.id
          router.push(`/photographer/${photographerId}`)
        }
      })
      // 鼠标悬停显示信息
      multiMarker.on('mouseover', (evt) => {
        if (evt && evt.geometry && evt.geometry.id) {
          // 查找摄影师数据
          const photographer = data.data.find(p => p.id === evt.geometry.id)
          if (photographer) {
            infoWindow.data = photographer
            infoWindow.visible = true
            // 获取像素坐标
            const latlng = new TMap.LatLng(photographer.latitude, photographer.longitude)
            const pixel = map.projectToContainer(latlng)
            infoWindow.x = pixel.x
            infoWindow.y = pixel.y
            infoWindowStyle.value = {
              position: 'fixed',
              left: `${pixel.x + 10}px`,
              top: `${pixel.y - 10}px`,
              zIndex: 1000
            }
          }
        }
      })
      // 鼠标移出隐藏信息
      multiMarker.on('mouseout', () => {
        infoWindow.visible = false
      })
    }
  })
})
</script>

<style scoped>
.map-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
}
.custom-info-window {
  background: #fff;
  border: 1px solid #409eff;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  padding: 10px 16px;
  min-width: 160px;
  font-size: 14px;
  pointer-events: none;
  color: #222;
}
</style>

<!-- 腾讯地图JS API需在public/index.html引入 -->
<!-- <script charset="utf-8" src="https://map.qq.com/api/gljs?v=1.exp&key=V4YBZ-QSA6T-YVMXO-VFSKN-U65Z7-KRF4H"></script> --> 