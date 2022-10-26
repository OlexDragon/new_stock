package irt.components.beans.jpa.btr;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @Getter @Setter @ToString @EqualsAndHashCode(exclude = {"number", "btrSerialNumbers"})
public class BtrWorkOrder implements Comparable<BtrWorkOrder>, Serializable {
	private static final long serialVersionUID = 7170319508920385541L;

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String number;

	@OneToMany(mappedBy = "workOrderId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<BtrSerialNumber> btrSerialNumbers;

	@Override
	public int compareTo(BtrWorkOrder o) {
		return o.getNumber().compareTo(getNumber());
	}
}
