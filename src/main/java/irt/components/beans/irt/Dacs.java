package irt.components.beans.irt;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Dacs {

	@JsonProperty("DAC1")
	private String dac1;

	@JsonProperty("DAC2")
	private String dac2;

	@JsonProperty("DAC3")
	private String dac3;

	@JsonProperty("DAC4")
	private String dac4;

	public Integer getDac2RowValue() {
		return Optional.ofNullable(dac2).map(v->v.split(" ")[0].replace("0x", "")).map(hex->Integer.parseInt(hex, 16)).orElse(null);
	}
}
