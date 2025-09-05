package irt.components.beans.jpa.btr;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import irt.components.beans.jpa.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name="btr_comments")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class BtrComment {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long	 id;

	private Long	 serialNumberId;
	private String	 comment;
	private Long	 userId;

	@Temporal(TemporalType.TIMESTAMP)
	@JsonFormat(pattern="dd MMM yyyy kk:mm")
	@Column(insertable = false, updatable= false)
	private Date date;


	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;
}
