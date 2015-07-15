// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser;

import java.io.IOException;

import com.testin.android.Logger;

/**
 * Read ELF file Header
 *
 * typedef struct {
 *     unsigned char   e_ident[EI_NIDENT];
 *     Elf32_Half      e_type;
 *     Elf32_Half      e_machine;
 *     Elf32_Word      e_version;
 *     Elf32_Addr      e_entry;
 *     Elf32_Off       e_phoff;
 *     Elf32_Off       e_shoff;
 *     Elf32_Word      e_flags;
 *     Elf32_Half      e_ehsize;
 *     Elf32_Half      e_phentsize;
 *     Elf32_Half      e_phnum;
 *     Elf32_Half      e_shentsize;
 *     Elf32_Half      e_shnum;
 *     Elf32_Half      e_shstrndx;
 * } Elf32_Ehdr;
 */
public class ReadHeader extends ReadHelper {
    // Offset from end of ident structure in half-word sizes.
    private static final int OFFSET_IDENT = 16;

    /**
     * File Type
     * -----------------------------------------------------
     *   Name      |    Value    |    Meaning
     * ET_NONE     |     0       | No file type
     * ET_REL      |     1       | Relocatable file
     * ET_EXEC     |     2       | Executable file
     * ET_DYN      |     3       | Shared object file
     * ET_CORE     |     4       | Core file
     * ET_LOOS     |   0xfe00    | Operating system-specific
     * ET_HIOS     |   0xfeff    | Operating system-specific
     * ET_LOPROC   |   0xff00    | Processor-specific
     * ET_HIPROC   |   0xffff    | Processor-specific
    */
    private static final int ET_NONE = 0;
    private static final int ET_REL = 1;
    private static final int ET_EXEC = 2;
    private static final int ET_DYN = 3;
    private static final int ET_CORE = 4;

    private String mElfFile = null;
    private int mFileType = 0;
    private int mMachineType = 0;
    private long mFileVersion = 0;
    private long mEntryAddr = 0L;
    private long mPhtOffset = 0L;
    private long mShtOffset = 0L;
    private long mMachineFlag = 0;
    private int mHeaderSize = 0;
    private int mPhtEntrySize = 0;
    private int mPhtEntryNumber = 0;
    private int mShtEntrySize = 0;
    private int mShtEntryNumber = 0;
    private int mIndexOfShtNameTable = 0;
    private String mFileFormat = null;

    public ReadHeader(String elfFile) throws IOException {
        super(elfFile);
        mElfFile = elfFile;
    }

    public void read() {
        try {
            ReadIdentification headerIndent = new ReadIdentification(mElfFile);
            headerIndent.read();
            if (headerIndent.readSucceed()) {
                setLSB(headerIndent.isLSB());
                mFileFormat = headerIndent.getFileFormat();
                // headerIndent.print();
            }

            seek(OFFSET_IDENT);
            mFileType = readHalf();
            mMachineType = readHalf();
            mFileVersion = readWord();
            mEntryAddr = readWord();
            mPhtOffset = readWord();
            mShtOffset = readWord();
            mMachineFlag = readWord();
            mHeaderSize = readHalf();
            mPhtEntrySize = readHalf();
            mPhtEntryNumber = readHalf();
            mShtEntrySize = readHalf();
            mShtEntryNumber = readHalf();
            mIndexOfShtNameTable = readHalf();
        } catch (IOException e) {
            Logger.LogError("ReadHeader read failed:" + e.toString());
            readFinish(false);
        }

        readFinish();
    }

    public long getShOffset() {
        return mShtOffset;
    }

    public int getShEntryNumber() {
        return mShtEntryNumber;
    }

    public int getShtEntrySize() {
        return mShtEntrySize;
    }

    public int getIndexOfShtNameTable() {
        return mIndexOfShtNameTable;
    }

    public String getFileFormat() {
        return mFileFormat;
    }

    public void print() {
        if (!readSucceed()) {
            Logger.LogDebug("Read EFL header failed!");
            return;
        }

        Logger.LogDebug("------------ EFL Header ------------");
        Logger.LogDebug("File Type: " + getFileTypeStr());
        Logger.LogDebug("Machine Type: " + mMachineType);
        Logger.LogDebug("File Version: " + mFileVersion);
        Logger.LogDebug("Entry Address: " + mEntryAddr);
        Logger.LogDebug("PHT Offset: " + mPhtOffset);
        Logger.LogDebug("SHT Offset: " + mShtOffset);
        Logger.LogDebug("Machine Flag: " + mMachineFlag);
        Logger.LogDebug("Header Size: " + mHeaderSize);
        Logger.LogDebug("PHT Entry Size: " + mPhtEntrySize);
        Logger.LogDebug("PHT Entry Number: " + mPhtEntryNumber);
        Logger.LogDebug("SHT Entry Size: " + mShtEntrySize);
        Logger.LogDebug("SHT Entry Number: " + mShtEntryNumber);
        Logger.LogDebug("Index Of SHT Name: " + mIndexOfShtNameTable);
    }

    public String getFileTypeStr() {
        String typeStr = "";

        switch (mFileType) {
            case ET_NONE:
                typeStr = "No file type";
                break;
            case ET_REL:
                typeStr = "Relocatable file";
                break;
            case ET_EXEC:
                typeStr = "Executable file";
                break;
            case ET_DYN:
                typeStr = "Shared object file";
                break;
            case ET_CORE:
                typeStr = "Core file";
                break;
            default:
                typeStr = "Reserved file type";
        }

        return typeStr;
    }
}
