package irt.components.beans.irt.calibration.measurement;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString  @JsonIgnoreProperties(ignoreUnknown = true)
public class Measurement {

	private List<MeasurementRow> gain;
	private List<MeasurementRow> saturation;
	private List<MeasurementRow> p1db;
	private List<MeasurementRow> detector;
}
