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

package tilt.handler.post;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.constants.Params;
import tilt.exception.TiltException;
import tilt.image.Picture;
import tilt.image.PictureRegistry;
import tilt.exception.ImageException;
import tilt.Utils;
import tilt.handler.TiltHandler;

/**
 * Handle an Ajax request for geoJson data about a picture
 * @author desmond
 */
public class TiltRecogniseHandler extends TiltHandler
{
    /** the image URL */
    String docid;
    /** the GeoJson we extract from the picture */
    String geoJson;
    /**
     * Handle a request for a GeoJon description of the text to image links
     * @param request the http request
     * @param response the http response
     * @param urn the remaining urn of the request
     * @throws TiltException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            String docId = request.getParameter(Params.DOCID);
            String pageId = request.getParameter(Params.PAGEID);
            if ( docId != null && pageId != null )
            {
                this.docid = Utils.ensureSlash(docId)+pageId;
                Picture p = null;
                try
                {
                    p = PictureRegistry.get(docid);
                }
                catch ( ImageException pe )
                {
                    throw new TiltException("Please do a POST first");
                }
                String geoJson = p.getGeoJson();
                if ( geoJson != null )
                {
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().println(geoJson);
                }
                else
                {
                    response.getOutputStream().println("<p>docid "+
                        docid+" not found</p>");
                }
            }
            else
                throw new Exception("Need a docid param: an image url!");
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
}
