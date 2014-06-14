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
import java.util.List;

import tilt.exception.*;
import tilt.image.*;
import tilt.Utils;
import tilt.Params;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.*;
/**
 * Handle a POST request
 * @author desmond
 */
public class TiltPostHandler extends TiltHandler
{
    File geoJSON;
    public TiltPostHandler()
    {
        encoding = "UTF-8";
    }
    /**
     * Parse the import params from the request
     * @param request the http request
     */
    private void parseImportParams( HttpServletRequest request ) 
        throws TiltException
    {
        try
        {
            FileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            // Parse the request
            List items = upload.parseRequest( request );
            for ( int i=0;i<items.size();i++ )
            {
                FileItem item = (FileItem) items.get( i );
                if ( item.isFormField() )
                {
                    String fieldName = item.getFieldName();
                    if ( fieldName != null )
                    {
                        if ( fieldName.equals(Params.GEOJSON) )
                        {
                            geoJSON = new File( fieldName, item.getString() );
                        }
                        else if ( fieldName.equals(Params.ENCODING) )
                        {
                            encoding = item.getString();
                        }
                    }
                }
                else if ( item.getName().length()>0 )
                {
                    try
                    {
                        // assuming that the contents are text
                        // item.getName retrieves the ORIGINAL file name
                        String type = item.getContentType();
                        if ( type != null && type.startsWith("application/") )
                        {
                            byte[] rawData = item.get();
                            //System.out.println(encoding);
                            geoJSON = new File( item.getName(), 
                                new String(rawData, encoding) );
                        }
                    }
                    catch ( Exception e )
                    {
                        throw new TiltException( e );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            if (ServletFileUpload.isMultipartContent(request) )
            {
                PictureRegistry.prune();
                parseImportParams( request );
                if ( geoJSON != null )
                {
                    String json = geoJSON.toString();
                    Object obj=JSONValue.parse(json);
                    response.setContentType("text/html;charset=UTF-8");
                    if ( obj instanceof JSONObject )
                    {
                        JSONObject g = (JSONObject)obj;
                        JSONObject props = (JSONObject)g.get("properties");
                        JSONObject geometry = (JSONObject)g.get("geometry");
                        if ( geometry != null && geometry.get("coordinates") 
                            instanceof JSONArray )
                        {
                            JSONArray cc = (JSONArray)geometry.get("coordinates");
                            response.getWriter().print("Coords:");
                            Picture p = new Picture( (String)props.get("url"), cc );
                        }
                        else
                            response.getWriter().println("missing coordinates");
                        StringBuilder sb = new StringBuilder();
                        sb.append("<!doctype html><html>");
                        sb.append("<head></head><body>");
                        sb.append("<img src=\"");
                        sb.append("http://");
                        sb.append(request.getServerName());
                        if ( request.getServerPort()!= 80 )
                        {
                            sb.append(":");
                            sb.append(request.getServerPort());
                        }
                        sb.append(request.getRequestURI());
                        sb.append("?");
                        sb.append(Params.DOCID);
                        sb.append("=");
                        sb.append(Utils.escape((String)props.get("url")));
                        sb.append("\"></body></</html>");
                        response.getWriter().println(sb.toString());
                    }
                    else
                        response.getWriter().println("<p>JSON file not geoJSON</p>");
                }
                else
                    response.getWriter().println("POST");
            } 
        }
        catch ( Exception e )
        {
            throw new TiltException( e );
        }
    }
}
