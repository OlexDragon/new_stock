package irt.components.beans.irt;

import java.util.Optional;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class IrtData {

	private String name;
	private IrtValue value;

	public IrtData(String line) {

		final String[] split = line.split(":");
		name = split[0].trim();

		Optional.of(split).filter(s->s.length>1).map(s->s[1]).map(String::trim).filter(s->!s.isEmpty()).map(IrtValue::new).ifPresent(v->value = v);
	}
}
