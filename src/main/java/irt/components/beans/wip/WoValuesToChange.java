package irt.components.beans.wip;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString(callSuper = true)
public class WoValuesToChange extends WoDetails {

	private String wo;
	private String wipFile;
	private WoDetails fromLOG;
	private WoDetails fromWIP;
}
