package irt.components.beans.irt.calibration;

import java.util.List;

import irt.components.beans.irt.HWInfo;
import irt.components.beans.irt.Info;
import irt.components.beans.jpa.calibration.CurrentLayout;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class ModuleInfo {

	private String name;
	private Integer index;
	private HWInfo hwInfo;
	private Info info;
	private List<CurrentLayout> layout;
}
