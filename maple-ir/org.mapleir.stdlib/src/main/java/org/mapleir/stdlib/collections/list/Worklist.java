package org.mapleir.stdlib.collections.list;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

public class Worklist<N> {

	private final LinkedList<N> worklist;
	private final Set<N> processed;
	private final LinkedBlockingDeque<Worker<N>> workers;

	public Worklist() {
		worklist = new LinkedList<>();
		processed = new HashSet<>();
		workers = new LinkedBlockingDeque<>();
	}

	public void addWorker(Worker<N> w) {
		if (!workers.contains(w)) {
			workers.add(w);
		}
	}

	public void removeWorker(Worker<N> w) {
		if (workers.contains(w)) {
			workers.remove(w);
		}
	}

	public void queueData(N n) {
		worklist.add(n);
	}

	public void queueData(Collection<N> ns) {
		worklist.addAll(ns);
	}

	public void processQueue() {
		while (!worklist.isEmpty()) {
			N m = worklist.removeFirst();

			if (processed.contains(m)) {
				continue;
			}

			process(m);
			processed.add(m);
		}
	}

	protected void process(N n) {
		for (Worker<N> w : workers) {
			w.process(this, n);
		}
	}

	public boolean hasProcessed(N n) {
		return processed.contains(n);
	}

	public int pending() {
		return worklist.size();
	}

	public static interface Worker<N> {
		void process(Worklist<N> worklist, N n);
	}
}