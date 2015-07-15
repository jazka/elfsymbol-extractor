// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.testin.android.Logger;

/**
 * Read ELF file Sections
 *
 */
public class ReadSection extends ReadHelper {
    private static final String DEBUG_LINE_SECTION = ".debug_line";
    private static final String SYMBOL_TABLE_SECTION = ".symtab";
    private static final String STRING_TABLE_SECTION = ".strtab";

    private long mShtOffset = 0L;
    private int mShtEntrySize = 0;
    private int mShtEntryNumber = 0;
    private int mIndexOfShtNameTable = 0;
    private List<SectionHeader> mSectionHeaderTable = null;
    private byte[] mNameTable = null;
    private int mNameTableSize = 0;
    private long mDebugLineOffset = 0L;
    private long mDebugLineSize = 0L;
    private long mSymbolTableOffset = 0L;
    private int mSymbolTableEntryNum = 0;
    private byte[] mStringTable = null;
    private long mStringTableSize = 0L;

    public ReadSection(String elfFile, boolean isLsb, long shOffset, int shEntryNumber,
            int shtEntrySize, int indexOfShtNameTable) throws IOException {
        super(elfFile);
        mShtOffset = shOffset;
        mShtEntryNumber = shEntryNumber;
        mShtEntrySize = shtEntrySize;
        mIndexOfShtNameTable = indexOfShtNameTable;
        mSectionHeaderTable = new ArrayList<SectionHeader>();
        setLSB(isLsb);
    }

    public void read() {
        if (mShtOffset == 0 ||
                mShtEntryNumber == 0 ||
                mShtEntrySize == 0 ||
                mIndexOfShtNameTable == 0) {
            Logger.LogError("ReadSection read failed with wrong structure!");
            readFinish(false);
            return;
        }

        try {
            readSectionHeader();
            readSectionNameTable();
            readSpecialSections();
        } catch (IOException e) {
            Logger.LogError("ReadSection read failed:" + e.toString());
            readFinish(false);
        }

        readFinish();
    }

    public long getDebugLineOffset() {
        return mDebugLineOffset;
    }

    public long getDebugLineSize() {
        return mDebugLineSize;
    }

    public long getSymbolTableOffset() {
        return mSymbolTableOffset;
    }

    public int getSymbolTableEntryNum() {
        return mSymbolTableEntryNum;
    }

    public byte[] getStrTable() {
        return mStringTable;
    }

    public long getStrTableSize() {
        return mStringTableSize;
    }

    public void print() {
        if (!readSucceed()) {
            Logger.LogDebug("Read Section Header failed!");
            return;
        }
        Logger.LogDebug("------------ EFL Sections ------------");
        int idx = 1;
        for (SectionHeader header : mSectionHeaderTable) {
            Logger.LogDebug("------------ Header #" + idx + "------------");
            Logger.LogDebug("sh_name:" + readSectionName(header.mNameIndex));
            Logger.LogDebug("sh_type:" + header.mType);
            Logger.LogDebug("sh_flags:" + header.mFlag);
            Logger.LogDebug("sh_addr:" + header.mAddr );
            Logger.LogDebug("sh_offset:" + header.mOffset );
            Logger.LogDebug("sh_size:" + header.mSize);
            Logger.LogDebug("sh_link:" + header.mLink);
            Logger.LogDebug("sh_info:" + header.mInfo);
            Logger.LogDebug("sh_addralign:" + header.mAddrAllign);
            Logger.LogDebug("sh_entsize:" + header.mEntryFixSize);
            idx++;
        }
    }

    private void readSectionHeader() throws IOException {
        seek(mShtOffset);
        for (int i = 0; i < mShtEntryNumber; i++) {
            SectionHeader header = new SectionHeader();
            header.mNameIndex = (int) readWord();
            header.mType = readWord();
            header.mFlag = readWord();
            header.mAddr =  readWord();
            header.mOffset = readWord();
            header.mSize = readWord();
            header.mLink = readWord();
            header.mInfo = readWord();
            header.mAddrAllign = readWord();
            header.mEntryFixSize = readWord();
            mSectionHeaderTable.add(header);
        }
    }

    private void readSectionNameTable() throws IOException {
        SectionHeader header = mSectionHeaderTable.get(mIndexOfShtNameTable);
        seek(header.mOffset);
        mNameTableSize = (int) header.mSize;
        mNameTable = new byte[mNameTableSize];
        readFully(mNameTable);
    }

    private void readSpecialSections() throws IOException {
        for (SectionHeader header : mSectionHeaderTable) {
            String name = readSectionName(header.mNameIndex);
            if (name.equals(DEBUG_LINE_SECTION)) {
                mDebugLineOffset = header.mOffset;
                mDebugLineSize = header.mSize;
            } else if (name.equals(SYMBOL_TABLE_SECTION)) {
                mSymbolTableOffset = header.mOffset;
                mSymbolTableEntryNum = (int) (header.mSize / header.mEntryFixSize);
            } else if (name.equals(STRING_TABLE_SECTION)) {
                seek(header.mOffset);
                mStringTableSize = header.mSize;
                mStringTable = new byte[(int) mStringTableSize];
                readFully(mStringTable);
            }
        }
    }

    private String readSectionName(int index) {
        String ret = Integer.toString(index);

        if (index >= mNameTableSize) {
            Logger.LogError("Section name index out of range!");
            return ret;
        }

        int nameLength = 0;
        for (int i = index; i < mNameTableSize; i++) {
            if (mNameTable[i] == 0) {
                break;
            }
            nameLength++;
        }
        ret = new String(mNameTable, index, nameLength);

        return ret;
    }

    /**
     * ELF file Section Header
     *
     * typedef struct {
     *     Elf32_Word  sh_name;
     *     Elf32_Word  sh_type;
     *     Elf32_Word  sh_flags;
     *     Elf32_Addr  sh_addr;
     *     Elf32_Off   sh_offset;
     *     Elf32_Word  sh_size;
     *     Elf32_Word  sh_link;
     *     Elf32_Word  sh_info;
     *     Elf32_Word  sh_addralign;
     *     Elf32_Word  sh_entsize;
     * } Elf32_Shdr;
     */
    public class SectionHeader {
        public int mNameIndex = -1;
        public long mType = 0;
        public long mFlag = 0;
        public long mAddr = 0L;
        public long mOffset = 0L;
        public long mSize = 0;
        public long mLink = 0;
        public long mInfo = 0;
        public long mAddrAllign = 0;
        public long mEntryFixSize = 0;
    }
}
