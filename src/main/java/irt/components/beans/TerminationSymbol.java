package irt.components.beans;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public enum TerminationSymbol {
	NON 	(null),
	ZERO	((byte) 0),
	LF		((byte)'\n'),
	IRT 	((byte) 126);

	private final Byte symbol;
}
