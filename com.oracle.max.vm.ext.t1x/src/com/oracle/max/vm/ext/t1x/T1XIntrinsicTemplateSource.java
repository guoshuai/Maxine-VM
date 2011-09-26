/*
 * Copyright (c) 2011, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.max.vm.ext.t1x;

import com.sun.max.unsafe.*;

/**
 * Wrappers for intrinsic methods.  The code is automatically generated by {@link T1XIntrinsicTemplateGenerator}.
 */
public class T1XIntrinsicTemplateSource {
// Checkstyle: stop
// START GENERATED CODE
    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$aboveOrEqual$ii(@Slot(1) int param0, @Slot(0) int param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.aboveOrEqual(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$aboveOrEqual$ll(@Slot(2) long param0, @Slot(0) long param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.aboveOrEqual(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$aboveThan$ii(@Slot(1) int param0, @Slot(0) int param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.aboveThan(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$aboveThan$ll(@Slot(2) long param0, @Slot(0) long param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.aboveThan(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$belowOrEqual$ii(@Slot(1) int param0, @Slot(0) int param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.belowOrEqual(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$belowOrEqual$ll(@Slot(2) long param0, @Slot(0) long param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.belowOrEqual(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$belowThan$ii(@Slot(1) int param0, @Slot(0) int param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.belowThan(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$belowThan$ll(@Slot(2) long param0, @Slot(0) long param1) {
        return UnsafeCast.asInt(com.oracle.max.cri.intrinsics.UnsignedMath.belowThan(param0, param1));
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$divide$ii(@Slot(1) int param0, @Slot(0) int param1) {
        return com.oracle.max.cri.intrinsics.UnsignedMath.divide(param0, param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_oracle_max_cri_intrinsics_UnsignedMath$divide$ll(@Slot(2) long param0, @Slot(0) long param1) {
        return com.oracle.max.cri.intrinsics.UnsignedMath.divide(param0, param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_oracle_max_cri_intrinsics_UnsignedMath$remainder$ii(@Slot(1) int param0, @Slot(0) int param1) {
        return com.oracle.max.cri.intrinsics.UnsignedMath.remainder(param0, param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_oracle_max_cri_intrinsics_UnsignedMath$remainder$ll(@Slot(2) long param0, @Slot(0) long param1) {
        return com.oracle.max.cri.intrinsics.UnsignedMath.remainder(param0, param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$compareAndSwapInt$iii(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) int param3) {
        return param0.compareAndSwapInt(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$compareAndSwapInt$wii(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) com.sun.max.unsafe.Offset param1, @Slot(1) int param2, @Slot(0) int param3) {
        return param0.compareAndSwapInt(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_sun_max_unsafe_Pointer$compareAndSwapLong$ill(@Slot(5) com.sun.max.unsafe.Pointer param0, @Slot(4) int param1, @Slot(2) long param2, @Slot(0) long param3) {
        return param0.compareAndSwapLong(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_sun_max_unsafe_Pointer$compareAndSwapLong$wll(@Slot(5) com.sun.max.unsafe.Pointer param0, @Slot(4) com.sun.max.unsafe.Offset param1, @Slot(2) long param2, @Slot(0) long param3) {
        return param0.compareAndSwapLong(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.vm.reference.Reference com_sun_max_unsafe_Pointer$compareAndSwapReference$iaa(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) com.sun.max.vm.reference.Reference param2, @Slot(0) com.sun.max.vm.reference.Reference param3) {
        return param0.compareAndSwapReference(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.vm.reference.Reference com_sun_max_unsafe_Pointer$compareAndSwapReference$waa(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) com.sun.max.unsafe.Offset param1, @Slot(1) com.sun.max.vm.reference.Reference param2, @Slot(0) com.sun.max.vm.reference.Reference param3) {
        return param0.compareAndSwapReference(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$getByte$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getByte(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$getChar$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getChar(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static double com_sun_max_unsafe_Pointer$getDouble$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getDouble(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static float com_sun_max_unsafe_Pointer$getFloat$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getFloat(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$getInt$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getInt(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_sun_max_unsafe_Pointer$getLong$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getLong(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.vm.reference.Reference com_sun_max_unsafe_Pointer$getReference$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getReference(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$getShort$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getShort(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.unsafe.Word com_sun_max_unsafe_Pointer$getWord$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        return param0.getWord(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readByte$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readByte(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readByte$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readByte(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readChar$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readChar(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readChar$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readChar(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static double com_sun_max_unsafe_Pointer$readDouble$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readDouble(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static double com_sun_max_unsafe_Pointer$readDouble$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readDouble(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static float com_sun_max_unsafe_Pointer$readFloat$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readFloat(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static float com_sun_max_unsafe_Pointer$readFloat$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readFloat(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readInt$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readInt(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readInt$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readInt(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_sun_max_unsafe_Pointer$readLong$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readLong(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_sun_max_unsafe_Pointer$readLong$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readLong(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.vm.reference.Reference com_sun_max_unsafe_Pointer$readReference$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readReference(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.vm.reference.Reference com_sun_max_unsafe_Pointer$readReference$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readReference(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readShort$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readShort(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_unsafe_Pointer$readShort$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readShort(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.unsafe.Word com_sun_max_unsafe_Pointer$readWord$i(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) int param1) {
        return param0.readWord(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static com.sun.max.unsafe.Word com_sun_max_unsafe_Pointer$readWord$w(@Slot(1) com.sun.max.unsafe.Pointer param0, @Slot(0) com.sun.max.unsafe.Offset param1) {
        return param0.readWord(param1);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setByte$iib(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) int param3) {
        param0.setByte(param1, param2, (byte) param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setDouble$iid(@Slot(4) com.sun.max.unsafe.Pointer param0, @Slot(3) int param1, @Slot(2) int param2, @Slot(0) double param3) {
        param0.setDouble(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setFloat$iif(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) float param3) {
        param0.setFloat(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setInt$iii(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) int param3) {
        param0.setInt(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setLong$iil(@Slot(4) com.sun.max.unsafe.Pointer param0, @Slot(3) int param1, @Slot(2) int param2, @Slot(0) long param3) {
        param0.setLong(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setReference$iia(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) com.sun.max.vm.reference.Reference param3) {
        param0.setReference(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setShort$iis(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) int param3) {
        param0.setShort(param1, param2, (short) param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$setWord$iiw(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(1) int param2, @Slot(0) com.sun.max.unsafe.Word param3) {
        param0.setWord(param1, param2, param3);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeByte$ib(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        param0.writeByte(param1, (byte) param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeByte$wb(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) com.sun.max.unsafe.Offset param1, @Slot(0) int param2) {
        param0.writeByte(param1, (byte) param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeDouble$id(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(0) double param2) {
        param0.writeDouble(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeDouble$wd(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) com.sun.max.unsafe.Offset param1, @Slot(0) double param2) {
        param0.writeDouble(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeFloat$if(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) float param2) {
        param0.writeFloat(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeFloat$wf(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) com.sun.max.unsafe.Offset param1, @Slot(0) float param2) {
        param0.writeFloat(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeInt$ii(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        param0.writeInt(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeInt$wi(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) com.sun.max.unsafe.Offset param1, @Slot(0) int param2) {
        param0.writeInt(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeLong$il(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) int param1, @Slot(0) long param2) {
        param0.writeLong(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeLong$wl(@Slot(3) com.sun.max.unsafe.Pointer param0, @Slot(2) com.sun.max.unsafe.Offset param1, @Slot(0) long param2) {
        param0.writeLong(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeReference$ia(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) com.sun.max.vm.reference.Reference param2) {
        param0.writeReference(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeReference$wa(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) com.sun.max.unsafe.Offset param1, @Slot(0) com.sun.max.vm.reference.Reference param2) {
        param0.writeReference(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeShort$is(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) int param2) {
        param0.writeShort(param1, (short) param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeShort$ws(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) com.sun.max.unsafe.Offset param1, @Slot(0) int param2) {
        param0.writeShort(param1, (short) param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeWord$iw(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) int param1, @Slot(0) com.sun.max.unsafe.Word param2) {
        param0.writeWord(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_unsafe_Pointer$writeWord$ww(@Slot(2) com.sun.max.unsafe.Pointer param0, @Slot(1) com.sun.max.unsafe.Offset param1, @Slot(0) com.sun.max.unsafe.Word param2) {
        param0.writeWord(param1, param2);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_vm_Intrinsics$leastSignificantBit$w(@Slot(0) com.sun.max.unsafe.Word param0) {
        return com.sun.max.vm.Intrinsics.leastSignificantBit(param0);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static int com_sun_max_vm_Intrinsics$mostSignificantBit$w(@Slot(0) com.sun.max.unsafe.Word param0) {
        return com.sun.max.vm.Intrinsics.mostSignificantBit(param0);
    }

    @T1X_INTRINSIC_TEMPLATE
    public static void com_sun_max_vm_Intrinsics$pause$() {
        com.sun.max.vm.Intrinsics.pause();
    }

    @T1X_INTRINSIC_TEMPLATE
    public static long com_sun_max_vm_intrinsics_Infopoints$here$() {
        return com.sun.max.vm.intrinsics.Infopoints.here();
    }

// END GENERATED CODE
// Checkstyle: resume
}
