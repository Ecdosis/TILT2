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
package tilt.handler.get;

import tilt.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.constants.Params;
import tilt.exception.TiltException;
import tilt.handler.TiltGetHandler;
import tilt.handler.post.Options;
import org.json.simple.*;
/**
 * Retrieve options for a given docid+pageid
 * @author desmond
 */
public class TiltOptionsHandler extends TiltGetHandler 
{
    String docid;
    String pageid;
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            docid = request.getParameter(Params.DOCID);
            if ( docid != null )
            {
                Connection conn = Connector.getConnection();
                String doc = conn.getFromDb(Database.OPTIONS,docid);
                if ( doc != null )
                {
                    JSONObject jobj = (JSONObject)JSONValue.parse(doc);
                    jobj.remove(JSONKeys._ID);
                    jobj.remove(JSONKeys.DOCID);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().print( jobj.toJSONString() );
                    return;
                }
            }
             // return default options
            JSONObject opts = Options.getDefaults();
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().print( opts.toJSONString() );
        }
        catch ( Exception e )
        {
        }
    }
}
