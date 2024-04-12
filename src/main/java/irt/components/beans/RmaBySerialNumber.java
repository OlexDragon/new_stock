package irt.components.beans;

import static irt.components.controllers.rma.RmaController.getPageRequest;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.data.domain.PageRequest;

import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;

public class RmaBySerialNumber extends RmaBy {

	public RmaBySerialNumber() {
		Objects.requireNonNull(rmaRepository);
	}

	@Override
	public Supplier<List<Rma>> containing(String value, String sortBy, int size) {
		final PageRequest pageRequest = getPageRequest(sortBy, size);
		return ()->rmaRepository.findBySerialNumberContaining(value, pageRequest);
	}

	@Override
	public Function<Status[], List<Rma>> containingIn(String value, String sortBy, int size) {

		final PageRequest pageRequest = getPageRequest(sortBy, size);
//		LogManager.getLogger().error("value: {}; sortBy: {}; size: {}; pageRequest: {}; rmaRepository: {};", value, sortBy, size, pageRequest, rmaRepository);
		return status->rmaRepository.findBySerialNumberContainingAndStatusIn(value, pageRequest, status);
	}
}
