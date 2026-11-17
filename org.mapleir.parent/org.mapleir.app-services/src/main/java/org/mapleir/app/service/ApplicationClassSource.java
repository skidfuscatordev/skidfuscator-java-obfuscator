package org.mapleir.app.service;

import java.util.*;

import org.mapleir.asm.ClassHelper;
import org.mapleir.stdlib.collections.itertools.ChainIterator;
import org.mapleir.asm.ClassNode;

public class ApplicationClassSource extends ClassSource {

	private final String name;
	private final List<LibraryClassSource> libraries;
    private final Map<String, LibraryClassSource> libraryIndex = new HashMap<>();
    private final List<LibraryClassSource> dynamicLibraries = new ArrayList<>();
    private final Map<LibraryClassSource,Integer> libraryRanks = new IdentityHashMap<>();
    private final Map<String, List<String>> duplicateClasses = new TreeMap<>();
    private boolean indexDirty = true;
	protected ClassTree classTree;
	protected boolean phantom;
	
	public ApplicationClassSource(String name, boolean phantom, Collection<ClassNode> classes) {
		this(name, phantom, ClassHelper.convertToMap(classes));
	}
	
	public ApplicationClassSource(String name, boolean phantom, Map<String, ClassNode> nodeMap) {
		super(nodeMap);
		this.name = (name == null ? "unknown" : name);
		this.phantom = phantom;
		libraries = new LinkedList<>();
	}
	
	public List<LibraryClassSource> getLibraries() {
		return Collections.unmodifiableList(libraries);
	}

	protected ClassTree _getClassTree() {
		return classTree;
	}
	
	public ClassTree getClassTree() {
		if (classTree == null) {
			classTree = new ClassTree(this, phantom);
			classTree.init();
		}
		return classTree;
	}
	
	@Override
	public void rebuildTable() {
		indexDirty = true;
		// rebuild app table
		super.rebuildTable();
		// rebuild lib tables
		for(LibraryClassSource lib : libraries) {
			lib.getParent().rebuildTable();
		}
	}
	
	public void addLibraries(LibraryClassSource... libs) {
		for(LibraryClassSource cs : libs) {
			if(!libraries.contains(cs)) {
				libraries.add(cs);
			}
		}

		libraries.sort(Comparator.comparingInt(LibraryClassSource::getPriority).reversed());
        indexDirty = true;
	}
	
    private void ensureLibraryIndex() {
        if (!indexDirty) return;
        libraryIndex.clear(); dynamicLibraries.clear(); duplicateClasses.clear(); libraryRanks.clear();
        // Priority descending; stable insertion order resolves ties. Application wins.
        for (LibraryClassSource source : libraries) {
            libraryRanks.put(source, libraryRanks.size());
            if (!source.isIterable()) { dynamicLibraries.add(source); continue; }
            for (String name : source.classNames()) {
                LibraryClassSource previous = libraryIndex.putIfAbsent(name, source);
                if (previous != null || contains(name)) {
                    List<String> origins = duplicateClasses.computeIfAbsent(name, ignored -> {
                        List<String> values = new ArrayList<>();
                        if (contains(name)) values.add(getName());
                        if (previous != null) values.add(previous.getParent().getName());
                        return values;
                    });
                    origins.add(source.getParent().getName());
                }
            }
        }
        indexDirty = false;
    }

    public Map<String, List<String>> getDuplicateClasses() {
        ensureLibraryIndex();
        Map<String, List<String>> result = new TreeMap<>();
        duplicateClasses.forEach((name, origins) -> result.put(name, Collections.unmodifiableList(new ArrayList<>(origins))));
        return Collections.unmodifiableMap(result);
    }

	public ClassNode findClassNode(String name) {
		LocateableClassNode n = findClass(name);
		
		if(n != null) {
			return n.node;
		} else {
			return null;
		}
	}
	
	@Override
	public LocateableClassNode findClass(String name) {
		if(name == null) {
			return null;
		}
		
		LocateableClassNode node = findClass0(name);
		
		if(node != null) {
			return node;
		} else {
            ensureLibraryIndex();
            LibraryClassSource source = libraryIndex.get(name);
            for (LibraryClassSource dynamic : dynamicLibraries) {
                if (source != null && libraryRanks.get(dynamic) > libraryRanks.get(source)) break;
                if (dynamic.contains(name)) return dynamic.findClass0(name);
            }
            return source == null ? null : source.findClass(name);
		}
	}

	@Override
	public void add(ClassNode node) {
		super.add(node);

		if (classTree != null) classTree.addVertex(node);
        indexDirty = true;
	}

	public boolean isLibraryClass(String name) {
		if(name == null) {
			return false;
		}
		
		/* quick check to see if it's app class instead. 
		 * (prevents attempted loading by runtime lib). */
		if(contains(name)) {
			return false;
		}
		
        ensureLibraryIndex();
        if (libraryIndex.containsKey(name)) return true;
        for (LibraryClassSource dynamic : dynamicLibraries) if (dynamic.contains(name)) return true;
        return false;
	}

	public boolean isApplicationClass(String name) {
		if(name == null) {
			return false;
		}
		
		return contains(name);
	}

	public Iterable<ClassNode> iterateWithLibraries() {
		return iterateWithLibraries(false);
	}
	
	public Iterable<ClassNode> iterateWithLibraries(boolean force) {
		return new Iterable<ClassNode>() {
			Iterator<LibraryClassSource> libIt = null;
			@Override
			public Iterator<ClassNode> iterator() {
				return new ChainIterator<ClassNode>() {
					@Override
					public Iterator<ClassNode> nextIterator() {
						if(libIt == null) {
							libIt = libraries.iterator();
							return ApplicationClassSource.this.iterator();
						} else {
							if(libIt.hasNext()) {
								LibraryClassSource lib = libIt.next();
								if(lib.isIterable() || force) {
									return lib.iterator();
								} else {
									return nextIterator();
								}
							} else {
								return null;
							}
						}
					}
				};
			}
		};
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
