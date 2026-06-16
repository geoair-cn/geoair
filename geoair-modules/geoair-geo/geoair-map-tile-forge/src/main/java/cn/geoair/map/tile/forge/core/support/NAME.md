根据 `StorageType`（4种）和 `TileFormat`（3种）的组合，共12个实现类 如下，命名规则为：
`[存储类型][瓦片格式]TileStorageSupport`， ：

1. **StorageType.LOCAL_ZIP 系列**
    - `LocalZipCompactV1TileStorageSupport`（本地ZIP存储 + compact_v1格式）
    - `LocalZipCompactV2TileStorageSupport`（本地ZIP存储 + compact_v2格式）
    - `LocalZipLooseTileStorageSupport`（本地ZIP存储 + loose格式）


2. **StorageType.S3_ZIP 系列**
    - `S3ZipCompactV1TileStorageSupport`（S3 ZIP存储 + compact_v1格式）
    - `S3ZipCompactV2TileStorageSupport`（S3 ZIP存储 + compact_v2格式）
    - `S3ZipLooseTileStorageSupport`（S3 ZIP存储 + loose格式）


3. **StorageType.LOCAL_UNZIPPED 系列**
    - `LocalUnzippedCompactV1TileStorageSupport`（本地解压存储 + compact_v1格式）
    - `LocalUnzippedCompactV2TileStorageSupport`（本地解压存储 + compact_v2格式）
    - `LocalUnzippedLooseTileStorageSupport`（本地解压存储 + loose格式）


4. **StorageType.S3_UNZIPPED 系列**
    - `S3UnzippedCompactV1TileStorageSupport`（S3解压存储 + compact_v1格式）
    - `S3UnzippedCompactV2TileStorageSupport`（S3解压存储 + compact_v2格式）
    - `S3UnzippedLooseTileStorageSupport`（S3解压存储 + loose格式）

### 命名说明

- 前缀对应 `StorageType`（如 `LocalZip` 对应 `LOCAL_ZIP`，`S3Unzipped` 对应 `S3_UNZIPPED`）。
- 中间部分对应 `TileFormat`（如 `CompactV1` 对应 `COMPACT_V1`，`Loose` 对应 `LOOSE`）。
- 后缀统一为 `TileStorageSupport`，明确实现的接口类型。
 
