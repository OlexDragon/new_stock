package irt.components.beans.jpa.rma;

import java.util.Date;

public interface Comment {

	Long getId();
	Long getRmaId();
	Date getDate();
	Boolean getHasFiles();
	void setHasFiles(Boolean has);

}
