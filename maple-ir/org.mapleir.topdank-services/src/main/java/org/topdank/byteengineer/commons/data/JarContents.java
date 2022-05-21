package org.topdank.byteengineer.commons.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mapleir.asm.ClassNode;

public class JarContents {

	private final DataContainer<JarClassData> classContents;
	private final DataContainer<JarResource> resourceContents;

	public JarContents() {
		classContents = new ClassNodeContainer();
		resourceContents = new ResourceContainer();
	}

	public JarContents(DataContainer<JarClassData> classContents, DataContainer<JarResource> classData, DataContainer<JarResource> resourceContents) {
		this.classContents = classContents == null ? new ClassNodeContainer() : classContents;
		this.resourceContents = resourceContents == null ? new ResourceContainer() : resourceContents;
	}

	public final DataContainer<JarClassData> getClassContents() {
		return classContents;
	}
	public final DataContainer<JarResource> getResourceContents() {
		return resourceContents;
	}

	public void merge(JarContents contents) {
		classContents.addAll(contents.classContents);
		resourceContents.addAll(contents.resourceContents);
	}

	public JarContents add(JarContents contents) {
		List<JarClassData> c1 = classContents;
		List<JarClassData> c2 = contents.classContents;

		List<JarResource> r1 = resourceContents;
		List<JarResource> r2 = contents.resourceContents;

		List<JarClassData> c3 = new ArrayList<>(c1.size() + c2.size());
		c3.addAll(c1);
		c3.addAll(c2);

		List<JarResource> r3 = new ArrayList<>(r1.size() + r2.size());
		r3.addAll(r1);
		r3.addAll(r2);


		// TODO add jar data here
		return new JarContents(new ClassNodeContainer(c3), null, new ResourceContainer(r3));
	}

	public static class ClassNodeContainer extends DataContainer<JarClassData> {
		private static final long serialVersionUID = -6169578803641192235L;

		private Map<String, JarClassData> lastMap = new HashMap<>();
		private boolean invalidated;

		public ClassNodeContainer() {
			this(16);
		}

		public ClassNodeContainer(int cap) {
			super(cap);
		}

		public ClassNodeContainer(Collection<JarClassData> data) {
			super(data);
		}

		@Override
		public boolean add(JarClassData c) {
			invalidated = true;
			return super.add(c);
		}

		@Override
		public boolean addAll(Collection<? extends JarClassData> c) {
			invalidated = true;
			return super.addAll(c);
		}

		@Override
		public boolean remove(Object c) {
			invalidated = true;
			return super.remove(c);
		}

		@Override
		public Map<String, JarClassData> namedMap() {
			if (invalidated) {
				invalidated = false;
				Map<String, JarClassData> nodeMap = new HashMap<>();
				Iterator<JarClassData> it = iterator();
				while (it.hasNext()) {
					JarClassData cn = it.next();
					if (nodeMap.containsKey(cn.getName())) {
						it.remove();
					} else {
						nodeMap.put(cn.getName(), cn);
					}
				}
				lastMap = nodeMap;
			}
			return lastMap;
		}
	}

	public static class ResourceContainer extends DataContainer<JarResource> {
		private static final long serialVersionUID = -6169578803641192235L;
		public ResourceContainer() {
			this(16);
		}

		public ResourceContainer(int cap) {
			super(cap);
		}

		public ResourceContainer(List<JarResource> data) {
			addAll(data);
		}

		@Override
		public Map<String, JarResource> namedMap() {
			Map<String, JarResource> map = new HashMap<String, JarResource>();
			for (JarResource resource : this) {
				map.put(resource.getName(), resource);
			}
			return map;
		}
	}
}
