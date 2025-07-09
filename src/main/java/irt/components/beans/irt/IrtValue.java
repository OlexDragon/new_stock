package irt.components.beans.irt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class IrtValue {

	private Status status = Status.NORMAL; 
	private BigDecimal value;
	private String unit;

	public IrtValue(String valueAsString) {

		Optional.of(valueAsString).map(String::trim).filter(s->!s.isEmpty()).map(s->s.split("\\s+"))
		.ifPresent(
				s->{
//					LogManager.getLogger().error("\n\t{}\n\tlength: {} : {}", valueAsString, s.length, s);
					int index = 0;
					if(s.length==3)
						setStatus(Status.valueOf(s[index++].charAt(0)));
					setValue(new BigDecimal(s[index++]));
					setUnit(s[index++]);
				});
	}

	@RequiredArgsConstructor @Getter 
	public enum Status{

		LESS('<'),
		NORMAL('\0'),
		GREATER('>');

		private final char symbol;

		public static Status valueOf(Character symbol) {
			return Optional.ofNullable(symbol).map(s->Arrays.stream(Status.values()).filter(p->symbol.equals(p.symbol)).findAny().orElse(Status.NORMAL)).orElse(Status.NORMAL);
		}
	}
}
