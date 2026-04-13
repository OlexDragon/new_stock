package irt.components.beans.jpa.rma;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import irt.components.beans.DateContainer;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class Rma implements DateContainer{

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long	 id;
	private String	 rmaNumber;
	private String	 serialNumber;
	private String	 partNumber;
	private String	 description;
	private Long	 userId;
	@Enumerated(EnumType.ORDINAL)
	private Status	 status;

//	@Temporal(TemporalType.TIMESTAMP)
	@JsonFormat(pattern="dd MMM yyyy kk:mm")
	@Column(columnDefinition = "TIMESTAMP", insertable = false, updatable= false)
	private Date date;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "userId", referencedColumnName = "id", insertable = false, updatable = false)
	private User user;

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "rmaId", referencedColumnName = "id", insertable = false, updatable = false)
	private List<RmaComment> rmaComments;

	@RequiredArgsConstructor @Getter
	public enum Status{
		IN_WORK		("Unit in work"							, ""),
		SHIPPED		("Shipped Units"						, "border border-5 border-dark"),
		READY		("Ready To Ship"						, "bg-warning-subtle border border-5 border-success"),
		CREATED		("Created RMA Number"					, "border border-5 border-warning"),
		CLOSED		("Units that cannot be repaired."		, "bg-danger-subtle border border-5 border-dark"),
		FIXED		("Repaired units"						, "border border-5 border-success"),
		WAITTING	("Waiting for the manager's decision."	, "bg-warning-subtle border border-5 border-danger"),
		FINALIZED	("Finalized"							, "bg-light-subtle border border-5 border-success");

		private final String description;
		private final String bootstrapCalsses;
	}
}
