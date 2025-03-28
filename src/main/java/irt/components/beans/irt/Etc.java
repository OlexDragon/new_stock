package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class Etc {

	@JsonProperty("STATUS (0x01)")
	private String status;

	@JsonProperty("EC (0x08)")
	private String ec;

	@JsonProperty("ETC (0x0A)")
	private String etc;

	@JsonProperty("EC_ALARM_LIMIT (0x10)")
	private String ecAlarmLimit;

	@JsonProperty("ETC_ALARM_LIMIT (0x12)")
	private String etcAlarmLimit;

	@JsonProperty("CONFIG (0x16)")
	private String config;

}
