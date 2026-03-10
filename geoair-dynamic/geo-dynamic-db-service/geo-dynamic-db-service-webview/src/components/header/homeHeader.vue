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
        <span class="el-dropdown-link" style="color: #bfcbd9">
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
        <span class="el-dropdown-link" style="color: #bfcbd9;cursor: pointer">
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
  //background-color: #304156;
  background-image: linear-gradient(15deg, #1b72de, #0e5ec2, #486180);
  color: #bfcbd9;
  width: 100%;
  line-height: 60px;

  .logo2 {
    flex-shrink: 0;
    display: block;
    height: 50px;
  }

  .version {
    padding: 30px 20px 0px 0px;
    font-size: 14px;
    line-height: 20px;
  }

  .menus {
    flex-shrink: 0;
    flex-grow: 1;
    display: flex;

    .activeMenu {
      //background-image: linear-gradient(90deg, #495f7a, #2f3d50, #495f7a);
      //background-image: radial-gradient( #486180, #283546);
      //opacity: 0.3;
      color: #f9fbfd;
      font-size: 22px;
    }

    .menu {
      margin: 0 5px;
      padding: 0 10px;
      font-size: 20px;
      font-weight: 700;
      cursor: pointer;
      position: relative;

      .submenus {
        padding: 5px 0;
        display: none;
        z-index: 1000;
        position: absolute;
        top: 60px;
        left: 0px;
        background-color: #304156;
        //padding: 0 10px;
        width: 200px;

        border-radius: 5px;
        //border: 1px solid #00ff00;
        .submenu {
          font-size: 16px;
          line-height: 40px;
          padding: 0 10px;
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;

          &:hover {
            background-color: #222d3b;
          }
        }
      }

      &:hover {
        background-color: #2a5893;

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
      color: #f9fbfd;
      font-size: 14px;
      margin-right: 15px;
      padding: 0 5px;
      line-height: 30px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 4px;
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
        background-color: #304156;

        line-height: 30px;

        .option {
          cursor: pointer;
          padding: 0 10px;

          &:hover {
            background-color: #222d3b;
          }
        }
      }
    }
  }
}
</style>
