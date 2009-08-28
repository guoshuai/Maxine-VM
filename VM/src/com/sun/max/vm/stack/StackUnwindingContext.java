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
package com.sun.max.vm.stack;

import com.sun.max.unsafe.*;

/**
 * Carries context that may be necessary while unwinding a stack for exception handling.
 * The information that needs to be carried, beside the exception object, is platform-dependent.
 * Platforms that needs additional information must extend this class.
 *
 * @author Laurent Daynes
 */
public class StackUnwindingContext {
    /**
     * The cause of the stack unwinding.
     */
    public final Throwable throwable;
    protected Word stackPointer;
    protected Word framePointer;
    protected boolean isTopFrame;

    public StackUnwindingContext(Word stackPointer, Word framePointer, Throwable throwable) {
        this.throwable = throwable;
        this.stackPointer = stackPointer;
        this.framePointer = framePointer;
        this.isTopFrame = true;
    }

    public void record(Pointer stackPointer, Pointer framePointer) {
        this.stackPointer = stackPointer;
        this.framePointer = framePointer;
        this.isTopFrame = false;
    }

    public Pointer stackPointer() {
        return stackPointer.asPointer();
    }

    public Pointer framePointer() {
        return framePointer.asPointer();
    }

    public boolean isTopFrame() {
        return isTopFrame;
    }
}
