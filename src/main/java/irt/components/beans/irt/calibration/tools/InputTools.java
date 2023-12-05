package irt.components.beans.irt.calibration.tools;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum InputTools {

	HP8648			("OUTP:STAT", "FREQ:CW" , "POW:AMPL", "{V}?", 1		 , ""),
	ANRITSU68047C	("RF"		, "F1"		, "L1"		, "O{V}", 1000000, "DB");

	private final String outpout;	// Turns the RF output on/off\n (Ex. OUTP:STAT?,  OUTPSTAT ON)
	private final String frequency;	// Sets the RF frequency\n (Ex. FREQ:CW?, FREQ:CW <value> <units>)
	private final String power;		// Sets the amplitude of the RF output\n (Ex. POW:AMPI.?, POW:AMPL <value> <units>)
	private final String toRead;	// String format to read data from the tool
	private final int	 freqFactor;// freqFactor * read Frequency = GHz
	private final String powerUnit;	

	public String commands(){
		return outpout + " " + frequency + " " + power;
	}
}
