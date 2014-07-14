/*
 * This file is part of TILT.
 *
 *  TILT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  TILT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TILT.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */

package tilt.image;
import java.net.URL;
import java.net.InetAddress;
import tilt.exception.ImageException;
import org.json.simple.*;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import tilt.exception.TiltException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.awt.Graphics;

import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import tilt.image.page.Page;

/**
 * Handle everything related to the abstract image in all its forms
 * @author desmond
 */
public class Picture {
    private static String PNG_TYPE = "PNG";
    
    String id;
    InetAddress poster;
    float ppAverage;
    Page page;
    File orig;
    File greyscale;
    File twotone;
    File cleaned;
    File baselines;
    File words;
    int numWords;
    /**
     * Create a picture. Pictures stores links to the various image files.
     * @param urlStr the remote picture url as a string
     * @param coords coordinates for the picture from the geoJSON file
     * @param numWords the number of words on the page
     * @param poster the ipaddress of the poster of the image (DDoS prevention)
     * @throws TiltException 
     */
    public Picture( String urlStr, JSONArray coords, int numWords, 
        InetAddress poster ) throws TiltException
    {
        try
        {
            URL url = new URL( urlStr );
            this.numWords = numWords;
            // use url as id for now
            id = urlStr;
            this.poster = poster;
            // try to register the picture
            PictureRegistry.register( this, urlStr );
            // fetch picture from url
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            orig = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            FileOutputStream fos = new FileOutputStream(orig);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            String mimeType = getFormatName();
            if ( !mimeType.equals(PNG_TYPE) )
                convertToPng();
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
    /**
     * Ensure that all files are deleted when we go
     * @throws ImageException 
     */
    public void dispose() throws ImageException
    {
        try
        {
            if ( orig != null )
                orig.delete();
            if ( greyscale != null )
                greyscale.delete();
            if ( twotone != null )
                twotone.delete();
            if ( baselines != null )
                baselines.delete();
            if ( words != null )
                words.delete();
            // dispose of other temporary files here
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Get the original Picture data
     * @return the raw picture byte array
     */
    byte[] getOrig() throws ImageException
    {
        try
        {
            FileInputStream fis = new FileInputStream( orig );
            byte[] data = new byte[(int)orig.length()];
            fis.read( data );
            fis.close();
            return data;
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Get the format of the picture
     * @return a mime type
     * @throws ImageException 
     */
    String getFormatName() throws ImageException
    {
        try
        {
            ImageInputStream iis = ImageIO.createImageInputStream(orig);
            Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) 
            {
                throw new RuntimeException("No readers found for "+id);
            }
            ImageReader reader = iter.next();
            iis.close();
            return reader.getFormatName();
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Convert the input file from anything to png
     * @throws ImageException 
     */
    void convertToPng() throws ImageException
    {
        try
        {
            BufferedImage bufferedImage = ImageIO.read(orig);
            File newFile = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( bufferedImage, "png", newFile );
            orig.delete();
            orig = newFile;
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Convert from the original png file to greyscale png. Save original.
     * @throws ImageException 
     */
    void convertToGreyscale() throws ImageException 
    {
        try
        {
            BufferedImage png = ImageIO.read(orig);
            BufferedImage grey = new BufferedImage(png.getWidth(), png.getHeight(),  
                BufferedImage.TYPE_BYTE_GRAY);  
            Graphics g = grey.getGraphics();  
            g.drawImage( png, 0, 0, null );  
            g.dispose();
            greyscale = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( grey, "png", greyscale );
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Convert from greyscale to twotone (black and white)
     * Adapted from OCRopus
     * Copyright 2006-2008 Deutsches Forschungszentrum fuer Kuenstliche 
     * Intelligenz or its licensors, as applicable.
     * http://ocropus.googlecode.com/svn/trunk/ocr-binarize/ocr-binarize-sauvola.cc
     * @throws Exception 
     */
    void convertToTwoTone() throws ImageException 
    {
        try
        {
            int MAXVAL = 256;
            double k = 0.34;
            if ( greyscale == null )
                convertToGreyscale();
            BufferedImage grey = ImageIO.read(greyscale);
            WritableRaster grey_image = grey.getRaster();
            WritableRaster bin_image = grey.copyData(null);
            int square = (int)Math.floor(grey_image.getWidth()*0.025);
            if ( square == 0 )
                square = Math.min(20,grey_image.getWidth());
            if ( square > grey_image.getHeight() )
                square = grey_image.getHeight();
            int whalf = square>>1;
            if ( whalf == 0 )
                throw new Exception("whalf==0!");
            int image_width  = grey_image.getWidth();
            int image_height = grey_image.getHeight();
            // Calculate the integral image, and integral of the squared image
            // original algorithm ate up too much memory, use floats for longs
            float[][] integral_image = new float[image_width][image_height];
            float[][] rowsum_image = new float[image_width][image_height];
            float[][] integral_sqimg = new float[image_width][image_height];
            float[][] rowsum_sqimg = new float[image_width][image_height];
            int xmin,ymin,xmax,ymax;
            double diagsum,idiagsum,diff,sqdiagsum,sqidiagsum,sqdiff,area;
            double mean,std,threshold;
            // for get/setPixel
            int[] iArray = new int[1];
            int[] oArray = new int[1];
            for ( int j=0;j<image_height;j++ )
            {
                grey_image.getPixel(0,j,iArray);
                rowsum_image[0][j] = iArray[0];
                rowsum_sqimg[0][j] = iArray[0]*iArray[0];
            }
            for ( int i=1;i<image_width;i++ )
            {
                for ( int j=0;j<image_height;j++)
                {
                    grey_image.getPixel(i,j,iArray);
                    rowsum_image[i][j] = rowsum_image[i-1][j] 
                        + iArray[0];
                    rowsum_sqimg[i][j] = rowsum_sqimg[i-1][j] 
                        + iArray[0]*iArray[0];
                }
            }
            for ( int i=0;i<image_width;i++ )
            {
                integral_image[i][0] = rowsum_image[i][0];
                integral_sqimg[i][0] = rowsum_sqimg[i][0];
            }
            for (int i=0; i<image_width; i++)
            {
                for ( int j=1; j<image_height; j++ )
                {
                    integral_image[i][j] = integral_image[i][j-1] 
                        + rowsum_image[i][j];
                    integral_sqimg[i][j] = integral_sqimg[i][j-1] 
                        + rowsum_sqimg[i][j];
                }
            }
            // compute mean and std.dev. using the integral image
            for ( int i=0; i<image_width; i++ )
            {
                for ( int j=0;j<image_height;j++ )
                {
                    xmin = Math.max(0,i-whalf);
                    ymin = Math.max(0,j-whalf);
                    xmax = Math.min(image_width-1,i+whalf);
                    ymax = Math.min(image_height-1,j+whalf);
                    area = (xmax-xmin+1)*(ymax-ymin+1);
                    grey_image.getPixel(i,j,iArray);
                    // area can't be 0 here
                    if ( area == 0 )
                        throw new Exception("area can't be 0 here!");
                    if ( xmin==0 && ymin==0 )
                    { 
                        // Point at origin
                        diff   = integral_image[xmax][ymax];
                        sqdiff = integral_sqimg[xmax][ymax];
                    }
                    else if ( xmin==0 && ymin !=0 )
                    { 
                        // first column
                        diff   = integral_image[xmax][ymax] 
                            - integral_image[xmax][ymin-1];
                        sqdiff = integral_sqimg[xmax][ymax] 
                            - integral_sqimg[xmax][ymin-1];
                    }
                    else if ( xmin!=0 && ymin==0 )
                    { 
                        // first row
                        diff   = integral_image[xmax][ymax] 
                            - integral_image[xmin-1][ymax];
                        sqdiff = integral_sqimg[xmax][ymax] 
                            - integral_sqimg[xmin-1][ymax];
                    }
                    else
                    { 
                        // rest of the image
                        diagsum    = integral_image[xmax][ymax] 
                            + integral_image[xmin-1][ymin-1];
                        idiagsum   = integral_image[xmax][ymin-1] 
                            + integral_image[xmin-1][ymax];
                        diff       = diagsum - idiagsum;
                        sqdiagsum  = integral_sqimg[xmax][ymax] 
                            + integral_sqimg[xmin-1][ymin-1];
                        sqidiagsum = integral_sqimg[xmax][ymin-1] 
                            + integral_sqimg[xmin-1][ymax];
                        sqdiff     = sqdiagsum - sqidiagsum;
                    }
                    mean = diff/area;
                    std  = Math.sqrt((sqdiff - diff*diff/area)/(area-1));
                    threshold = mean*(1+k*((std/128)-1));
                    if (iArray[0] < threshold) 
                        oArray[0] = 0;
                    else
                        oArray[0] = MAXVAL-1;
                    bin_image.setPixel(i,j,oArray);
                }
            }
            twotone = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            grey.setData( bin_image );
            ImageIO.write( grey, "png", twotone );
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Convert to cleaned from twotone
     * @throws Exception 
     */
    void convertToCleaned() throws ImageException 
    {
        try
        {
            if ( twotone == null )
                convertToTwoTone();
            BufferedImage tt = ImageIO.read(twotone);
            RemoveNoise rn = new RemoveNoise( tt );
            rn.clean();
            cleaned = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( tt, "png", cleaned );
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Convert to show lines from cleaned
     * @throws ImageException 
     */
    void convertToBaselines() throws ImageException
    {
        try
        {
            if ( cleaned == null )
                convertToCleaned();
            BufferedImage withLines = ImageIO.read(cleaned);
            FindLines fl = new FindLines( withLines, numWords );
            page = fl.getPage();
            ppAverage = fl.getPPAverage();
            baselines = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( withLines, "png", baselines );
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Convert to show identified words
     * @throws ImageException 
     */
    void convertToWords() throws ImageException 
    {
        try
        {
            if ( baselines == null )
                convertToBaselines();
            BufferedImage bandw = ImageIO.read(twotone);
            BufferedImage originalImage =  ImageIO.read(orig);
            FindWords fw = new FindWords( bandw, page );
            page.print( originalImage );
            words = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( originalImage, "png", words );
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
     }
    /**
     * Retrieve the data of a picture file
     * @return a byte array
     * @throws ImageException 
     */
    private byte[] getPicData( File src ) throws ImageException
    {
        byte[] data = new byte[(int)src.length()];
        try
        {
            FileInputStream fis = new FileInputStream(src);
            fis.read( data );
            return data;
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Read the original image 
     * @return the image as a byte array
     */
    public byte[] getOrigData() throws ImageException
    {
        return getPicData( orig );
    }
    /**
     * Read the cleaned image 
     * @return the image as a byte array
     */
    public byte[] getCleanedData() throws ImageException
    {
        if ( cleaned == null )
            convertToCleaned();
        return getPicData( cleaned );
    }
    /**
     * Get the greyscale version of the data
     * @return a byte array being the greyscale rendition of it
     * @throws ImageException 
     */
    public byte[] getGreyscaleData() throws ImageException
    {
        if ( greyscale == null )
            convertToGreyscale();
        return getPicData( greyscale );
    }
    /**
     * Get a twotone representation of the original
     * @return a byte array (at 256 bpp)
     * @throws ImageException 
     */
    public byte[] getTwoToneData() throws ImageException
    {
        if ( twotone == null )
            convertToTwoTone();
        return getPicData( twotone );
    }
    /**
     * Get a baselines representation of the original
     * @return a byte array (at 256 bpp)
     * @throws ImageException 
     */
    public byte[] getBaselinesData() throws ImageException
    {
        if ( baselines == null )
            convertToBaselines();
        return getPicData( baselines );
    }
    /**
     * Get a baselines representation of the original
     * @return a byte array (at 256 bpp)
     * @throws ImageException 
     */
    public byte[] getWordsData() throws ImageException
    {
        if ( words == null )
            convertToWords();
        return getPicData( words );
    }
    /**
     * Get the GeoJson shapes data
     * @return a GeoJson string
     */
    public String getGeoJson()
    {
        return page.toGeoJson();
    }
}
