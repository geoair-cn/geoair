package cn.geoair.map.dynamic.geoserver.beans;

import lombok.Data;

/**
 * @author ：张逢吉
 * @date ：Created in 13:19
 * @description： TODO
 */
@Data
public class GsWorkspace {
  /** 工作区名称 */
  private String name = "geoair";

  /** 工作区 URI */
  private String uri = "http://www.geoair.cn";
}
