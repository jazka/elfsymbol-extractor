// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.testin.android.elfparser.ReadHeader;
import com.testin.android.elfparser.ReadSection;
import com.testin.android.elfparser.ReadSymbolSection;
import com.testin.android.elfparser.ReadSymbolSection.SymbolTableEntry;
import com.testin.android.elfparser.debugline.DebugLineInfoEntry.LineInfoEntry;
import com.testin.android.elfparser.debugline.ReadDebugLineInfo;

/**
 * This tool is designed to parse ELF (Executable and Linkable Format) file
 * and extract the symbol infomations.
 */
public class ElfSymbolExtractor {
    private String mSrcFile = null;
    private String mDesFile = null;
    private String mDesFileName = null;

    public ElfSymbolExtractor() {
        mSrcFile = ElfSymbolTool.sElfFile;
        mDesFile = ElfSymbolTool.sSymbolFile;
        mDesFileName = ElfSymbolTool.sSymbolFileName;
    }

    public void extract() {
        if (mSrcFile == null || mDesFile == null || mDesFileName == null) {
            Logger.LogError("Extract failed!");
            return;
        }

        try {
            ReadHeader readHeader = new ReadHeader(mSrcFile);
            readHeader.read();
            if (!readHeader.readSucceed()) {
                return;
            }
            readHeader.print();

            ReadSection readSection = new ReadSection(mSrcFile, readHeader.isLSB(),
                    readHeader.getShOffset(), readHeader.getShEntryNumber(),
                    readHeader.getShtEntrySize(), readHeader.getIndexOfShtNameTable());
            readSection.read();
            if (!readSection.readSucceed()) {
                return;
            }
            readSection.print();

            ReadSymbolSection readSymbolSection = new ReadSymbolSection(mSrcFile, readHeader.isLSB(),
                    readSection.getSymbolTableOffset(), readSection.getSymbolTableEntryNum(),
                    readSection.getStrTable(), readSection.getStrTableSize());
            readSymbolSection.read();
            if (!readSymbolSection.readSucceed()) {
                return;
            }
            readSymbolSection.print();

            ReadDebugLineInfo readDebugLineInfo = new ReadDebugLineInfo(mSrcFile, readHeader.isLSB(),
                    readSection.getDebugLineOffset(), readSection.getDebugLineSize());
            readDebugLineInfo.read();
            if (!readDebugLineInfo.readSucceed()){
                return;
            }
            readDebugLineInfo.print();

            Logger.LogInfo("ELF SO file extract succeed!");

            writeSymbolFile(readHeader, readSymbolSection, readDebugLineInfo);
            Logger.LogInfo("Write symbol file succeed!");
        } catch (IOException e) {
            Logger.LogError("Extract failed:" + e.toString());
            return;
        }
    }

    public void writeSymbolFile(ReadHeader rh, ReadSymbolSection rss, ReadDebugLineInfo rdli) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(mDesFile));
            ZipEntry ze = new ZipEntry(mDesFileName);
            zos.putNextEntry(ze);
            writeSymbolHeader(zos, rh);
            writeSymbols(zos, rss, rdli);
            zos.closeEntry();
        } catch (Exception e) {
            Logger.LogError("Failed in writeSymbolFile: " + e.toString());
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    Logger.LogError("Failed in writeSymbolFile:" + e.toString());
                }
            }
        }
    }

    private void writeSymbolHeader(ZipOutputStream zos, ReadHeader rh) throws Exception {
        if (zos == null || rh == null) {
            throw new Exception("Failed in writeHeader due to null parameters!");
        }

        String tmp = "Version: " + ElfSymbolTool.SYMBOL_TOOL_VERSION + "\n";
        zos.write(tmp.getBytes("utf-8"));
        tmp = "Source File: " + mSrcFile + "\n";
        zos.write(tmp.getBytes("utf-8"));
        tmp = "Arch: " + ElfSymbolTool.sArch + "\n";
        zos.write(tmp.getBytes("utf-8"));
        tmp = "Format: " + rh.getFileFormat() + "\n";
        zos.write(tmp.getBytes("utf-8"));
        tmp = "File Type: " + rh.getFileTypeStr() + "\n";
        zos.write(tmp.getBytes("utf-8"));
    }

    private void writeSymbols(ZipOutputStream zos, ReadSymbolSection rss,
            ReadDebugLineInfo rdli) throws Exception {
        if (zos == null || rss == null || rdli == null) {
            throw new Exception("Failed in writeSymbols due to null parameters!");
        }
        int dliIndex = 0;
        SymbolTableEntry func = null;
        SymbolTableEntry preFunc = null;
        SymbolWriter sw = null;
        SymbolWriter preSw = null;
        LineInfoEntry lie = rdli.mAllLineInfoEntries.get(dliIndex);
        for (int i = 0; (lie != null) && (i < rss.mFunctionTable.size()); i++) {
            func = (SymbolTableEntry)rss.mFunctionTable.get(i);
            if ((preFunc == null) || (func.mStartAddress >= preFunc.mEndAddress)) {
                if (func.mEndAddress <= lie.mStartAddress) {
                    if (func.mSymbolSize != 0) {
                        if (preSw != null) {
                            preSw.mEndAddress = preFunc.mEndAddress;
                            preSw.write(zos);
                        }
                        preSw = null;
                        sw = new SymbolWriter();
                        sw.mStartAddress = func.mStartAddress;
                        sw.mEndAddress = func.mEndAddress;
                        sw.mFunction = rss.readSymbolName(func.mNameIndex);
                        sw.write(zos);
                    }
                } else {
                    while ((lie != null) && (lie.mStartAddress < func.mEndAddress)) {
                        if ((preSw == null) || (lie.mStartAddress != preSw.mStartAddress)) {
                            if (lie.mStartAddress >= func.mStartAddress) {
                                sw = new SymbolWriter();
                                sw.mStartAddress = lie.mStartAddress;
                                sw.mSourceFile = lie.mPathName;
                                sw.mLineNumber = lie.mLineNumber;
                                sw.mEndLineNumber = lie.mEndLineNumber;
                                sw.mFunction = rss.readSymbolName(func.mNameIndex);
                                if (preSw != null) {
                                    preSw.mEndAddress = sw.mStartAddress;
                                    preSw.write(zos);
                                }
                                preSw = sw;
                            } else {
                                if (preSw != null) {
                                    preSw.mEndAddress = func.mEndAddress;
                                    preSw.write(zos);
                                }
                                preSw = null;
                            }
                        }
                        dliIndex++;
                        if (dliIndex < rdli.mAllLineInfoEntries.size())
                            lie = rdli.mAllLineInfoEntries.get(dliIndex);
                        else
                            lie = null;
                    }
                    preFunc = func;
                }
            }
        }
        if (preSw != null) {
            preSw.mEndAddress = func.mEndAddress;
            preSw.write(zos);
        }
    }
}
