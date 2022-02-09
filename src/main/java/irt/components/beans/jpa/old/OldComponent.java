package irt.components.beans.jpa.old;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "components")
@NoArgsConstructor @AllArgsConstructor @Getter @Setter @ToString
public class OldComponent {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String partNumber;
	private String manufPartNumber;
	private String description;

	@OneToMany(mappedBy="idComponents", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("pnRevision DESC")
	private Set<PnRevision> revisions;
}
