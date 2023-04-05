package irt.components.beans.irt;

import java.math.BigDecimal;
import java.util.Optional;

import lombok.Getter;

@Getter
public class IrtData {

	private String name;
	private IrtValue value;

	public IrtData(String line) {

		final String[] split = line.split(":");
		name = split[0].trim();

		Optional.of(split).filter(s->s.length>1).map(s->s[1]).map(String::trim).filter(s->!s.isEmpty()).map(s->s.split("\\s+"))
		.map(
				s->{
					final IrtValue irtValue = new IrtValue();
					irtValue.setUnit(s[1]);
					irtValue.setValue(new BigDecimal(s[0]));
					return irtValue;
				})
		.ifPresent(v->value = v);
	}
}
