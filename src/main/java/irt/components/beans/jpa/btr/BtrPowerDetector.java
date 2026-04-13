package irt.components.beans.jpa.btr;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import irt.components.beans.jpa.User;
import irt.components.services.converter.JSonMapOfMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @RequiredArgsConstructor @Getter @Setter @ToString
public class BtrPowerDetector {

	@Id
	private Long	 serialNumberId;

	@NonNull
	@Convert(converter = JSonMapOfMapConverter.class)
	private Map<String, Map<String, String>>	 measurement;

	@NonNull
	private Long	 userId;

//	@Temporal(TemporalType.TIMESTAMP)
	@JsonFormat(pattern="dd MMM yyyy kk:mm")
	@Column(columnDefinition = "TIMESTAMP", insertable = false, updatable= false)
	private Date date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;
}
