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

/**
 * Handle everything related to the abstract image in all its forms
 * @author desmond
 */
public class Picture {
    private static String PNG_TYPE = "PNG";
    String id;
    File orig;
    File greyscale;
    File twotone;
    public Picture( String urlStr, JSONArray coords ) throws TiltException
    {
        try
        {
            URL url = new URL( urlStr );
            // use url as id for now
            id = urlStr;
            // fetch picture from url
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            orig = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            PictureRegistry.register( this, urlStr );
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
    public void dispose() throws ImageException
    {
        try
        {
            if ( orig != null )
                orig.delete();
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
            File newFile = File.createTempFile("TMP","tmp");
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
     * @throws Exception 
     */
    void convertToTwoTone() throws ImageException 
    {
        try
        {
            if ( greyscale == null )
                convertToGreyscale();
            BufferedImage grey = ImageIO.read(greyscale);
            WritableRaster wr = grey.getRaster();
            int square = (int)Math.floor(wr.getWidth()*0.025);
            for ( int y=0;y<wr.getHeight();y+=Math.min(square,wr.getHeight()-y) )
            {
                int h = Math.min(square,wr.getHeight()-y);
                for ( int x=0;x<wr.getWidth();x+=Math.min(square,wr.getWidth()-x) )
                {
                    // compute average pixel value
                    int w = Math.min(square,wr.getWidth()-x);
                    int[] dArray = new int[h*w];
                    int[] res = wr.getPixels(x,y,w,h,dArray);
                    int total = 0;
                    for ( int i=0;i<dArray.length;i++ )
                        total += dArray[i];
                    int average = total/dArray.length;
                    // set pixels that are darker to black and the rest to white
                    for ( int j=y;j<y+h;j++ )
                    {
                        int[] iArray = new int[1];
                        for ( int k=x;k<x+w;k++ )
                        {
                            res = wr.getPixel( k, j, iArray );
                            // +25 darker filters out noise
                            if ( res[0]+25 < average )
                                iArray[0] = 0;
                            else
                            {
                                iArray[0] = 255;
                            }
                            wr.setPixel( k, j, iArray );
                        }
                    }
                }
            }
            twotone = File.createTempFile(PictureRegistry.PREFIX,
                PictureRegistry.SUFFIX);
            ImageIO.write( grey, "png", twotone );
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
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
     * @param url the original url 
     * @return the image as a byte array
     */
    public byte[] getOrigData( String url ) throws ImageException
    {
        return getPicData( orig );
    }
    /**
     * Get the greyscale version of the data
     * @param url the docid
     * @return a byte array being the greyscale rendition of it
     * @throws ImageException 
     */
    public byte[] getGreyscaleData( String url ) throws ImageException
    {
        if ( greyscale == null )
            convertToGreyscale();
        return getPicData( greyscale );
    }
    /**
     * Get a twotone representation of the original
     * @param url the docid
     * @return a byte array (at 256 bpp)
     * @throws ImageException 
     */
    public byte[] getTwoToneData( String url ) throws ImageException
    {
        if ( twotone == null )
            convertToTwoTone();
        return getPicData( twotone );
    }
}
