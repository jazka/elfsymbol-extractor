// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser;

import java.io.IOException;

import com.testin.android.Logger;

/**
 * Read ELF file Identification
 *
 *    Name      |    Value    |    Purpose
 * EI_MAG0      |      0      | File identification
 * EI_MAG1      |      1      | File identification
 * EI_MAG2      |      2      | File identification
 * EI_MAG3      |      3      | File identification
 * EI_CLASS     |      4      | File class
 * EI_DATA      |      5      | Data encoding
 * EI_VERSION   |      6      | File version
 * EI_OSABI     |      7      | Operating system/ABI identification
 * EI_ABIVERSION|      8      | ABI version
 * EI_PAD       |      9      | Start of padding bytes
 * EI_NIDENT    |      16     | Size of e_ident[]
 */
public class ReadIdentification extends ReadHelper {
    private static final int EI_NIDENT = 16;
    // The magic values for the ELF identification.
    private static final byte[] ELF_IDENT = { 
        (byte) 0x7F, (byte) 'E', (byte) 'L', (byte) 'F'
    };
    // Identifies the file's class, or capacity.
    private static final byte ELFCLASS32 = 1;
    private static final byte ELFCLASS64 = 2;
    // Specifies the encoding of both the data structures used by object file container and
    //      data contained in object file sections
    private static final byte ELFDATA2LSB = 1;
    private static final byte ELFDATA2MSB = 2;
    // ELF header version number and it must be EV_CURRENT
    private static final byte EV_CURRENT = 1;

    private boolean mIs32Bit;

    public ReadIdentification(String elfFile) throws IOException {
        super(elfFile);
    }

    public void read() throws IOException {
        mFile.seek(0);
        mFile.readFully(mBuffer, 0, EI_NIDENT);

        if ((mBuffer[0] != ELF_IDENT[0]) ||
                (mBuffer[1] != ELF_IDENT[1]) ||
                (mBuffer[2] != ELF_IDENT[2]) ||
                (mBuffer[3] != ELF_IDENT[3])) {
            Logger.LogError("Invalid ELF file");
            readFinish(false);
            return;
        }

        if (mBuffer[4] == ELFCLASS32) {
            mIs32Bit = true;
        } else if (mBuffer[4] == ELFCLASS64) {
            mIs32Bit = false;
        } else {
            Logger.LogError("Invalid class!");
            readFinish(false);
            return;
        }

        if (mBuffer[5] == ELFDATA2LSB) {
            mIsLSB = true;
        } else if (mBuffer[5] == ELFDATA2MSB) {
            mIsLSB = false;
        } else {
            Logger.LogError("Invalid data format!");
            readFinish(false);
            return;
        }

        if (mBuffer[6] != EV_CURRENT) {
            Logger.LogError("Invalid header version!");
            readFinish(false);
            return;
        }

        readFinish();
    }

    public String getFileFormat() {
        return mIs32Bit ? "ELFCLASS32" : "ELFCLASS64";
    }

    public void print() {
        if (!readSucceed()) {
            Logger.LogDebug("Read EFL header identification failed!");
            return;
        }

        Logger.LogDebug("----- EFL Header Identification -------");
        Logger.LogDebug("File Class: " + getFileFormat());

        if (mIsLSB) {
            Logger.LogDebug("Data Encoding: ELFDATA2LSB");
        } else {
            Logger.LogDebug("Data Encoding: ELFDATA2MSB");
        }

        Logger.LogDebug("File Version: EV_CURRENT");
    }
}
