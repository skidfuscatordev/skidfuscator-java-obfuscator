package org.mapleir.dot4j.attr.builtin;

import org.mapleir.dot4j.attr.Attr;

public class RankDir extends Attr<String> {
	public static final RankDir TOP_TO_BOTTOM = new RankDir("TB"), BOTTOM_TO_TOP = new RankDir("BT"),
			LEFT_TO_RIGHT = new RankDir("LR"), RIGHT_TO_LEFT = new RankDir("RL");

	protected RankDir(String value) {
		super("rankdir", value);
	}
}
