/*
 *  Copyright (C) 2010-2014 JPEXS, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.jpexs.decompiler.flash.helpers;

import java.awt.*;
import java.io.*;
import java.awt.image.*;

/**
 * Adapted from
 * http://www.javaworld.com/article/2077561/learn-java/java-tip-60--saving-bitmap-files-in-java.html
 */
public class BMPFile extends Component {

    //--- Private constants
    private final static int BITMAPFILEHEADER_SIZE = 14;
    private final static int BITMAPINFOHEADER_SIZE = 40;
    //--- Private variable declaration
    //--- Bitmap file header
    private byte bitmapFileHeader[] = new byte[14];
    private byte bfType[] = {'B', 'M'};
    private int bfSize = 0;
    private int bfReserved1 = 0;
    private int bfReserved2 = 0;
    private int bfOffBits = BITMAPFILEHEADER_SIZE + BITMAPINFOHEADER_SIZE;
    //--- Bitmap info header
    private byte bitmapInfoHeader[] = new byte[40];
    private int biSize = BITMAPINFOHEADER_SIZE;
    private int biWidth = 0;
    private int biHeight = 0;
    private int biPlanes = 1;
    private int biBitCount = 24;
    private int biCompression = 0;
    private int biSizeImage = 0x030000;
    private int biXPelsPerMeter = 0x0;
    private int biYPelsPerMeter = 0x0;
    private int biClrUsed = 0;
    private int biClrImportant = 0;
    //--- Bitmap raw data
    private int bitmap[];
    //--- File section
    private FileOutputStream fo;

    //--- Private constructor
    private BMPFile() {
    }

    public static void saveBitmap(Image image, File file) throws IOException {
        BMPFile b = new BMPFile();
        b.fo = new FileOutputStream(file);
        try{
            b.save(image, image.getWidth(null), image.getHeight(null));
        }finally{
            try{
                b.fo.close();
            }catch(Exception ex){
                //ignore
            }
        }
    }
    /*
     *  The saveMethod is the main method of the process. This method
     *  will call the convertImage method to convert the memory image to
     *  a byte array; method writeBitmapFileHeader creates and writes
     *  the bitmap file header; writeBitmapInfoHeader creates the 
     *  information header; and writeBitmap writes the image.
     *
     */

    private void save(Image parImage, int parWidth, int parHeight) throws IOException {
        convertImage(parImage, parWidth, parHeight);
        writeBitmapFileHeader();
        writeBitmapInfoHeader();
        writeBitmap();

    }
    /*
     * convertImage converts the memory image to the bitmap format (BRG).
     * It also computes some information for the bitmap info header.
     *
     */

    private boolean convertImage(Image parImage, int parWidth, int parHeight) {
        int pad;
        bitmap = new int[parWidth * parHeight];
        PixelGrabber pg = new PixelGrabber(parImage, 0, 0, parWidth, parHeight,
                bitmap, 0, parWidth);
        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            return (false);
        }
        pad = (4 - ((parWidth * 3) % 4)) * parHeight;
        biSizeImage = ((parWidth * parHeight) * 3) + pad;
        bfSize = biSizeImage + BITMAPFILEHEADER_SIZE
                + BITMAPINFOHEADER_SIZE;
        biWidth = parWidth;
        biHeight = parHeight;
        return (true);
    }
    /*
     * writeBitmap converts the image returned from the pixel grabber to
     * the format required. Remember: scan lines are inverted in
     * a bitmap file!
     *
     * Each scan line must be padded to an even 4-byte boundary.
     */

    private void writeBitmap() throws IOException {
        int size;
        int value;
        int j;
        int i;
        int rowCount;
        int rowIndex;
        int lastRowIndex;
        int pad;
        int padCount;
        byte rgb[] = new byte[3];
        size = (biWidth * biHeight) - 1;
        pad = 4 - ((biWidth * 3) % 4);
        if (pad == 4) // <==== Bug correction
        {
            pad = 0;     // <==== Bug correction
        }
        rowCount = 1;
        padCount = 0;
        rowIndex = size - biWidth;
        lastRowIndex = rowIndex;
        for (j = 0; j < size; j++) {
            value = bitmap[rowIndex];
            rgb[0] = (byte) (value & 0xFF);
            rgb[1] = (byte) ((value >> 8) & 0xFF);
            rgb[2] = (byte) ((value >> 16) & 0xFF);
            fo.write(rgb);
            if (rowCount == biWidth) {
                padCount += pad;
                for (i = 1; i <= pad; i++) {
                    fo.write(0x00);
                }
                rowCount = 1;
                rowIndex = lastRowIndex - biWidth;
                lastRowIndex = rowIndex;
            } else {
                rowCount++;
            }
            rowIndex++;
        }
        //--- Update the size of the file
        bfSize += padCount - pad;
        biSizeImage += padCount - pad;
    }
    /*
     * writeBitmapFileHeader writes the bitmap file header to the file.
     *
     */

    private void writeBitmapFileHeader() throws IOException {

        fo.write(bfType);
        fo.write(intToDWord(bfSize));
        fo.write(intToWord(bfReserved1));
        fo.write(intToWord(bfReserved2));
        fo.write(intToDWord(bfOffBits));
    }
    /*
     *
     * writeBitmapInfoHeader writes the bitmap information header
     * to the file.
     *
     */

    private void writeBitmapInfoHeader() throws IOException {

        fo.write(intToDWord(biSize));
        fo.write(intToDWord(biWidth));
        fo.write(intToDWord(biHeight));
        fo.write(intToWord(biPlanes));
        fo.write(intToWord(biBitCount));
        fo.write(intToDWord(biCompression));
        fo.write(intToDWord(biSizeImage));
        fo.write(intToDWord(biXPelsPerMeter));
        fo.write(intToDWord(biYPelsPerMeter));
        fo.write(intToDWord(biClrUsed));
        fo.write(intToDWord(biClrImportant));

    }
    /*
     *
     * intToWord converts an int to a word, where the return
     * value is stored in a 2-byte array.
     *
     */

    private byte[] intToWord(int parValue) {
        byte retValue[] = new byte[2];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x00FF);
        return (retValue);
    }
    /*
     *
     * intToDWord converts an int to a double word, where the return
     * value is stored in a 4-byte array.
     *
     */

    private byte[] intToDWord(int parValue) {
        byte retValue[] = new byte[4];
        retValue[0] = (byte) (parValue & 0x00FF);
        retValue[1] = (byte) ((parValue >> 8) & 0x000000FF);
        retValue[2] = (byte) ((parValue >> 16) & 0x000000FF);
        retValue[3] = (byte) ((parValue >> 24) & 0x000000FF);
        return (retValue);
    }
}