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
import java.util.List;

import tilt.exception.*;
import tilt.image.*;
import tilt.Utils;
import tilt.constants.Params;
import tilt.constants.ImageType;
import java.net.InetAddress;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.*;
import tilt.constants.Service;
/**
 * Handle a POST request
 * @author desmond
 */
public class TiltPostHandler extends TiltHandler
{
    String geoJSON;
    String text;
    ImageType picType;
    InetAddress poster;
    public TiltPostHandler()
    {
        encoding = "UTF-8";
        picType = ImageType.original;
        text = "";
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
                            geoJSON = item.getString();
                        }
                        else if ( fieldName.equals(Params.ENCODING) )
                        {
                            encoding = item.getString();
                        }
                        else if ( fieldName.equals(Params.PICTYPE) )
                        {
                            picType = ImageType.read(item.getString());
                        }
                        else if ( fieldName.equals(Params.TEXT) )
                        {
                            text = item.getString();
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
                            geoJSON = new String(rawData, encoding);
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
    /**
     * Get the sender's IP-address (prevent DoS via too many uploads)
     * @param request raw request
     * @return the server'sIP as a string
     */
    private InetAddress getIPAddress( HttpServletRequest request ) 
        throws Exception
    {
        String ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        InetAddress addr = InetAddress.getByName(ipAddress);
        return addr;
    }
    /**
     * Count words in a text
     * @param txt a string
     * @return the number of space-delimited character-runs in txt
     */
    private int countWords( String txt )
    {
        int nWords = 0;
        int state = 0;
        for ( int i=0;i<txt.length();i++ )
        {
            switch ( state )
            {
                case 0: // initial
                    if ( Character.isWhitespace(txt.charAt(i)) )
                        state = 1;
                    else
                        state = 2;
                    break;
                case 1: // matching spaces
                    if ( !Character.isWhitespace(txt.charAt(i)) )
                        state = 2;
                    break;
                case 2:// matching text
                    if ( Character.isWhitespace(txt.charAt(i)) )
                    {
                        state = 1;
                        nWords++;
                    }
                    break;
            }
        }
        // coda
        if ( state == 2 )
            nWords++;
        return nWords;
    }
    /**
     * Get the plain text content of some html
     */
    String plainText( String html )
    {
        StringTokenizer st = new StringTokenizer(html,"<> ",true);
        StringBuilder sb = new StringBuilder();
        int state = 0;
        while ( st.hasMoreElements() )
        {
            String token = st.nextToken();
            switch ( state )
            {
                case 0: // looking for start-tag
                    if (token.equals("<") )
                        state = 1;
                    break;
                case 1: // reading start tag
                    if ( token.equals(">") )
                        state = 2;
                    else if ( token.toLowerCase().equals("head") )
                        state = 3;
                    break;
                case 2: // reading text
                    if ( !token.equals("<") )
                        sb.append( token );
                    else
                        state = 1;
                    break;
                case 3:
                    if ( token.equals("<") )
                        state = 4;
                    break;
                case 4:
                    if ( token.toLowerCase().equals("/head") )
                        state = 5;
                    break;
                case 5:
                    if ( token.equals(">") )
                        state = 0;
                    break;
            }
        }
        return sb.toString();
    }
    /**
     * Create a HTML IMG element referring to the image we need
     * @param request the http request
     * @param json the GeoJSON text
     * @return a HTML IMG element
     * @throws TiltException 
     */
    private String composeResponse( HttpServletRequest request, String json, 
        ImageType picType ) throws TiltException
    {
        StringBuilder sb = new StringBuilder();
        Object obj=JSONValue.parse(json);
        if ( obj instanceof JSONObject )
        {
            long numWords = 250;
            JSONObject g = (JSONObject)obj;
            JSONObject props = (JSONObject)g.get("properties");
            JSONObject geometry = (JSONObject)g.get("geometry");
            if ( geometry != null && geometry.get("coordinates") 
                instanceof JSONArray )
            {
                JSONArray cc = (JSONArray)geometry.get("coordinates");
                numWords = countWords( plainText(text) );
                // create the picture and store it in the picture registry
                Picture p = new Picture( (String)props.get("url"), 
                    cc, (int)numWords, poster );
                // it will be identified later by its docid during GET
            }
            sb.append("<img width=\"500\" src=\"");
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
            sb.append("&pictype=");
            sb.append(picType.toString());
            sb.append("\">");
        }
        else
            sb.append("<p>JSON file not geoJSON</p>");
        return sb.toString();
    }
    /**
     * Handle a POST request
     * @param request the raw request
     * @param response the response we will write to
     * @param urn the rest of the URL after stripping off the context
     * @throws TiltException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try
        {
            String service = Utils.first(urn);
            if ( service.equals(Service.TILT.toString()) )
            {
                poster = getIPAddress(request);
                if (ServletFileUpload.isMultipartContent(request) )
                    parseImportParams( request );
                else
                {
                    picType = ImageType.read(request.getParameter(Params.PICTYPE));
                    geoJSON = request.getParameter(Params.GEOJSON );
                    text = request.getParameter(Params.TEXT );
                }
                if ( geoJSON != null )
                {
                    PictureRegistry.prune();
                    String resp = composeResponse( request, geoJSON, picType );
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().println(resp);
                }
                else
                    response.getWriter().println("POST");
            }
        }
        catch ( Exception e )
        {
            response.setContentType("text/plain;charset=UTF-8");
            try
            {
                response.getWriter().print("<p>");
                response.getWriter().print(e.getMessage());
                response.getWriter().println("</p>");
            }
            catch ( Exception e2 )
            {
                throw new TiltException(e2);
            }
        }
    }
}
