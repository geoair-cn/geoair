<template>
  <div class="head">
    <div style="padding: 5px 10px">
      <img src="@/img/logo.png" alt="" class="logo2"/>
    </div>
    <!--    <div class="logo">DBApi</div>-->
    <span class="version">{{ version }}</span>
    <div class="menus">
      <div class="menu iconfont icon-database " :class="{'activeMenu':$route.path == '/datasource'}"
           @click="clickMenu('/datasource')">{{ $t("m.datasource") }}
      </div>
      <div class="menu iconfont icon-api" :class="{'activeMenu':$route.path.startsWith('/api')}"
           @click="clickMenu('/api')">API
      </div>
    </div>
    <div class="right">
      <!-- 新增：显示用户名 -->
      <div class="username" v-if="username">{{ username }}</div>
      <div style="line-height: 60px;margin: 0 5px">
      </div>

      <el-dropdown @command="changeLanguage" style="margin-right: 15px">
        <span class="el-dropdown-link">
          {{ languageName }}<i class="el-icon-arrow-down el-icon--right"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item :command="item" :key="index" v-for="(item,index) in langs">{{
              item.name
            }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>

      <!-- 新增：退出登录下拉菜单 -->
      <el-dropdown @command="handleCommand" style="margin-right: 15px">
        <span class="el-dropdown-link" style="cursor: pointer">
          <i class="el-icon-user"></i> 操作<i class="el-icon-arrow-down el-icon--right"></i>
        </span>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="logout" divided>{{ $t('m.logout') }}</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import * as dbApi from '@/api/dsApi'

export default {
  name: "homeHeader",
  data() {
    return {
      dialogVisible: false,
      langs: [
        {name: "English", value: "en"},
        {name: "中文", value: "cn"},
      ],
      currentLang: this.$i18n.locale,
      version: null,
      username: localStorage.getItem("username") // 从本地存储获取用户名
    };
  },
  methods: {
    handleCommand(command) {
      if (command === 'logout') {
        // 退出登录：清除本地存储的登录信息，跳转到登录页
        localStorage.removeItem("dsToken")
        localStorage.removeItem("username")
        this.$message.success("已成功退出登录");
        this.$router.push("/login");
      }
    },
    clickMenu(data) {
      this.$router.push(data);
    },
    changeLanguage(data) {
      this.$i18n.locale = data.value;
      localStorage.setItem("locale", data.value);
      this.currentLang = data.value;
    },
    getVersion() {
      dbApi.getSystemVersion()
          .then((response) => {
            this.version = response.data;
          })
          .catch((error) => {
          });
    },
  },
  created() {
    this.getVersion();
  },
  computed: {
    languageName() {
      const p = this.langs.filter((item) => item.value === this.currentLang)[0]
          .name;
      return p;
    },
  },
  // 新增：监听路由变化，确保用户名刷新（比如登录后返回首页）
  watch: {
    '$route'() {
      this.username = localStorage.getItem("username");
    }
  }
};
</script>

<style scoped lang="less">
.head {
  display: flex;
  /* 改为登录页同款渐变背景 */
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff; /* 文字默认白色 */
  width: 100%;
  line-height: 60px;
  box-shadow: 0 2px 10px rgba(102, 126, 234, 0.2);

  .logo2 {
    flex-shrink: 0;
    display: block;
    height: 50px;
  }

  .version {
    padding: 30px 20px 0px 0px;
    font-size: 14px;
    line-height: 20px;
    color: rgba(255, 255, 255, 0.8);
  }

  .menus {
    flex-shrink: 0;
    flex-grow: 1;
    display: flex;

    .activeMenu {
      color: #fff;
      font-size: 22px;
      /* 激活状态添加渐变背景 */
      background: rgba(255, 255, 255, 0.15);
      border-radius: 6px;
    }

    .menu {
      margin: 0 5px;
      padding: 0 10px;
      font-size: 20px;
      font-weight: 700;
      cursor: pointer;
      position: relative;
      border-radius: 6px;
      transition: all 0.3s ease;

      .submenus {
        padding: 5px 0;
        display: none;
        z-index: 1000;
        position: absolute;
        top: 60px;
        left: 0px;
        /* 子菜单渐变背景 */
        background: linear-gradient(135deg, #5a6edb 0%, #6b4298 100%);
        width: 200px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

        .submenu {
          font-size: 16px;
          line-height: 40px;
          padding: 0 10px;
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
          border-radius: 4px;
          margin: 0 5px;

          &:hover {
            background: rgba(255, 255, 255, 0.1);
          }
        }
      }

      &:hover {
        /*  hover 效果改为半透明背景 */
        background: rgba(255, 255, 255, 0.1);

        .submenus {
          display: block;
        }
      }
    }
  }

  .right {
    margin: 0 20px;
    flex-shrink: 0;
    display: flex;
    align-items: center; // 新增：垂直居中，适配用户名和按钮布局

    // 新增：用户名样式
    .username {
      color: #fff;
      font-size: 14px;
      margin-right: 15px;
      padding: 0 10px;
      line-height: 32px;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 6px;
      transition: all 0.3s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }

    // 下拉菜单链接样式
    .el-dropdown-link {
      color: rgba(255, 255, 255, 0.9);
      cursor: pointer;
      font-size: 14px;
      transition: all 0.3s ease;

      &:hover {
        color: #fff;
      }
    }

    .mode {
      font-family: Helvetica;
      font-weight: 900;
      font-size: 15px;
      margin-right: 10px;
    }

    .langs {
      position: relative;

      span {
        cursor: pointer;
        font-size: 18px;
      }

      .options {
        z-index: 1000;
        position: absolute;
        right: 0;
        // display: none;
        /* 语言选项渐变背景 */
        background: linear-gradient(135deg, #5a6edb 0%, #6b4298 100%);
        line-height: 30px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

        .option {
          cursor: pointer;
          padding: 0 10px;
          border-radius: 4px;
          margin: 0 5px;

          &:hover {
            background: rgba(255, 255, 255, 0.1);
          }
        }
      }
    }
  }
}
</style>
