package irt.components.beans.irt.calibration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterPLL implements Diagnostics {

	@JsonProperty("ST0")
	@JsonAlias({"ST0 Diagnostics (0x00)", "ST0 Register (0x00)", "CS_HVDD (0x01)"})
	String st0;

	@JsonProperty("ST1")
	@JsonAlias({"ST1 Diagnostics (0x01)", "ST1 Register (0x01)", "CS_LVDD (0x03)"})
	String st1;

	@JsonProperty("ST2")
	@JsonAlias({"ST2 Diagnostics (0x02)", "ST2 Register (0x02)"})
	String st2;

	@JsonProperty("ST3")
	@JsonAlias({"ST3 Diagnostics (0x03)", "ST3 Register (0x03)"})
	String st3;

	@JsonProperty("ST4")
	@JsonAlias({"ST4 Diagnostics (0x04)", "ST4 Register (0x04)"})
	String st4;

	@JsonProperty("ST5")
	@JsonAlias({"ST5 Diagnostics (0x05)", "ST5 Register (0x05)"})
	String st5;

	@JsonProperty("ST6")
	@JsonAlias({"ST6 Diagnostics (0x06)", "ST6 Register (0x06)"})
	String st6;

	@JsonProperty("ST7")
	@JsonAlias({"ST7 Diagnostics (0x07)", "ST7 Register (0x07)"})
	String st7;

	@JsonProperty("ST8")
	@JsonAlias({"ST8 Diagnostics (0x08)", "ST8 Register (0x08)"})
	String st8;

	@JsonProperty("ST9")
	@JsonAlias({"ST9 Diagnostics (0x09)", "ST9 Register (0x09)"})
	String st9;

	@JsonProperty("ST10")
	@JsonAlias({"ST10 Diagnostics (0x0a)", "ST10 Register (0x0a)"})
	String st10;

	@JsonProperty("ST11")
	@JsonAlias({"ST11 Diagnostics (0x0b)", "ST11 Register (0x0b)"})
	String st11;
}
