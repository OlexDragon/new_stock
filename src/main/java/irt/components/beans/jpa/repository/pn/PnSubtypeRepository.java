package irt.components.beans.jpa.repository.pn;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.pn.PnSubtype;
import irt.components.beans.jpa.pn.PnSubtypeKey;

public interface PnSubtypeRepository extends CrudRepository<PnSubtype, PnSubtypeKey> {

	List<PnSubtype> findByNameCodeOrderByType(Long nameCode);
}
