/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.vm.heap;

import com.sun.max.annotate.*;
import com.sun.max.profile.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.object.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.thread.*;

public interface HeapScheme extends VMScheme {

    /**
     * The clock that specifies the timing resolution for GC related timing.
     */
    Clock GC_TIMING_CLOCK = Clock.SYSTEM_MILLISECONDS;

    /**
     * Determines whether a given thread belongs to the garbage collector. This method is only called outside of
     * the garbage collector and so it is safe for the implementation to use the complete Java language semantics
     * including {@code instanceof}.
     *
     * This method is only called from a {@link VmThread} constructor. Subsequent tests for whether a given {@code VmThread}
     * instance is a GC thread should use the {@link VmThread#isGCThread()} method directly.
     *
     * @return whether a thread belongs to the GC (or otherwise it belongs to the mutator)
     */
    boolean isGcThread(Thread thread);

    /**
     * If the heap implementation uses an adjusted card table,
     * report the number of bits to shift an object address with.
     * Otherwise return -1;
     */
    int adjustedCardTableShift();

    /**
     * Given the size of the boot image, calculates the size of the auxiliary space that the substrate is to allocate
     * and pass to the target VM.
     */
    @HOSTED_ONLY
    int auxiliarySpaceSize(int bootImageSize);

    /**
     * Initialize the auxiliary space, which is provided by the substrate.
     * This space can e.g. be used to support write barriers in the primordial phase,
     * for instance by holding a primordial card table that covers the boot image.
     */
    void initializeAuxiliarySpace(Pointer primordialVmThreadLocals, Pointer auxiliarySpace);

    /**
     * Allocate a new array object and fill in its header and initial data.
     */
    Object createArray(DynamicHub hub, int length);

    /**
     * Allocate a new tuple and fill in its header and initial data. Obtain the cell size from the given tuple class
     * actor.
     */
    Object createTuple(Hub hub);

    /**
     * Creates a hybrid object that is both a tuple and an array,
     * but leaving out the array part beyond the tuple for now.
     */
    Object createHybrid(DynamicHub hub);

    /**
     * Expands the hybrid object to its full array length.
     * The implementation may modify the original object and return it
     * or it can create a new object that contains the same tuple values and return that.
     */
    Hybrid expandHybrid(Hybrid hybrid, int length);

    /**
     * Creates a shallow clone of an object.
     * The identity hash value may differ.
     * The new object is not locked.
     */
    Object clone(Object object);

    /**
     * Prevent the GC from moving the given object.
     *
     * Allocating very small amounts in the same thread before unpinning is strongly discouraged but not strictly
     * forbidden.
     *
     * Pinning and then allocating may cause somewhat premature OutOfMemoryException. However, the implementation is
     * supposed to not let pinning succeed in the first place if there is any plausible danger of that happening.
     *
     * Pinning and then allocating large amounts is prone to cause premature OutOfMemoryException.
     *
     * Example:
     *
     * ATTENTION: The period of time an object can remained pinned must be "very short". Pinning may block other threads
     * that wait for GC to happen. Indefinite pinning will create deadlock!
     *
     * Calling this method on an already pinned object has undefined consequences.
     *
     * Note to GC implementors: you really don't need to implement pinning. It's an entirely optional/experimental
     * feature. However, if present, there are parts of the JVM that will automatically take advantage of it.
     *
     * If pinning is not supported, make pin() and isPinned() always return false and declare @INLINE for both.
     * Then all pinning client code will automatically be eliminated.
     *
     * @return whether pinning succeeded - callers are supposed to have an alternative plan when it fails
     */
    boolean pin(Object object);

    /**
     * Allow the given object to be moved by the GC. Always quickly balance each call to the above with a call to this
     * method.
     *
     * Calling this method on an already unpinned object has undefined consequences.
     */
    void unpin(Object object);

    /**
     * @return whether the object is currently pinned
     */
    boolean isPinned(Object object);

    /**
     * Returns whether an address is anywhere in the heap.
     */
    boolean contains(Address address);

    /**
     * Performs a garbage collection.
     *
     * @param requestedFreeSpace the minimum amount of space the collection must free up
     * @return {@code true} if {@code requestedFreeSpace} was freed up, {@code false} otherwise
     */
    boolean collectGarbage(Size requestedFreeSpace);

    /**
     * Gets the amount of free memory on the heap. This method does not trigger a GC.
     *
     * @return  an approximation to the total amount of memory currently
     *          available for future allocated objects, measured in bytes.
     */
    Size reportFreeSpace();

    /**
     * Gets the amount of memory used by objects allocated on the heap. This method does not trigger a GC.
     *
     * @return  an approximation to the total amount of memory currently
     *          used by allocated objects, measured in bytes.
     */
    Size reportUsedSpace();

    /**
     * Returns the maximum <em>object-inspection age</em>, which is the number
     * of real-time milliseconds that have elapsed since the
     * least-recently-inspected heap object was last inspected by the garbage
     * collector.
     *
     * <p> For simple stop-the-world collectors this value is just the time
     * since the most recent collection.  For generational collectors it is the
     * time since the oldest generation was most recently collected.  Other
     * collectors are free to return a pessimistic estimate of the elapsed
     * time, or simply the time since the last full collection was performed.
     *
     * <p> Note that in the presence of reference objects, a given object that
     * is no longer strongly reachable may have to be inspected multiple times
     * before it can be reclaimed.
    * @return
     */
    long maxObjectInspectionAge();

    void runFinalization();

    /**
     * A request for the heap scheme to attempt to reduce its memory usage..
     * @param amount suggested amount to reduce
     * @return true if can/will reduce, false otherwise
     */
    boolean decreaseMemory(Size amount);

    /**
     * A hint that the heap scheme can increase its memory usage.
     * @param amount suggested amount to increase
     * @return true if can/will increase memory usage, false otherwise
     */
    boolean increaseMemory(Size amount);

    /**
     * Disables heap allocation on the current thread. This state is recursive. That is,
     * allocation is only re-enabled once {@link #enableAllocationForCurrentThread()} is
     * called the same number of times as this method has been called.
     *
     * It is a {@linkplain FatalError fatal error} if calls to this method and {@link #enableAllocationForCurrentThread()}
     * are unbalanced.
     */
    void disableAllocationForCurrentThread();

    /**
     * Re-enables heap allocation on the current thread. This state is recursive. That is,
     * allocation is only re-enabled once this method is
     * called the same number of times as {@link #disableAllocationForCurrentThread()} has been called.
     *
     * It is a {@linkplain FatalError fatal error} if calls to this method and {@link #disableAllocationForCurrentThread()}
     * are unbalanced.
     */
    void enableAllocationForCurrentThread();

    @INLINE
    void writeBarrier(Reference from, Reference to);

    /**
     * Is TLAB used in this heap scheme or not.
     * @return true if TLAB is used
     */
    @INLINE
    boolean usesTLAB();

    /**
     * Turns immortal memory allocation on for a heap scheme. All performed memory allocations happen in the immortal space.
     *
     * Should be used like:
     *      try {
     *          Heap.enableImmortalMemoryAllocation();
     *          ...
     *          <allocations which should happen on the immortal heap>
     *          ...
     *      } finally {
     *          Heap.disableImmortalMemoryAllocation();
     *      }
     *
     */
    void enableImmortalMemoryAllocation();

    /**
     * Turns immortal memory allocation off for a heap scheme. All performed memory allocations happen in the garbage collected
     * heap.
     */
    void disableImmortalMemoryAllocation();

}
