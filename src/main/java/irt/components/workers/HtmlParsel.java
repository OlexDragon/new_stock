package irt.components.workers;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;
import javax.swing.text.html.parser.ParserDelegator;

import org.springframework.data.util.Pair;

import irt.components.services.exception.HtmlParsingException;

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
			stopPositin.add(pos);
	}

	public int size() {
		throwIfError();
		return startPositin.size();
	}

	public Pair<Integer, Integer> getPositions(int index){
		throwIfError();
		return Pair.of(startPositin.get(index), stopPositin.get(index));
	}

	private void throwIfError() throws HtmlParsingException {

		if(startPositin.size() > stopPositin.size())
			throw new HtmlParsingException("HTML missing end tag " + tagName);

		if(startPositin.size() < stopPositin.size())
			throw new HtmlParsingException("HTML missing start tag " + tagName);
	}

	public String parseFirst(String html) throws IOException {

        try(final StringReader reader = new StringReader(html);){
        	new ParserDelegator().parse(reader, this, true);
        	if(size()>0) {
        		final Pair<Integer, Integer> positions = getPositions(0);
           		final Integer start = positions.getFirst();
           		final Integer stop = positions.getSecond();

           		return html.substring(start, stop);
         	}
        }
		return null;
	}

	public List<String> parseAll(String html) throws IOException{

		List<String> list = new ArrayList<String>();

		try(final StringReader reader = new StringReader(html);){
        	new ParserDelegator().parse(reader, this, true);
        	final int size = size();

        	for(int i=0; i<size; i++) {

        		final Pair<Integer, Integer> positions = getPositions(i);
           		final Integer start = positions.getFirst();
           		final Integer stop = positions.getSecond();
           		list.add(html.substring(start, stop));
         	}
        }
		return list;
	}
}
