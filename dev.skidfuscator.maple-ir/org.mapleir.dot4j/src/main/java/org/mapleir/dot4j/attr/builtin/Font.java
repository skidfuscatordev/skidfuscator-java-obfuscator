package org.mapleir.dot4j.attr.builtin;

import static org.mapleir.dot4j.attr.Attrs.*;

import org.mapleir.dot4j.attr.Attrs;

public class Font {

	private Font() {
	}

	public static Attrs config(String name, int size) {
		return attrs(name(name), size(size));
	}

	public static Attrs config(String name, double size, double dpi) {
		return attrs(name(name), size(size), dpi(dpi));
	}

	public static Attrs name(String name) {
		return attr("fontname", name);
	}

	public static Attrs size(double size) {
		return attr("fontsize", size);
	}
	
	public static Attrs dpi(double dpi) {
		return attr("dpi", dpi);
	}
}
