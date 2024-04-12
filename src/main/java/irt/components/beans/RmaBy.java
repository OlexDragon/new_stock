package irt.components.beans;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import irt.components.beans.jpa.repository.rma.RmaRepository;
import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;

public abstract class RmaBy {

	static RmaRepository rmaRepository;

	public static void setRepository(RmaRepository rmaRepository) {
		RmaBy.rmaRepository = rmaRepository;
	}

	public static RmaBy bySerialNumber() {
		return new RmaBySerialNumber();
	}

	public static RmaBy byRmaNumber() {
		return new RmaByRmaNumber();
	}

	public static RmaBy byDescription() {
		return new RmaByDescription();
	}

	public abstract Supplier<List<Rma>> containing(String value, String sortBy, int size);
	public abstract Function<Status[], List<Rma>> containingIn(String value, String sortBy, int size);
}
