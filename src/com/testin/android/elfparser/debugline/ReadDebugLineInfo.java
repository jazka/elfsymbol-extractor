// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser.debugline;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import com.testin.android.Logger;
import com.testin.android.elfparser.ReadHelper;
import com.testin.android.elfparser.debugline.DebugLineInfoEntry.LineInfoEntry;

/**
 * Read ELF debug line info
 * 
 */

public class ReadDebugLineInfo extends ReadHelper {
    private static final int ENTRY_VERSION_2 = 2;
    private static final int ENTRY_VERSION_3 = 3;
    private static final int ENTRY_VERSION_4 = 4;

    public Vector<LineInfoEntry> mAllLineInfoEntries = null;
    private String mFileName = null;
    private long mOffset = 0L;
    private long mSize = 0L;

    public ReadDebugLineInfo(String elfFile, boolean isLsb, long offset, long size) throws IOException {
        super(elfFile);
        mFileName = elfFile;
        mOffset = offset;
        mSize = size;
        setLSB(isLsb);
        mAllLineInfoEntries = new Vector<LineInfoEntry> ();
    }

    public void read() {
        if (mOffset == 0 || mSize == 0) {
            Logger.LogError("ReadDebugLineInfo read failed with wrong offset or size!");
            readFinish(false);
            return;
        }

        try {
            seek(mOffset);
            long offset = mOffset;
            long remainSize = mSize;
            long totalLength = -1L;
            int entryVer = 0;
            DebugLineInfoEntry entry = null;

            while(remainSize > 0) {
                totalLength = readWord();
                if (-1L == totalLength) {
                    totalLength = readTWord();
                }

                if (0 == entryVer) {
                    entryVer = readHalf();
                }

                switch (entryVer) {
                    case ENTRY_VERSION_2:
                        entry = new DebugLineInfoEntry2(mFileName, isLSB(), offset);
                        break;
                    case ENTRY_VERSION_3:
                        break;
                    case ENTRY_VERSION_4:
                }

                entry.read();
                if (!entry.readSucceed()) {
                    break;
                }
                // Collect debug line info
                mAllLineInfoEntries.addAll(entry.mLineInfoEntries);
                offset += entry.size();
                remainSize -= entry.size();
            }

            Collections.sort(mAllLineInfoEntries, new Comparator<LineInfoEntry>() {
                @Override
                public int compare(LineInfoEntry arg0, LineInfoEntry arg1) {
                    return (int)(arg0.mStartAddress - arg1.mStartAddress);
                }
            });
        } catch (IOException e) {
            Logger.LogError("ReadSection read failed:" + e.toString());
            readFinish(false);
        }

        readFinish();
    }

    public void print() {
        if (!readSucceed()) {
            Logger.LogDebug("Read Debug Line Info failed!");
            return;
        }
        Logger.LogDebug("------------ Debug Line Info ------------");
        for (LineInfoEntry lie : mAllLineInfoEntries) {
            Logger.LogDebug(lie.getString());
        }
    }
}
