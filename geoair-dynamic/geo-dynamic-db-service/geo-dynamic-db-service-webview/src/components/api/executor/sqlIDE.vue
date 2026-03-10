<template>
  <div :class="['cm_root', isFullScreen?'full':'']">
    <div class="cm">
      <div class="tool">
        <div>
          <span class="button2 iconfont icon-full" @click="isFullScreen = true;maximize()" v-if="!isFullScreen"></span>
          <span class="button2 iconfont icon-mini" @click="isFullScreen = false;minimize()" v-if="isFullScreen"></span>
          <span class="button iconfont icon-play" v-if="isFullScreen" @click="runSql(false)">{{
              $t('m.run_sql')
            }}</span>
          <span class="button iconfont icon-play" v-if="isFullScreen" @click="runSql(true)">{{
              $t('m.run_selected_sql')
            }}</span>
          <span class="button iconfont icon-play" v-if="isFullScreen" @click="parseSql">{{ $t('m.parse_sql') }}</span>
          <span class="button iconfont icon-play" v-if="isFullScreen" @click="formatSql">{{ $t('m.format') }}</span>
        </div>
        <div>
          <span class="button " @click="formatSql">{{ $t('m.format') }}</span>
          <span class="button" @click="selectTag">&lt;select></span>
          <span class="button" @click="geomAsTextTag">&lt;geomAsText></span>
          <span class="button" @click="geomAsGeomJsonTag">&lt;geomAsGeomJson></span>
          <span class="button" @click="ifTag">&lt;if></span>
          <span class="button" @click="foreachTag">&lt;foreach></span>
          <span class="button" @click="whereTag">&lt;where></span>
          <span class="button" @click="trimTag">&lt;trim></span>
          <span class="button" @click="likeTag">&lt;模糊></span>
          <span class="button" @click="equalTag">&lt;相等></span>
          <span class="button" @click="betweenTag">&lt;时间范围></span>
          <span class="button" @click="containsTag">&lt;空间包含></span>

        </div>
      </div>
      <div style="display: flex;justify-content: space-between;">
        <div style="width: 100vw;">
          <textarea :ref="textareaRef" v-model="code"></textarea>
        </div>
        <div v-if="isFullScreen" style="width: 30%;height:600px;padding: 5px;border: 1px solid #000000;">
          <label-tip :label="$t('m.sql_param')" :tip="$t('m.sql_param_tip')"></label-tip>
          <span class="button" @click="formatSqlParam" style="padding: 2px 8px; font-size: 12px;">
              {{ $t('m.format') }}
            </span>
          <span class="button" @click="syncSqlParam" style="padding: 2px 8px; font-size: 12px;">
            同步到API参数
            </span>
          <textarea :ref="paramTextareaRef"></textarea>
          <!--          <el-input type="textarea" rows="15" v-model="sqlParam"-->
          <!--                    style="font-size: 20px;font-family: Consolas, Helvetica, Arial, sans-serif"></el-input>-->
        </div>
      </div>
      <div class="result" v-if="isFullScreen" style="padding: 10px">
        <div v-if="error != null" class="error"><i class="el-icon-error"></i>{{ error }}</div>
        <div v-if="updateMsg != null" class="updateMsg"><i class="el-icon-success"></i>{{ updateMsg }}</div>
        <div v-if="sqlMeta != null" class="sqlMeta">
          <div style="color: #cc7832">SQL:</div>
          <div class="sql">{{ sqlMeta.sql }}</div>
          <div style="color: #cc7832">Parameter:</div>
          <div class="sql">{{ sqlMeta.jdbcParamValues }}</div>
        </div>
        <div class="table">
          <el-table :data="resultList" border stripe style="width: 100%;background-color:#c53939;"
                    v-if="resultList != null && resultList.length > 0" size="mini">
            <el-table-column :prop="item" :label="item" v-for="item in Object.keys(resultList[0])"></el-table-column>
          </el-table>
          <div v-if="resultList != null && resultList.length == 0">No Result</div>
        </div>
      </div>
      <!--    <el-button @click="show">show</el-button>-->
    </div>
  </div>
</template>

<script>
import CodeMirror from "codemirror";

import "codemirror/lib/codemirror.css";

import "codemirror/theme/solarized.css";
import "codemirror/theme/idea.css";
import "codemirror/theme/darcula.css";

import "codemirror/addon/hint/show-hint.css";
import "codemirror/addon/hint/show-hint.js";
import "codemirror/addon/hint/sql-hint.js";

import "codemirror/mode/sql/sql.js";
import {format} from "sql-formatter";
import LabelTip from "@/components/common/LabelTip.vue";
import * as dbApi from '@/api/dsApi'
import {CONTENT_TYPE} from "@/constant";

export default {
  name: "sqlIDE",
  components: {LabelTip},
  props: {
    value: String,
    thisApiInfo: {
      type: Object,
      default: undefined,
    },
    ds: String,
    textareaRef: {
      type: String,
      default: "",
    }
  },
  data() {
    return {
      sqlParam: "{}",
      paramTextareaRef: "paramTextarea", // 参数编辑器ref
      resultList: null,
      error: null,
      updateMsg: null,
      sqlMeta: null,
      isFullScreen: false,
      CodeMirror: null,
      code: "",//不能null，否则报错
      coder: null,
      options: {
        tabSize: 2,
        lineNumbers: true,
        line: true,
        mode: "text/x-mysql",
        theme: "darcula",
        readOnly: false,
        lineWrapping: false,
        autofocus: true,
        autoRefresh: true, //很重要，否则编辑API页面初始化加载不显示
        styleActiveLine: true,
        lint: true, // 代码出错提醒
        matchBrackets: true,
        extraKeys: {Tab: "autocomplete"}, //Tab可以弹出选择项
        hintOptions: {
          completeSingle: false,
          alignWithWord: false,
          hint: CodeMirror.hint.sql,
        },
      },
      // JSON参数编辑器配置
      paramOptions: {
        tabSize: 2,
        lineNumbers: true,
        mode: "application/json", // JSON模式
        theme: "darcula", // 保持与SQL编辑器相同的主题
        readOnly: false,
        lineWrapping: true,
        autoRefresh: true,
        styleActiveLine: true,
        matchBrackets: true,
        lint: true, // 启用JSON语法校验
        extraKeys: {
          "Ctrl-Space": "autocomplete", // JSON提示快捷键
          "Tab": (cm) => {
            // 自定义Tab行为：插入两个空格代替制表符
            const spaces = Array(cm.getOption("indentUnit") + 1).join(" ");
            cm.replaceSelection(spaces);
          }
        },
        hintOptions: {
          completeSingle: false
        }
      }
    };
  },
  mounted() {
    this._initialize();
    // 监听全屏状态变化，初始化参数编辑器
    this.$watch('isFullScreen', (newVal) => {

      this.initSqlParam()
      this._initParamEditor();
      // if (newVal && !this.paramCoder) {
      //
      // }else{
      //
      // }
    });
  },
  methods: {

    initSqlParam() {
      // 如果detail存在且包含相关参数，则初始化sqlParam
      if (this.thisApiInfo && typeof this.thisApiInfo === 'object') {
        console.log("initSqlParamApiInfo:", this.thisApiInfo)
        if (this.thisApiInfo.contentType === CONTENT_TYPE.FORM_URLENCODED) {
          let paramsJson = this.thisApiInfo.paramsJson;
          if (!Array.isArray(paramsJson)) {
            this.sqlParam = "{}";
          } else {
            let t = paramsJson.reduce((acc, item) => {
              if (item && typeof item === 'object' && 'name' in item) {
                // 如果需要去重，可以添加判断: if (!acc.hasOwnProperty(item.name))
                acc[item.name] = "";
              }
              return acc;
            }, {});
            console.log("t:", t)
            this.sqlParam = JSON.stringify(t);
          }
        } else {
          this.sqlParam = this.thisApiInfo.jsonParam;
        }
      } else {
        // 默认值
        this.sqlParam = "{}";
      }
      this.formatSqlParam()
    },
    // 初始化参数编辑器
    _initParamEditor() {
      this.paramCoder = CodeMirror.fromTextArea(
          this.$refs[this.paramTextareaRef],
          this.paramOptions
      );
      this.paramCoder.setSize("100%", "520px"); // 适应容器高度
      this.paramCoder.setValue(this.sqlParam);

      // 监听参数变化，同步到data
      this.paramCoder.on("change", (coder) => {
        this.sqlParam = coder.getValue();
      });
    },
    formatSqlParam() {
      try {
        const parsed = JSON.parse(this.sqlParam || '{}');
        const formatted = JSON.stringify(parsed, null, 2);
        this.sqlParam = formatted;
        if (this.paramCoder) {
          this.paramCoder.setValue(formatted);
        }
      } catch (e) {
        this.$message.error("JSON格式化异常");
        console.error('JSON格式化错误:', e);
      }
    },
    syncSqlParam() {
      if (this.thisApiInfo && typeof this.thisApiInfo === 'object') {
        if (this.thisApiInfo.contentType === CONTENT_TYPE.FORM_URLENCODED) {
          let sqlParam = this.sqlParam;
          let parse = JSON.parse(sqlParam);
          // 转换为目标格式的数组
          let hasChange = false;
          Object.keys(parse).forEach(item => {
            const newItem = {
              name: item,
              type: "string",
              note: item
            }
            const exists = this.thisApiInfo.paramsJson.find(
                param => param.name === newItem.name
            );
            // 如果不存在则添加
            if (!exists) {
              hasChange = true
              this.thisApiInfo.paramsJson.push(newItem);
            }
          });
          if (hasChange) {
            this.thisApiInfo.params = JSON.stringify(this.thisApiInfo.paramsJson)
          }
        } else {
          this.thisApiInfo.jsonParam = this.sqlParam;
        }
        console.log("syncSqlParam:", this.thisApiInfo)
        this.$message.success("同步参数成功");
      }
    },
    parseSql() {
      this.resultList = null
      this.updateMsg = null
      this.error = null
      this.sqlMeta = null
      dbApi.parseDynamicSql(this.coder.getValue(), this.sqlParam)
          .then((response) => {
            if (response.data.success) {
              this.sqlMeta = response.data.data

            } else {
              this.error = response.data.msg
            }
          }).catch((error) => {
      })
    },
    formatSql() {

      const sql = format(this.coder.getValue())
          .replace(/# /g, "#")
          .replace(/{ /g, "{")
          .replace(/ }/g, "}")
          .replace(/< foreach/g, "\n<foreach\n")
          .replace(/< \/ foreach >/g, "\n</foreach>\n")
          .replace(/< if/g, "\n<if")
          .replace(/< \/ if >/g, "\n</if>\n")
          .replace(/<\nwhere\n  >/g, "\n<where>\n")
          .replace(/< \/\nwhere\n  >/g, "\n</where>\n")
          .replace(/< trim/g, "\n<trim")
          .replace(/< \/ trim >/g, "\n</trim>\n");
      this.coder.setValue(sql);
      this.coder.closeHint();
    },

    runSql(selected) {
      if (this.ds == null) {
        this.$message.error("Please select datasource")
        return
      }
      let sql
      if (selected) {
        sql = this.coder.getSelection()
      } else {
        sql = this.coder.getValue()
      }

      if (sql == null || sql.trim() == '') {
        this.$message.error("Please input sql")
        return
      }
      this.resultList = null
      this.updateMsg = null
      this.error = null
      this.sqlMeta = null

      dbApi.executeSqlV2(sql, this.ds, this.sqlParam)
          .then((response) => {
            if (response.data.success) {
              if (Array.isArray(response.data.data)) {
                this.resultList = response.data.data

              } else {
                this.updateMsg = response.data.data
              }

            } else {
              this.error = response.data.msg
            }
          }).catch((error) => {
        this.$message.error(error)
      })
    },
    maximize() {
      this.coder.setSize("100%", "600px");
    },
    minimize() {
      this.coder.setSize("100%", "400px");
    },
    foreachTag() {
      this.coder.setValue(this.coder.getValue() + `\n<foreach open="(" close=")" collection="" separator="," item="item" index="index"> \n #{item} \n </foreach>`)
      this.coder.closeHint();
    },
    ifTag() {
      this.coder.setValue(this.coder.getValue() + `\n<if test="" >\n</if>`)
      this.coder.closeHint();
    },
    whereTag() {
      this.coder.setValue(this.coder.getValue() + `\n<where>\n</where>`)
      this.coder.closeHint();
    },
    trimTag() {
      this.coder.setValue(this.coder.getValue() + `\n<trim prefix="" suffix="" suffixesToOverride="" prefixesToOverride=""></trim>`)
      this.coder.closeHint();
    },
    likeTag() {
      this.coder.setValue(this.coder.getValue() + `\n<if test="queryContent != null and queryContent != ''"> \n AND  name LIKE CONCAT('%', #{queryContent}, '%') \n</if>`)
      this.coder.closeHint();
      let parse = JSON.parse(this.sqlParam);
      if (!parse.hasOwnProperty("queryContent")) {
        parse.queryContent = '';
      }
      this.sqlParam = JSON.stringify(parse);
      this.formatSqlParam();
    },
    equalTag() {
      this.coder.setValue(this.coder.getValue() +
          `\n<if test="fieldValue != null and fieldValue != ''">\n AND  fieldName  = #{fieldValue} \n</if>`)
      this.coder.closeHint();
      let parse = JSON.parse(this.sqlParam);
      if (!parse.hasOwnProperty("fieldValue")) {
        parse.fieldValue = '';
      }
      this.sqlParam = JSON.stringify(parse);
      this.formatSqlParam();
    },
    selectTag() {
      this.coder.setValue(this.coder.getValue() +
          `select *
           from tableName`)
    },
    geomAsTextTag() {
      this.coder.setValue(this.coder.getValue() +
          `\n ST_AsText(geom) as wkt_geom`)
    },
    geomAsGeomJsonTag() {
      this.coder.setValue(this.coder.getValue() +
          `\n ST_AsGeojson(geom) as geojson_geom`)
    },
    betweenTag() {
      this.coder.setValue(this.coder.getValue() +
          `\n<if test="timeGt != null and timeGt != '' and timeLt != null and timeLt != ''"> \n AND  timeFieldName  between \n  CAST(#{timeGt} AS timestamp)  \n AND \n CAST(#{timeLt} AS timestamp) \n  </if>`)
      this.coder.closeHint();
      let parse = JSON.parse(this.sqlParam);
      if (!parse.hasOwnProperty("timeGt")) {
        parse.timeGt = '1999-12-13 01:00:00';
      }
      if (!parse.hasOwnProperty("timeLt")) {
        parse.timeLt = '2025-12-13 01:00:00';
      }
      this.sqlParam = JSON.stringify(parse);
      this.formatSqlParam();
    },
    containsTag() {
      this.coder.setValue(this.coder.getValue() +
          `\n<if test="wkPolygonGeom != null and wkPolygonGeom != '' and wkPolygonGeom != null and wkPolygonGeom != ''"> \n  and st_contains( ST_GeomFromText(#{wkPolygonGeom}, 4326), the_geom)\n </if>`)
      this.coder.closeHint();
      let parse = JSON.parse(this.sqlParam);
      if (!parse.hasOwnProperty("wkPolygonGeom")) {
        parse.wkPolygonGeom = 'POLYGON((87.22 43.531,87.2249 44.0393,88.1021 44.039,88.10215 43.5308,87.22 43.531))';
      }
      this.sqlParam = JSON.stringify(parse);
      this.formatSqlParam();
    },

    _initialize() {
      //
      this.coder = CodeMirror.fromTextArea(
          this.$refs[this.textareaRef],
          this.options
      );
      this.coder.setSize("100%", "400px");
      // alert(this.value)
      this.coder.setValue(this.value || this.code);

      this.coder.on("change", (coder, changeObj) => {
        this.code = coder.getValue();
        if (/^[a-zA-Z.]/.test(changeObj.text[0])) {
          this.coder.showHint();
        }
      });

    },
  },
  watch: {
    //编辑api页面初次渲染的时候还获取不到props值，是先生成本组件再从父组件注入props值，所以要监听
    value: function (newVal, oldVal) {
      // console.log("监听到value :" + newVal)
      //
      this.code = newVal
      this.coder.setValue(newVal);
      //编辑页面初次渲染注入value值会触发codemirror change事件，导致页面显示代码提示框，应该关闭
      this.coder.closeHint();

    },
    // 监听detail变化，及时更新sqlParam
    thisApiInfo: {
      handler() {
        this.initSqlParam();
      },
      immediate: true // 确保初始化时也会执行
    }
  }
};
</script>

<style lang="less">
.full {
  padding: 10px;
  background-color: #3d4042;
  z-index: 10;
  position: fixed;
  top: 0px;
  left: 0px;
  width: calc(100vw);
  height: calc(100vh);
}

.cm_root {
  color: #A9B7C6;

  .cm {
    //position: fixed;
    //top: 10px;
    z-index: 1000;
    //flex-grow: 1;
    //display: flex;
    //position: relative;
    .tool {
      margin-bottom: -8px;
      display: flex;
      justify-content: space-between;

      .button {
        cursor: pointer;
        font-size: 14px;
        background-color: #2B2B2B;
        border: 1px solid #000000;
        //background-image: linear-gradient(45deg, , #40556e);

        padding: 3px 10px;
        margin: 0 5px;
        border-radius: 4px;


        &:hover {
          background-color: #1c1e1e;
          //font-weight: bold;
        }
      }

      .button2 {
        cursor: pointer;
        color: #A9B7C6;
        //background-color: #6386b0;
        //background-image: linear-gradient(45deg, , #40556e);

        padding: 3px 3px;
        margin: 0 5px;
        border-radius: 3px;
        font-size: 24px;
        font-weight: bold;
        //color: #017301;

        &:hover {
          background-color: rgba(90, 122, 161, 0.34);
          font-weight: bold;
        }
      }

    }

    .result {
      //background-color: #e06666;
    }

    .CodeMirror {
      flex-grow: 1;
      z-index: 1;
      width: 100%;
      height: auto;
    }

    .CodeMirror-code {
      line-height: 22px;
      font-size: 17px;
    }

    .code-mode-select {
      position: absolute;
      z-index: 2;
      right: 10px;
      top: 10px;
      max-width: 130px;
    }
  }
}


</style>


