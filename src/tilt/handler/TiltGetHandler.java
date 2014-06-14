/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */
package tilt.handler;
import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import javax.servlet.ServletOutputStream;
import tilt.exception.*;
import tilt.Params;
import tilt.image.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handle a GET request
 * @author desmond
 */
public class TiltGetHandler extends TiltHandler
{
    String docid;
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                Picture p = PictureRegistry.get(docid);
                byte[] pic = p.getOrigData(docid);
                if ( pic != null )
                {
                    ByteArrayInputStream bis = new ByteArrayInputStream(pic);
                    String mimeType = URLConnection.guessContentTypeFromStream(bis);
                    response.setContentType(mimeType);
                    ServletOutputStream sos = response.getOutputStream();
                    sos.write( pic );
                    sos.close();
                }
                else
                {
                    response.getOutputStream().println("image "+docid+" not found");
                }
            }
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
}
