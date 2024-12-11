package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class HPBMRegisterV31 {

	@JsonProperty("switch1")
	@JsonAlias({"Current_mon_1 (101)", "I_HS1 (0x03)", "CS_HVDD (0x01)", "VDD_CS (0x10)"})
	String switch1;

	@JsonProperty("switch2")
	@JsonAlias({"Current_mon_2 (103)", "I_HS2 (0x05)", "CS_LVDD (0x03)"})
	String switch2;

	@JsonProperty("switch3")
	@JsonAlias("Current_mon_3 (105)")
	String switch3;

	@JsonProperty("switch4")
	@JsonAlias("Current_mon_4 (107)")
	String switch4;

	@JsonProperty("switch5")
	@JsonAlias("Current_mon_5 (109)")
	String switch5;

	@JsonProperty("switch6")
	@JsonAlias("Current_mon_6 (111)")
	String switch6;
}
