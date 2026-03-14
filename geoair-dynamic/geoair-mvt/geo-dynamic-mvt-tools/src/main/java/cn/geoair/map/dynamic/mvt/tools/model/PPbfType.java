package cn.geoair.map.dynamic.mvt.tools.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PPbfType {

	rootPbf(0, "RootPbf"), Label(1, "Label"), Boundary(2, "Boundary"),

	;

	private Integer value;

	private String text;

	public static PPbfType findByValue(Integer value) {
		for (PPbfType type : PPbfType.values()) {
			if (type.getValue().equals(value)) {
				return type;
			}
		}
		return rootPbf;
	}

}
