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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.ins.method.*;
import com.sun.max.lang.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;
import com.sun.max.vm.runtime.*;


/**
 * Visual inspector and debugger for code discovered in the target VM that is not compiled Java.
 * That is, it's runtime assembled code such as a {@linkplain SafepointStub safepoint stub} or
 * is other native code about which little is known.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public class NativeMethodInspector extends MethodInspector {

    private final TeleTargetRoutine _teleTargetRoutine;
    private TargetCodeViewer _targetCodeViewer = null;

    public NativeMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent, null, teleTargetRoutine.teleRoutine());
        _teleTargetRoutine = teleTargetRoutine;
        createFrame(null);
    }

    @Override
    public TeleTargetRoutine teleTargetRoutine() {
        return _teleTargetRoutine;
    }

    @Override
    public String getTitle() {
        if (_teleTargetRoutine instanceof TeleRuntimeStub) {
            final TeleRuntimeStub teleRuntimeStub = (TeleRuntimeStub) _teleTargetRoutine;
            return Strings.capitalizeFirst(teleRuntimeStub.runtimeStub().name(), false);
        }
        return teleTargetRoutine().teleRoutine().getUniqueName();
    }

    @Override
    public String getToolTip() {
        return getTitle();
    }

    @Override
    public void createView(long epoch) {
        _targetCodeViewer =  new JTableTargetCodeViewer(inspection(), this, _teleTargetRoutine);
        frame().getContentPane().add(_targetCodeViewer);
        updateSize();
        frame().pack();
        frame().invalidate();
        frame().repaint();
    }

    @Override
    public void refreshView(long epoch) {
        if (isVisible()) {
            _targetCodeViewer.refresh(epoch);
        }
    }

    public void viewConfigurationChanged(long epoch) {
        _targetCodeViewer.redisplay();
    }

    /**
     * Receive request from codeViewer to close; there's only one, so close the whole MethodInspector.
     */
    @Override
    public void closeCodeViewer(CodeViewer codeViewer) {
        assert codeViewer == _targetCodeViewer;
        close();
    }

    /**
     * Global code selection has changed; update viewer.
     */
    @Override
    public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
        if (_targetCodeViewer.updateCodeFocus(codeLocation) && !isSelected()) {
            highlight();
        }
    }
}
