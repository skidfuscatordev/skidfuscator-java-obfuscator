package org.mapleir.deob.util;

public interface RenamingHeuristic {
	boolean shouldRename(String name, int access);

	RenamingHeuristic RENAME_NONE = (name, access) -> false;
	RenamingHeuristic RENAME_ALL = (name, access) -> true;
	RenamingHeuristic ALLATORI = (name, access) -> name.toLowerCase().equals("iiiiiiiiii");
	RenamingHeuristic NON_PRINTABLE = (name, access) -> {
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) < 0x20|| name.charAt(i) >= 0x7f) {
				return true;
			}
		}
		return false;
	};
}
