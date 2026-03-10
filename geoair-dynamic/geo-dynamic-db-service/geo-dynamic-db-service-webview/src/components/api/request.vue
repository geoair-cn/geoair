<template>
  <div style="padding: 20px">
    <!-- 仅当路由历史长度 > 1 时显示返回按钮 -->
    <!--    <el-button-->
    <!--        v-if="showBackButton"-->
    <!--        icon="el-icon-d-arrow-left"-->
    <!--        type="info"-->
    <!--        plain-->
    <!--        @click="$router.go(-1)"-->
    <!--        size="small"-->
    <!--    >-->
    <!--            {{ $t('m.back') }}-->
    <!--    </el-button>-->
    <h2>{{ $t('m.request_test') }}</h2>
    <el-tabs tab-position="top" type="border-card" @tab-click="handleTabClick">
      <el-tab-pane :label="$t('m.request_test')">
        <div class="mycontent">


          <h2>{{ $t('m.request') }}</h2>

          <h4>{{ $t('m.url') }}：</h4>
          <el-input v-model="url"></el-input>

          <el-alert type="warning" show-icon
                    v-show="this.$store.state.mode == 'cluster' || this.$store.state.mode == 'cluster in docker' "
                    :title="$t('m.ip_tip')" style="margin-top: 10px;">
          </el-alert>
          <h4>Header：</h4>

          <el-form label-width="150px" style="width: 600px" size="medium">
            <el-form-item label="Content-Type">
              <el-input v-model="contentType" disabled></el-input>
            </el-form-item>

          </el-form>

          <h4>{{ $t('m.parameters') }}：</h4>

          <div class="json-param-editor" v-show="contentType === CONTENT_TYPE.JSON">
      <textarea
          ref="jsonParamTextarea"
          v-model="jsonParam"
          v-show="false"
      ></textarea>
            <div class="my-codemirror json-param"></div>
            <el-button
                size="small"
                @click="formatJsonParam"
                class="button"
                style="margin-top: 5px;"
            >
              {{ $t('m.json_format') }}
            </el-button>
          </div>
          <el-form label-width="200px" style="width: 650px" size="medium"
                   v-show="contentType === CONTENT_TYPE.FORM_URLENCODED">
            <el-form-item v-for="(item,index) in params" :key="item.id" style="margin-bottom: 5px">
              <template slot="label">
                <data-tag :name="item.name" :type="item.type"></data-tag>
              </template>
              <el-input v-model="item.value" v-if="!item.type.startsWith('Array')" :placeholder="item.note">
                <!--          <template slot="append">{{ item.type }}</template>-->
              </el-input>
              <div v-show="item.type.startsWith('Array')">
                <div v-for="(childItem,childIndex) in item.values" :key="childIndex">
                  <el-input v-model="childItem.va" :placeholder="item.note" style="width: 400px">
                  </el-input>
                  <el-button slot="append" icon="el-icon-delete" type="danger" circle size="mini"
                             @click="deleteRow(index,childIndex)" style="margin-left: 4px;"></el-button>
                </div>

                <el-button icon="el-icon-plus" type="primary" circle size="mini" @click="addRow(index)"></el-button>
              </div>
            </el-form-item>

          </el-form>
          <!--          <el-button @click="request">{{ $t('m.send') }}</el-button>-->
          <el-button
              @click="request"
              :loading="isLoading"
              type="primary"
          >
            {{ $t('m.send') }}
          </el-button>
          <h4>{{ $t('m.result') }}：</h4>

          <el-table :data="tableData" v-show="showTable" size="mini" border stripe max-height="700">
            <el-table-column :prop="item" :label="item" v-for="item in keys" :key="item"></el-table-column>
          </el-table>
          <!-- 使用textarea作为CodeMirror的容器 -->
          <textarea
              ref="codeMirrorTextarea"
              v-model="response"
              v-show="false"
          ></textarea>

          <!-- CodeMirror将渲染到这里 -->
          <div class="my-codemirror" v-show="!showTable"></div>
          <el-button size="small" @click="format" class="button">{{ $t('m.json_format') }}
          </el-button>

        </div>
      </el-tab-pane>
      <el-tab-pane :label="$t('m.request_demo')">
        <call-example ref="callExample" :address="url" :detail="{path,params,access,jsonParam,contentType,token}"/>
      </el-tab-pane>
    </el-tabs>

  </div>

</template>

<script>
import * as dbApi from '@/api/dsApi'
import {CONTENT_TYPE, PRIVILEGE} from "@/constant";
import callExample from "@/components/api/common/callExample";
import CodeMirror from "codemirror";
import 'codemirror/lib/codemirror.css'
import 'codemirror/mode/javascript/javascript.js' // JSON 模式
import 'codemirror/theme/dracula.css' // 主题样式
import 'codemirror/addon/lint/lint.css'
import 'codemirror/addon/lint/lint.js'
import 'codemirror/addon/lint/json-lint.js' // JSON 校验

export default {
  name: "request",
  components: {callExample},
  data() {
    return {
      isLoading: false,
      CONTENT_TYPE: Object.freeze(CONTENT_TYPE),
      PRIVILEGE: Object.freeze(PRIVILEGE),
      api: {},
      params: [],
      path: null,
      access: PRIVILEGE.PUBLIC,
      address: null,
      response: "{}",
      keys: [],
      tableData: [],
      showTable: false,
      token: null,
      url: "",
      contentType: null,
      jsonParam: null,
      tokenUrl: null,
      coder: null, // CodeMirror实例
      cmOptions: {
        tabSize: 2,
        lineNumbers: true,
        mode: "application/json",
        theme: "dracula",
        lineWrapping: true,
        autoRefresh: true, // 解决初始化不显示问题
        styleActiveLine: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        lint: true,
        hintOptions: {
          completeSingle: false
        }
      },
      jsonParamCoder: null,
      // JSON参数编辑器配置
      jsonParamOptions: {
        tabSize: 2,
        lineNumbers: true,
        mode: "application/json",
        theme: "dracula",
        lineWrapping: true,
        autoRefresh: true,
        styleActiveLine: true,
        matchBrackets: true,
        autoCloseBrackets: true,
        lint: true,
        hintOptions: {
          completeSingle: false
        },
        placeholder: this.$t('m.input_json_param') // 提示文本
      }
    };
  },
  mounted() {
    this._initializeCodeMirror();
    // 初始化JSON参数编辑器
    this._initializeJsonParamCodeMirror();
    // 监听contentType变化，动态初始化编辑器
    this.$watch('contentType', (newVal) => {
      if (newVal === CONTENT_TYPE.JSON) {
        this.$nextTick(() => {
          this._initializeJsonParamCodeMirror();
        });
      }
    });
  },
  methods: {
    // 初始化CodeMirror实例
    _initializeCodeMirror() {
      this.coder = CodeMirror.fromTextArea(
          this.$refs.codeMirrorTextarea,
          this.cmOptions
      );
      // 设置尺寸
      this.coder.setSize("100%", "400px");

      // 监听内容变化，同步到response
      this.coder.on("change", (coder) => {
        this.response = coder.getValue();
      });
    },
    // 新增：初始化JSON参数编辑器
    _initializeJsonParamCodeMirror() {
      if (this.$refs.jsonParamTextarea && !this.jsonParamCoder) {
        this.jsonParamCoder = CodeMirror.fromTextArea(
            this.$refs.jsonParamTextarea,
            this.jsonParamOptions
        );
        // 设置尺寸
        this.jsonParamCoder.setSize("100%", "300px");

        // 监听内容变化，同步到jsonParam
        this.jsonParamCoder.on("change", (coder) => {
          this.jsonParam = coder.getValue();
        });

        // 初始值设置
        if (this.jsonParam) {
          this.jsonParamCoder.setValue(this.jsonParam);
        }
      }
    },

    // 新增：格式化JSON参数
    formatJsonParam() {
      try {
        const parsed = JSON.parse(this.jsonParam || '{}');
        const formatted = JSON.stringify(parsed, null, 2);
        this.jsonParam = formatted;
        if (this.jsonParamCoder) {
          this.jsonParamCoder.setValue(formatted);
        }
      } catch (e) {
        this.$message.error('JSON 参数格式错误，无法格式化');
      }
    },
    async getDetail(id) {
      await dbApi.getApiConfigDetail(id)
          .then((response) => {
            console.log(response.data)
            this.path = response.data.path;
            this.access = response.data.access;
            let params = response.data.paramsJson;
            params.forEach((t) => {
              if (t.type.startsWith("Array")) {
                t.values = [{va: ""}];
              }
            });
            this.params = params;
            // this.isSelect = response.data.isSelect;
            let humpIs = response.data.taskJson[0].humpIs;
            if (humpIs) {
              this.url = `http://${this.address}/${this.path}` + "?page=0&limit=5";
            } else {
              this.url = `http://${this.address}/${this.path}`;
            }
            this.contentType = response.data.contentType;
            this.jsonParam = response.data.jsonParam;
            if (this.jsonParam) {
              this.formatJsonParam();
            }
          })
          .catch((error) => {
            this.$message.error("get detail failed");
          });
    },
    async getAddress() {
      await dbApi.getIPPort()
          .then((response) => {
            this.address = response.data;
            this.url = `http://${this.address}/${this.path}`;
          })
          .catch((error) => {
            this.$message.error("get address failed");
          });
    },
    async getIP() {
      await dbApi.getIP()
          .then((response) => {
          })
          .catch((error) => {
            this.$message.error("get ip failed");
          });
    },
    request() {
      this.response = null;
      this.isLoading = true;
      let p = {};
      if (this.contentType === CONTENT_TYPE.FORM_URLENCODED) {
        this.params.forEach((t) => {
          //构造数组类型的请求参数
          if (t.type.startsWith("Array")) {
            const values = t.values.map((item) => item.va);
            p[t.name] = values;
          } else p[t.name] = t.value;
        });
      } else if (this.contentType === CONTENT_TYPE.JSON) {
        try {
          p = this.jsonParam ? JSON.parse(this.jsonParam) : {};
        } catch (e) {
          this.$message.error('JSON 参数格式错误，请检查');
          return;
        }
      }
      // let url = `http://${this.address}/api/${this.path}`
      let axiosPromise = undefined;
      if (this.contentType === CONTENT_TYPE.JSON) {
        axiosPromise = dbApi.postPxyJSON(this.url, p);
      } else {
        axiosPromise = dbApi.postPxyParams(this.url, p);
      }
      axiosPromise
          .then((response) => {
            if (response.status === 200) {
              this.isLoading = false;
              this.showTable = false;
              // 使用CodeMirror设置值
              this.response = JSON.stringify(response.data);
              // 使用CodeMirror设置值
              this.coder.setValue(this.response);
              this.format();
              // this.response = JSON.stringify(response.data);
              // this.format()

            }
          })
          .catch((error) => {
                if (error.status === 404) {
                  this.$message.error("接口不存在");
                } else {
                  this.$message.error(error.response?.data?.msg || "请求失败");
                }
                this.isLoading = false;
              }
          );
    },
    format() {
      try {
        const parsed = JSON.parse(this.response || '{}');
        const formatted = JSON.stringify(parsed, null, 2);
        this.response = formatted;
        this.coder.setValue(formatted);
      } catch (e) {
        this.$message.error('JSON 格式错误，无法格式化');
      }
    },
    tableShow() {
      if (this.response == null) return;
      let obj = JSON.parse(this.response);
      if (obj.success) {
        this.tableData = obj.data;
        if (obj.data.length > 0) {
          this.keys = Object.keys(obj.data[0]);
        } else {
          return;
        }
      } else {
        return;
      }
      this.showTable = true;
    },
    tableHide() {
      this.showTable = false;
    },
    addRow(index) {
      this.params[index].values.push({va: null});
    },
    deleteRow(index, childIndex) {
      this.params[index].values.splice(childIndex, 1);
    },
    handleTabClick(tab) {
      const label = tab.label;
      if (label === this.$t('m.request_demo')) {
        this.$nextTick(() => {
          this.$refs.callExample.refresh();
        });
      }
    },
  },
  created() {
    this.getDetail(this.$route.query.id);
    this.getAddress();
    this.getIP()
  },
  computed: {
    // 判断是否显示返回按钮
    showBackButton() {
      try {
        // 方法1: 适用于大多数Vue Router版本
        if (window.history && window.history.length > 1) {
          return true;
        }

        // 方法2: 兼容Vue Router的history模式
        if (this.$router && this.$router.history && this.$router.history.index > 0) {
          return true;
        }

        // 方法3: 兼容哈希模式路由
        if (this.$router && this.$router.app._history && this.$router.app._history.index > 0) {
          return true;
        }

        return false;
      } catch (error) {
        // 出错时默认不显示
        return false;
      }
    }
  },
  // 监听路由变化，确保按钮状态实时更新
  watch: {
    $route() {
      // 路由变化时重新计算显示状态
      this.showBackButton;
    },
    jsonParam(newVal) {
      if (this.jsonParamCoder && newVal !== this.jsonParamCoder.getValue()) {
        this.jsonParamCoder.setValue(newVal || '');
      }
    },
  }
};
</script>

<style scoped lang="less">
.json-param-editor {
  margin-bottom: 5px;
}

.my-codemirror.json-param {
  margin-bottom: 5px;

  /deep/ .CodeMirror {
    min-height: 300px;
    max-height: 300px;
  }
}

/* CodeMirror 样式调整 */
.my-codemirror {
  margin-top: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;

  /deep/ .CodeMirror {
    min-height: 200px;
    max-height: 500px;
    font-family: "Consolas", "Monaco", monospace;
    font-size: 14px;
  }

  /deep/ .CodeMirror-lint-markers {
    width: 24px;
  }
}

.my > .el-textarea__inner {
  font-family: "Consolas", Helvetica, Arial, sans-serif;
  /*font-size: 18px;*/
}

h2 {
  margin-bottom: 25px;
  text-align: center;
}

h4 {
  margin: 10px 0;
}

.path {
  font-size: 16px;
  border: 1px #ccc solid;
  padding: 10px 10px;
}

.url {
  padding: 5px;
  border: 1px solid #ccc;

  .input {
    background-color: rgba(1, 87, 36, 0.06);
    font-size: 16px;
    padding: 5px;
    border-width: 0px;
    outline: none;
  }
}

.button {
  margin: 10px 10px 10px 0;
}

.textarea {
  /deep/ .el-textarea__inner {
    font-family: "Consolas", Helvetica, Arial, sans-serif !important;
    font-size: 16px !important;
    line-height: 20px;
  }
}
</style>
