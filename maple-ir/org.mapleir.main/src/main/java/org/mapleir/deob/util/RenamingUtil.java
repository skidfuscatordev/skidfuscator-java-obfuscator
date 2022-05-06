package org.mapleir.deob.util;

public class RenamingUtil {

	public static final int AAA = 703;
	
	public static String createName(int n) {
		char[] buf = new char[(int) Math.floor(Math.log(25 * (n + 1)) / Math.log(26))];
		for (int i = buf.length - 1; i >= 0; i--) {
			buf[i] = (char) ('a' + (--n) % 26);
			n /= 26;
		}
		return String.valueOf(buf);
	}
	
	public static int numeric(String label) {
		int result = 0;
		for (int i = label.length() - 1; i >= 0; i--)
			result = result + (label.charAt(i) - 96) * (int) Math.pow(26, label.length() - (i + 1));
		return result;
	}
	
	public static int computeMinimum(int count) {
		/* char[] buf = new char[(int) Math.floor(Math.log(25 * (count + 1)) / Math.log(26))];
		for(int i=0; i < buf.length; i++) {
			buf[i] = 'a';
		}
		return String.valueOf(buf); */
		
		/* 1   - a
		 * 27  - aa
		 * 703 - aaa
		 * 
		 * etc*/
		int j = (int) Math.floor(Math.log(25 * (count + 1)) / Math.log(26));
		int r = 0;
		for (int i = j - 1; i >= 0; i--) {
			r += (int) Math.pow(26, j - (i + 1));
		}
		return r;
	}
	
	public static String getPackage(String name) {
		return name.substring(0, name.lastIndexOf('/') + 1);
	}
	
	public static String getClassName(String name) {
		return name.substring(name.lastIndexOf('/') + 1);
	}
}