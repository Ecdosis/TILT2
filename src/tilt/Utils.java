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
package tilt;
import java.awt.image.WritableRaster;
import java.util.Iterator;
import java.util.ArrayList;
import java.awt.Point;
import java.awt.geom.PathIterator;
import java.awt.image.Raster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import tilt.image.convexhull.*;
import tilt.image.geometry.Polygon;

/**
 * Some routines that need sharing by all
 * @author desmond
 */
public class Utils 
{
    /**
     * Convert a URL into a form suitable as a parameter
     * @param value the raw, unescapedURL
     * @return an escaped URL with / and  space escaped
     */
    public static String escape( String value )
    {
        StringBuilder sb = new StringBuilder();
        {
            for ( int i=0;i<value.length();i++ )
            if ( value.charAt(i) == ' ' )
                sb.append("%20");
            else if ( value.charAt(i) == '/' )
               sb.append("%2F");
            else
                sb.append( value.charAt(i) );
        }
        return sb.toString();
    }
    /**
     * Chop off the first component of a urn
     * @param urn the urn to chop
     * @return the first urn component
     */
    public static String first( String urn )
    { 
        int slashPos1 = -1;
        if ( urn.startsWith("/") )
            slashPos1 = urn.indexOf( "/" );
        int slashPos2 = urn.indexOf( "/", slashPos1+1 );
        if ( slashPos1 != -1 && slashPos2 != -1 )
            return urn.substring(slashPos1+1, slashPos2 );
        else if ( slashPos1 != -1 && slashPos2 == -1 )
            return urn.substring( slashPos1+1 );
        else if ( slashPos1 == -1 && slashPos2 != -1 )
            return urn.substring( 0,slashPos2 );
        else
            return urn;
    }
    /**
     * Extract the second component of a urn
     * @param urn the urn to extract from
     * @return the second urn component
     */
    public static String second( String urn )
    { 
        int start=-1,end=-1;
        for ( int state=0,i=0;i<urn.length();i++ )
        {
            char token = urn.charAt(i);
            switch ( state )
            {
                case 0:// always pass first char
                    state = 1;
                    break;
                case 1: 
                    if ( token == '/' )
                        state = 2;
                    break;
                case 2:
                    start=i;
                    if ( token == '/' )
                    {
                        state = -1;
                        end = i;
                    }
                    else
                        state = 3;
                    break;
                case 3:
                    if ( token == '/' )
                    {
                        end = i;
                        state = -1;
                    }
                    break;
            }
            if ( state == -1 )
                break;
        }
        if ( end == -1 )
            end = urn.length();
        if ( start == -1 )
            start = urn.length();
        return urn.substring( start, end );
    }
    /**
     * Pop off the frontmost part of the path
     * @param path the path to pop
     * @return the popped path
     */
    public static String pop( String path )
    {
        while ( path.length()>0 && path.startsWith("/") )
            path = path.substring(1);
        int pos = path.indexOf("/");
        if ( pos != -1 )
            path = path.substring( pos+1 );
        return path;
    }
    /**
     * Just escape quotes for a string to be jsonified
     * @param input the string to escape
     * @param replaceWith replace double quotes with this
     * @return the escaped string
     */
    public static String escapeQuotes( String input, String replaceWith )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<input.length();i++ )
        {
            char token = input.charAt(i);
            switch ( token )
            {
                case '"':
                    sb.append(replaceWith);
                    break;
                default:
                    sb.append(token);
                    break;
            }
        }
        return sb.toString();
    }
    /**
     * Creates a new raster that has a <b>copy</b> of the data in
     * <tt>ras</tt>.  This is highly optimized for speed.  There is
     * no provision for changing any aspect of the SampleModel.
     * However you can specify a new location for the returned raster.
     *
     * This method should be used when you need to change the contents
     * of a Raster that you do not "own" (ie the result of a
     * <tt>getData</tt> call).
     *
     * @param ras The Raster to copy.
     *
     * @param minX The x location for the upper left corner of the
     *             returned WritableRaster.
     *
     * @param minY The y location for the upper left corner of the
     *             returned WritableRaster.
     *
     * @return    A writable copy of <tt>ras</tt>
     */
    public static WritableRaster copyRaster(Raster ras, int minX, int minY) {
        WritableRaster ret = Raster.createWritableRaster
            (ras.getSampleModel(),
             new Point(0,0));
        ret = ret.createWritableChild
            (ras.getMinX()-ras.getSampleModelTranslateX(),
             ras.getMinY()-ras.getSampleModelTranslateY(),
             ras.getWidth(), ras.getHeight(),
             minX, minY, null);

        // Use System.arraycopy to copy the data between the two...
        DataBuffer srcDB = ras.getDataBuffer();
        DataBuffer retDB = ret.getDataBuffer();
        if (srcDB.getDataType() != retDB.getDataType()) {
            throw new IllegalArgumentException
                ("New DataBuffer doesn't match original");
        }
        int len   = srcDB.getSize();
        int banks = srcDB.getNumBanks();
        int [] offsets = srcDB.getOffsets();
        for (int b=0; b< banks; b++) {
            switch (srcDB.getDataType()) {
            case DataBuffer.TYPE_BYTE: {
                DataBufferByte srcDBT = (DataBufferByte)srcDB;
                DataBufferByte retDBT = (DataBufferByte)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            case DataBuffer.TYPE_INT: {
                DataBufferInt srcDBT = (DataBufferInt)srcDB;
                DataBufferInt retDBT = (DataBufferInt)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            case DataBuffer.TYPE_SHORT: {
                DataBufferShort srcDBT = (DataBufferShort)srcDB;
                DataBufferShort retDBT = (DataBufferShort)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            case DataBuffer.TYPE_USHORT: {
                DataBufferUShort srcDBT = (DataBufferUShort)srcDB;
                DataBufferUShort retDBT = (DataBufferUShort)retDB;
                System.arraycopy(srcDBT.getData(b), offsets[b],
                                 retDBT.getData(b), offsets[b], len);
                break;
            }
            }
        }

        return ret;
    }
    /**
     * Add a "/"to the end of a path if needed
     * @param path the original path
     * @return the path with ONE trailing slash
     */
    public static String ensureSlash( String path )
    {
        if ( path.endsWith("/") )
            return path;
        else
            return path+"/";
    }
    public static String getFromUrl( String url ) throws MalformedURLException, 
        IOException
    {
        URL loc = new URL(url);
        URLConnection pagesService = loc.openConnection();
        InputStream is = pagesService.getInputStream();
        StringBuilder sb = new StringBuilder();
        while ( is.available() != 0 )
        {
            byte[] data = new byte[is.available()];
            is.read( data );
            sb.append( new String(data) );
        }
        is.close();
        return sb.toString();
    }
    public static String getUrl( String serverName, String docid, 
        String pageid ) throws MalformedURLException, IOException
    {
        String url = "http://"+serverName+"/pages/uri_template";
        String template = getFromUrl(url);
        if  ( template.contains("{pageid}") )
            template= template.replace("{pageid}",pageid);
        if ( template.contains("{docid}") )
            template = template.replace("{docid}",docid);
        return template;
    }
}