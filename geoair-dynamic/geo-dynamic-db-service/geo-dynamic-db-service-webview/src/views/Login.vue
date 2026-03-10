<template>
  <div class="login-container">
    <!-- 背景装饰元素 -->
    <div class="bg-decoration bg-circle circle-1"></div>
    <div class="bg-decoration bg-circle circle-2"></div>
    <div class="bg-decoration bg-circle circle-3"></div>

    <!-- 登录卡片 -->
    <el-card class="login-card">
      <div class="login-header">
        <div class="login-icon">
          <i class="el-icon-s-tools"></i>
        </div>
        <h2 class="login-title">系统登录</h2>
        <p class="login-subtitle">db-service-api</p>
      </div>

      <!-- 登录表单 -->
      <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="0"
          class="login-form"
      >
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <div class="input-wrapper">
            <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                prefix-icon="el-icon-user"
                clearable
                class="custom-input"
            ></el-input>
          </div>
        </el-form-item>

        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <div class="input-wrapper">
            <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="el-icon-lock"
                clearable
                show-password
                class="custom-input"
            ></el-input>
          </div>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item class="login-btn-group">
          <el-button
              type="primary"
              @click="handleLogin"
              class="login-btn"
              :loading="isLoading"
          >
            <i class="el-icon-s-tools"></i>
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import * as dbApi from '@/api/dsApi'

export default {
  name: "Login",
  data() {
    return {
      // 登录表单数据
      loginForm: {
        username: "",  // 用户名
        password: ""   // 密码
      },
      // 表单验证规则
      loginRules: {
        username: [
          {required: true, message: "请输入用户名", trigger: "blur"},
          {min: 3, max: 10, message: "用户名长度在 3 到 10 个字符", trigger: "blur"}
        ],
        password: [
          {required: true, message: "请输入密码", trigger: "blur"},
          {min: 6, max: 16, message: "密码长度在 6 到 16 个字符", trigger: "blur"}
        ]
      },
      // 加载状态
      isLoading: false
    };
  },
  methods: {
    async handleLogin() {
      // 第一步：先校验表单
      this.$refs.loginFormRef.validate(async (valid) => {
        if (!valid) {
          this.$message.error("请完善表单信息！");
          return;
        }

        // 设置加载状态
        this.isLoading = true;

        try {
          // 第二步：等待接口请求完成
          const response = await dbApi.login(this.loginForm.username, this.loginForm.password);

          // 第三步：判断接口返回是否成功
          if (response.data.success) {
            localStorage.setItem("dsToken", response.data.data.token);
            localStorage.setItem("username", response.data.data.username);
            // 提示登录成功
            this.$message.success("登录成功！");

            // 第四步：获取跳转参数并跳转页面
            const redirect = this.$route.query.redirect || "/";

            // 安全跳转页面
            await this.$router.push({path: redirect}).catch(err => {
              if (err.name !== 'NavigationDuplicated') {
                console.error("跳转失败：", err);
                this.$router.push("/");
              }
            });
          } else {
            localStorage.removeItem("dsToken")
            localStorage.removeItem("username")
            // 接口返回失败
            this.$message.error("登录失败：" + (response.data.message || "用户名或密码错误"));
          }
        } catch (error) {
          // 捕获接口请求异常
          console.error("登录接口请求失败：", error);
          this.$message.error("登录失败：" + (error.message || "网络异常，请稍后重试"));
        } finally {
          // 重置加载状态
          this.isLoading = false;
        }
      });
    }
  }
};
</script>

<style scoped>
/* 页面整体样式 */
.login-container {
  width: 100vw;
  height: 100vh;
  /* 渐变背景 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

/* 背景装饰圆圈 */
.bg-decoration {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 8s ease-in-out infinite;
}

.circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.circle-2 {
  width: 300px;
  height: 300px;
  bottom: -150px;
  left: -150px;
  animation-delay: 2s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 20%;
  animation-delay: 4s;
}

/* 浮动动画 */
@keyframes float {
  0% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(10deg);
  }
  100% {
    transform: translateY(0) rotate(0deg);
  }
}

/* 登录卡片样式 */
.login-card {
  width: 420px;
  padding: 40px 30px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.18);
  transition: all 0.3s ease;
}

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 40px rgba(31, 38, 135, 0.3);
}

/* 登录头部 */
.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto 20px;
  font-size: 36px;
  color: white;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.login-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #666;
}

/* 表单样式 */
.login-form {
  margin-top: 10px;
}

/* 输入框容器 */
.input-wrapper {
  position: relative;
  margin-bottom: 1px;
}

/* 自定义输入框 */
.custom-input {
  height: 48px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  padding: 0 15px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.custom-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* 按钮组样式 */
.login-btn-group {
  text-align: center;
  margin-top: 20px;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.login-btn:hover {
  background: linear-gradient(135deg, #5a6edb 0%, #6b4298 100%);
  transform: scale(1.02);
}

.login-btn:active {
  transform: scale(0.98);
}
</style>
