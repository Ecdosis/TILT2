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
import tilt.exception.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import tilt.Utils;
import tilt.handler.post.TiltTestHandler;
import tilt.constants.Service;
import tilt.handler.post.TiltRecogniseHandler;

/**
 * Handle a PUT request (used for update)
 * @author desmond
 */
public class TiltPostHandler extends TiltHandler
{
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
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws TiltException
    {
        try {
            String service = Utils.first(urn);
            if ( service.equals(Service.TEST) ) {
                new TiltTestHandler().handle(request,response,Utils.pop(urn));
            }
            else if ( service.equals(Service.RECOGNISE) )
            {
                new TiltRecogniseHandler().handle(request, response, Utils.pop(urn));
            }
        }
        catch ( Exception e )
        {
        throw new TiltException(e);
        }
    }
}
