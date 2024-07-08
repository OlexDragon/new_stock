package irt.components.beans.jpa.repository;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;

public interface IrtArrayRepository extends CrudRepository<IrtArray, IrtArrayId> {
}
