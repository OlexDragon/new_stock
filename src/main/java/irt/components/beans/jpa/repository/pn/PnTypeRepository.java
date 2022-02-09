package irt.components.beans.jpa.repository.pn;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.pn.PnType;
import irt.components.beans.jpa.pn.PnTypeKey;

public interface PnTypeRepository extends CrudRepository<PnType, PnTypeKey> {
}
