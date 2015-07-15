// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android.elfparser;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.testin.android.Logger;

/**
 * Read ELF file Helper
 */
public abstract class ReadHelper {
    public static final int BYTE_SIZE = 1;
    public static final int HALF_SIZE = 2;
    public static final int WORD_SIZE = 4;
    public static final int TWORD_SIZE = 8;
    public static final int BUFFER_SIZE = 512;

    protected RandomAccessFile mFile = null;
    protected final byte[] mBuffer = new byte[BUFFER_SIZE];
    protected boolean mSucceed = true;
    protected boolean mIsLSB;

    public abstract void print();

    public ReadHelper(String fileName) throws IOException {
        mFile = new RandomAccessFile(fileName, "r");
    }

    public ReadHelper(String fileName, boolean isLSB) throws IOException {
        mFile = new RandomAccessFile(fileName, "r");
        mIsLSB = isLSB;
    }

    public boolean isLSB() {
        return mIsLSB;
    }

    public void setLSB(boolean isLsb) {
        mIsLSB = isLsb;
    }

    public void seek(long offset) throws IOException {
        mFile.seek(offset);
    }

    public void readFully(byte[] b) throws IOException {
        mFile.readFully(b);
    }

    public byte readByte() throws IOException {
        mFile.readFully(mBuffer, 0, BYTE_SIZE);
        return mBuffer[0];
    }

    public int readHalf() throws IOException {
        mFile.readFully(mBuffer, 0, HALF_SIZE);

        final int answer;
        if (mIsLSB) {
            answer = mBuffer[1] << 8 | mBuffer[0];
        } else {
            answer = mBuffer[0] << 8 | mBuffer[1];
        }

        return answer;
    }

    public long readWord() throws IOException {
        mFile.readFully(mBuffer, 0, WORD_SIZE);

        int answer = 0;
        if (mIsLSB) {
            for (int i = WORD_SIZE - 1; i >= 0; i--) {
                answer = (answer << 8) | (mBuffer[i] & 0xFF);
            }
        } else {
            final int N = WORD_SIZE - 1;
            for (int i = 0; i <= N; i++) {
                answer = (answer << 8) | mBuffer[i];
            }
        }

        return answer;
    }

    public long readTWord() throws IOException {
        mFile.readFully(mBuffer, 0, TWORD_SIZE);

        int answer = 0;
        if (mIsLSB) {
            for (int i = TWORD_SIZE - 1; i >= 0; i--) {
                answer = (answer << 8) | (mBuffer[i] & 0xFF);
            }
        } else {
            final int N = TWORD_SIZE - 1;
            for (int i = 0; i <= N; i++) {
                answer = (answer << 8) | mBuffer[i];
            }
        }

        return answer;
    }

    public boolean readSucceed() {
        return mSucceed;
    }

    public void readFinish() {
        readFinish(true);
    }

    public void readFinish(boolean succeed) {
        if (!succeed) {
            mSucceed = false;
        }

        try {
            if (mFile != null) {
                mFile.close();
            }
        } catch (IOException e) {
            Logger.LogError(e.toString());
            mSucceed = false;
        }
    }
}