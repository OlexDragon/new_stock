package irt.components.beans.irt.calibration;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class CurrentToSave {

	private boolean special;
	private String serialNumber;
	private String topId;
	private String moduleId;
	private List<ChannelLayout> layouts;
}
