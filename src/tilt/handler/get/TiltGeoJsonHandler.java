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
 *  (c) copyright Desmond Schmidt 2015
 */

package tilt.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.exception.TiltException;
import calliope.core.database.*;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import java.io.InputStream;
import tilt.constants.Params;
import tilt.handler.TiltGetHandler;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import tilt.Utils;
import java.net.URL;
import java.net.URLConnection;

/**
 * Get the GeoJson for an pageid, plus pageid
 * @author desmond
 */
public class TiltGeoJsonHandler extends TiltGetHandler
{
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws TiltException 
    {
        try 
        {
            String docid = request.getParameter(Params.DOCID);
            String pageid = request.getParameter(Params.PAGEID);
            if ( docid != null && pageid !=null )
            {
                // first see if it is in the database
                Connection conn = Connector.getConnection();
                String doc = conn.getFromDb(Database.TILT, 
                    Utils.ensureSlash(docid)+pageid);
                if ( doc != null )
                {
                    JSONObject jobj = (JSONObject)JSONValue.parse(doc);
                    jobj.remove(JSONKeys._ID);
                    jobj.remove(JSONKeys.DOCID);
                    response.setContentType("application/json");
                    response.getWriter().print( jobj.toJSONString() );
                }
                else // not already there: create an empty GeoJson object
                {
                    JSONObject empty = new JSONObject();
                    empty.put(JSONKeys.TYPE, "Feature" );
                    JSONObject geometry = new JSONObject();
                    geometry.put(JSONKeys.TYPE, "Polygon");
                    JSONArray coordinates = new JSONArray();
                    JSONArray topLeft = new JSONArray();
                    topLeft.add(0.0);
                    topLeft.add(0.0);
                    JSONArray topRight = new JSONArray();
                    topRight.add(100.0);
                    topRight.add(0.0);
                    JSONArray botLeft= new JSONArray();
                    botLeft.add(100.0);
                    botLeft.add(100.0);
                    JSONArray botRight= new JSONArray();
                    botRight.add(0.0);
                    botRight.add(100.0);
                    coordinates.add( topLeft);
                    coordinates.add(topRight);
                    coordinates.add(botLeft);
                    coordinates.add(botRight);
                    geometry.put("coordinates",coordinates);
                    empty.put("geometry",geometry);
                    JSONObject properties = new JSONObject();
                    properties.put( JSONKeys.DOCID, docid );
                    empty.put("properties",properties );
                    response.setContentType("application/json");
                    response.getWriter().print( empty.toJSONString() );
                }   
            }
            else
                throw new Exception("Missing docid or pageid");
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }
}
