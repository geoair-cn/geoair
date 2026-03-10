<template>
  <div>
    <el-form label-width="160px">
      <el-form-item :label="$t('m.datasource')">
        <el-select v-model="datasourceId">
          <el-option :value="item.id" :label="item.name" v-for="item in datasources">{{ item.name }}</el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <div slot="label">
          <label-tip label="SQL" :tip="$t('m.sql_warning')"></label-tip>
        </div>
        <div>
          <el-tabs v-model="currentActiveTabName" type="card" editable @edit="handleTabsEdit" tab-position="top">
            <el-tab-pane :key="item.name" v-for="(item, index) in editableTabs" :label="'SQL-'+item.name"
                         :name="item.name">
              <sqlide ref="codemirror" :textareaRef="'cms'+index" :value="item.sqlText" :ds="datasourceId" :thisApiInfo="thisApiInfo"></sqlide>


            </el-tab-pane>
          </el-tabs>

        </div>
      </el-form-item>
      <el-form-item>
        <div slot="label">
          <label-tip :label="$t('m.transaction')" :tip="$t('m.transaction_tip')"></label-tip>
        </div>
        <el-radio-group v-model="transaction">
          <el-radio :label="1">{{ $t('m.on') }}</el-radio>
          <el-radio :label="0">{{ $t('m.off') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <div slot="label">
          <label-tip label="分页" tip="分页传参形式&page=0&limit=10，分页从0开始"></label-tip>
        </div>
        <el-radio-group v-model="pageIs">
          <el-radio :label="1">{{ $t('m.on') }}</el-radio>
          <el-radio :label="0">{{ $t('m.off') }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item>
        <div slot="label">
          <label-tip label="结果集转驼峰" tip="数据库中字段名称会自动转驼峰，如：user_name会自动转成userName"></label-tip>
        </div>
        <el-radio-group v-model="humpIs">
          <el-radio :label="1">{{ $t('m.on') }}</el-radio>
          <el-radio :label="0">{{ $t('m.off') }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import sqlide from "@/components/api/executor/sqlIDE.vue";
import {EXECUTOR_TYPE} from "@/constant";
import * as dbApi from '@/api/dbApi'

export default {
  name: "SqlExecutor",
  data() {
    return {
      transformPlugins: [],
      transaction: 0,
      pageIs: 1,
      humpIs: 1,
      currentActiveTabName: '1', //当前选中的tab的name
      currentActiveTabIndex: 0, // 当前选中tab的索引值
      editableTabs: [{name: '1', sqlText: "", transformPlugin: null, transformPluginParam: null}],
      tabIndex: 1, //tab 总数
      datasourceId: null,
      datasources: []
    }
  },
  props: {
    detail: {
      type: Object
    },
    thisApiInfo: {
      type: Object
    }
  },
  methods: {
    getAllPlugins() {

    },
    getTaskJson() {
      let sqls = this.$refs.codemirror.map((item, index) => item.coder.getValue())
      let p = this.editableTabs.map((item, index) => {
        return {
          sqlText: sqls[index],
          transformPlugin: item.transformPlugin,
          transformPluginParam: item.transformPluginParam
        }
      })
      return {
        taskType: EXECUTOR_TYPE.SQL_EXECUTOR,
        sqlList: p,
        transaction: this.transaction,
        pageIs: this.pageIs,
        humpIs: this.humpIs,
        datasourceId: this.datasourceId
      }
    },
    check() {
      if (this.datasourceId == null) {
        this.$message.warning("SQL Executor: datasource empty!")
        return false
      }

      let sqls = this.$refs.codemirror.map((item, index) => item.coder.getValue())
      for (let sql of sqls) {
        console.log(sql, sql.trim())
        if (sql.trim() == '') {
          this.$message.warning("SQL Executor: SQL empty!")
          return false
        }
      }
      return true
    },
    getAllSource() {
      dbApi.getAllDataSources()
          .then((response) => {
            this.datasources = response.data
          }).catch((error) => {
        this.$message.error("Get all datasources Failed")
      })
    },
    handleTabsEdit(targetName, action) {
      if (action === 'add') {
        let newTabName = ++this.tabIndex + '';
        this.editableTabs.push({
          title: 'SQL ' + newTabName,
          name: newTabName,
          sqlText: "", transformPlugin: null, transformPluginParam: null
        });
        this.currentActiveTabName = newTabName;
      }
      if (action === 'remove') {
        if (this.editableTabs.length === 1) {
          this.$message.warning("At least one tab!")
          return;
        }
        let tabs = this.editableTabs;
        let activeName = this.currentActiveTabName;
        let i = 0;
        if (activeName === targetName) {
          tabs.forEach((tab, index) => {
            if (tab.name === targetName) {
              i = index;
              let nextTab = tabs[index + 1] || tabs[index - 1];
              if (nextTab) {
                activeName = nextTab.name;
              }
            }
          });
        }

        this.currentActiveTabName = activeName;
        this.editableTabs = tabs.filter(tab => tab.name !== targetName);
        // this.$store.commit('removeCm', i) // 删除 vuex中 的cmInstance
      }
    }
  },
  components: {
    sqlide
  },
  watch: {
    // 编辑API页面，本组件生成的时候，props还没注入进来，所以要监听
    detail: function (newVal, oldVal) {
      //
      this.transaction = newVal.transaction
      this.datasourceId = newVal.datasourceId

      // 生成子组件中的tabPane需要的数据格式
      for (let j = 0; j < newVal.sqlList.length; j++) {
        const b = newVal.sqlList[j]
        b.name = (j + 1) + '';
      }
      this.editableTabs = newVal.sqlList;
      this.tabIndex = newVal.sqlList.length;
      // console.log(this.editableTabs)
    },
    editableTabs(newV, oldV) {
      this.editableTabs.forEach((tab, index) => {
        if (tab.name === this.currentActiveTabName) {
          this.currentActiveTabIndex = index;
        }
      });
    },
    currentActiveTabName(newV, oldV) {
      this.editableTabs.forEach((tab, index) => {
        if (tab.name === this.currentActiveTabName) {
          this.currentActiveTabIndex = index;
        }
      });
    }
  },
  computed: {},
  created() {
    this.getAllSource();
    this.getAllPlugins()
  }
}
</script>

<style scoped>
.label {
  font-weight: 700;
  margin: 0 5px 0 20px;
}
</style>
