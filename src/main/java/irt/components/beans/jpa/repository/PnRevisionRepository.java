package irt.components.beans.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.old.PnRevision;
import irt.components.beans.jpa.old.PnRevisionKey;

public interface PnRevisionRepository extends CrudRepository<PnRevision, PnRevisionKey> {
}
