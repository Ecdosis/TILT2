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
import java.net.URLConnection;
import javax.servlet.ServletOutputStream;

/**
 * Handle requests for ordinary files like scripts
 * @author desmond
 */
public class TiltFileHandler extends TiltHandler 
{
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws TiltException {
        File f = new File(urn);
        try
        {
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int)f.length()];
            fis.read(data);
            String mimeType = URLConnection.guessContentTypeFromStream(fis);
            response.setContentType(mimeType);
            ServletOutputStream sos = response.getOutputStream();
            sos.write( data );
            sos.close();
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
