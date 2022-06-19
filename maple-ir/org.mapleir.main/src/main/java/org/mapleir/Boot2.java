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
import org.mapleir.deob.passes.rename.ClassRenamerPass;
import org.mapleir.deob.passes.rename.FieldRenamerPass;
import org.mapleir.deob.passes.rename.MethodRenamerPass;
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
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;

public class Boot2 {
	
	private static final Logger LOGGER = Logger.getLogger(Boot2.class);
	
	public static boolean logging = false;

	private static LibraryClassSource rt(ApplicationClassSource app, File rtjar) throws IOException {
		SingleJarDownloader<ClassNode> dl = new SingleJarDownloader<>(new JarInfo(rtjar));
		dl.download();

		return null; //new LibraryClassSource(app, dl.getJarContents().getClassContents());
	}

	public static void main(String[] args) throws Exception {

		logging = true;

		// Load input jar
		//  File f = locateRevFile(135);
		// Load input jar
		//  File f = locateRevFile(135);
		File f = new File(args[0]);

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

		IRCache irFactory = new IRCache(ControlFlowGraphBuilder::build);
		AnalysisContext cxt = new BasicAnalysisContext.BasicContextBuilder()
				.setApplication(app)
				.setInvocationResolver(new DefaultInvocationResolver(app))
				.setCache(irFactory)
				.setApplicationContext(new SimpleApplicationContext(app))
				.setDataFlowAnalysis(new LiveDataFlowAnalysisImpl(irFactory))
				.build();

		for (ClassNode cn : cxt.getApplication().iterate()) {
			 // if (!cn.name.equals("qzh/hdx/i"))
			 // 	continue;
			// if (cn.name.hashCode() != -5913103)
			// 	continue;
			for (MethodNode m : cn.getMethods()) {
				 // if (!m.name.equals("mapTypes"))
				 // 	continue;
				cxt.getIRCache().getFor(m);
			}
		}
		System.out.println("Generated " + cxt.getIRCache().size() + " cfgs");

		// do passes
		PassGroup masterGroup = new PassGroup("MasterController");
		for (IPass p : getTransformationPasses()) {
			masterGroup.add(p);
		}
		run(cxt, masterGroup);


		for(Entry<MethodNode, ControlFlowGraph> e : cxt.getIRCache().entrySet()) {
			MethodNode mn = e.getKey();
			ControlFlowGraph cfg = e.getValue();
			cfg.verify();
		}

		for(Entry<MethodNode, ControlFlowGraph> e : cxt.getIRCache().entrySet()) {
			MethodNode mn = e.getKey();
			// if (!mn.name.equals("openFiles"))
			// 	continue;
			ControlFlowGraph cfg = e.getValue();

			// System.out.println(cfg);
			//  CFGUtils.easyDumpCFG(cfg, "pre-destruct");
			cfg.verify();

			BoissinotDestructor.leaveSSA(cfg);

			 // CFGUtils.easyDumpCFG(cfg, "pre-reaalloc");
			LocalsReallocator.realloc(cfg);
			 // CFGUtils.easyDumpCFG(cfg, "post-reaalloc");
			// System.out.println(cfg);
			cfg.verify();
			 // System.out.println("Rewriting " + mn.name);
			(new ControlFlowGraphDumper(cfg, mn)).dump();
			 // System.out.println(InsnListUtils.insnListToString(mn.instructions));
		}
		
		dumpJar(app, dl, masterGroup, "out/rewritten.jar");
		
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
		RenamingHeuristic heuristic = RenamingHeuristic.NON_PRINTABLE;
		return new IPass[] {
//				new ConcreteStaticInvocationPass(),
				new ClassRenamerPass(heuristic),
				new MethodRenamerPass(heuristic),
				new FieldRenamerPass(),
				// new CallgraphPruningPass(),
				
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

}
