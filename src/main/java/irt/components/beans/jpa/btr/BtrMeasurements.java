package irt.components.beans.jpa.btr;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonFormat;

import irt.components.beans.jpa.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class BtrMeasurements {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long	 serialNumberId;
	private String	 measurement;
	private Long	 userId;

	@Temporal(TemporalType.TIMESTAMP)
	@JsonFormat(pattern="dd MMM yyyy hh:mm")
	@Column(insertable = false, updatable= false)
	private Date date;


	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;
}
