package irt.components.beans.jpa.eco;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import irt.components.beans.jpa.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class Eco {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long	 id;
	private String	 ecoNumber;
	private Integer	 version = 0;
	private String	 partNumber;
	private String	 description;
	private String	 body;
	private Long	 userId;
	@Enumerated(EnumType.ORDINAL)
	private Status	 status = Status.OPEN;
	private Boolean hasFiles;

	@Temporal(TemporalType.TIMESTAMP)
	@JsonFormat(pattern="dd MMM yyyy kk:mm")
	@Column(insertable = false, updatable= false)
	private Date date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;

	public Eco(String ecoNumber, String partNumber, String ecoDescription, String ecoBody, Long userId) {
		this.ecoNumber	 = ecoNumber;
		this.partNumber	 = partNumber;
		this.description = ecoDescription;
		this.body	 = ecoBody;
		this.userId		 = userId;
	}

	public enum Status{
		CLOSED,
		OPEN;
	}
}
