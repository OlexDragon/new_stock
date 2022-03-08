package irt.components.beans.jpa.pn;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table
@IdClass(PnSubtypeKey.class)
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class PnSubtype {

	@Id private Long code;
	@Id private Long nameCode;
	private String type;
}
