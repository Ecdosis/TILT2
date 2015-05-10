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
import java.net.InetAddress;
import tilt.exception.TiltException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import tilt.Utils;

/**
 * Abstract super-class for all handlers: PUT, POST, DELETE, GET
 * @author ddos
 */
abstract public class TiltHandler 
{
    protected String encoding;
    /**
     * Get the sender's IP-address (prevent DoS via too many uploads)
     * @param request raw request
     * @return the server'sIP as a string
     */
    protected InetAddress getIPAddress( HttpServletRequest request ) 
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
     * Compose a url to retrieve a page's text
     * @param request the http request object
     * @param docid the document the text comes from
     * @param pageid the pageid of the text desired
     * @return a url to retrieve the text
     */
    protected String composeTextUrl( HttpServletRequest request, String docid, 
        String pageid )
    {
        return "http://"+request.getServerName()+"/pages/text?docid="
            +docid+"&pageid="+pageid;
    }
    /**
     * Getthe crop rect for the given docid+pageid combination
     * @param server the server host name
     * @param docid its document identifier
     * @param pageid its page identifier
     * @return an array of 2-element double arrays
     * @throws TiltException 
     */
    protected Double[][] getCropRect( String server, String docid, 
        String pageid ) throws TiltException
    {
        try
        {
            String url = "http://"+server+"/pages/crop_rect?docid="
                +docid+"&pageid="+pageid;
            String jstr = Utils.getFromUrl( url );
            JSONArray jarr = (JSONArray)JSONValue.parse( jstr );
            if ( jarr.size()==4 )
            {
                Double[][] coords = new Double[4][2];
                for ( int i=0;i<4;i++ )
                {
                    JSONArray item = (JSONArray)jarr.get(i);
                    if ( item.size()==2 )
                    {
                        coords[i][0] = (Double)item.get(0);
                        coords[i][1] = (Double)item.get(1);
                    }
                    else
                        throw new Exception("point coordinates wrong size");
                }
                return coords;
            }
            else
                throw new Exception("Data returned not a set of coordinates");
        }
        catch ( Exception e )
        {
            throw new TiltException(e);
        }
    }

    public abstract void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException;
}
