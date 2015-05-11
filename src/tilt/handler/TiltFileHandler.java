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

package tilt.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.exception.TiltException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Handle requests for ordinary files like scripts
 * @author desmond
 */
public class TiltFileHandler extends TiltHandler 
{
    /**
     * Look in all the likely places for this relatively specified file
     * @param relPath the relative path from somewhere
     * @return the file object or null
     */
    private File getStaticFile( String relPath )
    {
        //System.out.println("Getting file "+relPath);
        File parent1 = new File(System.getProperty("user.dir"));
        String path = TiltGetHandler.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();
        File parent2 = new File(path).getParentFile();
        File file = new File(parent1,relPath);
        if ( !file.exists())
            file = new File(parent2,relPath);
        File parent3 = parent2.getParentFile();
        while( parent3 != null && !file.exists() )
        {
            file = new File(parent3,relPath);
            parent3 = parent3.getParentFile();
        }
        return file;
    }
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws TiltException {
        try
        // serve up any other form of data in its native format
        // this is for secondary requests made by this service itself
        {
            File file = getStaticFile(urn);
            if ( file != null )
            {
                FileInputStream fis = new FileInputStream(file);
                int len = (int)file.length();
                byte[] data = new byte[len];
                fis.read( data );
                fis.close();
                if ( file.getName().endsWith(".png") )
                    response.setContentType("image/png");
                else if ( file.getName().endsWith(".jpg") )
                    response.setContentType("image/jpg");
                else if ( file.getName().endsWith(".json") )
                    response.setContentType("application/json");
                else if ( file.getName().endsWith(".html") )
                    response.setContentType("text/html");
                else if ( file.getName().endsWith(".js") )
                    response.setContentType("text/javascript");
                else if ( file.getName().endsWith(".css") )
                    response.setContentType("text/css");
                else if ( file.getName().endsWith(".txt"))
                    response.setContentType("text/plain");
                else    // else assume binary data
                {
                    //System.out.println(urn);
                    response.setContentType("application/octet-stream");
                }
                response.getOutputStream().write(data);
            }
            else
                throw new FileNotFoundException(file.getAbsolutePath());
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
