/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
     * Convert 
     * @throws Exception 
     */
    void convertToTwoTone() throws ImageException 
    {
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
