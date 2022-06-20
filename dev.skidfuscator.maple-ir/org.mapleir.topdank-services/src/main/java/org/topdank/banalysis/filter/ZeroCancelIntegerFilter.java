package org.topdank.banalysis.filter;

public class ZeroCancelIntegerFilter extends IntegerFilter {
	
	public ZeroCancelIntegerFilter(int number) {
		super(number);
	}
	
	@Override
	public boolean accept(Integer i) {
		if (number == -1)
			return true;
		return super.accept(i);
	}
}