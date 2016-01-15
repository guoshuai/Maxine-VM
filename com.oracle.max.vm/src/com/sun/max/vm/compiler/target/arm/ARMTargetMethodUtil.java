/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.vm.compiler.target.arm;

import com.oracle.max.asm.target.armv7.*;
import com.oracle.max.cri.intrinsics.*;
import com.sun.cri.ci.*;
import com.sun.max.annotate.*;
import com.sun.max.lang.*;
import com.sun.max.unsafe.*;
import com.sun.max.vm.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.compiler.target.*;
import com.sun.max.vm.runtime.*;
import com.sun.max.vm.stack.*;
import com.sun.max.vm.stack.armv7.*;

/**
 * A utility class factoring out code common to all ARMV7 target method.
 * IT DOES NOT !!!!!!!!!!!!!!!!!!!!!!!
 * HANDLE THUMB MODE
 * <p/>
 * APN for ARM we plan to initially do a simple fixup
 * calculate either a relative or an absolute address
 * put it in scratch (r12) via
 * movw movt
 * for relative call/jmp ADD PC,r12
 * for absolute call/jmp MOV PC,r12
 * <p/>
 * inefficient: for 24bit RELATIVE offsets can be done in a branch single instruction
 * and we are taking 3.
 * <p/>
 * for absolute it takes 3, but could be done in 2 -- calculate address using constants that can be shifted
 * then do the MOV PC <--
 */
public final class ARMTargetMethodUtil {

    /**
     * X86 Opcode of a RIP-relative call instruction.
     * <p/>
     * ARM this is a STMFD (save my registers) and a
     * PC relative branch instruction
     */
    // RIP_CALL using an ADD to PC, hope it;s the right way round for the regs.
    public static final int RIP_CALL = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28)
            | (0x8 << 20) | (ARMV7.r15.encoding << 12) | (ARMV7.r12.encoding << 16);


    /**
     * X86 Opcode of a register-based call instruction.
     * <p/>
     * ARM this is a  STMFD (save my registers) and a
     * move of a register into the PC
     */
    // REG_CALL using a MOV to the PC
    public static final int REG_CALL = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28)
            | (0xd << 21) | (ARMV7.r15.encoding << 12) | ARMV7.r12.encoding;

    /**
     * X86 Opcode of a RIP-relative jump instruction.
     * ARM again this is a PC relative branch instruction!
     * as its a JMP we do not save the stack
     * We do this as an add to the PC ...
     */
    public static final int RIP_JMP = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28)
            | (0x8 << 20) | (ARMV7.r15.encoding << 12) | (ARMV7.r12.encoding << 16);

    /**
     * X86 Opcode of a (near) return instruction.
     * ARM here we must do a LDMFD and we move the return address to the PC
     */
    //public static final int RET = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28)
    //| (0xd << 21) | (ARMV7.r15.encoding << 12) |  ARMV7.r14.encoding;

    public static final int RET = ((ARMV7Assembler.ConditionFlag.Always.value() & 0xf) << 28)
            | (0x8 << 24) | (0xb << 20) | (0xd << 16) | (1 << 15);


    /**
     * X86 Size (in bytes) of a RIP-relative call instruction.
     * ARM this is 4 bytes, or we might need to calculate it
     * according to the way we calculate the relative address?
     * do we need to include the register save STMFD
     */
    public static final int RIP_CALL_INSTRUCTION_SIZE = 4;

    /**
     * Lock to avoid race on concurrent icache invalidation when patching target methods.
     * ARM I think we need to insert an isb instruction.
     */
    private static final Object PatchingLock = new Object(); // JavaMonitorManager.newVmLock("PATCHING_LOCK");
    private static final boolean JUMP_WITH_LINK = true;

    public static int registerReferenceMapSize() {
        return UnsignedMath.divide(ARMV7.cpuRegisters.length, Bytes.WIDTH);
    }

    public static boolean isPatchableCallSite(CodePointer callSite) {
        // X86 We only update the disp of the call instruction.
        // The compiler(s) ensure that disp of the call be aligned to a word boundary.
        // This may cause up to 7 nops to be inserted before a call.

        // For ARM we Cmimic X86 but dont have all the alignment restrictions
        //All instructions are 32bits so we do not
        // need to do insertion of nops etc
        // presumably we need to update the 24bit immediate displacement of the branch
        // and/or patch a movw movt sequence with an absolute address.
        // currently this will be a patch of the movw movt
        // an push of the PC and an add to the PC with the disp.
        final Address callSiteAddress = callSite.toAddress();
        final Address endOfCallSite = callSiteAddress.plus(RIP_CALL_INSTRUCTION_LENGTH - 1);
        return callSiteAddress.isWordAligned();
        //return callSiteAddress.plus(1).isWordAligned() ? true :
        // last byte of call site:
        //callSiteAddress.roundedDownBy(8).equals(endOfCallSite.roundedDownBy(8));
    }

    /**
     * Gets the target of a 32-bit relative CALL instruction.
     *
     * @param tm      the method containing the CALL instruction
     * @param callPos the offset within the code of {@code targetMethod} of the CALL
     * @return the absolute target address of the CALL
     */
    public static CodePointer readCall32Target(TargetMethod tm, int callPos) {
        final CodePointer callSite = tm.codeAt(callPos);
        int disp32;
        if (MaxineVM.isHosted()) {
            final byte[] code = tm.code();
            assert code[0] == (byte) RIP_CALL;
            Log.println("ERRORERROR in readCall32Target if this is EVERCALLED");
            disp32 =
                    (code[callPos + 4] & 0xff) << 24 |
                            (code[callPos + 3] & 0xff) << 16 |
                            (code[callPos + 2] & 0xff) << 8 |
                            (code[callPos + 1] & 0xff) << 0;
        } else {
            final Pointer callSitePointer = callSite.toPointer();
            disp32 = 0;
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                disp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                disp32 = disp32 << 16;
                disp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
                /*if (VMOptions.verboseOption.verboseCompilation) {
                    Log.println(disp32);
                }
		*/
            }
            if (VMOptions.verboseOption.verboseCompilation) {

                /*Log.print("READCALL32TARGET ");
                Log.print(disp32);
                Log.print(" ");
                Log.print(tm.toString());
                Log.print(" CALLPOS ");
                Log.println(callPos);
                Log.println(callSitePointer);
		*/
            }

            assert (disp32 != 0);
            if (VMOptions.verboseOption.verboseCompilation) {
                //Log.println("POTENTIALLy neeed additional checks for DEOPT readCall32Target");
            }
            /*assert callSitePointer.readByte(0) == (byte) RIP_CALL
                // deopt might replace the first call in a method with a jump (redirection)
                || (callSitePointer.readByte(0) == (byte) RIP_JMP && callPos == 0)
                : callSitePointer.readByte(0);
            disp32 = callSitePointer.readInt(1);
	*/

        }
        return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(disp32);
    }

    private static void manipulateBuffer(byte[] code, int callOffset, int instruction) {

        code[callOffset + 3] = (byte) (instruction & 0xff);
        code[callOffset + 2] = (byte) ((instruction >> 8) & 0xff);
        code[callOffset + 1] = (byte) ((instruction >> 16) & 0xff);
        code[callOffset] = (byte) ((instruction >> 24) & 0xff);

    }

    /**
     * Patches the offset operand of a 32-bit relative CALL instruction.
     *
     * @param tm         the method containing the CALL instruction
     * @param callOffset the offset within the code of {@code targetMethod} of the CALL to be patched
     * @param target     the absolute target address of the CALL
     * @return the target of the call prior to patching
     */
    public static CodePointer fixupCall32Site(TargetMethod tm, int callOffset, CodePointer target) {
        CodePointer callSite = tm.codeAt(callOffset);
        if (!isPatchableCallSite(callSite)) {
            // Every call site that is fixed up here might also be patched later. To avoid failed patching,
            // check for alignment of call site also here.
            // TODO(cwi): This is a check that I would like to have, however, T1X does not ensure proper alignment yet
// when it stitches together templates that contain calls.
            FatalError.unexpected(" invalid patchable call site:  " + tm + "+" + callOffset + " " +
                    callSite.toHexString());
            Log.println("unpatchable call site? " + tm + " " + callSite.to0xHexString());
        }


        //int disp32 = target.toInt() - callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).toInt() ; // APN 16bytes 4 instructions out?
        long disp64 = target.toLong() - callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).toLong(); // APN 16bytes 4 instructions out?
        int disp32 = (int) disp64;
	/*
        if (VMOptions.verboseOption.verboseCompilation) {
            if (!MaxineVM.isHosted()) {
                Log.print("CALLER ");
                Log.print(tm.toString());
                Log.print(" ");
                Log.println(callSite);
                if (target.toTargetMethod() != null) {
                    Log.print("CALLEETARGET ");
                    Log.print(target.toTargetMethod().toString());
                    Log.print(" ");
                    Log.println(target);
                } else {
                    Log.print("CALLEETARGET ");
                    Log.print(" NULL ");
                    Log.print(" ");
                    Log.println(target);
                }

                Log.print(Integer.toString(disp32, 16));
                Log.print(" DISP32 ");
                Log.println(disp32);
                Log.print(Long.toString(disp64, 16));
                Log.print(" DISP64 ");
                Log.println(disp64);
            }
        }
	*/
        if (disp64 != disp32) {

            Log.println("Code displacement out of 32-bit range");
            disp64 = disp32;

        }
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");

        int oldDisp32 = 0;

        if (MaxineVM.isHosted()) {
            final byte[] code = tm.code();

            if (true) {
                if (JUMP_WITH_LINK) {

                    int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                    code[callOffset + 0] = (byte) (instruction & 0xff);
                    code[callOffset + 1] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 2] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 3] = (byte) ((instruction >> 24) & 0xff);
                    int tmp32 = disp32 >> 16;
                    instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                    code[callOffset + 4] = (byte) (instruction & 0xff);
                    code[callOffset + 5] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 6] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 7] = (byte) ((instruction >> 24) & 0xff);
                    instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
                    code[callOffset + 8] = (byte) (instruction & 0xff);
                    code[callOffset + 9] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 10] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 11] = (byte) ((instruction >> 24) & 0xff);
                    instruction = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
                    code[callOffset + 12] = (byte) (instruction & 0xff);
                    code[callOffset + 13] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 14] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 15] = (byte) ((instruction >> 24) & 0xff);
                } else {
                    // OK this is where we need to patch
                    // BASIC IDEA WE SETUP R12 WITH THE RELATIVE OFFSET
                    // THEN WE ADD IT TO THE PC ...
                    // THIS IS AN INTERWORKING BRANCH, SO IF WE WERE USING THUMB ETC WE WOULD NEED TO ALTER THE ADDRESS OFFSET
                    // IF WE WANTED TO STAY IN THUMB MODE AND/OR TO TRANSITION FORM ARM<->THUMB
                    // disp32 = 25;
                    // callOffset -= 20; // DIRTY HACK
                    Log.println("ERRORERRORERROR ARMTargetMEthodUtil");
                    int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                    code[callOffset + 0] = (byte) (instruction & 0xff);
                    code[callOffset + 1] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 2] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 3] = (byte) ((instruction >> 24) & 0xff);
                    int tmp32 = disp32 >> 16;
                    tmp32 = tmp32 & 0xffff;
                    instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                    code[callOffset + 4] = (byte) (instruction & 0xff);
                    code[callOffset + 5] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 6] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 7] = (byte) ((instruction >> 24) & 0xff);
                    instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r15, ARMV7.r15, ARMV7.r12, 0, 0);
                    code[callOffset + 8] = (byte) (instruction & 0xff);
                    code[callOffset + 9] = (byte) ((instruction >> 8) & 0xff);
                    code[callOffset + 10] = (byte) ((instruction >> 16) & 0xff);
                    code[callOffset + 11] = (byte) ((instruction >> 24) & 0xff);
                }
            }
        } else {
            final Pointer callSitePointer = callSite.toPointer();

            /*if (VMOptions.verboseOption.verboseCompilation) {

                Log.print("FIXUP CALL SITE ");
                Log.print(tm.toString());
                Log.print(" DISP ");
                Log.print(disp32);
                Log.print(" CALLOFFSET ");
                Log.println(callOffset);

                Log.println(callSitePointer);
            }
	    */
            oldDisp32 = 0;
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                oldDisp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                oldDisp32 = oldDisp32 << 16;
                oldDisp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
                /*if (VMOptions.verboseOption.verboseCompilation) {
                    Log.print("oldDisp32 ");
                    Log.println(oldDisp32);
                }
		*/
            }
            int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
            callSitePointer.writeByte(0, (byte) (instruction & 0xff));
            callSitePointer.writeByte(1, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(2, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(3, (byte) ((instruction >> 24) & 0xff));
            int tmp32 = disp32 >> 16;
            instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
            callSitePointer.writeByte(4, (byte) (instruction & 0xff));
            callSitePointer.writeByte(5, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(6, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(7, (byte) ((instruction >> 24) & 0xff));
            instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
            callSitePointer.writeByte(8, (byte) (instruction & 0xff));
            callSitePointer.writeByte(9, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(10, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(11, (byte) ((instruction >> 24) & 0xff));
            instruction = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
            callSitePointer.writeByte(12, (byte) (instruction & 0xff));
            callSitePointer.writeByte(13, (byte) ((instruction >> 8) & 0xff));
            callSitePointer.writeByte(14, (byte) ((instruction >> 16) & 0xff));
            callSitePointer.writeByte(15, (byte) ((instruction >> 24) & 0xff));
            int checkDISP = 0;
            if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                checkDISP = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                checkDISP = checkDISP << 16;
                checkDISP += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
            }
        }


        return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(oldDisp32);
    }


    private static final int RIP_CALL_INSTRUCTION_LENGTH = 16; // ARM it's two instructions
    // STMFD and the B branch

    private static final int RIP_JMP_INSTRUCTION_LENGTH = 4;  // ARM it's one instruction the B branch

    public static CodePointer ripCallOFFSET(TargetMethod tm, CodePointer callSite) {
        final Pointer callSitePointer = callSite.toPointer();
        int oldDisp32 = 0;
	// based on readCall32Target .... so should be ok.


        if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
                // just enough checking to make sure it has been patched before ...
                // and does not contain nops
                oldDisp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
                oldDisp32 = oldDisp32 << 16;
                oldDisp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
                // +8 as ARM PC is +8 ahead
                // another +8 as the ADD instruction is 8 bytes ahead of the first movw


                /*
                * HERE WE ARE CALCULATING THE NUMBER TO ADD TO THE pc BASED ON THE VALUE OF THE PC
                * AT THE MOVW IN THE MOVW MOVT ADD, BLX SEQUENCE
                * BECAUSE IT HAS TO WORK WITH   CompilationBroker.visitFrame
                * THEREFORE, the offset in the movw movt, is the offset needed at the ADD, and it ACTUALLY OMITS
                * THE +8 that the ARM internal CPU PC is ahead of the actual PC-value of the ADD instruction.
                *
                *

                 */
                if (VMOptions.verboseOption.verboseCompilation) {

                    //Log.println("ripCALLOFFSET " +oldDisp32);

                }

        }
	assert(oldDisp32 != 0);
	return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(oldDisp32).plus(8);


    }
    public static boolean isARMV7RIPCall(TargetMethod tm, CodePointer callSite) {
        final Pointer callSitePointer = callSite.toPointer();
        int addInstrn = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
	int temp = 0;
        if (((callSitePointer.readByte(3) & 0xff) == 0xe3)  && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3) &&
		((callSitePointer.readByte(2)>>4) == 0x0) && ((callSitePointer.readByte(4 + 2) >> 4) == 4)) {
            // basic match of movw movt

		temp = (callSitePointer.readByte(3) << 24) | (callSitePointer.readByte(2) << 16) | (callSitePointer.readByte(1) << 8) | callSitePointer.readByte(0);
		//Log.print("MOVW ");Log.println(temp);
		temp = 0;
		temp = (callSitePointer.readByte(3+4) << 24) | (callSitePointer.readByte(2+4) << 16) | (callSitePointer.readByte(1+4) << 8) | callSitePointer.readByte(0+4);
		//Log.print("MOVT ");Log.println(temp);
		temp = 0;
		temp = ((callSitePointer.readByte(3+8) << 24)&0xff000000) | ((callSitePointer.readByte(2+8) << 16)&0xff0000) | ((callSitePointer.readByte(1+8) << 8)&0xff00) | (callSitePointer.readByte(0+8)&0xff);
		//Log.print("ADD ");Log.println(temp);
		temp = 0;
		temp = ((callSitePointer.readByte(3+12) << 24)&0xff000000) | ((callSitePointer.readByte(2+12) << 16)&0xff0000) | ((callSitePointer.readByte(1+12) << 8)&0xff00) | (callSitePointer.readByte(0+12)&0xff);
		//Log.print("BLX ");Log.println(temp);
		//Log.print(callSitePointer.readByte(3+12));Log.print(" ");Log.print(callSitePointer.readByte(2+12));Log.print(" "); Log.print(callSitePointer.readByte(1+12));Log.print(" ");Log.println(callSitePointer.readByte(0+12));

		//Log.print("EXPECTED ADD ");Log.println(addInstrn);
		


            if(((callSitePointer.readByte(3+8) & 0xff) == ((addInstrn >> 24)&0xff)) &&
                    ((callSitePointer.readByte(2+8) & 0xff) == ((addInstrn >> 16) &0xff)) &&
                            ((callSitePointer.readByte(1+8) & 0xff) == ((addInstrn >> 8)&0xff)) &&
                                    ((callSitePointer.readByte(0+8) & 0xff) == (addInstrn&0xff))) {
                // full match of add r12,r12,pc
                if (VMOptions.verboseOption.verboseCompilation) {

                   Log.println("MATCHED RIP CALL");
                }
                    return true;
            } else if (VMOptions.verboseOption.verboseCompilation) {
                Log.println("DIDNOT match ADD");
        }


        }else if(VMOptions.verboseOption.verboseCompilation) {
		Log.println("DIDNOT match movwmovt");
	}
        if (VMOptions.verboseOption.verboseCompilation) {

            Log.println("NOT RIP CALL");
        }
        return false;
    }


    /*
    Returns the address of a page sized buffer that we will use to insturmentation traces to
    Initially we assume this is 4096 bytes long 
    
    Initialises *(buffer+4092) to point to the the start of the buffer
    */
    @C_FUNCTION
    public static native int  maxine_instrumentationBuffer();
	
    @C_FUNCTION
    public static native int maxine_flush_instrumentationBuffer();


    /* does icache flushin of an address range*/
    @C_FUNCTION
    public static native void maxine_cacheflush(Pointer start, int  length);


    /**
     * Thread safe patching of the displacement field in a direct call.
     *
     * @return the target of the call prior to patching
     */
    public static CodePointer mtSafePatchCallDisplacement(TargetMethod tm, CodePointer callSite, CodePointer target) {
        if (VMOptions.verboseOption.verboseCompilation) {
            //Log.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!mtSafePatchCallDisplacement !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        if (!isPatchableCallSite(callSite)) {
            throw FatalError.unexpected(" invalid patchable call site:  " + callSite.toHexString());
        }
        final Pointer callSitePointer = callSite.toPointer();
        long disp64 = target.toLong() - callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");
        int oldDisp32 = 0;
        if (((callSitePointer.readByte(3) & 0xff) == 0xe3) && ((callSitePointer.readByte(4 + 3) & 0xff) == 0xe3)) {
            // just enough checking to make sure it has been patched before ...
            // and does not contain nops
            oldDisp32 = (callSitePointer.readByte(4 + 0) & 0xff) | ((callSitePointer.readByte(4 + 1) & 0xf) << 8) | ((callSitePointer.readByte(4 + 2) & 0xf) << 12);
            oldDisp32 = oldDisp32 << 16;
            oldDisp32 += (callSitePointer.readByte(0) & 0xff) | ((callSitePointer.readByte(1) & 0xf) << 8) | ((callSitePointer.readByte(2) & 0xf) << 12);
            if (VMOptions.verboseOption.verboseCompilation) {

                //Log.println(oldDisp32);
            }
        }
        //int oldDisp32 = callSitePointer.readInt(1);
        /*if (VMOptions.verboseOption.verboseCompilation) {
            Log.print("OLD ");
            Log.println(Integer.toHexString(oldDisp32));
            Log.print("DISP32 ");
            Log.println(Integer.toHexString(disp32));
            Log.print("DISP64 ");
            Log.println(Long.toHexString(disp64));
        }
	*/
        if (oldDisp32 != disp64) {
            synchronized (PatchingLock) {
                // Just to prevent concurrent writing and invalidation to the same instruction cache line
                //callSitePointer.writeInt(1,  disp32);
                int instruction = ARMV7Assembler.movwHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, disp32 & 0xffff);
                callSitePointer.writeByte(0, (byte) (instruction & 0xff));
                callSitePointer.writeByte(1, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(2, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(3, (byte) ((instruction >> 24) & 0xff));
                int tmp32 = disp32 >> 16;
                instruction = ARMV7Assembler.movtHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12, tmp32 & 0xffff);
                callSitePointer.writeByte(4, (byte) (instruction & 0xff));
                callSitePointer.writeByte(5, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(6, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(7, (byte) ((instruction >> 24) & 0xff));
                instruction = ARMV7Assembler.addRegistersHelper(ARMV7Assembler.ConditionFlag.Always, false, ARMV7.r12, ARMV7.r15, ARMV7.r12, 0, 0);
                callSitePointer.writeByte(8, (byte) (instruction & 0xff));
                callSitePointer.writeByte(9, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(10, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(11, (byte) ((instruction >> 24) & 0xff));
                instruction = ARMV7Assembler.blxHelper(ARMV7Assembler.ConditionFlag.Always, ARMV7.r12);
                callSitePointer.writeByte(12, (byte) (instruction & 0xff));
                callSitePointer.writeByte(13, (byte) ((instruction >> 8) & 0xff));
                callSitePointer.writeByte(14, (byte) ((instruction >> 16) & 0xff));
                callSitePointer.writeByte(15, (byte) ((instruction >> 24) & 0xff));
		maxine_cacheflush(callSitePointer,24);
            }
        }
        return callSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(oldDisp32);
    }

    /**
     * Patches a position in a target method with a direct jump to a given target address.
     *
     * @param tm     the target method to be patched
     * @param pos    the position in {@code tm} at which to apply the patch
     * @param target the target of the jump instruction being patched in
     */
    public static void patchWithJump(TargetMethod tm, int pos, CodePointer target) {
        // We must be at a global safepoint to safely patch TargetMethods
        Log.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!patchWithJump!!!!hould only occur with Deoptimisation? isJumpToStaticTrampoline will need fixing!!!!!!!!!!!!!!!!!");
	FatalError.check(false,"pathWithJump not implemented");
        FatalError.check(VmOperation.atSafepoint(), "should only be patching entry points when at a safepoint");

        final Pointer patchSite = tm.codeAt(pos).toPointer();

        long disp64 = target.toLong() - patchSite.plus(RIP_JMP_INSTRUCTION_LENGTH).toLong();
        int disp32 = (int) disp64;
        FatalError.check(disp64 == disp32, "Code displacement out of 32-bit range");

        patchSite.writeByte(0, (byte) RIP_JMP);
        patchSite.writeByte(1, (byte) disp32);
        patchSite.writeByte(2, (byte) (disp32 >> 8));
        patchSite.writeByte(3, (byte) (disp32 >> 16));
        patchSite.writeByte(4, (byte) (disp32 >> 24));
    }

    /**
     * Indicate with the instruction in a target method at a given position is a jump to a specified destination.
     * Used in particular for testing if the entry points of a target method were patched to jump to a trampoline.
     *
     * @param tm         a target method
     * @param pos        byte index relative to the start of the method to a call site
     * @param jumpTarget target to compare with the target of the assumed jump instruction
     * @return {@code true} if the instruction is a jump to the target, false otherwise
     */
    public static boolean isJumpTo(TargetMethod tm, int pos, CodePointer jumpTarget) {
	return false;
        /*Log.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!ARM isJumpTo WRONG!!!!!!!!!!!!!!!!!!!!!!!");
        final Pointer jumpSite = tm.codeAt(pos).toPointer();
        if (jumpSite.readByte(0) == (byte) RIP_JMP) {
            final int disp32 = jumpSite.readInt(1);
            final Pointer target = jumpSite.plus(RIP_CALL_INSTRUCTION_LENGTH).plus(disp32);
            return jumpTarget.toPointer().equals(target);
        }
        return false;
	*/
    }

    // Disable instance creation.
    private ARMTargetMethodUtil() {
    }

    @HOSTED_ONLY
    public static boolean atFirstOrLastInstruction(StackFrameCursor current) {
        // check whether the current ip is at the first instruction or a return
        // which means the stack pointer has not been adjusted yet (or has already been adjusted back)
        TargetMethod tm = current.targetMethod();
        CodePointer entryPoint = tm.callEntryPoint.equals(CallEntryPoint.C_ENTRY_POINT) ?
                CallEntryPoint.C_ENTRY_POINT.in(tm) :
                CallEntryPoint.OPTIMIZED_ENTRY_POINT.in(tm);

        //return entryPoint.equals(current.vmIP()) || current.stackFrameWalker().readByte(current.vmIP().toAddress(), 0) == RET;
        return entryPoint.equals(current.vmIP()) || current.stackFrameWalker().readInt(current.vmIP().toAddress(), 0) == RET;

    }

    @HOSTED_ONLY
    public static boolean acceptStackFrameVisitor(StackFrameCursor current, StackFrameVisitor visitor) {
        AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
        Pointer sp = current.sp();
        // Only during a stack walk in the context of the Inspector can execution
        // be anywhere other than at a safepoint.
        if (atFirstOrLastInstruction(current) || (generator != null && generator.inPrologue(current.vmIP(), current.targetMethod()))) {
            sp = sp.minus(current.targetMethod().frameSize());
        }
        StackFrameWalker stackFrameWalker = current.stackFrameWalker();
        StackFrame stackFrame = new ARMV7JavaStackFrame(stackFrameWalker.calleeStackFrame(), current.targetMethod(), current.vmIP().toPointer(), sp, sp);
        return visitor.visitFrame(stackFrame);
    }

    public static VMFrameLayout frameLayout(TargetMethod tm) {
        // APN confusion here is, we plan to use a frame pointer fp, which is to store the top of
        // the stack (activation record) for a procedure
        // whereas the stack pointer sp is the tail of the stack itself where we add onto
        return new OptoStackFrameLayout(tm.frameSize(), true, ARMV7.r13);
    }

    /**
     * Advances the stack walker such that {@code current} becomes the callee.
     *
     * @param current the frame just visited by the current stack walk
     * @param csl     the layout of the callee save area in {@code current}
     * @param csa     the address of the callee save area in {@code current}
     */
    public static void advance(StackFrameCursor current, CiCalleeSaveLayout csl, Pointer csa) {
        assert csa.isZero() == (csl == null);
        TargetMethod tm = current.targetMethod();
        Pointer sp = current.sp();
        Pointer ripPointer = sp.plus(tm.frameSize());
        if (MaxineVM.isHosted()) {
            // Only during a stack walk in the context of the Inspector can execution
            // be anywhere other than at a safepoint.
            AdapterGenerator generator = AdapterGenerator.forCallee(current.targetMethod());
            if (generator != null && generator.advanceIfInPrologue(current)) {
                return;
            }
            if (atFirstOrLastInstruction(current)) {
                ripPointer = sp;
            }
        }

        StackFrameWalker sfw = current.stackFrameWalker();
        Pointer callerIP = sfw.readWord(ripPointer, 0).asPointer();
        Pointer callerSP = ripPointer.plus(Word.size()); // Skip return instruction pointer on stack
        Pointer callerFP;
        if (!csa.isZero() && csl.contains(ARMV7.r11.encoding)) {
            // Read RBP from the callee save area
            callerFP = sfw.readWord(csa, csl.offsetOf(ARMV7.r11.encoding)).asPointer();
        } else {
            // Propagate RBP unchanged
            callerFP = current.fp();
        }

        current.setCalleeSaveArea(csl, csa);

        boolean wasDisabled = SafepointPoll.disable();
        sfw.advance(callerIP, callerSP, callerFP);
        if (!wasDisabled) {
            SafepointPoll.enable();
        }
    }

    public static Pointer returnAddressPointer(StackFrameCursor frame) {
        TargetMethod tm = frame.targetMethod();
        Pointer sp = frame.sp();
        return sp.plus(tm.frameSize());
    }

    public static int callInstructionSize(byte[] code, int pos) {
        Log.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!ARMTargetMethodUtil.REG RIP_CALL ISSUE");
        if ((code[pos] & 0xFF) == RIP_CALL) {
            return RIP_CALL_INSTRUCTION_SIZE;
        }
        if ((code[pos] & 0xff) == REG_CALL) {
            return 2;
        }
        if ((code[pos + 1] & 0xff) == REG_CALL) {
            return 3;
        }
        return -1;
    }
}
