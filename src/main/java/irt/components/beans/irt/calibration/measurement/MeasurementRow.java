package irt.components.beans.irt.calibration.measurement;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class MeasurementRow {

	private String key;
	private List<MeasurementValue> values;
}
