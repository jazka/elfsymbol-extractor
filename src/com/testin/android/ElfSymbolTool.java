// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2015 Testin.  All rights reserved.
//
// This file is an original work developed by Testin

package com.testin.android;

import java.io.File;

/**
 * This tool is designed to parse ELF (Executable and Linkable Format) file
 * and extract the symbol infomations.
 */
public class ElfSymbolTool {
    public static final String SYMBOL_TOOL_VERSION = "1.0.0";

    public static String sElfFile;
    public static String sArch;
    public static String sSymbolFile;
    public static String sSymbolFileName;

    public static void main(String[] args) {
        if (!parseArgs(args)) {
            printHelp();
            return;
        }
        Logger.LogInfo("Input: " + sElfFile);
        Logger.LogInfo("Arch: " + sArch);
        Logger.LogInfo("Output: " + sSymbolFile);
        new ElfSymbolExtractor().extract();
    }

    public static void printHelp() {
        System.out.println("This tool could get symbol infos from ELF SO file to help Android NDK stack symbolization.");
        System.out.println("It is developed to used in Testin Crash+ system(http://crash.testin.cn/)");
        System.out.println("----------------- Usage -------------------");
        System.out.println("Java -jar ElfSymbolExtractor.jar <input> <arch> [<output>]");
        System.out.println("<input>\tELF SO file usually located in /project path/obj/local/armeabi/");
        System.out.println("<arch>\tCPU arch of SO file building, should be one of armeabi/armeabi-v7a/mips/x86");
        System.out.println("<output>\tOutput path, current dir is default if you don't specify one");
    }

    private static boolean parseArgs(String[] args) {
        if ((args == null) || (args.length < 2)) {
            Logger.LogError("Wrong args!");
            return false;
        }

        sElfFile = args[0];
        File elf = new File(sElfFile);
        if ((elf == null) || (!elf.isFile()) || (!elf.exists())) {
            Logger.LogError("The elf file " + sElfFile + " is not avail!");
            return false;
        }

        sArch = args[1];
        if (!(sArch.equals("armeabi")) &&
                !(sArch.equals("armeabi-v7a")) &&
                !(sArch.equals("mips")) &&
                !(sArch.equals("x86"))) {
            Logger.LogError("The arch " + sArch + " is unsupport");
            return false;
        }

        String outPath = "";
        if (args.length == 3) {
            outPath = args[2];
            if (!outPath.endsWith(File.separator)) {
                outPath = outPath + File.separator;
            }
        }
        sSymbolFileName = "TestinSymbol_" + sArch + "_" + elf.getName().replace(".so", "") + ".symbol";
        sSymbolFile = outPath + sSymbolFileName + ".zip";

        return true;
    }

}
