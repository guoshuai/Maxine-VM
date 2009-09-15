/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.max.vm.compiler.c1x;

import com.sun.c1x.ri.*;

/**
 * The <code>MaxRiExceptionHandler</code> represents an exception handler
 * in the compiler interface.
 *
 * @author Ben L. Titzer
 */
public class MaxRiExceptionHandler implements RiExceptionHandler {

    private final int startBCI;
    private final int endBCI;
    private final int handlerBCI;
    private final int classCPI;
    private final RiType classType;

    /**
     * Creates a new exception handler with the specified ranges.
     * @param startBCI the start index of the protected range
     * @param endBCI the end index of the protected range
     * @param catchBCI the index of the handler
     * @param classCPI the index of the throwable class in the constant pool
     * @param classType the type caught by this exception handler
     */
    public MaxRiExceptionHandler(int startBCI, int endBCI, int catchBCI, int classCPI, RiType classType) {
        this.startBCI = startBCI;
        this.endBCI = endBCI;
        this.handlerBCI = catchBCI;
        this.classCPI = classCPI;
        this.classType = classType;
    }

    /**
     * Gets the start index of the protected range.
     * @return the start index
     */
    public int startBCI() {
        return startBCI;
    }

    /**
     * Gets the end index of the protected range.
     * @return the end index
     */
    public int endBCI() {
        return endBCI;
    }

    /**
     * Gets the bytecode index of the handler block.
     * @return the handler block index
     */
    public int handlerBCI() {
        return handlerBCI;
    }

    /**
     * Gets the constant pool index of the throwable type this handler catches.
     * @return the constant pool index of the throwable type
     */
    public int catchClassIndex() {
        return classCPI;
    }

    /**
     * Checks whether this is a handler for all exceptions.
     * @return <code>true</code> if this handler catches all exceptions
     */
    public boolean isCatchAll() {
        return classCPI == 0;
    }

    public RiType catchKlass() {
        return classType;
    }
}
