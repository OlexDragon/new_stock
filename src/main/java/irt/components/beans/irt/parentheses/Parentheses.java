package irt.components.beans.irt.parentheses;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

 @RequiredArgsConstructor @Getter @Setter @ToString(exclude = {"parent"})
public class Parentheses {

	private final Parentheses parent;
	private final Character startParenthese;
	private final Character stopParenthese;

	private Integer startIndex;
	private Integer stopIndex;

	private final List<Parentheses> internalParentheses = new ArrayList<>();
}
