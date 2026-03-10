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

// 获取上下文路径
export function getContext() {
    return request({
        url: '/apiConfig/context',
        method: 'post'
    });
}

// 新增API配置
export function addApiConfig(formData) {
    return request({
        url: '/apiConfig/add',
        headers: {'Content-Type': 'application/json'},
        method: 'post',
        data: formData
    });
}

// 解析SQL参数
export function parseParam(sql) {
    return request({
        url: '/apiConfig/parseParam',
        method: 'get',
        params: {sql}
    });
}

// 获取所有API配置
export function getAllApiConfigs() {
    return request({
        url: '/apiConfig/getAll',
        method: 'get'
    });
}

// 获取API树形结构
export function getApiTree() {
    return request({
        url: '/apiConfig/getApiTree',
        method: 'post'
    });
}

// 搜索API配置
export function searchApiConfigs(params) {
    return request({
        url: '/apiConfig/search',
        method: 'post',
        params: {
            name: params.name,
            note: params.note,
            path: params.path,
            groupId: params.groupId
        }
    });
}

// 获取API配置详情
export function getApiConfigDetail(id) {
    return request({
        url: `/apiConfig/detail/${id}`,
        method: 'post'
    });
}

// 删除API配置
export function deleteApiConfig(id) {
    return request({
        url: `/apiConfig/delete/${id}`,
        method: 'post'
    });
}
// 删除API配置
export function copyApiConfig(id) {
    return request({
        url: `/apiConfig/copy/${id}`,
        method: 'post'
    });
}

// 更新API配置
export function updateApiConfig(formData) {
    return request({
        url: '/apiConfig/update',
        method: 'post',
        headers: {'Content-Type': 'application/json'},
        data: formData
    });
}

// 上线API
export function onlineApi(id) {
    return request({
        url: `/apiConfig/online/${id}`,
        method: 'get'
    });
}

// 下线API
export function offlineApi(id) {
    return request({
        url: `/apiConfig/offline/${id}`,
        method: 'get'
    });
}

// 导出API文档
export function exportApiDocs(ids) {
    return request({
        url: '/apiConfig/apiDocs',
        method: 'get',
        params: {ids},
        responseType: 'blob' // 用于文件下载
    });
}

// 导出API配置
export function downloadConfig(ids) {
    return request({
        url: '/apiConfig/downloadConfig',
        method: 'post',
        params: {ids: ids},
        responseType: 'blob' // 用于文件下载
    });
}

// 导出分组配置
export function downloadGroupConfig(ids) {
    return request({
        url: '/apiConfig/downloadGroupConfig',
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
        url: '/apiConfig/import',
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
        url: '/apiConfig/importGroup',
        method: 'post',
        data: formData,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
}

// 执行SQL
export function executeSql(sql, datasourceId, sqlParam) {
    return request({
        url: '/apiConfig/sql/execute',
        method: 'post',
        params: {sql: sql, datasourceId: datasourceId, params: sqlParam}
    });
}
export function executeSqlV2(sql, datasourceId, sqlParam) {
    return request({
        url: '/apiConfig/sql/executeV2',
        method: 'post',
        headers: {'Content-Type': 'application/json'},
        data: {sql: sql, datasourceId: datasourceId, params: sqlParam}
    });
}

// 解析动态SQL
export function parseDynamicSql(sql, params) {
    return request({
        url: '/apiConfig/parseDynamicSql',
        method: 'post',
        params: {
            sql: sql,
            params: (params)
        }
    });
}

// 分页列出API配置信息
export function listDbApiConfigPage(param) {
    return request({
        url: '/apiConfig/listDbApiConfigPage',
        method: 'post',
        data: param
    });
}

// 新增数据源
export function addDataSource(dataSource) {
    return request({
        url: '/datasource/add',
        method: 'post',
        params: dataSource
    });
}

// 获取所有数据源
export function getAllDataSources() {
    return request({
        url: '/datasource/getAll',
        method: 'post'
    });
}

// 获取数据源详情
export function getDataSourceDetail(id) {
    return request({
        url: `/datasource/detail/${id}`,
        method: 'get'
    });
}

// 删除数据源
export function deleteDataSource(id) {
    return request({
        url: `/datasource/delete/${id}`,
        method: 'post'
    });
}

// 更新数据源
export function updateDataSource(dataSource) {
    return request({
        url: '/datasource/update',
        method: 'post',
        data: dataSource
    });
}

// 测试数据源连接
export function testDataSourceConnection(dataSource) {
    return request({
        url: '/datasource/connect',
        method: 'post',
        data: dataSource
    });
}

// 导出数据源配置
export function exportDataSources(ids) {
    return request({
        url: '/datasource/export',
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
        url: '/datasource/import',
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
        url: '/datasource/listDbapiDatasourcePage',
        method: 'post',
        data: param
    });
}

// 创建API分组
export function createGroup(groupName) {
    return request({
        url: '/group/create',
        method: 'post',
        params: {name: groupName}
    });
}

// 删除API分组
export function deleteGroup(id) {
    return request({
        url: `/group/delete/${id}`,
        method: 'post'
    });
}

// 获取所有API分组
export function getAllGroups() {
    return request({
        url: '/group/getAll',
        method: 'post'
    });
}

// 更新API分组
export function updateGroup(group) {
    return request({
        url: '/group/update',
        method: 'post',
        params: group
    });
}

// 分页列出API分组信息
export function listDbApiGroupPage(param) {
    return request({
        url: '/group/listDbApiGroupPage',
        method: 'post',
        data: param
    });
}

// 获取系统版本
export function getSystemVersion() {
    return request({
        url: '/system/version',
        method: 'get'
    });
}

// 获取系统模式
export function getSystemMode() {
    return request({
        url: '/system/mode',
        method: 'get'
    });
}

// 获取 IP 和端口（包含上下文路径）
export function getIPPort() {
    return request({
        url: '/system/getIPPort',
        method: 'post'
    });
}

// 获取 IP 和端口（不包含上下文路径）
export function getIP() {
    return request({
        url: '/system/getIP',
        method: 'post'
    });
}

// 获取所有表及表结构信息
export function getAllTables(sourceId) {
    return request({
        url: '/table/getAllTables',
        method: 'get',
        params: {
            sourceId: sourceId
        }
    });
}

// 获取指定表的所有列信息
export function getAllColumns(sourceId, table) {
    return request({
        url: '/table/getAllColumns',
        method: 'get',
        params: {
            sourceId: sourceId,
            table: table
        }
    });
}
