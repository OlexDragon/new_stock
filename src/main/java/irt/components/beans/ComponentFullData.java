package irt.components.beans;

import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @ToString
public class ComponentFullData {

	private Component component;
	private final ComponentQuantityResponse componentQuantityResponse;
	@Setter
	private BomsResponse bomsResponse;
	@Setter
	private RelatedFilesResponse relatedFilesResponse;

	public ComponentFullData(ComponentQuantityResponse componentQuantityResponse) {

		this.componentQuantityResponse = componentQuantityResponse;

		// Set Component
		Optional.ofNullable(componentQuantityResponse).map(ComponentQuantityResponse::getComponentQuantities).filter(cqs->cqs!=null && cqs.length>0).map(cqs->cqs[0]).map(ComponentQuantity::getComponent)
		.ifPresent(c->component=c);
	}
}
