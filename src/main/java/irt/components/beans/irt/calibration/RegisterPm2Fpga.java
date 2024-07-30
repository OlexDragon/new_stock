package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterPm2Fpga implements Register{

	@JsonProperty("version")
	@JsonAlias("VERSION         (0x01)")
	String version;

	@JsonProperty("uptime")
	@JsonAlias("UPTIME          (0x03)")
	String uptime;

	@JsonProperty("config")
	@JsonAlias("CONFIG          (0x05)")
	String config;

	@JsonProperty("status")
	@JsonAlias("STATUS          (0x07)")
	String status;

	@JsonProperty("drainEn")
	@JsonAlias("DRAIN_EN        (0x08)")
	String drainEn;

	@JsonProperty("gateEn")
	@JsonAlias("GATE_EN         (0x09)")
	String gateEn;

	@JsonProperty("dbgTestMsb")
	@JsonAlias("DBG_TEST_MSB    (0x10)")
	String dbgTestMsb;

	@JsonProperty("dbgTestLsb")
	@JsonAlias("DBG_TEST_LSB    (0x11)")
	String dbgTestLsb;
}
