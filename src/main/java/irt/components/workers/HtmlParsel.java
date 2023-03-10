package irt.components.workers;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;

import irt.components.services.exception.HtmlParsingException;
import javafx.util.Pair;

public class HtmlParsel extends ParserCallback {

	private final List<Integer> startPositin = new ArrayList<>();
	private final List<Integer> stopPositin = new ArrayList<>();
	private String tagName;

	public HtmlParsel(String tagName) {
		this.tagName = tagName;
	}

	@Override
	public void handleStartTag(Tag tag, MutableAttributeSet a, int pos) {
		if(tag.toString().equals(tagName)) 
			startPositin.add(pos);
	}

	@Override
	public void handleEndTag(Tag tag, int pos) {
		if(tag.toString().equals(tagName)) 
			stopPositin.add(0, pos);
	}

	public int size() {
		throwIfError();
		return startPositin.size();
	}

	public Pair<Integer, Integer> getPositions(int index){
		throwIfError();
		return new Pair<>(startPositin.get(index), stopPositin.get(index));
	}

	private void throwIfError() throws HtmlParsingException {

		if(startPositin.size() > stopPositin.size())
			throw new HtmlParsingException("HTML missing end tag " + tagName);

		if(startPositin.size() < stopPositin.size())
			throw new HtmlParsingException("HTML missing start tag " + tagName);
	}
}
