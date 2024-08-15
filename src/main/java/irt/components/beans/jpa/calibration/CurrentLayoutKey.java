package irt.components.beans.jpa.calibration;

import java.io.Serializable;
import java.sql.Date;

import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class CurrentLayoutKey implements Serializable{
	private static final long serialVersionUID = -7373856540437419252L;

	@Id private String topId;
	@Id private String moduleId;
	@Id private Date creationDate;
}
