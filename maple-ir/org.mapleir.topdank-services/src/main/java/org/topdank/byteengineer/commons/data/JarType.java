package org.topdank.byteengineer.commons.data;

/**
 * Type of Jar Stored.
 * @author Bibl
 */
public enum JarType {
	
	/** Local file **/
	FILE("file:"),
	/** External URL **/
	WEB("");
	
	private final String prefix;
	
	private JarType(String prefix) {
		this.prefix = prefix;
	}
	
	/** Gets the prefix for the JarURLConnection. **/
	public String prefix() {
		return prefix;
	}
}