package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;

public class Style extends Attr<String> {

	public static final Style DASHED = new Style("dashed"), DOTTED = new Style("dotted"), SOLID = new Style("solid"),
			INVIS = new Style("invis"), BOLD = new Style("bold"), FILLED = new Style("filled"),
			RADIAL = new Style("radial"), DIAGONALS = new Style("diagonals"), ROUNDED = new Style("rounded");

	public Style(String value) {
		super("style", value);
	}

	public static Style lineWidth(int width) {
		return new Style("setlinewidth(" + width + ")");
	}

	public Style and(Style style) {
		return new Style(value + "," + style.value);
	}
}
