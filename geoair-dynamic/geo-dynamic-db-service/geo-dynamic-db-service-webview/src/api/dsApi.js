import request from "@/request";


export function postPxyJSON(url, params) {
    return request({
        url: url,
        headers: {'Content-Type': 'application/json'},
        method: 'post',
        data: params
    });
}

export function postPxyParams(url, params) {
    return request({
        url: url,
        headers: {'Content-Type': 'application/x-www-form-urlencoded'},
        method: 'post',
        params: params
    });
}

export function login(username, password) {
    return request({
        url: window.Config.baseUrl + '/ds_api/system/login',
        method: 'get',
        params: {
            username: username,
            password: password
        }
    });
}

export function validateToken(dsToken) {
    return request({
        url: window.Config.baseUrl + '/ds_api/system/validateToken',
        method: 'get',
        params: {
            token: dsToken
        }
    });
}

export function logout(dsToken) {
    return request({
        url: window.Config.baseUrl + '/ds_api/system/logout',
        method: 'get',
        params: {
            token: dsToken
        }
    });
}

// 获取上下文路径
export function getContext() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/context',
        method: 'post',
        params: {
            dsToken: dsToken
        }
    });
}

// 新增API配置
export function addApiConfig(formData) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/add',
        headers: {'Content-Type': 'application/json'},
        method: 'post',
        data: formData,
        params: {
            dsToken: dsToken
        }
    });
}

// 解析SQL参数
export function parseParam(sql) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/parseParam',
        method: 'get',
        params: {sql}
    });
}

// 获取所有API配置
export function getAllApiConfigs() {
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/getAll',
        method: 'get'
    });
}

// 获取API树形结构
export function getApiTree() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/getApiTree',
        method: 'post',
        params: {
            dsToken: dsToken
        }
    });
}

// 搜索API配置
export function searchApiConfigs(params) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/search',
        method: 'post',
        params: {
            name: params.name,
            note: params.note,
            path: params.path,
            groupId: params.groupId,
            dsToken: dsToken
        }
    });
}

// 获取API配置详情
export function getApiConfigDetail(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/apiConfig/detail/${id}`,
        method: 'post',
        params: {
            dsToken: dsToken
        }
    });
}

// 删除API配置
export function deleteApiConfig(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/apiConfig/delete/${id}`,
        method: 'post',
        params: {
            dsToken: dsToken
        }
    });
}

// 删除API配置
export function copyApiConfig(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/apiConfig/copy/${id}`,
        method: 'post',
        params: {
            dsToken: dsToken
        }
    });
}

// 更新API配置
export function updateApiConfig(formData) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/update',
        method: 'post',
        headers: {'Content-Type': 'application/json'},
        data: formData,
        params: {
            dsToken: dsToken
        }
    });
}

// 上线API
export function onlineApi(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/apiConfig/online/${id}`,
        method: 'get',
        params: {
            dsToken: dsToken
        }
    });
}

// 下线API
export function offlineApi(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/apiConfig/offline/${id}`,
        method: 'get',
        params: {
            dsToken: dsToken
        }
    });
}

// 导出API文档
export function exportApiDocs(ids) {

    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/apiDocs',
        method: 'get',
        params: {ids},
        responseType: 'blob' // 用于文件下载
    });
}

// 导出API配置
export function downloadConfig(ids) {
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/downloadConfig',
        method: 'post',
        params: {ids: ids},
        responseType: 'blob' // 用于文件下载
    });
}

// 导出分组配置
export function downloadGroupConfig(ids) {
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/downloadGroupConfig',
        method: "post",
        params: {ids: ids},
        responseType: 'blob' // 用于文件下载
    });
}

// 导入API配置
export function importAPI(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/import',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
}

// 导入分组配置
export function importGroup(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/importGroup',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
}

// 执行SQL
export function executeSql(sql, datasourceId, sqlParam) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/sql/execute',
        method: 'post',
        params: {sql: sql, datasourceId: datasourceId, params: sqlParam, dsToken: dsToken}
    });
}

export function executeSqlV2(sql, datasourceId, sqlParam) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/sql/executeV2',
        method: 'post',
        headers: {'Content-Type': 'application/json'},
        data: {sql: sql, datasourceId: datasourceId, params: sqlParam, dsToken: dsToken}
    });
}

// 解析动态SQL
export function parseDynamicSql(sql, params) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/apiConfig/parseDynamicSql',
        method: 'post',
        params: {
            dsToken: dsToken,
            sql: sql,
            params: (params)
        }
    });
}


// 新增数据源
export function addDataSource(dataSource) {

    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/add',
        method: 'post',
        params: dataSource
    });
}

// 获取所有数据源
export function getAllDataSources() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/getAll',
        method: 'post',
        params: {
            dsToken: dsToken
        }
    });
}

// 获取数据源详情
export function getDataSourceDetail(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/datasource/detail/${id}`,
        method: 'get'
        , params: {
            dsToken: dsToken
        }
    });
}

// 删除数据源
export function deleteDataSource(id) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + `/ds_api/datasource/delete/${id}`,
        method: 'post'
        , params: {
            dsToken: dsToken
        }
    });
}

// 更新数据源
export function updateDataSource(dataSource) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/update',
        method: 'post',
        data: dataSource
        , params: {
            dsToken: dsToken
        }
    });
}

// 测试数据源连接
export function testDataSourceConnection(dataSource) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/connect',
        method: 'post',
        data: dataSource
        , params: {
            dsToken: dsToken
        }
    });
}

// 导出数据源配置
export function exportDataSources(ids) {
    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/export',
        method: 'post',
        params: {ids: ids.join(",")},
        responseType: 'blob' // 用于文件下载
    });
}

// 导入数据源配置
export function importDataSources(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/import',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
}

// 分页列出数据源信息
export function listDbapiDatasourcePage(param) {
    return request({
        url: window.Config.baseUrl + '/ds_api/datasource/listDbapiDatasourcePage',
        method: 'post',
        data: param
    });
}

// 创建API分组
export function createGroup(groupName) {
    return request({
        url: window.Config.baseUrl + '/ds_api/group/create',
        method: 'post',
        params: {name: groupName}
    });
}

// 删除API分组
export function deleteGroup(id) {
    return request({
        url: window.Config.baseUrl + `/ds_api/group/delete/${id}`,
        method: 'post'
    });
}

// 获取所有API分组
export function getAllGroups() {
    return request({
        url: window.Config.baseUrl + '/ds_api/group/getAll',
        method: 'post'
    });
}

// 更新API分组
export function updateGroup(group) {
    return request({
        url: window.Config.baseUrl + '/ds_api/group/update',
        method: 'post',
        params: group
    });
}

// 分页列出API分组信息
export function listDbApiGroupPage(param) {
    return request({
        url: window.Config.baseUrl + '/ds_api/group/listDbApiGroupPage',
        method: 'post',
        data: param
    });
}

// 获取系统版本
export function getSystemVersion() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/system/version',
        method: 'get'
        , params: {
            dsToken: dsToken
        }
    });
}

// 获取系统模式
export function getSystemMode() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/system/mode',
        method: 'get'
        , params: {
            dsToken: dsToken
        }
    });
}

// 获取 IP 和端口（包含上下文路径）
export function getIPPort() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/system/getIPPort',
        method: 'post'
        , params: {
            dsToken: dsToken
        }
    });
}

// 获取 IP 和端口（不包含上下文路径）
export function getIP() {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/system/getIP',
        method: 'post'
        , params: {
            dsToken: dsToken
        }
    });
}

// 获取所有表及表结构信息
export function getAllTables(sourceId) {
    const dsToken = localStorage.getItem('dsToken')
    return request({
        url: window.Config.baseUrl + '/ds_api/table/getAllTables',
        method: 'get',
        params: {
            dsToken: dsToken,
            sourceId: sourceId
        }
    });
}

// 获取指定表的所有列信息
export function getAllColumns(sourceId, table) {
    return request({
        url: window.Config.baseUrl + '/ds_api/table/getAllColumns',
        method: 'get',
        params: {
            sourceId: sourceId,
            table: table
        }
    });
}
