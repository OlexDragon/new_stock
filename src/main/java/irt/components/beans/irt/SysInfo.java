package irt.components.beans.irt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class SysInfo {

	private String desc;
	private String vendor;
	private String sn;
	private String build_date;
	private String fw_version;
	private String hw_id;
	private String contact;
	private String uptime;
}
