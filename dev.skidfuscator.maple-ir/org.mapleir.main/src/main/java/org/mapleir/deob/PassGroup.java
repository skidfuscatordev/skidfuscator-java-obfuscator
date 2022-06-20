package org.mapleir.deob;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.mapleir.Boot;

public class PassGroup implements IPass {

	private final String name;
	private final List<IPass> passes;
	
	public PassGroup(String name) {
		this.name = name;
		passes = new ArrayList<>();
	}
	
	public PassGroup add(IPass p) {
		passes.add(p);
		return this;
	}

	public PassGroup add(IPass... p) {
		passes.addAll(Arrays.asList(p));
		return this;
	}
	
	public PassGroup remove(IPass p) {
		passes.remove(p);
		return this;
	}
	
	public IPass getPass(Predicate<IPass> p) {
		List<IPass> list = getPasses(p);
		if(list.size() == 1) {
			return list.get(0);
		} else {
			return null;
		}
	}
	
	public List<IPass> getPasses(Predicate<IPass> p) {
		return passes.stream().filter(p).collect(Collectors.toList());
	}

	@Override
	public PassResult accept(PassContext pcxt) {		
		List<IPass> completed = new ArrayList<>();
		Map<IPass, PassResult> lastResults = new HashMap<>();
		IPass last = null;

		Throwable error = null;

		outer: for(;;) {
			last = null;
			completed.clear();

			if(name != null) {
				System.out.printf("Running %s group.%n", name);
			}
			boolean redoRound = false;
			for(int i=0; i < passes.size(); i++) {
				IPass p = passes.get(i);

				PassResult lastResult = lastResults.get(p);
				if(lastResult != null) {
					if(!lastResult.shouldRepeat()) {
						continue;
					}
				}

				if(Boot.logging) {
					Boot.section0("...took %fs." + (i == 0 ? "%n" : ""), "Running " + p.getId());
				} else {
					System.out.println("Running " + p.getId());
				}
				PassContext newCxt = new PassContext(pcxt.getAnalysis(), last, new ArrayList<>(completed));
				PassResult newResult;
				try {
					newResult = p.accept(newCxt);
					lastResults.put(p, newResult);
				} catch(Throwable t) {
					error = t;
					break outer;
				}

				if(!newResult.shouldContinue()) {
					error = newResult.getError();
					break outer;
				}

				redoRound |= (newResult.shouldRepeat() && newResult.shouldContinue());

				completed.add(p);
				last = p;
			}

			if(!redoRound) {
				break;
			}

			System.out.println();
			System.out.println();
		}

		if(error != null) {
			return PassResult.with(pcxt, this).fatal(error).make();
		} else {
			return PassResult.with(pcxt, this).finished().make();
		}
	}
}
