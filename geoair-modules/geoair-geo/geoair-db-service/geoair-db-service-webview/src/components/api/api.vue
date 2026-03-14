<template>

  <div class="api_root">
    <div class="group">
      <api-tree></api-tree>
    </div>
    <div class="api">
      <div class="tool">
        <div class="left">
          <div class="search">
            <div>
              <span class="label">{{ $t('m.api_group') }}</span>
              <el-select v-model="groupId" placeholder="" style="width:140px;" size="mini" clearable>
                <el-option v-for="(item,index) in groups" :label="item.name" :value="item.id" :key="index"></el-option>
              </el-select>
              <span class="label">{{ $t('m.name') }}</span>
              <el-input v-model="keyword.name" style="width:150px;" clearable size="mini"></el-input>
              <span class="label">{{ $t('m.note') }}</span>
              <el-input v-model="keyword.note" style="width:150px;" clearable size="mini"></el-input>
              <span class="label">{{ $t('m.path') }}</span>
              <el-input v-model="keyword.path" style="width:200px;" clearable size="mini">
                <template slot="prepend">/{{ context }}/</template>
              </el-input>
              <el-button type="primary" icon="el-icon-search" @click="search" plain size="mini">{{
                  $t('m.search')
                }}
              </el-button>
            </div>

          </div>
        </div>
        <div class="right">
          <el-dropdown @command="" style="margin-right: 15px">
            <span class="el-dropdown-link" style="line-height: 30px">
              {{ $t('m.tool') }}<i class="el-icon-arrow-down"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="item">
                <span class="iconfont icon-document" @click="dialogVisible2=true">{{ $t('m.export_api_doc') }}</span>
              </el-dropdown-item>
              <el-dropdown-item command="item">
                <span class="el-icon-download" @click="dialogVisible3=true">{{ $t('m.export_api') }}</span>
              </el-dropdown-item>
              <el-dropdown-item command="item">
                <el-upload action="/apiConfig/import" accept=".json" :on-success="importSuccess" :headers="headers"
                           :on-error="importFail" :file-list="fileList">
                  <span class="el-icon-upload2">{{ $t('m.import_api') }}</span>
                </el-upload>
              </el-dropdown-item>
              <el-dropdown-item command="item">
                <span class="iconfont icon-group" @click="dialogVisible4=true">{{ $t('m.export_api_groups') }}</span>
              </el-dropdown-item>

              <el-dropdown-item command="item">
                <el-upload action="/apiConfig/importGroup" accept=".json" :on-success="importGroupSuccess"
                           :headers="headers" :on-error="importFail" :file-list="groupFile">
                  <span class="iconfont icon-group">{{ $t('m.import_api_groups') }}</span>
                </el-upload>
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>

        </div>

      </div>

      <div class="table">
        <el-table :data="tableData" border stripe width="100%" size="mini">
          <el-table-column label="id" prop="id"></el-table-column>
          <el-table-column :label="$t('m.name')">
            <template slot-scope="scope">
              <i class="iconfont icon-on_line1 circle" v-if="scope.row.status == 1"></i>
              <i class="iconfont icon-off_line circle offline" v-else></i>
              <i class="el-icon-lock circle lock" v-if="scope.row.access == PRIVILEGE.PRIVATE"></i>
              <i class="el-icon-unlock circle " v-else></i>
              <span :title="scope.row.note">{{ scope.row.name }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('m.path')">
            <template slot-scope="scope">
              <span>/{{ context }}/{{ scope.row.path }}</span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('m.note')" prop="note">
          </el-table-column>
          <el-table-column label="Content-Type" prop="contentType" sortable></el-table-column>
          <el-table-column :label="$t('m.parameters')">
            <template slot-scope="scope">
              <div v-show="scope.row.contentType === CONTENT_TYPE.FORM_URLENCODED ">
                <data-tag v-for="item in scope.row.p" :name="item.name" :type="item.type"></data-tag>
              </div>
              <div v-show="scope.row.contentType === CONTENT_TYPE.JSON ">{{ scope.row.jsonParam }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="updateTime" :label="$t('m.update_time')" sortable></el-table-column>
          <el-table-column :label="$t('m.operation')" width="220px">
            <template slot-scope="scope">
              <!--              <el-button plain size="mini" type="info" @click="detail(scope.row.id)" circle><i class="iconfont icon-detail"></i></el-button>-->

              <el-tooltip class="item" effect="dark" :content="$t('m.edit')" placement="top">
                <el-button plain size="mini" type="warning" @click="handleEdit(scope.row.id)" circle><i
                    class="el-icon-edit"></i></el-button>
              </el-tooltip>
              <el-tooltip class="item" effect="dark" content="复制" placement="top">
                <el-button plain size="mini" type="info" @click="handleCopy(scope.row.id)" circle>
                  <i class="el-icon-share"></i>
                </el-button>
              </el-tooltip>
              <el-tooltip class="item" effect="dark" :content="$t('m.online')" placement="top">
                <el-button plain size="mini" v-if="scope.row.status === 0" type="warning" @click="online(scope.row.id)"
                           circle>
                  <i class="iconfont icon-on_line2"></i>
                </el-button>
              </el-tooltip>

              <el-tooltip class="item" effect="dark" :content="$t('m.offline')" placement="top">
                <el-button plain size="mini" v-if="scope.row.status === 1" type="info" @click="offline(scope.row.id)"
                           circle>
                  <i class="iconfont icon-off_line1"></i>
                </el-button>
              </el-tooltip>

              <el-tooltip class="item" effect="dark" :content="$t('m.request_test')" placement="top">
                <el-button plain size="mini" v-if="scope.row.status === 1" type="primary"
                           @click="httpTest(scope.row.id)"
                           :title="$t('m.request_test')" circle>
                  <i class="iconfont icon-HTTPRequest"></i>
                </el-button>
              </el-tooltip>

              <el-tooltip class="item" effect="dark" :content="$t('m.delete')" placement="top">
                <el-button plain size="mini" type="danger" @click="handleDelete(scope.row.id)" circle>
                  <i class="el-icon-delete"></i>
                </el-button>
              </el-tooltip>

            </template>
          </el-table-column>
        </el-table>
      </div>

      <!--      <el-dialog :title="$t('m.api_group')" :visible.sync="dialogVisible" @close="getAllGroups">-->
      <!--        <group></group>-->
      <!--      </el-dialog>-->

      <el-dialog :title="$t('m.export_api_doc')" :visible.sync="dialogVisible2" @open="getApiTree">
        <el-tree :data="treeData" show-checkbox node-key="id" :props="defaultProps" ref="tree"></el-tree>
        <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible2 = false">{{ $t('m.cancel') }}</el-button>
        <el-button type="primary" @click="dialogVisible2 = false;exportDocs()">{{ $t('m.export') }}</el-button>
      </span>
      </el-dialog>

      <el-dialog :title="$t('m.export_api')" :visible.sync="dialogVisible3" @open="getApiTree">
        <el-tree :data="treeData" show-checkbox node-key="id" :props="defaultProps" ref="tree2"></el-tree>
        <span slot="footer" class="dialog-footer"><el-button @click="dialogVisible3 = false">{{
            $t('m.cancel')
          }}</el-button>
        <el-button type="primary" @click="dialogVisible3 = false;exportConfig()">{{ $t('m.export') }}</el-button>
      </span>
      </el-dialog>

      <el-dialog :title="$t('m.export_api_groups')" :visible.sync="dialogVisible4" @open="getAllGroups">
        <el-checkbox-group v-model="checkList">
          <el-checkbox v-for="item in groups" :label="item.id">{{ item.name }}
            <span style="color: #ccc">{{ item.id }}</span>
          </el-checkbox>

        </el-checkbox-group>

        <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible4 = false">{{ $t('m.cancel') }}</el-button>
        <el-button type="primary" @click="dialogVisible4 = false;exportGroupConfig()">{{ $t('m.export') }}</el-button>
      </span>
      </el-dialog>
    </div>
  </div>


</template>

<script>
import group from "@/components/api/group";
import ApiTree from "@/components/api/ApiTree.vue";
import {CONTENT_TYPE, PRIVILEGE} from "@/constant";
import * as dbApi from '@/api/dsApi'
import {MessageBox} from 'element-ui'

export default {
  name: "api",
  data() {
    return {


      CONTENT_TYPE: Object.freeze(CONTENT_TYPE),
      PRIVILEGE: Object.freeze(PRIVILEGE),
      dialogVisible: false,
      dialogVisible2: false,
      dialogVisible3: false,
      dialogVisible4: false,
      createDialog: false,
      keyword: {
        name: null,
        note: null,
        path: null,
      },
      tableData: [],
      groups: [],
      groupId: "",
      treeData: [],
      defaultProps: {
        children: "children",
        label: "name",
      },
      headers: {
        Authorization: localStorage.getItem("token"),
      },
      fileList: [],
      groupFile: [],
      checkList: [],
      context: null,
    };
  },
  components: {group, ApiTree},
  methods: {
    getAllApiTree() {
      dbApi.getApiTree().then(response => {
        this.treeData = response.data
      })
          .catch((error) => {
            // this.$message.error("查询所有api失败")
          });
    },
    importSuccess(response, file, fileList) {
      this.fileList = [];
      this.$message.success("Import Success");
      // this.getAllApis();
    },
    importGroupSuccess(response, file, fileList) {
      this.groupFile = [];
      this.$message.success("Import Success");
      this.getAllGroups();
    },
    importFail(error, file, fileList) {
      this.$message.error("Import failed!  " + error.message);
    },

    search() {
      dbApi.searchApiConfigs({
        name: this.keyword.name,
        note: this.keyword.note,
        path: this.keyword.path,
        groupId: this.groupId,
      })
          .then((response) => {
            const list = response.data;
            list.forEach((t) => {
              const obj = JSON.parse(t.params);
              t["p"] = obj;
            });
            this.tableData = list;
          })
          .catch((error) => {
            this.$message.error("Search Failed");
          });
    },
    getContext() {
      dbApi.getContext().then((response) => {
        this.context = response.data;
      })
          .catch((error) => {
            this.$message.error("Failed");
          });
    },
    handleDelete(id) {
      MessageBox.confirm(
          '确定要删除这条API配置吗？删除后将无法恢复。',
          '删除确认',
          {
            confirmButtonText: this.$t('m.ok'), // 确认按钮文本（可使用国际化）
            cancelButtonText: this.$t('m.cancel'),   // 取消按钮文本（可使用国际化）
            type: 'warning' // 警告类型，显示黄色图标
          }
      )
          .then(() => {
            // 用户点击确认后执行删除操作
            dbApi.deleteApiConfig(id)
                .then((response) => {
                  this.$message.success("Delete Success");
                  this.search(); // 删除成功后重新加载列表
                })
                .catch((error) => {
                  this.$message.error("Delete Failed");
                });
          })
          .catch(() => {
            // 用户点击取消后执行（可选）
            this.$message.info("取消成功"); // 可添加"已取消删除"提示
          });
    },
    handleCopy(id) {
      MessageBox.confirm(
          '确定要复制这条API配置吗？ ',
          '复制确认',
          {
            confirmButtonText: this.$t('m.ok'), // 确认按钮文本（可使用国际化）
            cancelButtonText: this.$t('m.cancel'),   // 取消按钮文本（可使用国际化）
            type: 'warning' // 警告类型，显示黄色图标
          }
      )
          .then(() => {
            // 用户点击确认后执行删除操作
            dbApi.copyApiConfig(id)
                .then((response) => {
                  this.$message.success("Copy Success");
                  this.search(); // 删除成功后重新加载列表
                })
                .catch((error) => {
                  this.$message.error("Copy Failed");
                });
          })
          .catch(() => {
            // 用户点击取消后执行（可选）
            this.$message.info("取消成功"); // 可添加"已取消删除"提示
          });
    },
    online(id) {
      dbApi.onlineApi(id)
          .then((response) => {
            this.$message.success("Publish Success");
            this.search();
          })
          .catch((error) => {
            this.$message.error("Publish Failed");
          });
    },
    offline(id) {
      dbApi.offlineApi(id).then((response) => {
        this.$message.success("Already Offline");
        this.search();
      }).catch((error) => {
        this.$message.error("Failed");
      });
    },
    httpTest(id) {
      // this.$router.push({path: "/api/request", query: {id: id}});
      // this.$router.push({path: "/requestApi", query: {id: id}});

      const targetUrl = this.$router.resolve({
        name: 'directRequest', // 路由名称（推荐用name，而非path）
        query: {id: id}      // 传递id参数
      }).href;

      // 2. 新开标签页打开（_blank表示新标签页）
      window.open(targetUrl, '_blank');


    },
    detail(id) {
      this.$router.push({path: "/api/detail", query: {id: id}});
    },
    handleEdit(id) {
      this.$router.push({path: "/api/edit", query: {id: id}});
    },
    handleAdd() {
      this.$router.push({path: "/api/add"});
    },
    getAllGroups() {
      dbApi.getAllGroups().then(response => {
        this.groups = response.data
      })
          .then((response) => {
            this.groups = response.data;
          })
          .catch((error) => {
          });
    },
    getApiTree() {
      dbApi.getApiTree()
          .then((response) => {
            this.treeData = response.data;
          })
          .catch((error) => {
          });
    },
    exportDocs() {
      let a = this.$refs.tree.getCheckedKeys().filter((t) => {
        return t != undefined;
      });
      const ids = a.join(",");
      if (ids == "") {
        return;
      }
      dbApi.exportApiDocs(ids)
          .then((res) => {
            console.log(res);
            const link = document.createElement("a");
            let blob = new Blob([res.data], {type: "application/x-msdownload"});
            link.style.display = "none";
            link.href = URL.createObjectURL(blob);
            link.setAttribute("download", "API Doc.md");
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
          })
          .catch((error) => {
            this.$message.error("Export Failed");
            console.error(error);
          });
    },
    exportConfig() {
      let a = this.$refs.tree2.getCheckedKeys(true).filter((t) => {
        return t != undefined;
      });
      const ids = a.join(",");
      if (ids == "") {
        return;
      }
      dbApi.downloadConfig(ids)
          .then((res) => {
            console.log(res);
            const link = document.createElement("a");
            let blob = new Blob([res.data], {type: "application/x-msdownload"});
            link.style.display = "none";
            link.href = URL.createObjectURL(blob);
            link.setAttribute("download", "api_config.json");
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
          })
          .catch((error) => {
            this.$message.error("Export Failed");
            console.error(error);
          });
    },
    exportGroupConfig() {
      console.log(this.checkList);
      const ids = this.checkList.join(",");
      dbApi.downloadGroupConfig(ids)
          .then((res) => {
            console.log(res);
            const link = document.createElement("a");
            let blob = new Blob([res.data], {type: "application/x-msdownload"});
            link.style.display = "none";
            link.href = URL.createObjectURL(blob);
            link.setAttribute("download", "api_group_config.json");
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
          })
          .catch((error) => {
            this.$message.error("Export Failed");
            console.error(error);
          });
    },
  },

  created() {
    this.search();
    this.getAllGroups();
    this.getContext();
    this.getAllApiTree();
  },
};
</script>

<style scoped lang="less">
.api_root {
  display: flex;

  height: calc(100vh - 65px);

  .group {
    width: 350px;
    padding: 20px 10px;

    //flex-grow: 0;
    //flex-shrink: 0;
    //background-color: #324256;
  }

  .api {
    padding: 20px 10px;
    width: calc(100vw - 330px);

    .tool {
      box-shadow: 0px 0px 3px 2px rgba(196, 194, 194, 0.34);
      padding: 10px 5px 5px 5px;
      margin-bottom: 20px;
      display: flex;
      justify-content: space-between;

      .left {
        .search {
          margin: 0 0 5px 0;

          .label {
            font-weight: 700;
            font-size: 14px;
            margin: 0 5px 0 10px;
          }
        }
      }

      .right {

      }


    }

    .table {
      height: calc(100vh - 170px);
      overflow: auto;
      padding: 10px;
      box-shadow: 0px 0px 3px 2px rgba(196, 194, 194, 0.34);

    }
  }
}


i {
  font-size: 14px;

}

.circle {
  border-radius: 10px;
  font-size: 18px;
  font-weight: 700;
  /*background-color: #05566b;*/
  padding: 3px;
  color: #38be2e;
}

.offline {
  color: #737373;
}

.lock {
  color: #c10b0b;
}

.tag {
  margin-right: 5px;
}

ul {
  margin-bottom: 10px;

  li {
    display: inline-block;
    margin-right: 10px;
  }
}
</style>
