package org.mapleir.dot4j.attr;

public interface Attrs {

	Attrs applyTo(MapAttrs mapAttrs);
	
	default Attrs applyTo(Attrs attrs) {
		if(attrs instanceof MapAttrs) {
			return applyTo((MapAttrs) attrs);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	default Object get(String key) {
		return applyTo(new MapAttrs()).get(key);
	}
	
	default boolean isEmpty() {
		return applyTo(new MapAttrs()).isEmpty();
	}
	
	static Attrs attr(String key, Object value) {
		return new MapAttrs().put(key, value);
	}
	
	static Attrs attrs(Attrs... attrss) {
		MapAttrs mapAttrs = new MapAttrs();
		for(Attrs attrs : attrss) {
			attrs.applyTo(mapAttrs);
		}
		return mapAttrs;
	}
}
