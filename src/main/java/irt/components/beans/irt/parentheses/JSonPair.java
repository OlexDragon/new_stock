package irt.components.beans.irt.parentheses;

import lombok.Getter;

@Getter
public class JSonPair {

	private boolean valueIsString;
	private String name;
	private String value;

	public JSonPair(String text) {
		final String[] split = text.split(":", 2);

		if(split.length==0)
			return;

		name = split[0].trim();

		if(split.length>1) {

			value = split[1].trim();

			if(value.contains("'")) {

				valueIsString = true;
				value = value.replaceAll("'", "");

			}else 
				valueIsString = !value.replaceAll("\\d", "").isEmpty();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder().append('"').append(name).append('"').append(':');

		if(valueIsString)
			sb.append('"').append(value).append('"');
		else
			sb.append(value);

		return sb.toString();
	}
}
