package org.mapleir;

import org.apache.log4j.Logger;
import org.mapleir.app.client.SimpleApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.context.AnalysisContext;
import org.mapleir.context.BasicAnalysisContext;
import org.mapleir.context.IRCache;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.PassResult;
import org.mapleir.deob.dataflow.LiveDataFlowAnalysisImpl;
import org.mapleir.deob.util.RenamingHeuristic;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.code.expr.invoke.InvocationExpr;
import org.mapleir.stdlib.util.JavaDesc;
import org.mapleir.stdlib.util.JavaDescSpecifier;
import org.mapleir.stdlib.util.JavaDescUse;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.SingleJarDownloader;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class DataFlowDemoBoot {

	private static final Logger LOGGER = Logger.getLogger(DataFlowDemoBoot.class);

	public static boolean logging = false;
	private static long timer;
	private static Deque<String> sections;

	private static LibraryClassSource rt(ApplicationClassSource app, File rtjar) throws IOException {
		section("Loading " + rtjar.getName() + " from " + rtjar.getAbsolutePath());
		SingleJarDownloader<ClassNode> dl = new SingleJarDownloader<>(new JarInfo(rtjar));
		dl.download();

		return null; //new LibraryClassSource(app, dl.getJarContents().getClassContents());
	}

	public static void main(String[] args) throws Exception {

		sections = new LinkedList<>();
		logging = true;

		// Load input jar
		//  File f = locateRevFile(135);
		File f = new File("res/jump.jar");

		section("Preparing to run on " + f.getAbsolutePath());
		SingleJarDownloader<ClassNode> dl = new SingleJarDownloader<>(new JarInfo(f));
		dl.download();
		String appName = f.getName().substring(0, f.getName().length() - 4);
		ApplicationClassSource app = new ApplicationClassSource(
				appName,
				false,
				dl.getJarContents().getClassContents().stream().map(JarClassData::getClassNode).collect(Collectors.toList())
		);
//		
// 		ApplicationClassSource app = new ApplicationClassSource("test", ClassHelper.parseClasses(CGExample.class));
//		app.addLibraries(new InstalledcoRuntimeClassSource(app));

		File rtjar = new File("res/rt.jar");
		File androidjar = new File("res/android.jar");
		app.addLibraries(rt(app, rtjar), rt(app, androidjar));
		section("Initialising context.");


		IRCache irFactory = new IRCache(ControlFlowGraphBuilder::build);
		AnalysisContext cxt = new BasicAnalysisContext.BasicContextBuilder()
				.setApplication(app)
				.setInvocationResolver(new DefaultInvocationResolver(app))
				.setCache(irFactory)
				.setApplicationContext(new SimpleApplicationContext(app))
				.setDataFlowAnalysis(new LiveDataFlowAnalysisImpl(irFactory))
				.build();

		section("Expanding callgraph and generating cfgs.");
		// IRCallTracer tracer = new IRCallTracer(cxt);
		// for(MethodNode m : cxt.getApplicationContext().getEntryPoints()) {
		// 	tracer.trace(m);
		// }

		// traceCalls(cxt, new JavaDescSpecifier("lol", "lol", ".*", JavaDesc.DescType.METHOD));

		for (ClassNode cn : cxt.getApplication().iterate()) {
//			 if (!cn.name.equals("android/support/v4/media/session/MediaSessionCompat$MediaSessionImplApi18"))
//			 	continue;
			for (MethodNode m : cn.getMethods()) {
//				 if (!m.name.equals("setRccState"))
//				 	continue;
				cxt.getIRCache().getFor(m);
			}
		}
		section0("...generated " + cxt.getIRCache().size() + " cfgs in %fs.%n", "Preparing to transform.");

		// do passes
		PassGroup masterGroup = new PassGroup("MasterController");
		for (IPass p : getTransformationPasses()) {
			masterGroup.add(p);
		}
		run(cxt, masterGroup);
		section0("...done transforming in %fs.%n", "Preparing to transform.");


        // enumerateStrings(cxt);

        // xrefConsole(cxt);

		for(Entry<MethodNode, ControlFlowGraph> e : cxt.getIRCache().entrySet()) {
			MethodNode mn = e.getKey();
			ControlFlowGraph cfg = e.getValue();
			cfg.verify();
		}
    }

    private static void xrefConsole(AnalysisContext cxt) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("xref> ");
            String l = sc.nextLine();
            if (l.isEmpty())
                break;
            String[] parts = l.split("#");
            String owner = parts[0];
            String[] parts2 = parts[1].split(" ");
            String name = parts2[0];
            String desc = parts2[1];
            JavaDesc.DescType type = JavaDesc.DescType.valueOf(parts2[2].toUpperCase());
            cxt.getDataflowAnalysis().findAllRefs(new JavaDescSpecifier(owner, name, desc, type))
                    .filter(cu -> cu.flowType == JavaDescUse.UseType.CALL)
                    .map(cu -> (InvocationExpr)cu.flowElement).forEach(ie -> {
                System.out.println("xref-> " + ie.getBlock().getGraph().getJavaDesc());
                System.out.println(ie.toString());
            });
        }
    }

    private static void enumerateStrings(AnalysisContext cxt) {
        cxt.getDataflowAnalysis().enumerateConstants().forEach(ce -> {
            if (ce.getConstant() != null && ce.getConstant() instanceof String) {
                System.out.println(ce.getBlock().getGraph().getJavaDesc() + ", " + ce.getConstant());
            }
        });
    }

    private static void run(AnalysisContext cxt, PassGroup group) {
		PassContext pcxt = new PassContext(cxt, null, new ArrayList<>());
		PassResult result = group.accept(pcxt);

		if(result.getError() != null) {
			throw new RuntimeException(result.getError());
		}
	}

	private static IPass[] getTransformationPasses() {
		RenamingHeuristic heuristic = RenamingHeuristic.RENAME_ALL;
		return new IPass[] {
//				new ConcreteStaticInvocationPass(),
//				new ClassRenamerPass(heuristic),
//				new MethodRenamerPass(heuristic),
//				new FieldRenamerPass(),
//				new CallgraphPruningPass(),

				// new PassGroup("Interprocedural Optimisations")
				// 	.add(new ConstantParameterPass())
				// new LiftConstructorCallsPass(),
//				 new DemoteRangesPass(),

				// new ConstantExpressionReorderPass(),
				// new FieldRSADecryptionPass(),
				// new ConstantParameterPass(),
//				new ConstantExpressionEvaluatorPass(),
// 				new DeadCodeEliminationPass()

		};
	}

	static File locateRevFile(int rev) {
		return new File("res/gamepack" + rev + ".jar");
	}

	private static Set<MethodNode> findEntries(ApplicationClassSource source) {
		Set<MethodNode> set = new HashSet<>();
		/* searches only app classes. */
		for(ClassNode cn : source.iterate())  {
			for(MethodNode m : cn.getMethods()) {
				if((m.getName().length() > 2 && !m.getName().equals("<init>")) || m.node.instructions.size() == 0) {
					set.add(m);
				}
			}
		}
		return set;
	}

	private static double lap() {
		long now = System.nanoTime();
		long delta = now - timer;
		timer = now;
		return (double)delta / 1_000_000_000L;
	}

	public static void section0(String endText, String sectionText) {
		if(sections.isEmpty()) {
			lap();
			LOGGER.info(sectionText);
		} else {
			/* remove last section. */
			sections.pop();
			LOGGER.info(String.format(endText, lap()));
			LOGGER.info(sectionText);
		}

		/* push the new one. */
		sections.push(sectionText);
	}

	private static void section(String text) {
		section0("...took %fs.", text);
	}
}
