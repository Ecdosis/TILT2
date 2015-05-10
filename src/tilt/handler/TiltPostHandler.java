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
import org.json.simple.JSONArray;
/**
 * Handle a PUT request (used for update)
 * @author desmond
 */
public class TiltPostHandler extends TiltHandler
{
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
    protected Double[][] coordsToArray( JSONArray cc ) 
        throws ArrayIndexOutOfBoundsException
    {
        Double[][] coords = new Double[4][2];
        if ( cc.size()!= 4 )
            throw new ArrayIndexOutOfBoundsException("coordinates must be 4 points");
        for ( int i=0;i<4;i++ )
        {
            JSONArray vector = (JSONArray)cc.get(i);
            if ( vector.size() != 2 )
                throw new ArrayIndexOutOfBoundsException("Point required");
            for ( int j=0;j<2;j++ )
            {
                coords[i][j] = (Double)vector.get(j);
            }
        }
        return coords;
    }
}
