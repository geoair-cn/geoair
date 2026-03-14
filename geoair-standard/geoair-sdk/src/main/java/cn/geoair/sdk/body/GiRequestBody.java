package cn.geoair.sdk.body;

import java.io.OutputStream;

/**
 * @author ：zhangjun
 * @date ：Created in 2023/2/28 16:24 @description： 请求body
 */
public interface GiRequestBody {

	/**
	 * 写出数据，不关闭流
	 * @param out out流
	 */
	void write(OutputStream out);

	String getContentType();

}
