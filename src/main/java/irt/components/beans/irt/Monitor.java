package irt.components.beans.irt;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Monitor {

	private String inpower;
	private String outpower;
	private String reflpower;
	private String lo;
	private String attenuation;
	private String lock;
	private String mute;
	@JsonProperty("rc")
	private String referenceSource;
	@JsonProperty("apc_enable")
	private String apc;

	private BigDecimal temperature;
}
