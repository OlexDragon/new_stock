package irt.components.beans.jpa.repository.btr;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import irt.components.beans.jpa.btr.BtrWorkOrder;

public interface BtrWorkOrderRepository extends CrudRepository<BtrWorkOrder, Long> {

	Optional<BtrWorkOrder> findByNumber(String woNumber);
	List<BtrWorkOrder> findByNumberContainingOrderByNumberDesc(String value);
}
