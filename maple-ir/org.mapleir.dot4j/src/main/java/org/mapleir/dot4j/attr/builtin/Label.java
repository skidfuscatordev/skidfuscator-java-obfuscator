package org.mapleir.dot4j.attr.builtin;

public class Label {

	final String value;
	final boolean html;
	
	public Label(String value, boolean html) {
		this.value = value;
		this.html = html;
	}
	
	public boolean isHtml() {
		return html;
	}
	
	public String serialised() {
		if(html) {
			return "<" + value + ">";
		} else {
			return "\"" + value.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
		}
	}
	
	@Override
	public String toString() {
		return value;
	}
	
	public static Label of(String value) {
		return new Label(value, false);
	}

	public static Label of(Object value) {
		return value instanceof Label ? (Label) value : of(value.toString());
	}
}
