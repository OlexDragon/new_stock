package irt.components.workers;

import org.springframework.web.util.UriUtils;

public class IrtPathEncoder {

	public static String encode(String s) {

		return UriUtils.encodePath(s, "UTF-8");
	}
}
