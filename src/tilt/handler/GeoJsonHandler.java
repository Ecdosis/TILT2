/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.constants.Params;
import tilt.exception.TiltException;
import tilt.image.Picture;
import tilt.image.PictureRegistry;

/**
 * Handle an Ajax request for geoJson data about a picture
 * @author desmond
 */
public class GeoJsonHandler extends TiltHandler
{
    String docid;
    String geoJson;
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                Picture p = PictureRegistry.get(docid);
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
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
}
