package org.mapleir.dot4j.model;

import java.util.Optional;
import java.util.stream.Stream;

public enum Compass {
	NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST, CENTER;
	
	final String value;
	
	private Compass() {
		value = getDotName(name());
	}
	
	private static String getDotName(String name) {
		StringBuilder sb = new StringBuilder();
		String[] words = name.split("_");
		for(String word : words) {
			sb.append(Character.toLowerCase(word.charAt(0)));
		}
		return sb.toString();
	}
	
    public static Optional<Compass> of(String value) {
        return Stream.of(values()).filter(c -> c.value.equals(value)).findFirst();
    }
}
