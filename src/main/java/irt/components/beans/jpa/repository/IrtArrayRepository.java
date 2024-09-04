package irt.components.beans.jpa.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.IrtArray;
import irt.components.beans.jpa.IrtArrayId;

public interface IrtArrayRepository extends CrudRepository<IrtArray, IrtArrayId> {

	List<IrtArray> findByIrtArrayIdName(String string);
}
