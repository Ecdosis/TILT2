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
import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import javax.servlet.ServletOutputStream;
import tilt.exception.*;
import tilt.constants.Params;
import tilt.constants.ImageType;
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
    ImageType imageType;
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            imageType = ImageType.read(request.getParameter(Params.PICTYPE));
            docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                Picture p = PictureRegistry.get(docid);
                byte[]pic = null;
                switch (imageType) 
                {
                    case original:
                        pic = p.getOrigData(docid);
                        break;
                    case greyscale:
                        pic = p.getGreyscaleData(docid);
                        break;
                    case twotone:
                        pic = p.getTwoToneData(docid);
                        break;
                }  
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
                    response.getOutputStream().println("<p>image "+
                        docid+" not found</p>");
                }
            }
            else
                response.getOutputStream().println(
                    "<p>please specify a docid</p>");
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
}
