package cn.geoair.comp.knife4j.ext.core.model;

/**
 * <p>ApiModelInfo class.</p>
 *
 * @author ：张俊
 * @date ：Created in 2022/8/23 16:14 @description： 主页信息模型
 * @version $Id: $Id
 */
public class ApiModelInfo {

	/** Constant <code>DEFAULT</code> */
	public static final ApiModelInfo DEFAULT;

	private final String version;

	private final String title;

	private final String author;

	private final String description;

	/**
	 * <p>Constructor for ApiModelInfo.</p>
	 *
	 * @param title a {@link java.lang.String} object
	 * @param description a {@link java.lang.String} object
	 * @param author a {@link java.lang.String} object
	 * @param version a {@link java.lang.String} object
	 */
	public ApiModelInfo(String title, String description, String author, String version) {
		this.title = title;
		this.description = description;
		this.author = author;
		this.version = version;
	}

	/**
	 * <p>Getter for the field <code>title</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * <p>Getter for the field <code>description</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * <p>Getter for the field <code>version</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getVersion() {
		return this.version;
	}

	/**
	 * <p>Getter for the field <code>author</code>.</p>
	 *
	 * @return a {@link java.lang.String} object
	 */
	public String getAuthor() {
		return this.author;
	}

	static {
		DEFAULT = new ApiModelInfo("ApiDoc Documentation", "ApiDoc Documentation 1.0", "geoair", "1.0");
	}

}
