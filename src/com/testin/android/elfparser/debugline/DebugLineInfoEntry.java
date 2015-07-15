// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser.debugline;

import java.io.IOException;
import java.util.Vector;

import com.testin.android.Logger;
import com.testin.android.elfparser.ReadHelper;

/**
 * Read ELF debug line info
 * 
 * ------------------------------------------------------------
 *         Field              |      Type      |    Meaning
 *      total_length          |     uword      | The size in bytes of the statement information for this compilation
 *                                                   unit (not including the total_length field itself)
 *         version            |     uhalf      | Version identifier for the statement information format
 *      prologue_length       |     uword      | The number of bytes following the prologue_length field to the beginning
 *                                                   of the first byte of the statement program itself
 * minimum_instruction_length |     ubyte      | The size in bytes of the smallest target machine instruction
 *      default_is_stmt       |     ubyte      | The initial value of the is_stmt register
 *         line_base          |     sbyte      | This parameter affects the meaning of the special opcodes
 *         line_range         |     ubyte      | This parameter affects the meaning of the special opcodes
 *        opcode_base         |     ubyte      | The number assigned to the first special opcode.
 *  standard_opcode_lengths   | array of ubyte | This array specifies the number of LEB128 operands 
 *    include_directories     |   sequence     | The path names
 *      file_names            |   sequence     | The file entries
 */

public abstract class DebugLineInfoEntry extends ReadHelper {
    protected static enum OpcodeType {
        SPECIAL_OPCODE,
        STANDARD_OPCODE,
        EXTENDED_OPCODE;
    }

    public Vector<LineInfoEntry> mLineInfoEntries = new Vector<LineInfoEntry> ();

    protected long mOffset = 0L;
    protected long mTotalLength = -1L;
    protected int mFstFieldSize = 0;  // Size of total_length field
    protected int mVersion = 0;
    protected long mPrologueLength = 0L;
    protected int mMinInstructionLength = 0;
    protected int mMaxInstructionLength = 0;
    protected int mDefaultIsStmt = 0;
    protected byte mLineBase = 0;
    protected int mLineRange = 0;
    protected int mOpcodeBase = 0;
    protected byte[] mStandardOpcodeLengths = null;
    protected boolean mIs32Bit = true;
    protected long mRestLength = 0L;
    protected Vector<StringBuffer> mDirs = new Vector<StringBuffer> ();
    protected Vector<FileNameEntry> mFileNameEntries = new Vector<FileNameEntry> ();

    protected long mAddress = 0L;
    protected long mFileIndex = 1L;
    protected long mLine = 1L;
    protected long mColumn = 0L;
    protected long mOpIndex = 0L;
    protected boolean mIsStmt = false;
    protected boolean mBasicBlock = false;
    protected boolean mEndEquence = false;
    protected boolean mPrologueEnd = false;
    protected boolean mEpilogue_begin = false;
    protected long mIsA = 0L;
    protected long mDiscriminator = 0L;

    public DebugLineInfoEntry(String elfFile, boolean isLsb, long offset) throws IOException {
        super(elfFile);
        mOffset = offset;
        setLSB(isLsb);
    }

    public abstract void read();

    public byte readByte(boolean isSubLen) throws IOException {
        if (isSubLen) {
            mRestLength -= BYTE_SIZE;
        }
        return readByte();
    }

    public int readHalf(boolean isSubLen) throws IOException {
        if (isSubLen) {
            mRestLength -= HALF_SIZE;
        }
        return readHalf();
    }

    public long readWord(boolean isSubLen) throws IOException {
        if (isSubLen) {
            mRestLength -= WORD_SIZE;
        }
        return readWord();
    }

    public long readTWord(boolean isSubLen) throws IOException {
        if (isSubLen) {
            mRestLength -= TWORD_SIZE;
        }
        return readTWord();
    }

    public long size() {
        return (mTotalLength + mFstFieldSize);
    }

    protected void initRegisters()
    {
        mAddress = 0L;
        mFileIndex = 1L;
        mLine = 1L;
        mColumn = 0L;
        mOpIndex = 0L;
        mIsStmt = false;
        mBasicBlock = false;
        mEndEquence = false;
        mPrologueEnd = false;
        mEpilogue_begin = false;
        mIsA = 0L;
        mDiscriminator = 0L;
    }

    protected void initRestLength() {
        mRestLength = (mTotalLength - mPrologueLength);
        mRestLength -= HALF_SIZE;
        mRestLength -= (mIs32Bit ? WORD_SIZE : TWORD_SIZE);
    }

    protected OpcodeType getOpcodeType(byte b) {
        if (b == 0) {
            return OpcodeType.EXTENDED_OPCODE;
        }
        if ((b > 0) && (b < mOpcodeBase)) {
            return OpcodeType.STANDARD_OPCODE;
        }
        return OpcodeType.SPECIAL_OPCODE;
    }

    public void readDirs() throws IOException {
        mDirs.add(new StringBuffer("."));
        char c = (char) readByte(true);
        while (c != 0) {
            StringBuffer dir = new StringBuffer();
            while (c != 0) {
                dir.append(c);
                c = (char) readByte(true);
            }
            mDirs.add(dir);
            c = (char) readByte(true);
        }
    }

    public void readFileNameEntries() throws IOException {
        FileNameEntry localFne = new FileNameEntry();
        localFne.mFileName.append(".");
        mFileNameEntries.add(localFne);

        char c = (char) readByte(true);
        while (c != 0) {
            FileNameEntry fne = new FileNameEntry();
            while (c != 0) {
                fne.mFileName.append(c);
                c = (char) readByte(true);
            }
            fne.mIndexOfDirs = readULEB128();
            fne.mLastModTime = readULEB128();
            fne.mFileLength = readULEB128();
            mFileNameEntries.add(fne);

            c = (char) readByte(true);
        }
    }

    public void print() {
        if (!readSucceed()) {
            Logger.LogDebug("Read Debug Line Entry failed!");
            return;
        }
        Logger.LogDebug("------------ Debug Line Entry ------------");
        Logger.LogDebug("total_length: " + mTotalLength);
        Logger.LogDebug("version: " + mVersion);
        Logger.LogDebug("prologue_length: " + mPrologueLength);
        Logger.LogDebug("minimum_instruction_length: " + mMinInstructionLength);
        Logger.LogDebug("default_is_stmt: " + mDefaultIsStmt);
        Logger.LogDebug("line_base: " + mLineBase);
        Logger.LogDebug("line_range: " + mLineRange);
        Logger.LogDebug("opcode_base: " + mOpcodeBase);
        Logger.LogDebug("------- Dirs: ");
        for (StringBuffer dir : mDirs) {
            Logger.LogDebug(dir.toString());
        }
        Logger.LogDebug("------- File Name Entries: ");
        for (FileNameEntry fne : mFileNameEntries) {
            Logger.LogDebug(fne.mFileName.toString());
        }
    }
    
    protected long readULEB128() throws IOException {
        long ret = 0L;
        long shift = 0L;
        byte b;
        do {
            b = readByte(true);
            ret += ((b & 0x7F) << (int)shift);
            shift += 7L;
        } while ((b & 0x80) != 0);

        return ret;
    }

    protected long readSLEB128() throws IOException {
        long ret = 0L;
        long shift = 0L;
        boolean negative = false;
        byte b;
        do {
            b = readByte(true);
            ret += ((b & 0x7F) << (int)shift);
            shift += 7L;
            negative = ((b & 0x40) != 0);
        } while ((b & 0x80) != 0);

        return negative ? (ret - (1 << (int)shift)) : ret;
    }

    protected void addToLineInfoEntries() {
        long fixAddr = mAddress + 1L;
        if ((!mLineInfoEntries.isEmpty()) && 
                (fixAddr == (mLineInfoEntries.lastElement()).mStartAddress)) {
            mLineInfoEntries.lastElement().mEndLineNumber = mLine;
            return;
        }
        mLineInfoEntries.add(new LineInfoEntry(mFileIndex, mLine, fixAddr));
    }

    protected class FileNameEntry {
        public StringBuffer mFileName = new StringBuffer();
        public long mIndexOfDirs= 0L;
        public long mLastModTime = 0L;
        public long mFileLength = 0L;
    }

    public class LineInfoEntry {
        public String mPathName = "";
        public String mFileName = "";
        public long mIndexOfFileNameTable = 0L;
        public long mLineNumber = 0L;
        public long mEndLineNumber = 0L;
        public long mStartAddress = 0L;

        public LineInfoEntry(long index, long line, long address) {
            mIndexOfFileNameTable = index;
            mFileName = mFileNameEntries.elementAt((int) mIndexOfFileNameTable).mFileName.toString();
            mPathName = mDirs.get((int) (mFileNameEntries.elementAt((int) mIndexOfFileNameTable)).mIndexOfDirs)
                + "/" + mFileName;
            mLineNumber = line;
            mStartAddress = address;
        }

        public String getString() {
          StringBuilder sb = new StringBuilder();
          sb.append(Long.toHexString(mStartAddress));
          sb.append("\t");
          sb.append(mFileName);
          sb.append(":");
          if (0L == mEndLineNumber) {
            sb.append(mLineNumber);
          }
          else if (mEndLineNumber == mLineNumber) {
            sb.append(mLineNumber);
          } else if (mEndLineNumber > mLineNumber) {
            sb.append(mLineNumber);
            sb.append("-");
            sb.append(mEndLineNumber);
          } else {
            sb.append(mEndLineNumber);
            sb.append("-");
            sb.append(mLineNumber);
          }
          sb.append("\n");
          return sb.toString();
        }
    }
}