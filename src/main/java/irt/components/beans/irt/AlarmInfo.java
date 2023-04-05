package irt.components.beans.irt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class AlarmInfo {

	private String devname;
	private Integer number;
	private String summary;
	private String summary_status;
	private List<Alarm> alarms;

}
