package org.mapleir.context;

import org.mapleir.app.client.ApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.InvocationResolver;
import org.mapleir.deob.dataflow.DataFlowAnalysis;
import org.mapleir.deob.intraproc.DumbExceptionAnalysis;
import org.mapleir.deob.intraproc.ExceptionAnalysis;
import org.mapleir.ir.cfg.ControlFlowGraph;

public class BasicAnalysisContext implements AnalysisContext {
	private final DumbExceptionAnalysis exceptionAnalysis = new DumbExceptionAnalysis();
	
	private final ApplicationClassSource app;
	private final InvocationResolver resolver;
	private final IRCache cache;
	private final DataFlowAnalysis dfa;
	private ApplicationContext appCxt;
	
	private BasicAnalysisContext(BasicContextBuilder b) {
		app = b.app;
		resolver = b.resolver;
		cache = b.cache;
		dfa = b.dfa;
		appCxt = b.appCxt;
	}
	
	@Override
	public IRCache getIRCache() {
		return cache;
	}
	@Override
	public InvocationResolver getInvocationResolver() {
		return resolver;
	}

	@Override
	public ApplicationClassSource getApplication() {
		return app;
	}

	@Override
	public ExceptionAnalysis getExceptionAnalysis(ControlFlowGraph cfg) {
		return exceptionAnalysis;
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return appCxt;
	}

	@Override
	public DataFlowAnalysis getDataflowAnalysis() {
		return dfa;
	}

	public static class BasicContextBuilder {
		private ApplicationClassSource app;
		private InvocationResolver resolver;
		private IRCache cache;
		private DataFlowAnalysis dfa;
		private ApplicationContext appCxt;
		
		public BasicContextBuilder() {
		}

		public BasicContextBuilder setApplicationContext(ApplicationContext appCxt) {
			this.appCxt = appCxt;
			return this;
		}

		public BasicContextBuilder setApplication(ApplicationClassSource app) {
			this.app = app;
			return this;
		}

		public BasicContextBuilder setInvocationResolver(InvocationResolver resolver) {
			this.resolver = resolver;
			return this;
		}

		public BasicContextBuilder setCache(IRCache cache) {
			this.cache = cache;
			return this;
		}

		public BasicContextBuilder setDataFlowAnalysis(DataFlowAnalysis dfa) {
			this.dfa = dfa;
			return this;
		}
		
		public AnalysisContext build() {
			return new BasicAnalysisContext(this);
		}
	}
}
