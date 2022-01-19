package irt.components.values.units;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Value {

	private BigDecimal value;
	private String unit;
}
