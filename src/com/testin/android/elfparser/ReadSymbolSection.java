// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import com.testin.android.Logger;

/**
 * Read ELF file Symbol Section
 *
 */
public class ReadSymbolSection extends ReadHelper {
    private static final int SYMBOL_TYPE_FUNCTION = 2;

    public Vector<SymbolTableEntry> mFunctionTable = null;
    private long mOffset = 0L;
    private int mEntryNumber = 0;
    private byte[] mStrTable = null;
    private long mStrTableSize = 0L;
    private List<SymbolTableEntry> mSymbolTableEntries = null;

    public ReadSymbolSection(String elfFile, boolean isLsb, long offset,
            int entryNumber, byte[] stringTable, long stringTableSize) throws IOException {
        super(elfFile);
        mOffset = offset;
        mEntryNumber = entryNumber;
        mStrTable = stringTable;
        mStrTableSize = stringTableSize;
        mSymbolTableEntries = new ArrayList<SymbolTableEntry>();
        mFunctionTable = new Vector<SymbolTableEntry>();
        setLSB(isLsb);
    }

    public void read() {
        if (mOffset == 0 || mEntryNumber == 0) {
            Logger.LogError("ReadSymbolSection read failed with wrong structure!");
            readFinish(false);
            return;
        }

        try {
            readSymbolTable();
            Collections.sort(mFunctionTable, new Comparator<SymbolTableEntry>() {
                public int compare(SymbolTableEntry arg0, SymbolTableEntry arg1) {
                    return (int)(arg0.mStartAddress - arg1.mStartAddress);
                }
            });
        } catch (IOException e) {
            Logger.LogError("ReadSymbolSection read failed:" + e.toString());
            readFinish(false);
        }

        readFinish();
    }

    public void print() {
        if (!readSucceed()) {
            Logger.LogDebug("Read SymbolSection failed!");
            return;
        }
        Logger.LogDebug("------------ EFL Symbol Table Section ------------");
        int idx = 1;
        for (SymbolTableEntry ste : mSymbolTableEntries) {
            Logger.LogDebug("------------ Symbol Entry #" + idx + "------------");
            Logger.LogDebug("st_name: " + readSymbolName(ste.mNameIndex));
            Logger.LogDebug("st_value: " + ste.mSymbolValue);
            Logger.LogDebug("st_size: " + ste.mSymbolSize);
            Logger.LogDebug("st_info: " + ste.mSymbolInfo);
            Logger.LogDebug("st_other: " + ste.mSymbolOther);
            Logger.LogDebug("st_shndx: " + ste.mRelatedSectionIdx);
            Logger.LogDebug("start_address: " + ste.mStartAddress);
            Logger.LogDebug("end_address: " + ste.mEndAddress);
            idx++;
        }
    }

    public String readSymbolName(int index) {
        String ret = Integer.toString(index);

        if (mStrTable == null) {
            return ret;
        }

        int nameLength = 0;
        for (int i = index; i < mStrTableSize; i++) {
            if (mStrTable[i] == 0) {
                break;
            }
            nameLength++;
        }
        ret = new String(mStrTable, index, nameLength);

        return ret;
    }

    private void readSymbolTable() throws IOException {
        seek(mOffset);
        for (int i = 0; i < mEntryNumber; i++) {
            SymbolTableEntry ste = new SymbolTableEntry();
            ste.mNameIndex = (int) readWord();
            ste.mSymbolValue = (int) readWord();
            ste.mSymbolSize = (int) readWord();
            ste.mSymbolInfo =  readByte();
            ste.mSymbolOther = readByte();
            ste.mRelatedSectionIdx = readHalf();
            int type = ste.mSymbolInfo & 0xF;
            if ((SYMBOL_TYPE_FUNCTION == type) &&
                    (ste.mRelatedSectionIdx != 0)) {
                ste.mStartAddress = ste.mSymbolValue;
                ste.mEndAddress = ste.mSymbolValue + ste.mSymbolSize;
                mFunctionTable.add(ste);
            }
            mSymbolTableEntries.add(ste);
        }
    }

    /**
     * ELF file Symbol Table Section Header
     *
     * typedef struct {
     *     Elf32_Word  st_name;
     *     Elf32_Addr  st_value;
     *     Elf32_Word  st_size;
     *     unsigned char   st_info;
     *     unsigned char   st_other;
     *     Elf32_Half  st_shndx;
     * } Elf32_Sym;
     */
    public class SymbolTableEntry {
        public int mNameIndex = -1;
        public int mSymbolValue = 0;
        public int mSymbolSize = 0;
        public byte mSymbolInfo = 0;
        public byte mSymbolOther = 0;
        public int mRelatedSectionIdx = 0;
        public int mStartAddress = -1;  // Add this member to relate to debug line info
        public int mEndAddress = -1;  // Add this member to relate to debug line info
    }
}
