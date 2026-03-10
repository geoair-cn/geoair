<template>
  <div class="login-container">
    <!-- 登录卡片 -->
    <el-card class="login-card">
      <h2 class="login-title">系统登录</h2>

      <!-- 登录表单 -->
      <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="80px"
          class="login-form"
      >
        <!-- 用户名输入框 -->
        <el-form-item label="用户名" prop="username">
          <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              prefix-icon="el-icon-user"
              clearable
          ></el-input>
        </el-form-item>

        <!-- 密码输入框 -->
        <el-form-item label="密码" prop="password">
          <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="el-icon-lock"
              clearable
              show-password
          ></el-input>
        </el-form-item>

        <!-- 登录按钮 -->
        <el-form-item class="login-btn-group">
          <el-button type="primary" @click="handleLogin" icon="el-icon-s-tools">
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
          {min: 1, max: 10, message: "用户名长度在 3 到 10 个字符", trigger: "blur"}
        ],
        password: [
          {required: true, message: "请输入密码", trigger: "blur"},
          {min: 1, max: 16, message: "密码长度在 6 到 16 个字符", trigger: "blur"}
        ]
      }
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

        try {
          // 第二步：等待接口请求完成（核心修复：用 await 等待异步接口返回）
          const response = await dbApi.login(this.loginForm.username, this.loginForm.password);

          // 第三步：判断接口返回是否成功
          if (response.data.success) {
            localStorage.setItem("dsToken", response.data.data.token);
            localStorage.setItem("username", response.data.data.username);
            // 提示登录成功
            this.$message.success("登录成功！");

            // 第四步：获取跳转参数并跳转页面
            const redirect = this.$route.query.redirect || "/";

            // 安全跳转页面，处理可能的路由异常
            await this.$router.push({path: redirect}).catch(err => {
              if (err.name !== 'NavigationDuplicated') {
                console.error("跳转失败：", err);
                this.$router.push("/");
              }
            });
          } else {
            // 接口返回失败（比如用户名密码错误）
            this.$message.error("登录失败：" + (response.data.message || "用户名或密码错误"));
          }
        } catch (error) {
          // 捕获接口请求异常（比如网络错误、接口500等）
          console.error("登录接口请求失败：", error);
          this.$message.error("登录失败：" + (error.message || "网络异常，请稍后重试"));
        }
      });
    }
  }
}
;
</script>

<style scoped>
/* 页面整体样式 */
.login-container {
  width: 100vw;
  height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  justify-content: center;
  align-items: center;
}

/* 登录卡片样式 */
.login-card {
  width: 400px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

/* 登录标题 */
.login-title {
  text-align: center;
  margin-bottom: 20px;
  color: #1989fa;
}

/* 表单样式 */
.login-form {
  margin-top: 10px;
}

/* 按钮组样式 */
.login-btn-group {
  text-align: center;
}
</style>
