<template>
  <div>
    <el-tabs tab-position="left">
      <el-tab-pane :label="$t('m.basic')">
        <el-form label-width="160px">
          <el-form-item :label="$t('m.name')">
            <el-input v-model="detail.name" style="max-width:600px"></el-input>
          </el-form-item>
          <el-form-item :label="$t('m.path')">
            <el-input v-model="detail.path" style="max-width:600px">
              <template slot="prepend">http://{{ address }}/</template>
            </el-input>
          </el-form-item>
          <el-form-item :label="$t('m.api_group')">
            <el-select v-model="detail.groupId" style="width:300px" disabled>
              <el-option :value="item.id" v-for="item in groups" :label="item.name">{{ item.name }}</el-option>
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('m.note')">
            <el-input v-model="detail.note" style="max-width:600px"></el-input>
          </el-form-item>
          <el-form-item label="Content Type">
            <div slot="label">
              <label-tip label="Content Type" :tip="$t('m.content_type_info')"></label-tip>
            </div>
            <el-select v-model="detail.contentType" style="width:300px">
              <el-option v-for="item in types" :label="item" :value="item"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('m.parameters')">
            <div slot="label">
              <span v-show="detail.contentType === CONTENT_TYPE.FORM_URLENCODED">{{ $t('m.request_params') }}</span>
              <span v-show="detail.contentType === CONTENT_TYPE.JSON">{{ $t('m.request_param_demo') }}</span>
            </div>
            <div v-show="detail.contentType === CONTENT_TYPE.FORM_URLENCODED">

              <el-table :data="detail.paramsJson" border stripe max-height="700" size="mini"
                        :empty-text="$t('m.no_param')">
                <el-table-column prop="name" :label="$t('m.name')" width="220px">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.name"></el-input>
                  </template>
                </el-table-column>
                <el-table-column :label="$t('m.type')" width="220px">
                  <template slot-scope="scope">
                    <el-select v-model="scope.row.type" :options="options">
                      <el-option v-for="item in options" :key="item.value" :label="item.label"
                                 :value="item.value"></el-option>
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column :label="$t('m.description')" width="300px">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.note"></el-input>
                  </template>
                </el-table-column>

                <el-table-column :label="$t('m.operation')" width="100px">
                  <template slot-scope="scope">
                    <el-button @click="deleteRow(scope.$index)" circle type="danger" icon="el-icon-delete"
                               size="mini"></el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-button @click="addRow" icon="el-icon-plus" type="primary" circle size="mini"></el-button>
            </div>
            <div v-show="detail.contentType === CONTENT_TYPE.JSON" class="json-param-editor">
              <!-- 隐藏的textarea作为CodeMirror的数据源 -->
              <textarea
                  ref="jsonParamTextarea"
                  v-model="detail.jsonParam"
                  v-show="false"
              ></textarea>

              <!-- CodeMirror编辑器将渲染到这里 -->
              <div class="my-codemirror json-param"></div>

              <!-- 格式化按钮 -->
              <div class="editor-actions">
                <el-button
                    size="small"
                    @click="formatJsonParam"
                    type="primary"
                    icon="el-icon-refresh"
                >
                  {{ $t('m.json_format') }}
                </el-button>
                <el-tooltip placement="top-start" effect="dark">
                  <div slot="content">{{ $t('m.app_json_tip') }}</div>
                  <i class="el-icon-info tip"></i>
                </el-tooltip>
              </div>
            </div>
          </el-form-item>

          <el-form-item>

          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane :label="$t('m.executor')">
        <div v-for="(item,index) in detail.taskJson">
          <sql-executor v-if="item.taskType === 1" ref="executor" :detail="item" :thisApiInfo="detail"></sql-executor>
        </div>
      </el-tab-pane>


    </el-tabs>
  </div>
</template>

<script>
import sqlCode from "@/components/api/common/SqlCode";
import SqlExecutor from "@/components/api/executor/SqlExecutor";
import {CONTENT_TYPE, DATA_TYPE, EXECUTOR_TYPE, PLUGIN_TYPE, PRIVILEGE} from "@/constant";
import * as dbApi from '@/api/dsApi'
// 导入CodeMirror相关资源
import CodeMirror from "codemirror";
import 'codemirror/lib/codemirror.css'
import 'codemirror/mode/javascript/javascript.js' // JSON模式
import 'codemirror/theme/dracula.css' // 主题
import 'codemirror/addon/lint/lint.css'
import 'codemirror/addon/lint/lint.js'
import 'codemirror/addon/lint/json-lint.js' // JSON校验

export default {
  components: {sqlCode, SqlExecutor},
  data() {
    return {
      CONTENT_TYPE: Object.freeze(CONTENT_TYPE),
      PRIVILEGE: Object.freeze(PRIVILEGE),
      address: null,
      detail: {
        name: null,
        note: null,
        path: null,
        paramsJson: [],
        groupId: null,
        access: PRIVILEGE.PUBLIC, //访问权限
        cachePlugin: {pluginName: null, pluginParam: null, pluginType: PLUGIN_TYPE.CACHE_PLUGIN, apiId: null},
        alarmPlugins: [{pluginName: null, pluginParam: null, pluginType: PLUGIN_TYPE.ALARM_PLUGIN, apiId: null}],
        globalTransformPlugin: {
          pluginName: null,
          pluginParam: null,
          pluginType: PLUGIN_TYPE.GLOBALTRANSFORM_PLUGIN,
          apiId: null
        },
        taskJson: [{
          taskType: EXECUTOR_TYPE.SQL_EXECUTOR,
          sqlList: [{sqlText: "--xxxxxx", transformPlugin: null, transformPluginParam: null}],
          transaction: 0,
          datasourceId: null
        }],
        jsonParam: null,
        contentType: CONTENT_TYPE.FORM_URLENCODED,
      },
      options: [
        {label: "string", value: DATA_TYPE.STRING},
        {label: "bigint", value: DATA_TYPE.BIGINT},
        {label: "double", value: DATA_TYPE.DOUBLE},
        {label: "date", value: DATA_TYPE.DATE},
        {label: "Array<string>", value: DATA_TYPE.ARRAY_STRING},
        {label: "Array<bigint>", value: DATA_TYPE.ARRAY_BIGINT},
        {label: "Array<double>", value: DATA_TYPE.ARRAY_DOUBLE},
        {label: "Array<date>", value: DATA_TYPE.ARRAY_DATE},
      ],
      table: null,
      tables: [],
      columns: [],
      column: null,

      groups: [],
      types: [CONTENT_TYPE.FORM_URLENCODED, CONTENT_TYPE.JSON],
      cachePlugins: [],
      transformPlugins: [],
      alarmPlugins: [],
      globalTransformPlugins: [],
      // 新增CodeMirror实例和配置
      jsonParamCoder: null,
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
        placeholder: this.$t('m.param_demo_placeholder')
      }
    };
  },
  props: ["id", "groupId"],
  watch: {
    // 监听contentType变化，动态初始化编辑器
    'detail.contentType'(newVal) {
      if (newVal === CONTENT_TYPE.JSON) {
        this.$nextTick(() => {
          this._initializeJsonParamCodeMirror();
        });
      }
    },
    // 监听jsonParam变化，同步到编辑器
    'detail.jsonParam'(newVal) {
      if (this.jsonParamCoder && newVal !== this.jsonParamCoder.getValue()) {
        this.jsonParamCoder.setValue(newVal || '');
      }
    }
  },
  methods: {
    _initializeJsonParamCodeMirror() {
      // 确保元素已存在且编辑器未初始化
      if (this.$refs.jsonParamTextarea && !this.jsonParamCoder) {
        this.jsonParamCoder = CodeMirror.fromTextArea(
            this.$refs.jsonParamTextarea,
            this.jsonParamOptions
        );
        // 设置编辑器尺寸
        this.jsonParamCoder.setSize("100%", "300px");

        // 监听内容变化，同步到数据模型
        this.jsonParamCoder.on("change", (coder) => {
          this.detail.jsonParam = coder.getValue();
        });

        // 初始化值
        if (this.detail.jsonParam) {
          this.jsonParamCoder.setValue(this.detail.jsonParam);
        }
      }
    },

    // 新增：格式化JSON参数
    formatJsonParam() {
      try {
        // 处理空值情况
        const jsonValue = this.detail.jsonParam || '{}';
        const parsed = JSON.parse(jsonValue);
        const formatted = JSON.stringify(parsed, null, 1);
        // 更新数据模型和编辑器
        this.detail.jsonParam = formatted;
        if (this.jsonParamCoder) {
          this.jsonParamCoder.setValue(formatted);
        }
      } catch (e) {
        this.$message.error('JSON 格式错误，无法格式化');
        console.error('JSON格式化错误:', e);
      }
    },
    isNull(item) {
      if (typeof item == 'undefined' || item == null || item == '') {
        return true
      } else {
        return false
      }
    },
    // 检查必填项
    checkValue() {
      if (this.isNull(this.detail.name)) {
        this.$message.warning("API name empty!")
        return false
      }
      if (this.isNull(this.detail.path)) {
        this.$message.warning("API path empty!")
        return false
      }
      if (this.isNull(this.detail.groupId)) {
        this.$message.warning("API group empty!")
        return false
      }
      if (this.detail.contentType == CONTENT_TYPE.FORM_URLENCODED) {
        for (let o of this.detail.paramsJson) {
          if (this.isNull(o.name)) {
            this.$message.warning("Request parameter name empty!")
            return false;
          }
          if (this.isNull(o.type)) {
            this.$message.warning("Request parameter type empty!")
            return false;
          }
        }
      }

      //检查执行器中的必填项
      for (let e of this.$refs.executor) {
        if (!e.check()) {
          return false;
        }
      }

      return true;
    },
    addAlarmRow() {
      this.detail.alarmPlugins.push({
        pluginName: null,
        pluginParam: null,
        pluginType: PLUGIN_TYPE.ALARM_PLUGIN,
        apiId: this.id
      })
    },
    addRow() {
      this.detail.paramsJson.push({name: null, type: null, note: null});
    },
    deleteRow(index) {
      this.detail.paramsJson.splice(index, 1);
    },

    getAddress() {
      dbApi.getIPPort()

          .then((response) => {
            this.address = response.data;
          })
          .catch((error) => {
            // this.$message.error("失败")
          });
    },
    getDetail(id) {
      dbApi.getApiConfigDetail(id)
          .then((response) => {
            this.detail = response.data
            // 防止前端报错
            if (response.data.cachePlugin == null) {
              this.detail.cachePlugin = {
                pluginName: null,
                pluginParam: null,
                pluginType: PLUGIN_TYPE.CACHE_PLUGIN,
                apiId: id
              }
            }
            if (response.data.globalTransformPlugin == null) {
              this.detail.globalTransformPlugin = {
                pluginName: null,
                pluginParam: null,
                pluginType: PLUGIN_TYPE.GLOBALTRANSFORM_PLUGIN,
                apiId: id
              }
            }

            if (response.data.alarmPlugins == null || response.data.alarmPlugins.length == 0) {
              this.detail.alarmPlugins = [{
                pluginName: null,
                pluginParam: null,
                pluginType: PLUGIN_TYPE.ALARM_PLUGIN,
                apiId: id
              }];
            }

            console.log(this.detail)
            // 数据加载完成后更新编辑器内容
            this.$nextTick(() => {
              if (this.jsonParamCoder && this.detail.jsonParam) {
                this.jsonParamCoder.setValue(this.detail.jsonParam);
              }
            });
          });
    },

    getAllGroups() {
      dbApi.getAllGroups()
          .then((response) => {
            this.groups = response.data;
          })
          .catch((error) => {
          });
    },
    getAllPlugin() {
      // this.axios
      //     .post("/plugin/all")
      //     .then((response) => {
      //       this.cachePlugins = response.data.cache;
      //       this.transformPlugins = response.data.transform;
      //       this.alarmPlugins = response.data.alarm;
      //       this.globalTransformPlugins = response.data.globalTransform;
      //     })
      //     .catch((error) => {
      //     });
    },
  },
  mounted() {
    this.getAddress();
    //编辑页面
    if (this.id !== undefined) {
      this.getDetail(this.id);
    }
    // 新增页面
    else {
      //从侧边栏分组上点击的创建API按钮，会默认选中对应的分组
      this.detail.groupId = this.groupId
    }
    this.getAllGroups();
    this.getAllPlugin();
    // 初始化为JSON类型时，初始化编辑器
    if (this.detail.contentType === CONTENT_TYPE.JSON) {
      this.$nextTick(() => {
        this._initializeJsonParamCodeMirror();
      });
    }
  }

};
</script>

<style scoped lang="less">
// 新增CodeMirror相关样式
.json-param-editor {
  margin-bottom: 10px;
}

.my-codemirror.json-param {
  margin-bottom: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;

  /deep/ .CodeMirror {
    min-height: 300px;
    max-height: 300px;
    font-family: "Consolas", "Monaco", monospace;
    font-size: 14px;
  }

  /deep/ .CodeMirror-lint-markers {
    width: 24px;
  }
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tip {
  color: #111111;
  font-size: 14px;
  font-weight: 100;
  cursor: pointer;
}

.my > .el-textarea__inner {
  font-family: "Consolas", Helvetica, Arial, sans-serif;
  /*font-size: 18px;*/
}

.mydialog > .el-dialog {
  margin-top: 20px !important;
  margin-bottom: 0px !important;
}

i {
  color: #0698a5;
  font-size: 18px;
  font-weight: 700;
  margin-right: 5px;
}

.tip {
  /*display: inline-block !important;*/
  // margin-left: 2px;
  /*background-color: #fdf6ec;*/
  /*padding: 15px;*/
  color: #111111;
  font-size: 14px;
  font-weight: 100;
}

a {
  font-size: 16px;
  color: #afafaf;
  margin: 0 5px;

  &:hover {
    color: #000000;
  }
}

.textarea {
  /deep/ .el-textarea__inner {
    font-family: "Consolas", Helvetica, Arial, sans-serif !important;
    font-size: 16px !important;
    line-height: 20px;
  }
}

.label {
  margin: 0 5px 0 20px !important;
  font-weight: 700;
}
</style>
