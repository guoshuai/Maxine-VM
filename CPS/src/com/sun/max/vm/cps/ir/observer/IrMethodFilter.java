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
package com.sun.max.vm.cps.ir.observer;

import com.sun.max.vm.cps.ir.*;

/**
 * This class implements an IrObserver filter that matches on the name of a method. This is useful
 * for tracing, graphing, or debugging the compilation of a particular method with a known name.
 * <p>
 * To enable the IrMethodFilter, specify the following system properties:
 * <p>
 * <pre>
 *     -Dmax.ir.observer.filters=IrMethodFilter
 *     -Dmax.ir.observer.method=(method name filters separated by ',')
 * </pre>
 * <p>
 * To simplify tracing of all IR's for a particular method, the {@code "max.ir.trace"} property can be used as described
 * {@linkplain IrObserverConfiguration here}.
 *
 * @author Ben L. Titzer
 */
public class IrMethodFilter extends IrObserverAdapter {

    public static final String PROPERTY_FILTER = "max.ir.observer.method";

    private final IrObserver observer;
    private final String[] filters;

    public IrMethodFilter(IrObserver observer, String[] filters) {
        this.filters = filters;
        this.observer = observer;
    }

    public IrMethodFilter(IrObserver observer) {
        this(observer, System.getProperty(PROPERTY_FILTER, "").split(","));
    }

    @Override
    public void observeAllocation(IrMethod irMethod) {
        if (match(irMethod)) {
            observer.observeAllocation(irMethod);
        }
    }

    @Override
    public void observeBeforeGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (match(irMethod)) {
            observer.observeBeforeGeneration(irMethod, irGenerator);
        }
    }

    @Override
    public void observeAfterGeneration(IrMethod irMethod, IrGenerator irGenerator) {
        if (match(irMethod)) {
            observer.observeAfterGeneration(irMethod, irGenerator);
        }
    }

    @Override
    public void observeBeforeTransformation(IrMethod irMethod, Object context, Object transform) {
        if (match(irMethod)) {
            observer.observeBeforeTransformation(irMethod, context, transform);
        }
    }

    @Override
    public void observeAfterTransformation(IrMethod irMethod, Object context, Object transform) {
        if (match(irMethod)) {
            observer.observeAfterTransformation(irMethod, context, transform);
        }
    }

    @Override
    public void finish() {
        observer.finish();
    }

    private boolean match(IrMethod irMethod) {
        final String signature = irMethod.classMethodActor().format("%H.%n(%p)");
        for (String filter : filters) {
            if (signature.contains(filter)) {
                return true;
            }
        }
        return false;
    }
}