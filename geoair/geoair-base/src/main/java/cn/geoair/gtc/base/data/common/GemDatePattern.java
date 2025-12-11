package cn.geoair.gtc.base.data.common;

import java.util.Objects;

public enum GemDatePattern {



    NULL("", ""),
    ISO8601Long(GemDatePattern.Constant.ISO8601Long, "Y-m-d H:i:s"),
    ISO8601Short(GemDatePattern.Constant.ISO8601Short, "Y-m-d"),
    ChineseDate(GemDatePattern.Constant.ChineseDate, "Y年m月d日"),
    ShortDate(GemDatePattern.Constant.ShortDate, "n/j/Y"),
    LongDate(GemDatePattern.Constant.LongDate, "l, F d, Y"),
    FullDateTime(GemDatePattern.Constant.FullDateTime, "l, F d, Y g:i:s A"),
    MonthDay(GemDatePattern.Constant.MonthDay, "F d"),
    ShortTime(GemDatePattern.Constant.ShortTime, "g:i A"),
    LongTime(GemDatePattern.Constant.LongTime, "g:i:s A"),
    SortableDateTime(GemDatePattern.Constant.SortableDateTime, "Y-m-d\\TH:i:s"),
    UniversalSortableDateTime(GemDatePattern.Constant.UniversalSortableDateTime, "Y-m-d H:i:sO"),
    YearMonth(GemDatePattern.Constant.YearMonth, "F, Y");

    private String format;
    private String jsFormat;

    public final String str;

    public static class Constant{
    	/**
    	 * "yyyy-MM-dd HH:mm:ss"
    	 */
      	public final static String ISO8601Long = "yyyy-MM-dd HH:mm:ss";
      	public final static String ISO8601Short = "yyyy-MM-dd";
      	public final static String ChineseDate = "yyyy年MM月dd日";
      	public final static String ShortDate = "n/j/Y";
      	public final static String LongDate = "l, F d, Y";
      	public final static String FullDateTime = "l, F d, Y g:i:s A";
      	public final static String MonthDay = "F d";
      	public final static String ShortTime = "g:i A";
      	public final static String LongTime = "g:i:s A";
      	public final static String SortableDateTime = "Y-m-d\\TH:i:s";
      	public final static String UniversalSortableDateTime = "Y-m-d H:i:sO";
      	public final static String YearMonth = "F, Y";
    }

    GemDatePattern(String format, String jsFormat) {
        this.format = format;
        this.jsFormat = jsFormat;
        this.str = format;
    }

    public String getFormat() {
        return this.format;
    }

    public String getJsFormat() {
        return jsFormat;
    }

    public void setJsFormat(String jsFormat) {
        this.jsFormat = jsFormat;
    }



    public static GemDatePattern getByJsFormat(String jsFormat) {
    	GemDatePattern[] ts = GemDatePattern.class.getEnumConstants();
		for(GemDatePattern t :ts) {
			if (Objects.equals(t.getJsFormat(),jsFormat)) {
				return t;
			}
		}
		return GemDatePattern.NULL;
    }
}
