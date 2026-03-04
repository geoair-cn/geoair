package cn.geoair.comp.knife4j.ext.model;

/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:14 @description： 主页信息模型
 */
public class ApiModelInfo {

	public static final ApiModelInfo DEFAULT;

	private final String version;

	private final String title;

	private final String author;

	private final String description;

	public ApiModelInfo(String title, String description, String author, String version) {
		this.title = title;
		this.description = description;
		this.author = author;
		this.version = version;
	}

	public String getTitle() {
		return this.title;
	}

	public String getDescription() {
		return this.description;
	}

	public String getVersion() {
		return this.version;
	}

	public String getAuthor() {
		return this.author;
	}

	static {
		DEFAULT = new ApiModelInfo("ApiDoc Documentation", "ApiDoc Documentation 1.0", "geoway", "1.0");
	}

}
