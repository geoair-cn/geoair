package cn.geoair.sdk.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 创建人: 张俊 创建时间: 2023/2/28 15:42 描述: 文件包装对象
 */

public interface GiSdkMultipartFile {

	String getName();

	String getOriginalFilename();

	String getContentType();

	boolean isEmpty();

	long getSize();

	byte[] getBytes() throws IOException;

	InputStream getInputStream() throws IOException;

	void writeTo(OutputStream out) throws IOException;

}
