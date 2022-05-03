package irt.components.beans.irt.parentheses;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class JSonTree {

	public JSonTree(String text) {
		getParentheses(text);
	}


	private List<Parentheses> getParentheses(String text) {

		final AtomicInteger index = new AtomicInteger();

		Parentheses parent = new Parentheses(null, null, null);
		scanForParentheses(text, index, parent );

		return parent.getInternalParentheses();
	}

	private void scanForParentheses(String text, final AtomicInteger index, Parentheses parent) {

		do {

			final Optional<Parentheses> oParentheses = getParenthesesObject(text.charAt(index.get()), parent);
			if(oParentheses.isPresent()) {

				final Parentheses parentheses = oParentheses.get();
				parentheses.setStartIndex(index.get());
				parent.getInternalParentheses().add(parentheses);
				index.incrementAndGet();
				scanForParentheses(text, index, parentheses);
				
			}else if(endParentheseCheck(parent, text, index.get()))
					return;

		}while(index.incrementAndGet() < text.length());
	}

	private Optional<Parentheses> getParenthesesObject(char charAt, Parentheses parent) {

		if(charAt == '[')
			return Optional.of(new SquareBracket(parent));

		if(charAt == '{')
			return Optional.of(new Brace(parent));

		return Optional.empty();
	}

	private boolean endParentheseCheck(Parentheses parentheses, String text, int index) {

		final char charAt = text.charAt(index);

		if(charAt == '}') 
			if(parentheses.getStopParenthese() == '}') {
				parentheses.setStopIndex(index);
				return true;
			}else
				throw new RuntimeException("Parentheses mismatch: " + text);

		if(charAt == ']') 
			if(parentheses.getStopParenthese() == ']') {
				parentheses.setStopIndex(index);
				return true;
			}else
				throw new RuntimeException("Parentheses mismatch: " + text);
			

		return false;
	}
}
