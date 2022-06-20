package org.mapleir.serviceframework.api;

public interface IServiceQuery<T> {

	boolean accept(IServiceReference<T> ref);
}