package org.topdank.banalysis.filter;

public class IntegerFilter implements Filter<Integer> {
	
	protected int number;
	
	public IntegerFilter(int number) {
		this.number = number;
	}
	
	@Override
	public boolean accept(Integer t) {
		return number == t;
	}
}