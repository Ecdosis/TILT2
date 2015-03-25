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
import java.io.IOException;
import java.io.File;
import tilt.exception.TiltException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Color;

import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import javax.servlet.http.HttpServletRequest;
import tilt.image.page.Page;
import tilt.handler.post.TextIndex;
import tilt.align.Matchup;
import tilt.image.page.Word;
import tilt.handler.post.Options;

/**
 * Handle everything related to the abstract image in all its forms
 * @author desmond
 */
public class Picture {
    private final static String PNG_TYPE = "PNG";
    String id;
    InetAddress poster;
    float ppAverage;
    Page page;
    File orig;
    File greyscale;
    File twotone;
    File cleaned;
    File baselines;
    File reduced;
    File blurred;
    File words;
    int blur;
    Double[][] coords;
    TextIndex text;
    boolean linked;
    Options options;
    /**
     * Create a picture. Pictures stores links to the various image files.
     * @param options options from the geoJSON file
     * @param text the text to align with
     * @param poster the ipaddress of the poster of the image (DDoS prevention)
     * @throws TiltException 
     */
    public Picture( Options options, String url, TextIndex text, 
        InetAddress poster ) throws TiltException
    {
        try
        {
            // use url as id for now
            id = url;
            this.text = text;
            this.poster = poster;
            // try to register the picture
            PictureRegistry.register( this, url );
            this.options = options;
            this.coords = new Double[4][2];
            for ( int i=0;i<4;i++ )
            {
                JSONArray vector = (JSONArray)options.coords.get(i);
                for ( int j=0;j<2;j++ )
                {
                    this.coords[i][j] = (Double)vector.get(j);
                }
            }
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
     * Fetch the image from a url and convert to png. Can take time.
     * @throws ImageException 
     */
    public void load() throws ImageException
    {
        try
        {
            URL netUrl = new URL( this.id );
            // fetch picture from url
            ReadableByteChannel rbc = Channels.newChannel(netUrl.openStream());
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
            throw new ImageException(e);
        }
    }
    /**
     * Do the coordinates cover the entire picture or only part of it?
     * @return true if the coords cover the entire picture area
     */
    boolean isWholePicture()
    {
        boolean whole = true;
        // top-left
        if ( coords[0][0].doubleValue() > 0.0 )
            whole = false;
        if ( coords[0][1].doubleValue() > 0.0 )
            whole = false;
        // top-right
        if ( coords[1][0].doubleValue() < 100.0 )
            whole = false;
        if ( coords[1][1].doubleValue() > 0.0 )
            whole = false;
        // bot-right
        if ( coords[2][0].doubleValue() < 100.0 )
            whole = false;
        if ( coords[2][1].doubleValue() < 100.0 )
            whole = false;
        // bot-left
        if ( coords[3][0].doubleValue() > 0.0 )
            whole = false;
        if ( coords[3][1].doubleValue() < 100.0 )
            whole = false;
        return whole;
    }
    /**
     * Crop the original if needed
     */
    Rectangle getCropRect() throws IOException
    {
        int x,y,width,height;
        BufferedImage bi = ImageIO.read(orig);
        if ( !isWholePicture() )
        {
            x = (int)Math.round(bi.getWidth()*coords[0][0].doubleValue()/100.0);
            y = (int)Math.round(bi.getHeight()*coords[0][1].doubleValue()/100.0);
            width = (int)Math.round(bi.getWidth()
                *(coords[1][0].doubleValue()-coords[0][0].doubleValue())/100.0);
            height = (int)Math.round(bi.getHeight()
                *(coords[3][1].doubleValue()-coords[0][1].doubleValue())/100.0);
        }
        else
        {
            x = 0;
            y = 0;
            width = bi.getWidth();
            height = bi.getHeight();
        }
        return new Rectangle( x, y, width, height );

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
        catch ( IOException ioe )
        {
            throw new ImageException(ioe);
        }
    }
    /**
     * Get the format of the picture
     * @return a mime type
     * @throws ImageException 
     */
    final String getFormatName() throws ImageException
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
    final void convertToPng() throws ImageException
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
    int getPropVal( Double prop, int value )
    {
        return (int)Math.round(prop.doubleValue()*value/100.0);
    }
    /**
     * Convert from the original png file to greyscale png. Save original.
     * @throws ImageException 
     */
    public void convertToGreyscale() throws ImageException 
    {
        try
        {
            BufferedImage png = ImageIO.read(orig);
            BufferedImage grey = new BufferedImage(png.getWidth(), 
                png.getHeight(), BufferedImage.TYPE_BYTE_GRAY); 
            Graphics g = grey.getGraphics();  
            g.drawImage( png, 0, 0, null );  
            g.dispose();
            if ( !isWholePicture() )
            {
                // clear excluded regions
                Graphics2D g2d = grey.createGraphics();
                g2d.setColor(Color.white);
                if ( coords[0][0].doubleValue()>0.0 )
                {
                    int w = grey.getWidth();
                    int h = grey.getHeight();
                    g2d.fillRect(0,0,getPropVal(coords[0][0],w),h);
                }
                if ( coords[1][0].doubleValue()<100.0 )
                {
                    int x = getPropVal(coords[1][0],grey.getWidth());
                    g2d.fillRect(x,0,grey.getWidth()-x,grey.getHeight());
                }
                if ( coords[2][1].doubleValue()<100.0 )
                {
                    int y = getPropVal(coords[2][1],grey.getHeight());
                    g2d.fillRect(0,y,grey.getWidth(),grey.getHeight()-y);
                }
                if ( coords[0][1].doubleValue()>0.0 )
                {
                    int y = getPropVal(coords[0][1],grey.getHeight());
                    g2d.fillRect(0,0,grey.getWidth(),y);
                }
                g2d.dispose();
            }
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
    public void convertToTwoTone() throws ImageException 
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
    public void convertToCleaned() throws ImageException 
    {
        try
        {
            if ( twotone == null )
                convertToTwoTone();
            BufferedImage tt = ImageIO.read(twotone);
            RemoveNoise rn = new RemoveNoise( tt, options );
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
     * Convert to cleaned from twotone
     * @throws Exception 
     */
    public void convertToBlurred() throws ImageException 
    {
        try
        {
            if ( cleaned == null )
                convertToCleaned();
            BufferedImage tt = ImageIO.read(cleaned);
            BlurImage bi = new BlurImage( tt, options.blur );
            BufferedImage out = bi.blur();
            blurred = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( out, "png", blurred );
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Convert to show lines from blurred
     * @throws ImageException 
     */
    public void convertToBaselines() throws ImageException
    {
        try
        {
            if ( blurred == null )
                convertToBlurred();
            BufferedImage withLines = ImageIO.read(blurred);
            FindLinesBlurred fl = new FindLinesBlurred( withLines, 
                text.numWords(), options );
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
     * Convert to reduced lines from baselines
     * @throws ImageException 
     */
    public void convertToReduced() throws ImageException
    {
        try
        {
            if ( baselines == null )
                convertToBaselines();
            BufferedImage reducedLines = ImageIO.read(cleaned);
            ReduceLines rl = new ReduceLines( reducedLines, page, options );
            reduced = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( reducedLines, "png", reduced );
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
    public void convertToWords() throws ImageException 
    {
        try
        {
            if ( baselines == null )
                convertToBaselines();
            BufferedImage bandw = ImageIO.read(cleaned);
            BufferedImage originalImage =  ImageIO.read(orig);
            FindWords fw = new FindWords( bandw, page, options );
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
     * Generate the text to image links
     * @throws ImageException 
     */
    public void convertToLinks() throws ImageException
    {
        if ( this.linked )
        {
            page.resetShapes();
            words = null;
        }
        if ( words == null )
            convertToWords();
        float ppc = page.pixelsPerChar( text.numChars() );
        int[] shapeWidths = page.getShapeWidths();
        int[] wordWidths = text.getWordWidths( ppc );
        Matchup m = new Matchup( wordWidths, shapeWidths );
        try
        {
            int[][][] alignments = m.align();
            int[] shapeOffsets = page.getShapeLineStarts();
            Word[] wordObjs = text.getWords( ppc );
            BufferedImage clean = ImageIO.read(cleaned);
            page.align( alignments, shapeOffsets, wordObjs, clean.getRaster() );
            this.linked = true;
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
    public byte[] getBlurredData() throws ImageException
    {
        if ( blurred == null )
            convertToBlurred();
        return getPicData( blurred );
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
     * Get a reduced baselines representation of the original
     * @return a byte array (at 256 bpp)
     * @throws ImageException 
     */
    public byte[] getReducedData() throws ImageException
    {
        if ( reduced == null )
            convertToReduced();
        return getPicData( reduced );
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
    public String getGeoJson() throws ImageException
    {
        try
        {
            // do this always, because the user will want it redone
            BufferedImage image = ImageIO.read(words);
//            double hScale = (coords[2][0].doubleValue()-coords[0][0].doubleValue())/100.0;
//            double vScale = (coords[3][1].doubleValue()-coords[1][1].doubleValue())/100.0;
//            return page.toGeoJson( (int)Math.round(hScale*image.getWidth()), 
//                (int)Math.round(vScale*image.getHeight()) );
            return page.toGeoJson( image.getWidth(),image.getHeight() );
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
}
