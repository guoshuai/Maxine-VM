/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.graal;

import java.lang.reflect.*;
import java.util.concurrent.*;

import com.oracle.graal.api.code.*;
import com.oracle.graal.api.meta.*;
import com.oracle.graal.compiler.*;
import com.oracle.graal.compiler.target.*;
import com.oracle.graal.graph.*;
import com.oracle.graal.java.*;
import com.oracle.graal.lir.*;
import com.oracle.graal.nodes.*;
import com.oracle.graal.nodes.spi.*;
import com.oracle.graal.phases.*;
import com.oracle.graal.phases.OptimisticOptimizations.Optimization;
import com.oracle.graal.phases.PhasePlan.PhasePosition;
import com.oracle.graal.phases.common.*;
import com.oracle.graal.phases.tiers.*;
import com.oracle.graal.printer.*;
import com.oracle.max.vm.ext.graal.amd64.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.config.*;
import com.sun.max.program.*;
import com.sun.max.vm.*;
import com.sun.max.vm.MaxineVM.Phase;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.hosted.*;
import com.sun.max.vm.type.*;

/**
 * Integration of the Graal compiler into Maxine's compilation framework.
 *
 * Graal makes sophisticated use of {@link Unsafe} field offsets in its graph manipulations, and provides
 * a method {@link FieldIntrospection#rescanAllFieldOffsets()} to (re)compute the offsets. Since these differ
 * between the host VM and Maxine, they have to be recomputed. Graal stores the offsets in arrays
 * held in {@link FieldIntrospection} and {@link NodeClass}, so the usual {@link JDKInterceptor} mechanismn isn't useful.
 * So the fix up is done in the {@link MaxineVM.Phase#SERIALIZING_IMAGE} phase before the data objects are created,
 * and, importantly, after all compilation is complete!
 *
 * The Graal debug environment similarly needs to be re-established when the VM runs. It is setup for debugging snippet
 * installation in the boot image generation, but the config is stored in a thread local so it does not survive in the
 * boot image. We establish it in the {@link MaxineVM.Phase#STARTING} phase, as compilations can happen after that.
 */
public class MaxGraal extends RuntimeCompiler.DefaultNameAdapter implements RuntimeCompiler {

    private static class MaxGraphCache implements GraphCache {

        private final ConcurrentMap<ResolvedJavaMethod, StructuredGraph> cache = new ConcurrentHashMap<ResolvedJavaMethod, StructuredGraph>();

        @Override
        public void put(StructuredGraph graph) {
            cache.put(graph.method(), graph);
        }

        @Override
        public StructuredGraph get(ResolvedJavaMethod method) {
            return cache.get(method);
        }
    }

    private static final String NAME = "Graal";
    // The singleton instance of this compiler
    private static MaxGraal singleton;

    // Graal doesn't provide an instance that captures all this additioinal state needed
    // for a compilation. Instead these values are passed to compileMethod.
    private MaxRuntime runtime;
    private Backend backend;
    private Replacements replacements;
    private OptimisticOptimizations optimisticOpts;
    private MaxGraphCache cache;
    private Suites suites;

    public MaxGraal() {
        if (singleton == null) {
            singleton = this;
        }
    }

    public static MaxRuntime runtime() {
        assert singleton != null && singleton.runtime != null;
        return singleton.runtime;
    }

    private static class TraceDefaultAndSetMaxineFieldOffset extends FieldIntrospection.DefaultCalcOffset {
        @Override
        public long getOffset(Field field) {
            long hostOffset = super.getOffset(field);
            long maxOffset = FieldActor.fromJava(field).offset();
            Trace.line(2, field.getDeclaringClass().getName() + "." + field.getName() + " has host offset: " + hostOffset + ", Maxine offset: " + maxOffset);
            return maxOffset;
        }

    }

    @Override
    public void initialize(Phase phase) {
        if (phase == MaxineVM.Phase.HOSTED_COMPILING) {
            MaxGraalOptions.initialize();
            createGraalCompiler(phase);
        } else if (phase == MaxineVM.Phase.SERIALIZING_IMAGE) {
            TraceNodeClasses.scan();
            FieldIntrospection.rescanAllFieldOffsets(new TraceDefaultAndSetMaxineFieldOffset());
        } else if (phase == MaxineVM.Phase.STARTING) {
            // Compilations can occur after this, so set up the debug environment
            DebugEnvironment.initialize(System.out);
        }
    }

    private static class TraceNodeClasses extends FieldIntrospection {
        TraceNodeClasses() {
            super(null);
        }

        public static void scan() {
            Trace.line(2, "NodeClass.allClasses");
            for (FieldIntrospection nodeClass : allClasses.values()) {
                Trace.line(2, nodeClass);
            }
        }

        @Override
        protected void rescanFieldOffsets(CalcOffset calc) {


        }
    }

    private void createGraalCompiler(Phase phase) {
        // This sets up the debug environment for the boot image build
        DebugEnvironment.initialize(System.out);
        MaxTargetDescription td = new MaxTargetDescription();
        runtime = new MaxRuntime(td);
        backend = new MaxAMD64Backend(runtime, td);
        // We want exception handlers generated
        optimisticOpts = OptimisticOptimizations.ALL.remove(Optimization.UseExceptionProbability, Optimization.UseExceptionProbabilityForOperations);
        cache = new MaxGraphCache();
        changeInlineLimits();
        // Disable for now as it causes problems in DebugInfo
        GraalOptions.PartialEscapeAnalysis = false;
        suites = Suites.createSuites(GraalOptions.CompilerConfiguration);
        replacements = runtime.init();
        GraalBoot.forceValues();
    }

    /**
     * Graal's inlining limits are very aggressive, too high for the Maxine environment.
     * So we dial them down here.
     */
    private static void changeInlineLimits() {
        GraalOptions.NormalBytecodeSize = 30;
        GraalOptions.NormalComplexity = 30;
        GraalOptions.NormalCompiledCodeSize = 150;
    }

    @NEVER_INLINE
    public static void unimplemented(String methodName) {
        ProgramError.unexpected("unimplemented: " + methodName);
    }

    @Override
    public TargetMethod compile(ClassMethodActor methodActor, boolean isDeopt, boolean install, CiStatistics stats) {
        PhasePlan phasePlan = new PhasePlan();
        ResolvedJavaMethod method = MaxResolvedJavaMethod.get(methodActor);

        StructuredGraph graph = (StructuredGraph) method.getCompilerStorage().get(Graph.class);
        if (graph == null) {
            GraphBuilderPhase graphBuilderPhase = new MaxGraphBuilderPhase(runtime, GraphBuilderConfiguration.getDefault(), optimisticOpts);
            phasePlan.addPhase(PhasePosition.AFTER_PARSING, graphBuilderPhase);
            // Hosted (boot image) compilation is more complex owing to the need to handle the unsafe features of VM programming,
            // requiring more phases to be run. All this is very similar to what happens in MaxReplacementsImpl
            if (MaxineVM.isHosted()) {
                // In Graal, @Fold is purely a snippet notion; we handle Maxine's more general use with similar code.
                // We do this early to reduce the size of a graph that contains isHosted code.
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, new MaxFoldPhase(runtime));
                // Any folded isHosted code is only removed by a CanonicalizationPhase
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, new CanonicalizerPhase.Instance(runtime, null));
                phasePlan.addPhase(PhasePosition.AFTER_PARSING, new MaxIntrinsicsPhase());
                phasePlan.addPhase(PhasePosition.AFTER_PARSING,
                                new MaxWordTypeRewriterPhase.MakeWordFinalRewriter(runtime, runtime.maxTargetDescription.wordKind));
                // In order to have Maxine's INLINE annotation interpreted, we have to disable the standard inlining phase
                // and substitute a custom phase that checks the method annotation.
                phasePlan.disablePhase(InliningPhase.class);
                phasePlan.addPhase(PhasePosition.HIGH_LEVEL,
                                new MaxHostedInliningPhase(runtime, replacements, new Assumptions(GraalOptions.OptAssumptions), cache, phasePlan, optimisticOpts));
                // Important to remove bogus null checks on Word types
                phasePlan.addPhase(PhasePosition.HIGH_LEVEL, new MaxWordTypeRewriterPhase.MaxNullCheckRewriter(runtime, runtime.maxTargetDescription.wordKind));
                phasePlan.addPhase(PhasePosition.HIGH_LEVEL,
                                new MaxWordTypeRewriterPhase.KindRewriter(runtime, runtime.maxTargetDescription.wordKind));

            }
            graph = new StructuredGraph(method);
        }
        // Graal sometimes calls Class.forName on its own classes using the context class loader
        // so we have to override it as VM classes are ordinarily hidden
        ClassLoader savedContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(VMClassLoader.VM_CLASS_LOADER);
        try {
            CompilationResult result = GraalCompiler.compileMethod(runtime, replacements, backend, runtime.maxTargetDescription,
                            method, graph, cache, phasePlan, optimisticOpts, new SpeculationLog(), suites);
            return GraalMaxTargetMethod.create(methodActor, MaxCiTargetMethod.create(result), true);
        } catch (Throwable ex) {
            throw ex;
        } finally {
            Thread.currentThread().setContextClassLoader(savedContextClassLoader);
        }
    }

    @Override
    public Nature nature() {
        return Nature.OPT;
    }

    @Override
    public boolean matches(String compilerName) {
        return compilerName.equals(NAME);
    }

    /**
     * Since we are not yet compiling the boot image with Graal, a lot of node types are not being included in the
     * image, which causes problems when trying to compile with Graal at runtime early in the bootstrap, so we force
     * them to be included here.
     */
    private static class GraalBoot {

        @SuppressWarnings("unchecked")
        static void forceValues() {
            for (BootImagePackage p : VMConfiguration.activeConfig().bootImagePackages) {
                if (p.name().contains("com.oracle.graal.nodes") || p.name().contains("com.oracle.max.vm.ext.graal.nodes") || p.name().contains("com.oracle.graal.lir")) {
                    String[] classes = p.listClasses(JavaPrototype.javaPrototype().packageLoader().classpath);
                    for (String className : classes) {
                        try {
                            Class< ? > klass = Class.forName(className);
                            if (Node.class.isAssignableFrom(klass)) {
                                NodeClass.get(klass);
                            } else if (LIRInstruction.class.isAssignableFrom(klass) && klass != LIRInstruction.class) {
                                LIRInstructionClass.get((Class< ? extends LIRInstruction>) klass);
                            } else if (CompositeValue.class.isAssignableFrom(klass)) {
                                CompositeValueClass.get((Class< ? extends CompositeValue>) klass);
                            }
                        } catch (Exception ex) {
                            throw ProgramError.unexpected();
                        }
                    }
                }
            }
        }

    }

}