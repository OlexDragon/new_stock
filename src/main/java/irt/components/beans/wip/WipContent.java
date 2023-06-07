package irt.components.beans.wip;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@RequiredArgsConstructor @Getter @Setter @ToString
public class WipContent {

	private final String workOrder;
	private final String partNumber;
	private final String description;

	private int	qty;
	private WipContent fromLogFile;

	public Status getStatus() {

		if(fromLogFile==null)
			return Status.MOT_IN_LOG;

		if(fromLogFile.getDescription().equals(description) && fromLogFile.getPartNumber().equals(partNumber) && fromLogFile.getQty() == qty && fromLogFile.getWorkOrder().equals(workOrder))
			return Status.EQUALE;

		return Status.NOT_EQUALE;
	}

	public enum Status{
		MOT_IN_LOG,
		EQUALE,
		NOT_EQUALE
	}
}
