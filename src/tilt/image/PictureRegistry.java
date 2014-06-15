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
import java.io.File;
import java.net.InetAddress;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import tilt.exception.ImageException;
import tilt.exception.DoSException;

/**
 * Keep a track of temporary files and delete them after a set time
 * @author desmond
 */
public class PictureRegistry 
{
    static long FORGET_TIME = 100000;
    static String PREFIX = "TMP";
    static String SUFFIX = ".tmp";
    static TreeMap<Long,Picture> map;
    static HashMap<String,Long> urls;
    static HashMap<InetAddress,String> posters;
    /**
     * We only need one instance of this class
     */
    static 
    {
        map = new TreeMap<Long,Picture>();
        urls = new HashMap<String,Long>();
        posters = new HashMap<InetAddress,String>();
        // clean out temp directory
        String tmpDir = System.getProperty("java.io.tmpdir");
        File dir = new File( tmpDir );
        File[] contents = dir.listFiles();
        for ( int i=0;i<contents.length;i++ )
        {
            String name = contents[i].getName();
            if ( name.startsWith(PREFIX)&&name.endsWith(SUFFIX) )
                contents[i].delete();
        }
    }
    /**
     * Add a picture to the registry (cache)
     * @param pic the picture object
     * @param url its url or id
     * @throws DoSException 
     */
    public static void register( Picture pic, String url ) throws DoSException
    {
        String id = posters.get(pic.poster);
        if ( id != null )
            throw new DoSException("Please wait before uploading a new image");
        Long key = new Long(System.currentTimeMillis());
        map.put( key, pic );
        urls.put( url, key );
        posters.put(pic.poster,url);
    }
    /**
     * Weed out files that haven't been accessed for some time
     */
    public static void prune() throws ImageException
    {
        Set<Long> keys = map.keySet();
        Iterator<Long> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            Long key = iter.next();
            if ( System.currentTimeMillis()-key.longValue() > FORGET_TIME )
            {
                Picture p = map.get(key);
                p.dispose();
                map.remove( key );
                // clear from urls also
                Set<String> urlKeys = urls.keySet();
                Iterator<String> iter2 = urlKeys.iterator();
                while ( iter2.hasNext() )
                {
                    String url = iter2.next();
                    Long value = urls.get(url);
                    if ( value.longValue()==key.longValue() )
                    {
                        urls.remove( url );
                        Set<InetAddress> iKeys = posters.keySet();
                        Iterator<InetAddress> iiter = iKeys.iterator();
                        while ( iiter.hasNext() )
                        {
                            InetAddress iKey = iiter.next();
                            if ( posters.get(iKey).equals(url) )
                            {
                                posters.remove( iKey );
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            else
                break;
        }
    }
    /**
     * Get an individual picture object, using its url as an id
     * @param url the url
     * @return the correspnding Picture object
     * @throws ImageException 
     */
    public static Picture get( String url ) throws ImageException
    {
        Long key = urls.get( url );
        if ( key != null )
            return map.get(key);
        else
            throw new ImageException("Picture "+url+" not found");
    }
}
