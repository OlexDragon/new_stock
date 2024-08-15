package irt.components.beans.jpa.calibration;

import java.io.Serializable;
import java.sql.Date;
import java.util.Calendar;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import irt.components.beans.irt.calibration.ChannelLayout;
import irt.components.services.converter.ChannelLayoutListConverter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@IdClass(CurrentLayoutKey.class)
@NoArgsConstructor @Getter @Setter @ToString
public class CurrentLayout implements Serializable{
	private static final long serialVersionUID = -8272976815048975808L;

	@Id private String topId;
	@Id private String moduleId;
	@Id 
	@Column(insertable = false, updatable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date creationDate;

	@Column(name="layout")
	@Convert(converter = ChannelLayoutListConverter.class)
	@NonNull
	private List<ChannelLayout> layouts;

	public CurrentLayout(String topId, String moduleId, List<ChannelLayout> layouts) {
		this.topId = topId;
		this.moduleId = moduleId;
		setLayouts(layouts);
		creationDate =  new Date(Calendar.getInstance().getTimeInMillis());
	}
}
