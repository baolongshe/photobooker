<template>
  <div class="technical-notes">
    <div v-if="isAdmin">
      <!-- 页面头部 -->
      <div class="page-header">
        <div class="container mx-auto px-4 sm:px-6 lg:px-8">
          <h1 class="text-4xl font-bold text-dark mb-4">技术笔记</h1>
          <p class="text-black text-lg">记录开发过程中的技术问题和解决方案</p>
        </div>
      </div>
      <div class="container mx-auto px-4 sm:px-6 lg:px-8 py-12 note-content">
        <el-button type="primary" @click="addDialogVisible = true" style="margin-bottom: 24px;">添加笔记</el-button>
        <el-dialog v-model="addDialogVisible" title="添加技术笔记" width="500px">
          <el-form>
            <el-form-item label="标题"><el-input v-model="newNote.title" /></el-form-item>
            <el-form-item label="内容"><el-input v-model="newNote.content" type="textarea" rows="6" /></el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="addDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitNote">提交</el-button>
          </template>
        </el-dialog>
        <!-- Java跨域解决方案 -->
        <section id="cors-solution" class="mb-16">
          <div class="bg-white rounded-xl shadow-lg overflow-hidden">
            <div class="bg-gradient-to-r from-blue-500 to-purple-600 p-6">
              <h2 class="text-2xl font-bold text-black mb-2">
                <i class="fa fa-code mr-3"></i>
                Java跨域问题解决方案
              </h2>
              <p class="text-black">解决前后端分离项目中的跨域访问问题</p>
            </div>
            
            <div class="p-8">
              <!-- 问题描述 -->
              <div class="mb-8">
                <h3 class="text-xl font-bold text-dark mb-4 flex items-center">
                  <i class="fa fa-exclamation-triangle text-black mr-3"></i>
                  问题描述
                </h3>
                <div class="bg-yellow-50 border-l-4 border-yellow-400 p-4 rounded">
                  <p class="text-black">
                    在前后端分离的项目中，前端运行在 <code class="bg-yellow-100 px-2 py-1 rounded">http://localhost:3000</code>，
                    后端运行在 <code class="bg-yellow-100 px-2 py-1 rounded">http://localhost:8080</code>，
                    由于浏览器的同源策略限制，前端无法直接访问后端API，出现跨域错误。
                  </p>
                </div>
              </div>

              <!-- 解决方案 -->
              <div class="mb-8">
                <h3 class="text-xl font-bold text-dark mb-4 flex items-center">
                  <i class="fa fa-lightbulb text-black mr-3"></i>
                  解决方案
                </h3>
                
                <!-- 方案1: Spring Boot配置 -->
                <div class="mb-6">
                  <h4 class="text-lg font-semibold text-dark mb-3">方案1: Spring Boot CORS配置</h4>
                  <div class="bg-gray-50 p-4 rounded-lg">
                    <p class="text-sm text-black mb-3">创建CORS配置类：</p>
                    <pre class="bg-gray-900 text-black p-4 rounded overflow-x-auto text-sm"><code>package com.xhxi.photobooker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")  // 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}</code></pre>
                  </div>
                </div>

                <!-- 方案2: 注解方式 -->
                <div class="mb-6">
                  <h4 class="text-lg font-semibold text-dark mb-3">方案2: 使用@CrossOrigin注解</h4>
                  <div class="bg-gray-50 p-4 rounded-lg">
                    <p class="text-sm text-black mb-3">在Controller类或方法上添加注解：</p>
                    <pre class="bg-gray-900 text-black p-4 rounded overflow-x-auto text-sm"><code>// 在Controller类上添加
@RestController
@RequestMapping("/photo")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    // 或者在具体方法上添加
    @PostMapping("/user/login")
    @CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
    public Result login(@RequestBody UserLoginDTO userLoginDTO) {
        // 登录逻辑
    }
}</code></pre>
                  </div>
                </div>

                <!-- 方案3: 过滤器方式 -->
                <div class="mb-6">
                  <h4 class="text-lg font-semibold text-dark mb-3">方案3: 自定义CORS过滤器</h4>
                  <div class="bg-gray-50 p-4 rounded-lg">
                    <p class="text-sm text-black mb-3">创建CORS过滤器：</p>
                    <pre class="bg-gray-900 text-black p-4 rounded overflow-x-auto text-sm"><code>package com.xhxi.photobooker.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
        response.setHeader("Access-Control-Max-Age", "3600");
        
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }
    
    @Override
    public void init(FilterConfig filterConfig) {}
    
    @Override
    public void destroy() {}
}</code></pre>
                  </div>
                </div>
              </div>

              <!-- 测试验证 -->
              <div class="mb-8">
                <h3 class="text-xl font-bold text-dark mb-4 flex items-center">
                  <i class="fa fa-check-circle text-black mr-3"></i>
                  测试验证
                </h3>
                <div class="grid md:grid-cols-2 gap-6">
                  <div class="bg-green-50 border border-green-200 rounded-lg p-4">
                    <h4 class="font-semibold text-black mb-2">前端测试代码</h4>
                    <pre class="bg-gray-900 text-black p-3 rounded text-sm overflow-x-auto"><code>// 使用fetch测试
fetch('http://localhost:8080/photo/user/login', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
    },
    body: JSON.stringify({
        username: 'admin',
        password: '123456'
    })
})
.then(response => response.json())
.then(data => console.log('Success:', data))
.catch(error => console.error('Error:', error));

// 使用axios测试
axios.post('http://localhost:8080/photo/user/login', {
    username: 'admin',
    password: '123456'
})
.then(response => {
    console.log('登录成功:', response.data);
})
.catch(error => {
    console.error('登录失败:', error);
});</code></pre>
                  </div>
                  
                  <div class="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <h4 class="font-semibold text-black mb-2">浏览器检查</h4>
                    <ul class="text-sm text-black space-y-2">
                      <li>• 打开浏览器开发者工具 (F12)</li>
                      <li>• 查看Network标签页</li>
                      <li>• 检查请求头中是否包含CORS相关字段</li>
                      <li>• 确认没有跨域错误信息</li>
                      <li>• 验证OPTIONS预检请求是否成功</li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 常见问题 -->
              <div class="mb-8">
                <h3 class="text-xl font-bold text-dark mb-4 flex items-center">
                  <i class="fa fa-question-circle text-black mr-3"></i>
                  常见问题
                </h3>
                <div class="space-y-4">
                  <div class="bg-orange-50 border-l-4 border-orange-400 p-4 rounded">
                    <h4 class="font-semibold text-black mb-2">问题1: 预检请求失败</h4>
                    <p class="text-black text-sm">
                      确保OPTIONS请求能够正确处理，在过滤器中设置正确的响应状态码。
                    </p>
                  </div>
                  
                  <div class="bg-orange-50 border-l-4 border-orange-400 p-4 rounded">
                    <h4 class="font-semibold text-black mb-2">问题2: 携带Cookie失败</h4>
                    <p class="text-black text-sm">
                      如果前端需要携带Cookie，需要设置 <code class="bg-orange-100 px-1 rounded">allowCredentials(true)</code>，
                      并且不能使用通配符 <code class="bg-orange-100 px-1 rounded">*</code> 作为允许的源。
                    </p>
                  </div>
                  
                  <div class="bg-orange-50 border-l-4 border-orange-400 p-4 rounded">
                    <h4 class="font-semibold text-black mb-2">问题3: 自定义请求头被拒绝</h4>
                    <p class="text-black text-sm">
                      确保在CORS配置中包含所有需要的自定义请求头，特别是Authorization等认证相关的头部。
                    </p>
                  </div>
                </div>
              </div>

              <!-- 最佳实践 -->
              <div class="mb-8">
                <h3 class="text-xl font-bold text-dark mb-4 flex items-center">
                  <i class="fa fa-star text-black mr-3"></i>
                  最佳实践
                </h3>
                <div class="grid md:grid-cols-2 gap-6">
                  <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                    <h4 class="font-semibold text-black mb-3">安全考虑</h4>
                    <ul class="text-sm text-black space-y-2">
                      <li>• 生产环境中不要使用 <code class="bg-yellow-100 px-1 rounded">*</code> 作为允许的源</li>
                      <li>• 明确指定允许的HTTP方法</li>
                      <li>• 限制允许的请求头</li>
                      <li>• 设置合理的缓存时间</li>
                    </ul>
                  </div>
                  
                  <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                    <h4 class="font-semibold text-black mb-3">性能优化</h4>
                    <ul class="text-sm text-black space-y-2">
                      <li>• 合理设置maxAge减少预检请求</li>
                      <li>• 使用缓存策略</li>
                      <li>• 避免不必要的CORS配置</li>
                      <li>• 监控CORS相关的错误日志</li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 相关链接 -->
              <div>
                <h3 class="text-xl font-bold text-dark mb-4 flex items-center">
                  <i class="fa fa-link text-black mr-3"></i>
                  相关链接
                </h3>
                <div class="flex flex-wrap gap-3">
                  <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS" 
                     class="bg-blue-100 hover:bg-blue-200 text-blue-800 px-3 py-2 rounded-lg text-sm transition-colors">
                    <i class="fa fa-external-link mr-2"></i>
                    MDN CORS文档
                  </a>
                  <a href="https://spring.io/guides/gs/rest-service-cors/" 
                     class="bg-green-100 hover:bg-green-200 text-green-800 px-3 py-2 rounded-lg text-sm transition-colors">
                    <i class="fa fa-external-link mr-2"></i>
                    Spring CORS指南
                  </a>
                  <a href="https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html" 
                     class="bg-purple-100 hover:bg-purple-200 text-purple-800 px-3 py-2 rounded-lg text-sm transition-colors">
                    <i class="fa fa-external-link mr-2"></i>
                    Spring Framework CORS
                  </a>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 其他技术笔记 -->
        <section class="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div class="bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow">
            <div class="bg-gradient-to-r from-green-500 to-blue-600 p-6">
              <h3 class="text-xl font-bold text-black">
                <i class="fa fa-leaf mr-2"></i>
                Spring Boot 笔记
              </h3>
            </div>
            <div class="p-6">
              <p class="text-black mb-4">Spring Boot框架使用技巧和最佳实践</p>
              <a href="#" class="text-black hover:text-black font-medium">
                查看详情 <i class="fa fa-arrow-right ml-1"></i>
              </a>
            </div>
          </div>

          <div class="bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow">
            <div class="bg-gradient-to-r from-purple-500 to-pink-600 p-6">
              <h3 class="text-xl font-bold text-black">
                <i class="fa fa-js mr-2"></i>
                Vue.js 技巧
              </h3>
            </div>
            <div class="p-6">
              <p class="text-black mb-4">Vue.js开发中的实用技巧和组件设计</p>
              <a href="#" class="text-black hover:text-black font-medium">
                查看详情 <i class="fa fa-arrow-right ml-1"></i>
              </a>
            </div>
          </div>

          <div class="bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition-shadow">
            <div class="bg-gradient-to-r from-orange-500 to-red-600 p-6">
              <h3 class="text-xl font-bold text-black">
                <i class="fa fa-database mr-2"></i>
                数据库优化
              </h3>
            </div>
            <div class="p-6">
              <p class="text-black mb-4">MySQL数据库性能优化和查询技巧</p>
              <a href="#" class="text-black hover:text-black font-medium">
                查看详情 <i class="fa fa-arrow-right ml-1"></i>
              </a>
            </div>
          </div>
        </section>
      </div>
    </div>
    <div v-else style="padding: 80px 0; text-align: center;">
      <el-empty description="无权限访问，仅限管理员查看" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

const addDialogVisible = ref(false)
const newNote = ref({ title: '', content: '' })

const token = localStorage.getItem('token')
const isAdmin = computed(() => {
  return !!token && userStore.user && userStore.user.role === 1
})

const submitNote = async () => {
  if (!newNote.value.title || !newNote.value.content) {
    ElMessage.warning('请填写完整的笔记信息')
    return
  }
  // TODO: 调用后端API保存笔记，成功后刷新列表
  ElMessage.success('笔记添加成功（请补充后端API逻辑）')
  addDialogVisible.value = false
  newNote.value = { title: '', content: '' }
}

onMounted(() => {
  // 可选：自动跳转到首页
  // if (!isAdmin.value) router.replace({ name: 'home' })
  nextTick(() => {
    const els = document.querySelectorAll('.note-content *') as NodeListOf<HTMLElement>
    els.forEach(el => {
      el.style.color = '#222'
      el.style.background = 'transparent'
    })
  })
})
</script>

<style scoped>
.technical-notes {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 60px 0 40px;
  text-align: center;
}

.page-header h1,
.page-header h2,
.page-header h3,
.page-header h4,
.page-header h5,
.page-header h6,
.page-header p {
  color: white !important;
}

.page-header p {
  color: rgba(255, 255, 255, 0.9) !important;
}

.note-content,
.note-content * {
  color: #222 !important;
  background: transparent !important;
}

.note-content pre,
.note-content code {
  color: #222 !important;
  background: #f5f5f5 !important;
}

@media (max-width: 768px) {
  .page-header {
    padding: 40px 0 30px;
  }
  .page-header h1 {
    font-size: 2rem;
  }
  .page-header p {
    font-size: 1rem;
  }
  .note-content {
    padding: 16px;
  }
}
</style> 