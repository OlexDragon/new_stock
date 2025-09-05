package irt.components.beans.jpa.btr;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString(exclude = {"workOrder", "parent"})
public class BtrSerialNumber implements Serializable{
	private static final long serialVersionUID = 4801466027104313701L;

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long workOrderId;
	private String serialNumber;
	private String partNumber;
	private String description;
	private Long parentId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workOrderId", referencedColumnName = "id", insertable = false, updatable = false)
	private BtrWorkOrder workOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id", referencedColumnName = "parentId", insertable = false, updatable = false)
	private BtrSerialNumber parent;

	@OneToMany(fetch = FetchType.EAGER)
	@JoinColumn(name = "parentId", referencedColumnName = "id", insertable = false, updatable = false)
	@OrderBy("serialNumber")
	private List<BtrSerialNumber> children;

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "serialNumberId", referencedColumnName = "id", insertable = false, updatable = false)
	private List<BtrComment> comments;

	@OneToMany(fetch = FetchType.LAZY)
	@JoinColumn(name = "serialNumberId", referencedColumnName = "id", insertable = false, updatable = false)
	private List<BtrMeasurements> measurements;
}
