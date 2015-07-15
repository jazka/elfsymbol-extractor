// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser.debugline;

import java.io.IOException;

import com.testin.android.Logger;

/**
 * Read debug line info with version 2
 *
 */

public class DebugLineInfoEntry2 extends DebugLineInfoEntry {

    public DebugLineInfoEntry2(String elfFile, boolean isLsb, long offset) throws IOException {
        super(elfFile, isLsb, offset);
    }

    public void read() {
        if (mOffset <= 0) {
            Logger.LogError("DebugLineInfoEntry2 read failed with wrong offset!");
            readFinish(false);
            return;
        }

        try {
            seek(mOffset);

            mTotalLength = readWord();
            mFstFieldSize = WORD_SIZE;
            if (-1L == mTotalLength) {
                mTotalLength = readTWord();
                mIs32Bit = false;
                mFstFieldSize += TWORD_SIZE;
            }

            mVersion = readHalf();
            mPrologueLength = mIs32Bit ? readWord() : readTWord();
            mMinInstructionLength = (int) readByte();
            mDefaultIsStmt = (int) readByte();
            mLineBase = readByte();
            mLineRange = (int) readByte();
            mOpcodeBase = (int) readByte();
            mStandardOpcodeLengths = new byte[mOpcodeBase - 1];
            readFully(mStandardOpcodeLengths);

            readDirs();
            readFileNameEntries();
            initRestLength();
            readOpCode();
        } catch (Exception e) {
            Logger.LogError("DebugLineInfoEntry2 read failed:" + e.toString());
            readFinish(false);
        }

        readFinish();
    }

    private void readOpCode() throws Exception {
        while (mRestLength > 0L) {
            byte b = readByte(true);
            OpcodeType type = getOpcodeType(b);
            switch (type) {
                case SPECIAL_OPCODE:
                    readSpecialOpcode(b);
                    break;
                case STANDARD_OPCODE:
                    readStandardOpcode(b);
                    break;
                case EXTENDED_OPCODE:
                    readExtendedOpcode();
                    break;
            }
        }
    }

    private void readSpecialOpcode(byte specialOpcode) throws Exception {
        long so = specialOpcode & 0xFF;
        if (so < (mOpcodeBase & 0xFF)) {
            throw new Exception("Read special opcode error for " + specialOpcode);
        }

        long adjust = so - mOpcodeBase;
        long lineInc = mLineBase + adjust % mLineRange;
        long addressInc = adjust / mLineRange * mMinInstructionLength;
        mLine += lineInc;
        mAddress += addressInc;
        mBasicBlock = false;
        addToLineInfoEntries();
    }

    private void readStandardOpcode(byte standardOpcode) throws Exception {
        long so = standardOpcode & 0xFF;
        if (so >= (mOpcodeBase & 0xFF)) {
            throw new Exception("Read standard opcode error for " + standardOpcode);
        }

        switch (standardOpcode) {
            case 1:
                addToLineInfoEntries();
                mBasicBlock = false;
                break;
            case 2:
                mAddress += (readULEB128() * mMinInstructionLength);
                break;
            case 3:
                mLine += readSLEB128();
                break;
            case 4:
                mFileIndex = readULEB128();
                break;
            case 5:
                mColumn = readULEB128();
                break;
            case 6:
                mIsStmt = (!mIsStmt);
                break;
            case 7:
                mBasicBlock = true;
                break;
            case 8:
                long adjust = 255L - mOpcodeBase;
                mAddress += (adjust / mLineRange * mMinInstructionLength);
                break;
            case 9:
                readHalf();
                mAddress += (readHalf(true) & 0xFFFF);
                break;
            default:
                throw new Exception("Unsupport standard opcode: " + standardOpcode);
        }
    }

    private void readExtendedOpcode() throws Exception {
        readULEB128();
        byte opcode = readByte(true);
        switch (opcode) {
            case 1:
                initRegisters();
                break;
            case 2:
                mAddress = mIs32Bit ? (readWord(true) & 0xFFFFFFFF) : readTWord(true);
                break;
            case 3:
                FileNameEntry fne = new FileNameEntry();
                char c = (char) readByte(true);
                while (c != 0) {
                    fne.mFileName.append(c);
                    c = (char) readByte(true);
                }
                fne.mIndexOfDirs = readULEB128();
                fne.mLastModTime = readULEB128();
                fne.mFileLength = readULEB128();
                mFileNameEntries.add(fne);
                break;
            case 4:
                mDiscriminator = readULEB128();
                break;
            default:
                throw new Exception("Unsupport extended opcode:" + opcode);
        }
    }
}