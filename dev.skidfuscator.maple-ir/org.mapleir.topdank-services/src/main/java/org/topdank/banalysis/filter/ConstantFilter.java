package org.topdank.banalysis.filter;

public class ConstantFilter<T> implements Filter<T> {
	
	protected ConstantType type;
	protected T cst;
	
	public ConstantFilter() {
		cst = null;
		type = ConstantType.UNCERTAINTY_NULL;
	}
	
	public ConstantFilter(T cst) {
		if (cst == null) {
			cst = null;
			type = ConstantType.UNCERTAINTY_NULL;
		} else {
			this.cst = cst;
			type = ConstantType.NORMAL;
		}
	}
	
	@Override
	public boolean accept(T t) {
		if (type == ConstantType.UNCERTAINTY_NULL)
			return true;
		return cst.equals(t);
	}
	
	private static enum ConstantType {
		NORMAL(), UNCERTAINTY_NULL();
	}
}