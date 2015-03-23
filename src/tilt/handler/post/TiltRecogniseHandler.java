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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import tilt.constants.Params;
import tilt.exception.TiltException;
import tilt.image.Picture;
import tilt.image.PictureRegistry;
import tilt.Utils;
import tilt.constants.ImageType;
import tilt.handler.TiltPostHandler;
import java.net.InetAddress;
import calliope.core.database.*;
import calliope.core.constants.Database;

/**
 * Handle an Ajax request for geoJson data about a picture
 * @author desmond
 */
public class TiltRecogniseHandler extends TiltPostHandler
{
    /** the image URL */
    String docid;
    /** the image page id */
    String pageid;
    /** the GeoJson we extract from the picture */
    String geoJson;
    ImageType picType;
    TextIndex text;
    
    /**
     * Handle a request for geoJEON text-t-image links from editor
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
            this.docid = request.getParameter(Params.DOCID);
            this.pageid = request.getParameter(Params.PAGEID);
            this.geoJson = request.getParameter(Params.GEOJSON);
            String textParam = request.getParameter( Params.TEXT );
            if ( docid != null && pageid != null && geoJson != null && textParam != null )
            {
                String url = Utils.getUrl(request.getServerName(),docid,pageid);
                text = new TextIndex( textParam, "en_GB" );
                Picture p = PictureRegistry.get(url);
                if ( p == null )
                {
                    Object obj = JSONValue.parse(geoJson);
                    if ( obj instanceof JSONObject )
                    {
                        JSONObject g = (JSONObject)obj;
                        Options opts = new Options((JSONObject)g.get("properties"));
                        JSONObject geometry = (JSONObject)g.get("geometry");
                        if ( geometry != null && geometry.get("coordinates") 
                            instanceof JSONArray )
                        {
                            InetAddress poster = getIPAddress(request);
                            JSONArray cc = (JSONArray)geometry.get("coordinates");
                            opts.setCoords(cc);
                            p = new Picture( opts, url, text, poster);
                            PictureRegistry.update( url, p );
                            // it will be identified later by its url during GET
                        }
                        else
                            throw new Exception("Invalid geoJSON");
                    }
                    else
                        throw new Exception("Invalid geoJSON");
                }
                picType = ImageType.original;
                response.setContentType("text/plain");
                response.setCharacterEncoding("UTF-8");
                float progress = 0.0f;
                do
                {
                    switch ( picType )
                    {
                        case original:
                            p.convertToGreyscale();
                            picType = ImageType.greyscale;
                            progress = 12.5f;
                            break;
                        case greyscale:
                            p.convertToTwoTone();
                            picType = ImageType.twotone;
                            progress = 25.0f;
                            break;
                        case twotone:
                            p.convertToCleaned();
                            picType = ImageType.cleaned;
                            progress = 37.5f;
                            break;
                        case cleaned:
                            p.convertToBlurred();
                            picType = ImageType.blurred;
                            progress = 50.0f;
                            break;
                        case blurred:
                            p.convertToBaselines();
                            picType = ImageType.baselines;
                            progress = 62.5f;
                            break;
                        case baselines:
                            p.convertToWords();
                            picType = ImageType.words;
                            progress = 75.0f;
                            break;
                        case reduced:
                            p.convertToReduced();
                            picType = ImageType.reduced;
                            progress = 87.5f;
                            break;
                        case words:
                            p.convertToLinks();
                            picType = ImageType.link;
                            progress = 100.0f;
                            break;
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append( Math.round(progress) );
                    sb.append( " " );
                    sb.append( picType.toString() );
                    response.getWriter().println(sb.toString());
                    response.getWriter().flush();
                }
                while ( picType != ImageType.link );
                Connection conn = Connector.getConnection();
                geoJson = p.getGeoJson();
                conn.putToDb( Database.TILT, 
                    Utils.ensureSlash(docid)+pageid, geoJson );
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
