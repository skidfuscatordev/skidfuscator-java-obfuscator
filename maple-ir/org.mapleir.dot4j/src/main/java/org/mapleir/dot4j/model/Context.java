package org.mapleir.dot4j.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.mapleir.dot4j.attr.Attributed;
import org.mapleir.dot4j.attr.Attrs;
import org.mapleir.dot4j.attr.SimpleAttributed;
import org.mapleir.dot4j.attr.builtin.ComplexLabel;

public class Context {

	private static final ThreadLocal<Stack<Context>> CONTEXT = ThreadLocal.withInitial(Stack::new);
	private DotGraph graph;
	private final Map<ComplexLabel, Node> nodes = new HashMap<>();
	private final Attributed<Context> nodeAttributes = new SimpleAttributed<>(this);
	private final Attributed<Context> edgeAttributes = new SimpleAttributed<>(this);
	private final Attributed<Context> graphAttributes = new SimpleAttributed<>(this);

    private Context() {
        this(null);
    }

    private Context(DotGraph graph) {
        this.graph = graph;
    }
    
	public Attributed<Context> nodeAttrs() {
		return nodeAttributes;
	}

	public Attributed<Context> edgeAttrs() {
		return edgeAttributes;
	}

	public Attributed<Context> graphAttrs() {
		return graphAttributes;
	}
	
	private Node newNode(ComplexLabel label) {
		return nodes.computeIfAbsent(label, l -> addNode(new Node().setName(l).with(nodeAttributes)));
	}
	
	private Node addNode(Node node) {
		if(graph != null) {
			graph.addSource(node);
		}
		return node;
	}
	
	private DotGraph newGraph() {
		DotGraph graph = new DotGraph();
		if(this.graph != null) {
			this.graph = graph;
		}
		return graph;
	}
	
	static Edge createEdge(Source<?> source, Target target) {
		Edge edge = new Edge(source, target, Attrs.attrs());
		return current().map(ctx -> edge.with(ctx.edgeAttributes)).orElse(edge);
	}
	
	static Node createNode(ComplexLabel label) {
		return current().map(ctx -> ctx.newNode(label)).orElseGet(() -> new Node().setName(label));
	}
	
	static DotGraph createGraph() {
		return current().map(Context::newGraph).orElseGet(DotGraph::new);
	}
	
    public static <T> T use(ThrowingFunction<Context, T> actions) {
        return use(null, actions);
    }

    public static <T> T use(DotGraph graph, ThrowingFunction<Context, T> actions) {
        Context ctx = begin(graph);
        try {
            return actions.apply(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            end();
        }
    }
    
    public static Context begin(DotGraph graph) {
    	Context ctx = new Context(graph);
        CONTEXT.get().push(ctx);
        return ctx;
    }

    public static void end() {
        Stack<Context> cs = CONTEXT.get();
        if (!cs.empty()) {
            Context ctx = cs.pop();
            if (ctx.graph != null) {
                ctx.graph.getGraphAttr().with(ctx.graphAttributes);
            }
        }
    }
	
	public static Optional<Context> current() {
		Stack<Context> cs = CONTEXT.get();
		return cs.empty() ? Optional.empty() : Optional.of(cs.peek());
	}
	
	public static Context get() {
		Stack<Context> cs = CONTEXT.get();
		if(cs.empty()) {
			throw new IllegalStateException("Not in a context");
		}
		return cs.peek();
	}
}
