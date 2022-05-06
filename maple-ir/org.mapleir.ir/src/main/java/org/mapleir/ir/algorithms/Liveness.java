package org.mapleir.ir.algorithms;

import org.mapleir.ir.locals.Local;

import java.util.Set;

public interface Liveness<N> {

	Set<Local> in(N n);
	
	Set<Local> out(N n);
}