package irt.components.beans.irt.calibration;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString
public class ChannelLayout implements Serializable{
	private static final long serialVersionUID = 8606797075494590247L;

	private String id;
	private Integer value;
	private List<GatelLayout> gates;
	private List<SwitchLayout> switches;
}
