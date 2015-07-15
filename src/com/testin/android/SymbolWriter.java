// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// file is an original work developed by Testin

package com.testin.android;

import java.util.zip.ZipOutputStream;

public class SymbolWriter
{
    public long mStartAddress = 0L;
    public long mEndAddress = 0L;
    public String mFunction = null;
    public String mSourceFile = null;
    public long mLineNumber = 0L;
    public long mEndLineNumber = 0L;


    public void write(ZipOutputStream zos) throws Exception {
        if (zos == null) {
            throw new Exception("Failed in SymbolWriter.write with null parameters!");
        }
        zos.write(getString().getBytes("utf-8"));
    }

    private String getString() {
        StringBuilder ret = new StringBuilder();
        ret.append(Long.toHexString(mStartAddress));
        ret.append("\t");

        ret.append(Long.toHexString(mEndAddress));
        ret.append("\t");

        ret.append(mFunction);
        if (mSourceFile == null) {
            ret.append("\n");
            return ret.toString();
        }
        ret.append("\t");
        ret.append(mSourceFile);
        ret.append(":");
        if (0L == mEndLineNumber) {
            ret.append(mLineNumber);
        }
        else if (mEndLineNumber == mLineNumber) {
            ret.append(mLineNumber);
        } else if (mEndLineNumber > mLineNumber) {
            ret.append(mLineNumber);
            ret.append("-");
            ret.append(mEndLineNumber);
        } else {
            ret.append(mEndLineNumber);
            ret.append("-");
            ret.append(mLineNumber);
        }

        ret.append("\n");
        return ret.toString();
    }
}
