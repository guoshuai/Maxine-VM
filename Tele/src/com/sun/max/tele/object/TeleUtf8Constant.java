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
package com.sun.max.tele.object;

import com.sun.max.tele.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.reference.*;

/**
 * Canonical surrogate for an object of type {@link Utf8Constant} in the VM.
 *
 * @author Michael Van De Vanter
 */
public final class TeleUtf8Constant extends TelePoolConstant {

    protected TeleUtf8Constant(TeleVM vm, Reference utf8ConstantReference) {
        super(vm, utf8ConstantReference);
    }

    // The field is final once non-null; cache it.
    private Utf8Constant utf8Constant;

    /**
     * @return a local copy of this object in the VM.
     */
    public Utf8Constant utf8Constant() {
        if (utf8Constant == null) {
            Reference reference = vm().teleFields().Utf8Constant_string.readReference(reference());
            TeleString teleString = (TeleString) heap().makeTeleObject(reference);
            if (teleString != null) {
                utf8Constant = SymbolTable.makeSymbol(teleString.getString());
            }
        }
        return utf8Constant;
    }

    @Override
    protected Object createDeepCopy(DeepCopier context) {
        // Translate into local equivalent
        return utf8Constant();
    }

    @Override
    public String maxineRole() {
        return "Utf8Constant";
    }

    @Override
    public String maxineTerseRole() {
        return "Utf8Const";
    }

}
