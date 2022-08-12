package org.mapleir;

import org.apache.log4j.Logger;
import org.mapleir.app.client.SimpleApplicationContext;
import org.mapleir.app.service.ApplicationClassSource;
import org.mapleir.app.service.CompleteResolvingJarDumper;
import org.mapleir.app.service.LibraryClassSource;
import org.mapleir.context.AnalysisContext;
import org.mapleir.context.BasicAnalysisContext;
import org.mapleir.context.IRCache;
import org.mapleir.deob.IPass;
import org.mapleir.deob.PassContext;
import org.mapleir.deob.PassGroup;
import org.mapleir.deob.PassResult;
import org.mapleir.deob.dataflow.LiveDataFlowAnalysisImpl;
import org.mapleir.deob.passes.*;
import org.mapleir.deob.passes.constparam.ConstantExpressionEvaluatorPass;
import org.mapleir.deob.passes.rename.ClassRenamerPass;
import org.mapleir.deob.util.RenamingHeuristic;
import org.mapleir.ir.algorithms.BoissinotDestructor;
import org.mapleir.ir.algorithms.LocalsReallocator;
import org.mapleir.ir.cfg.ControlFlowGraph;
import org.mapleir.ir.cfg.builder.ControlFlowGraphBuilder;
import org.mapleir.ir.codegen.ControlFlowGraphDumper;
import org.mapleir.asm.ClassNode;
import org.mapleir.asm.MethodNode;
import org.topdank.byteengineer.commons.data.JarClassData;
import org.topdank.byteengineer.commons.data.JarInfo;
import org.topdank.byteio.in.SingleJarDownloader;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class Boot {

	private static final Logger LOGGER = Logger.getLogger(Boot.class);

	public static boolean logging = true;
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
		File f = new File(args[0]);

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
//		app.addLibraries(new InstalledRuntimeClassSource(app));

		File rtjar = new File(System.getProperty("java.home"), "lib/rt.jar");
		//File androidjar = new File("res/android.jar");
		app.addLibraries(rt(app, rtjar));
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

		for (ClassNode cn : cxt.getApplication().iterate()) {
//			 if (!cn.getName().equals("android/support/v4/media/session/MediaSessionCompat$MediaSessionImplApi18"))
//			 	continue;
			for (MethodNode m : cn.getMethods()) {
//				 if (!m.getName().equals("setRccState"))
//				 	continue;
				cxt.getIRCache().getFor(m);
			}
		}
		section0("...generated " + cxt.getIRCache().size() + " cfgs in %fs.%n", "Preparing to transform.");

		// do passes
		PassGroup masterGroup = new PassGroup("MasterController");
		masterGroup.add(getTransformationPasses());
		run(cxt, masterGroup);
		section0("...done transforming in %fs.%n", "Preparing to transform.");


		for(Entry<MethodNode, ControlFlowGraph> e : cxt.getIRCache().entrySet()) {
			MethodNode mn = e.getKey();
			ControlFlowGraph cfg = e.getValue();
			try {
				cfg.verify();
			} catch (Exception ex){
				ex.printStackTrace();
			}

		}

		section("Retranslating SSA IR to standard flavour.");
		for(Entry<MethodNode, ControlFlowGraph> e : cxt.getIRCache().entrySet()) {
			MethodNode mn = e.getKey();
			// if (!mn.getName().equals("openFiles"))
			// 	continue;
			ControlFlowGraph cfg = e.getValue();

			// System.out.println(cfg);
			//  CFGUtils.easyDumpCFG(cfg, "pre-destruct");
			try {
				cfg.verify();
			} catch (Exception ex){
				ex.printStackTrace();
			}

			BoissinotDestructor.leaveSSA(cfg);

			 // CFGUtils.easyDumpCFG(cfg, "pre-reaalloc");
			LocalsReallocator.realloc(cfg);
			 // CFGUtils.easyDumpCFG(cfg, "post-reaalloc");
			// System.out.println(cfg);
			try {
				cfg.verify();
			} catch (Exception ex){
				ex.printStackTrace();
			}
			 // System.out.println("Rewriting " + mn.getName());
			(new ControlFlowGraphDumper(cfg, mn)).dump();
			 // System.out.println(InsnListUtils.insnListToString(mn.instructions));
		}

		section("Rewriting jar.");
		dumpJar(app, dl, masterGroup, args[0] + "-out.jar");

		section("Finished.");
	}

	private static void dumpJar(ApplicationClassSource app, SingleJarDownloader<ClassNode> dl, PassGroup masterGroup, String outputFile) throws IOException {
		(new CompleteResolvingJarDumper(dl.getJarContents(), app) {
			@Override
			public int dumpResource(JarOutputStream out, String name, byte[] file) throws IOException {
//				if(name.startsWith("META-INF")) {
//					System.out.println(" ignore " + name);
//					return 0;
//				}
				if(name.equals("META-INF/MANIFEST.MF")) {
					ClassRenamerPass renamer = (ClassRenamerPass) masterGroup.getPass(e -> e.is(ClassRenamerPass.class));

					if(renamer != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(baos));
						BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(file)));

						String line;
						while((line = br.readLine()) != null) {
							String[] parts = line.split(": ", 2);
							if(parts.length != 2) {
								bw.write(line);
								continue;
							}

							if(parts[0].equals("Main-Class")) {
								String newMain = renamer.getRemappedName(parts[1].replace(".", "/")).replace("/", ".");
								LOGGER.info(String.format("%s -> %s%n", parts[1], newMain));
								parts[1] = newMain;
							}

							bw.write(parts[0]);
							bw.write(": ");
							bw.write(parts[1]);
							bw.write(System.lineSeparator());
						}

						br.close();
						bw.close();

						file = baos.toByteArray();
					}
				}
				return super.dumpResource(out, name, file);
			}
		}).dump(new File(outputFile));
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
				// Const param

				// Rename
//				new ClassRenamerPass(heuristic),
//				new MethodRenamerPass(heuristic),
//				new FieldRenamerPass(),

				// Default
				//new CallgraphPruningPass(),
				//new ConcreteStaticInvocationPass(),
				//new ConstantExpressionReorderPass(),
				//new ConstantParameterPass(),
				//new DeadCodeEliminationPass(),
				//new DemoteRangesPass(), // Not done
				//new DetectIrreducibleFlowPass(),
				//new LiftConstructorCallsPass()
				new ConstantExpressionEvaluatorPass(),
				//new DeadBlockRemoverPass()
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

	public static void section0(String endText, String sectionText, boolean quiet) {
		if(sections.isEmpty()) {
			lap();
			if(!quiet)
				LOGGER.info(sectionText);
		} else {
			/* remove last section. */
			sections.pop();
			if(!quiet) {
				LOGGER.info(String.format(endText, lap()));
				LOGGER.info(sectionText);
			} else {
				lap();
			}
		}

		/* push the new one. */
		sections.push(sectionText);
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

