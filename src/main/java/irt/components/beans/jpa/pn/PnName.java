package irt.components.beans.jpa.pn;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class PnName {

	@Id private Long code;
	private String name;

	@OneToMany(mappedBy="nameCode", fetch = FetchType.EAGER)
	private Set<PnType> pnTypes; 
}
