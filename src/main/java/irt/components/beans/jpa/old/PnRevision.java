package irt.components.beans.jpa.old;

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
@IdClass(PnRevisionKey.class)
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class PnRevision {

	@Id private Long idComponents;
	@Id private Long pnRevision;
}
