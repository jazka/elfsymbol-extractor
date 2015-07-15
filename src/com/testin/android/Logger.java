// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android;

/**
 * Output the logs with 4 levels
 */
public class Logger {
    private static final boolean mIsDebug = false;
    public static void LogInfo(String msg) {
        System.out.print("[I]");
        System.out.println(msg);
    }

    public static void LogDebug(String msg) {
        if (!mIsDebug) {
            return;
        }
        System.out.print("[D]");
        System.out.println(msg);
    }

    public static void LogWarn(String msg) {
        System.out.print("[W]");
        System.out.println(msg);
    }

    public static void LogError(String msg) {
        System.out.print("[E]");
        System.out.println(msg);
    }
}