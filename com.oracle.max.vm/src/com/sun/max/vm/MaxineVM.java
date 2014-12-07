/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm;

import com.sun.max.annotate.*;
import com.sun.max.config.BootImagePackage;
import com.sun.max.lang.Classes;
import com.sun.max.platform.Platform;
import com.sun.max.program.ProgramWarning;
import com.sun.max.unsafe.*;
import com.sun.max.util.Utf8Exception;
import com.sun.max.vm.actor.member.ClassMethodActor;
import com.sun.max.vm.actor.member.MethodActor;
import com.sun.max.vm.classfile.ClassfileReader;
import com.sun.max.vm.compiler.CompilationBroker;
import com.sun.max.vm.compiler.target.RegisterConfigs;
import com.sun.max.vm.compiler.target.Stubs;
import com.sun.max.vm.heap.Heap;
import com.sun.max.vm.heap.ImmortalHeap;
import com.sun.max.vm.hosted.CompiledPrototype;
import com.sun.max.vm.jdk.JDK;
import com.sun.max.vm.jni.DynamicLinker;
import com.sun.max.vm.jni.NativeInterfaces;
import com.sun.max.vm.log.VMLog;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.VmThreadLocal;
import com.sun.max.vm.thread.VmThreadMap;
import com.sun.max.vm.ti.VMTI;
import com.sun.max.vm.type.TypeDescriptor;

import java.lang.reflect.AccessibleObject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.sun.max.lang.Classes.getPackageName;
import static com.sun.max.platform.Platform.platform;
import static com.sun.max.vm.VMConfiguration.vmConfig;
import static com.sun.max.vm.VMOptions.register;

/**
 * The global VM context. There is a {@linkplain #vm() single VM context} in existence at any time.
 * The {@linkplain VMConfiguration configuration} for a VM context can be accessed via the
 * {@link #config} field although the preferred mechanism for accessing the configuration for the
 * global VM context is {@linkplain VMConfiguration#vmConfig()}.
 * <p>
 * Other functionality encapsulated in this class includes:
 * <li>The {@link #isHosted()} method that can be used to guard blocks of code that will be omitted from a boot image.</li>
 * <li>The current execution {@linkplain #phase phase} of the VM, denoting what language and VM capabilities
 * have been initialized and are available.</li>
 * <li>Some global native methods used at runtime that don't particularly
 * belong to other classes (e.g. {@link #native_currentTimeMillis()}, {@link #native_exit(int)}, etc).</li>
 * <li>Methods for {@linkplain #registerCriticalMethod(CriticalMethod) registering} methods to be
 * loaded & compiled into the boot image.</li>
 */
public final class MaxineVM {

    public static final String NAME = "Maxine Virtual Machine";
    public static final int MAJOR_VERSION = 2;
    public static final int MINOR_VERSION = 0;
    public static final String VERSION_STRING = Integer.toString(MAJOR_VERSION) + "." + Integer.toString(MINOR_VERSION);
    public static final String HOME_URL = "http://labs.oracle.com/projects/maxine/";
    public static final int HARD_EXIT_CODE = -2;

    /**
     * The set of packages denoting classes for which {@link #isBootImageClass(String)} will return true.
     */
    @HOSTED_ONLY
    private static final Map<String, BootImagePackage> BOOT_IMAGE_CODE_BASE_PACKAGES = new ConcurrentHashMap<String, BootImagePackage>();

    @HOSTED_ONLY
    private static final Map<Class, Boolean> HOSTED_CLASSES = new ConcurrentHashMap<Class, Boolean>();

    @HOSTED_ONLY
    private static final Set<String> KEEP_CLINIT_CLASSES = new HashSet<String>();

    @HOSTED_ONLY
    private static final String[] TEST_PACKAGES = {"test.", "jtt."};

    private static final VMOption HELP_OPTION = register(new VMOption("-help", "Prints this help message.") {
        @Override
        protected boolean haltsVM() {
            return true;
        }
        @Override
        public boolean parseValue(Pointer optionValue) {
            VMOptions.printUsage(Category.STANDARD);
            return true;
        }
    }, MaxineVM.Phase.PRISTINE);

    /**
     * The current VM context.
     */
    @INSPECTED
    @CONSTANT
    private static MaxineVM vm;

    /**
     * The exit code returned by the VM process.
     */
    private static int exitCode = 0;

    private static long startupTime;
    private static long startupTimeNano;

    /**
     * Allows the Inspector access to the thread locals block for the primordial thread.
     */
    @INSPECTED
    private static Address primordialTLBlock;
    @INSPECTED
    private static int primordialTLBlockSize;


    public enum Phase {
        /**
         * Running on a host VM in order to construct a target VM or to run tests.
         */
        BOOTSTRAPPING,

        /**
         * Starting to compile while in {@link HOSTED_ONLY} mode (e.g. creating the compiled boot image).
         */
        HOSTED_COMPILING,

        /**
         * Starting the serialization of a graph of host VM objects into the boot image.
         */
        SERIALIZING_IMAGE,

        /**
         * Starting writing the serialized graph (with all address set a relative offset) into byte streams.
         */
        WRITING_IMAGE,

        /**
         * Executing target VM code, but many features do not work yet.
         */
        PRIMORDIAL,

        /**
         * Java thread synchronization initialized and available (but may not do anything yet).
         */
        PRISTINE,

        /**
         * All pure Java language features work; now building the application sandbox.
         */
        STARTING,

        /**
         * Executing application code.
         */
        RUNNING,

        /**
         * VM about to terminate, all non-daemon threads terminated, shutdown hooks run, but {@link VmOperation} thread still live.
         * Last chance to interpose, but be careful what you do. In particular, thread creation is not permitted.
         */
        TERMINATING
    }

    /**
     * An enum for the properties whose values must be obtained from the native environment at runtime. The enum
     * constants in this class are used to read values from the native_properties_t struct defined in
     * Native/substrate/maxine.c returned by {@link MaxineVM#native_properties()}.
     *
     */
    public enum NativeProperty {
        USER_NAME,
        USER_HOME,
        USER_DIR;

        /**
         * Gets the value of this property from a given C struct.
         *
         * @param cStruct the value returned by a call to {@link MaxineVM#native_properties()}
         * @return the value of this property in {@code cStruct} converted to a {@link String} value (which may be {@code null})
         */
        public String value(Pointer cStruct) {
            final Pointer cString = cStruct.readWord(ordinal() * Word.size()).asPointer();
            if (cString.isZero()) {
                return null;
            }
            try {
                return CString.utf8ToJava(cString);
            } catch (Utf8Exception utf8Exception) {
                throw FatalError.unexpected("Could not convert C string value of " + this + " to a Java string");
            }
        }
    }

    /**
     * Registers the complete set of packages that (potentially) comprise the boot image being constructed.
     */
    @HOSTED_ONLY
    public static void registerBootImagePackages(List<BootImagePackage> packages) {
        for (BootImagePackage pkg : packages) {
            BOOT_IMAGE_CODE_BASE_PACKAGES.put(pkg.name(), pkg);
        }
    }

    @HOSTED_ONLY
    public static void registerKeepClassInit(String className) {
        KEEP_CLINIT_CLASSES.add(className);
    }

    /**
     * Global variable determining whether class initializers are to be discarded
     * or preserved by the {@link ClassfileReader}.
     */
    @HOSTED_ONLY
    public static boolean preserveClinitMethods = System.getProperty("max.loader.preserveClinitMethods") != null;

    @HOSTED_ONLY
    public static boolean keepClassInit(TypeDescriptor classDescriptor) {
        final String className = classDescriptor.toJavaString();
        final boolean result = preserveClinitMethods || KEEP_CLINIT_CLASSES.contains(className);
        return result;
    }

    public static String name() {
        return NAME;
    }

    public static String description() {
        return "The " + NAME + " Ver. " + VERSION_STRING + "  <" + HOME_URL + ">";
    }

    /**
     * Gets the current VM context.
     */
    @INLINE
    public static MaxineVM vm() {
        return vm;
    }

    /**
     * Initializes or changes the current VM context.
     * This also {@linkplain Platform#set(Platform) sets} the current platform context
     * to {@code vm.configuration.platform}. That is,
     * changing the VM context also changes the platform context.
     *
     * @param vm the new VM context (must not be {@code null})
     * @return the previous VM context
     */
    @HOSTED_ONLY
    public static MaxineVM set(MaxineVM vm) {
        MaxineVM old = MaxineVM.vm;
        Platform.set(platform());
        MaxineVM.vm = vm;
        return old;
    }

    /**
     * Determines if the current execution environment is hosted on another JVM.
     *
     * @return {@code true} if being executed on another JVM, {@code false} if executing the bootstrapped/target VM.
     */
    public static boolean isHosted() {
        return true;
    }

    @LOCAL_SUBSTITUTION
    @FOLD
    public static boolean isHosted_() {
        return false;
    }

    /**
     * Determines if this is a {@link BuildLevel#DEBUG debug} build of the VM.
     * @return {@code true} if this is a debug build
     */
    @FOLD
    public static boolean isDebug() {
        return vm().config.debugging();
    }

    /**
     * Determines if a given constructor, field or method exists only for hosted execution.
     *
     * @param member the member to check
     * @return {@code true} if the member is only valid while executing hosted
     */
    @HOSTED_ONLY
    public static boolean isHostedOnly(AccessibleObject member) {
        return member.getAnnotation(HOSTED_ONLY.class) != null ||
               !Platform.platform().isAcceptedBy(member.getAnnotation(PLATFORM.class)) ||
               !JDK.thisVersionOrNewer(member.getAnnotation(JDK_VERSION.class)) ||
               isHostedOnly(Classes.getDeclaringClass(member));
    }

    /**
     * Determines if a given class exists only for hosted execution and should not be part of a generated target image.
     * <p>
     * A <i>direct hosted-only class</i> is defined as a class that is:
     * <ul>
     * <li>or annotated with {@link HOSTED_ONLY}
     * <li>or in a package that ends with {@code ".hosted"}
     * <li>or annotated with {@link PLATFORM} that does not match the target platform
     * <li>or annotated with {@link JDK_VERSION} that is not at least the JDK version used for building the boot image
     * <li>
     * </ul>
     * <p>
     * A <i>hosted-only</i> class is defined as:
     * <ul>
     * <li>a direct hosted-only class
     * <li>or a subclass of a hosted-only class
     * <li>or a nested class in a hosted-only class
     * <li>or an array of {@code T} where {@code T} is a hosted-only class
     * </ul>
     *
     * @param javaClass the class to check
     * @return {@code true} if the class is hosted-only
     */
    @HOSTED_ONLY
    public static boolean isHostedOnly(Class< ? > javaClass) {
        // We keep a cache of what we know
        final Boolean value = HOSTED_CLASSES.get(javaClass);
        if (value != null) {
            return value;
        }

        boolean result = false; // default assumption
        final String pkgName = getPackageName(javaClass);

        // Direct part of definition
        // There are potential deadlock issues when one compiler thread is involved in JDK
        // class initialization and another is checking annotations on the same class.
        // Since no JDK classes are HOSTED_ONLY, PLATFORM or JDK_VERSION, we suppress the check immediately

        ClassLoader cl = javaClass.getClassLoader();

        if (cl != null) {
            if (javaClass.getAnnotation(HOSTED_ONLY.class) != null) {
                result = true;
            } else if (pkgName.endsWith(".hosted")) {
                // May want to replace this 'magic' interpretation of ".hosted"
                // with a sentinel class (e.g. HOSTED_ONLY_PACKAGE).
                result = true;
            } else if (!Platform.platform().isAcceptedBy(javaClass.getAnnotation(PLATFORM.class))) {
                result = true;
            } else if (!JDK.thisVersionOrNewer(javaClass.getAnnotation(JDK_VERSION.class))) {
                result = true;
            } else {

                // Indirect part of definition, cover all the possible cases

                if (javaClass.isArray()) {
                    final Class< ? > componentClass = javaClass.getComponentType();
                    result = isHostedOnly(componentClass);
                } else {
                    final Class superClass = javaClass.getSuperclass();
                    if (superClass != null && isHostedOnly(superClass)) {
                        result = true;
                    } else {
                        final Class< ? > enclosingClass = getEnclosingClass(javaClass);
                        if (enclosingClass != null && isHostedOnly(enclosingClass)) {
                            result = true;
                        }
                    }
                }
            }
        }
        HOSTED_CLASSES.put(javaClass, result);
        //Trace.line(1, "setHostedOnly: " + javaClass.getName() + " " + result);
        return result;
    }

    private static Class<?> getEnclosingClass(Class<?> javaClass) {
        try {
            final Class< ? > enclosingClass = javaClass.getEnclosingClass();
            return enclosingClass;
        } catch (LinkageError linkageError) {
            ProgramWarning.message("Error trying to get the enclosing class for " + javaClass + ": " + linkageError);
        }
        return null;
    }

    public static boolean isPrimordial() {
        return vm().phase == Phase.PRIMORDIAL;
    }

    public static boolean isPristine() {
        return vm().phase == Phase.PRISTINE;
    }

    public static boolean isStarting() {
        return vm().phase == Phase.STARTING;
    }

    public static boolean isPrimordialOrPristine() {
        final Phase phase = vm().phase;
        return phase == Phase.PRIMORDIAL || phase == Phase.PRISTINE;
    }

    public static boolean isRunning() {
        return vm().phase == Phase.RUNNING;
    }

    public static long getStartupTime() {
        return startupTime;
    }

    public static long getStartupTimeNano() {
        return startupTimeNano;
    }

    /**
     * Determines if a given class name denotes a class that is specified as part of the boot image.
     * This cannot be based solely on the package name as the package may enumerate
     * a specific set of classes.
     */
    public static boolean isBootImageClass(String className) {
        BootImagePackage pkg = BOOT_IMAGE_CODE_BASE_PACKAGES.get(getPackageName(className));
        if (pkg != null) {
            // check for explicit class
            return pkg.isBootImageClass(className);
        }
        return false;
    }

    public static void setExitCode(int code) {
        exitCode = code;
    }

public static int test(int arg) {
        for (int i = 0; i < arg; i++) {
            test(1, 1, 1, 1, 1, 1);
        }
        return test(1, 1, 1, 1, 1, 1);
    }

    public static int test(int i1, int i2, int i3, int i4, int i5, int i6) {
        if (i1 == 0) {
            i1 = 2;
        } else {
            i2 = 2;
        }
        for (int i = 0; i < 10; i++) {
            if (i == 0) {
                i3 = 2;
            } else {
                i4 = 2;
            }

           /* for (int j = 0; j < 10; j++) {
                if (j == 0) {
                    i5 = 2;
                } else {
                    i6 = 2;
                }
            }*/
        }

        return i1 + i2 + i3 + i4 + i5 + i6;
    }
    /**
     * VM initialization point called by the substrate.
     *
     * ATTENTION: this signature must match 'VMRunMethod' in "com.oracle.max.vm.native/substrate/maxine.c"
     *
     * VM startup, initialization and exit code reporting routine running in the VM startup native thread.
     *
     * This must work without having established a valid Java 'Thread' or 'VmThread'. Hence, no JNI callbacks are
     * supported in this routine.
     *
     * Also, there is no heap at first. In this early phase, we cannot allocate any objects.
     *
     * @return 0 indicating initialization succeeded, non-0 if not
     */
    @VM_ENTRY_POINT
    public static int run(Pointer tlBlock, int tlBlockSize, Pointer bootHeapRegionStart, Word dlopen, Word dlsym, Word dlerror, Pointer vmInterface, Pointer jniEnv, Pointer jmmInterface, Pointer jvmtiInterface, int argc, Pointer argv) {
        primordialTLBlock = tlBlock;
        primordialTLBlockSize = tlBlockSize;
        Pointer etla = tlBlock.plus(platform().pageSize - Address.size() + VmThreadLocal.tlaSize().toInt());
        SafepointPoll.setLatchRegister(etla); // r10 in ARMV7
	//while(test(0) != 8) ;

        // This one field was not marked by the data prototype for relocation
        // to avoid confusion between "offset zero" and "null".
        // Fix it manually:
        Heap.bootHeapRegion.setStart(bootHeapRegionStart);

        VMLog.vmLog().initialize(MaxineVM.Phase.PRIMORDIAL);

        // The dynamic linker must be initialized before linking critical native methods
        DynamicLinker.initialize(dlopen, dlsym, dlerror);

        // Link the critical native methods:
        CriticalNativeMethod.linkAll();
	Log.println("lined critical methods");
        DynamicLinker.markCriticalLinked();

	Log.println("marked critical");
        // Initialize the trap system:
        Trap.initialize();
	Log.println("Traps done");
        ImmortalHeap.initialize();

	Log.println("immortals done");
        NativeInterfaces.initialize(vmInterface, jniEnv, jmmInterface);

	Log.println("interfaces initialised");
        // Perhaps this should be later, after VM has initialized
        startupTime = System.currentTimeMillis();
        startupTimeNano = System.nanoTime();

	Log.println("timers");
        MaxineVM vm = vm();
	Log.println("vm created");
        vmConfig().initializeSchemes(MaxineVM.Phase.PRIMORDIAL);

	Log.println("schemes inited");
        vm().stubs.intialize();
	Log.println("stubs");
        vm.phase = Phase.PRISTINE;

        VMOptions.parsePristine(argc, argv);
	Log.println("parse pristine");
        return exitCode;
    }

    public static String getExecutablePath() {
        try {
            return CString.utf8ToJava(native_executablePath());
        } catch (Utf8Exception e) {
            throw FatalError.unexpected("Could not convert C string value of executable path to a Java string");
        }
    }

    /**
     * Request the given method to be statically compiled in the boot image.
     */
    @HOSTED_ONLY
    public static ClassMethodActor registerImageMethod(ClassMethodActor method) {
        CompiledPrototype.registerVMEntryPoint(method);
        return method;
    }

    /**
     * Request the given method to have its invocation stub be compiled in the boot image.
     */
    @HOSTED_ONLY
    public static MethodActor registerImageInvocationStub(MethodActor method) {
        CompiledPrototype.registerImageInvocationStub(method);
        return method;
    }

    @HOSTED_ONLY
    public static void registerCriticalMethod(CriticalMethod criticalEntryPoint) {
        registerImageMethod(criticalEntryPoint.classMethodActor);
    }

    /*
     * Global native functions: these functions implement a thin layer over basic native
     * services that are needed to implement higher-level Java VM services. Note that
     * these native functions *ONLY* work on the target VM, not in bootstrapping or
     * inspecting modes.
     *
     * These service methods cannot block, and cannot use object references.
     */

    @C_FUNCTION
    public static native long native_nanoTime();

    @C_FUNCTION
    public static native long native_currentTimeMillis();

    @C_FUNCTION
    public static native Pointer native_executablePath();

    @C_FUNCTION
    public static native Pointer native_environment();

    /**
     * Gets a pointer to a C struct whose fields are NULL terminated C char arrays. The fields of this struct are read
     * and converted to {@link String} values by {@link NativeProperty#value(Pointer)}. The {@code native_properties_t}
     * struct declaration is in Native/substrate/maxine.c.
     */
    @C_FUNCTION
    public static native Pointer native_properties();

    @C_FUNCTION
    public static native float native_parseFloat(Pointer pointer, float nan);

    @C_FUNCTION
    public static native double native_parseDouble(Pointer pointer, double nan);

    @C_FUNCTION
    public static native void native_exit(int code);

    @C_FUNCTION
    public static native void native_trap_exit(int code, Address address);

    /**
     * Generate a core file of the vm process.
     * Note that this doesn't exit the vm which can progress normally after the core generation.
     */
    @C_FUNCTION
    public static native void core_dump();

    @INSPECTED
    public final VMConfiguration config;
    public Phase phase = Phase.BOOTSTRAPPING;
    public final RegisterConfigs registerConfigs;
    public final Stubs stubs;
    public final CompilationBroker compilationBroker;
    public final SafepointPoll safepointPoll;
    public final TrapFrameAccess trapFrameAccess;

    public MaxineVM(VMConfiguration configuration) {
        this.config = configuration;
        this.registerConfigs = RegisterConfigs.create();
        this.stubs = new Stubs(registerConfigs);
        this.safepointPoll = SafepointPoll.create();
        this.trapFrameAccess = TrapFrameAccess.create();
        this.compilationBroker = CompilationBroker.create();
    }

    public static void reportPristineMemoryFailure(String memoryAreaName, String operation, Size numberOfBytes) {
        Log.println("Error occurred during initialization of VM");
        Log.print("Failed to ");
        Log.print(operation);
        Log.print(' ');
        Log.print(numberOfBytes.toLong());
        Log.print(" bytes (");
        Log.printlnToPowerOfTwoUnits(numberOfBytes);
        Log.print(") of memory for ");
        Log.println(memoryAreaName);
        MaxineVM.native_exit(1);
    }

    /**
     * Low level VM exit. This method does not run any shutdown hooks or finalizers.
     * This is where {@link Runtime#exit(int)} and {@link Runtime#halt(int)} bottom out.
     *
     * @param code exit code for the VM process
     */
    public static void exit(int code) {

        VMOptions.beforeExit();

        // This prevents further thread creation
        VmThreadMap.ACTIVE.setVMTerminating();
        SignalDispatcher.terminate();

        try {
            VMTI.handler().vmDeath();
        } catch (Throwable throwable) {
            System.err.println("Exception thrown by VMTI handler: " + throwable);
        }
        // TODO: need to revisit this. Likely, we would want to bring all
        // threads to a safepoint before running the terminating phase.
        VMLog.vmLog().initialize(MaxineVM.Phase.TERMINATING);
        vmConfig().initializeSchemes(MaxineVM.Phase.TERMINATING);
        VmOperationThread.terminate();

        // Drop back to PRIMORDIAL
        MaxineVM vm = vm();
        vm.phase = MaxineVM.Phase.PRIMORDIAL;

        native_exit(code);
    }
}
