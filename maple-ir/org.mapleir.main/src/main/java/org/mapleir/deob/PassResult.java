package org.mapleir.deob;

public class PassResult {

	private final PassContext cxt;
	private final IPass pass;
	private final boolean shouldRepeat;
	private final boolean shouldContinue;
	private final Throwable error;
	
	PassResult(PassResult result, Throwable error) {
		this.cxt = result.cxt;
		this.pass = result.pass;
		this.error = error;
		shouldRepeat = false;
		shouldContinue = false;
	}
	
	private PassResult(PassContext cxt, IPass pass, boolean shouldRepeat, boolean shouldContinue, Throwable error) {
		this.cxt = cxt;
		this.pass = pass;
		this.shouldRepeat = shouldRepeat;
		this.shouldContinue = shouldContinue;
		this.error = error;
	}
	
	public PassContext getContext() {
		return cxt;
	}
	
	public IPass getPass() {
		return pass;
	}
	
	public boolean shouldContinue() {
		return shouldContinue;
	}
	
	public boolean shouldRepeat() {
		return shouldRepeat;
	}
	
	public Throwable getError() {
		return error;
	}
	
	public static PassResultBuilder with(PassContext cxt, IPass pass) {
		return new PassResultBuilder(cxt, pass);
	}

	public static class PassResultBuilder {
		private PassContext cxt;
		private IPass pass;
		private boolean shouldRepeat;
		private boolean shouldContinue;
		private Throwable error;
		
		private PassResultBuilder(PassContext cxt, IPass pass) {
			this.cxt = cxt;
			this.pass = pass;
			shouldContinue = true;
		}
		
		public PassResultBuilder finished() {
			shouldRepeat = false;
			return this;
		}
		
		public PassResultBuilder finished(int delta) {
			if(delta <= 0) {
				shouldRepeat = false;
			}
			return this;
		}
		
		public PassResultBuilder stop() {
			shouldContinue = false;
			return this;
		}
		
		public PassResultBuilder fatal(Throwable error) {
			this.error = error;
			shouldContinue = false;
			shouldRepeat = false;
			return this;
		}
		
		public PassResult make() {
			return new PassResult(cxt, pass, shouldRepeat, shouldContinue, error);
		}
	}
}
