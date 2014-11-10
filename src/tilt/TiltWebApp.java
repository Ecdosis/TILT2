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

package tilt;

import calliope.core.exception.CalliopeExceptionMessage;
import calliope.core.exception.CalliopeException;
import tilt.handler.*;
import tilt.exception.TiltException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author desmond
 */
public class TiltWebApp extends HttpServlet
{
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, java.io.IOException
    {
        try
        {
            String method = req.getMethod();
            String target = req.getRequestURI();
            target = Utils.pop( target );
            TiltHandler handler;
            if ( method.equals("GET") )
                handler = new TiltGetHandler();
            else if ( method.equals("PUT") )
                handler = new TiltPutHandler();
            else if ( method.equals("DELETE") )
                handler = new TiltDeleteHandler();
            else if ( method.equals("POST") )
                handler = new TiltPostHandler();
            else
                throw new TiltException("Unknown http method "+method);
            resp.setStatus(HttpServletResponse.SC_OK);
            handler.handle( req, resp, target );
        }
        catch ( Exception e )
        {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CalliopeException he = new CalliopeException( e );
            resp.setContentType("text/html");
            try 
            {
                resp.getWriter().println(
                    new CalliopeExceptionMessage(he).toString() );
            }
            catch ( Exception e2 )
            {
                e.printStackTrace( System.out );
            }
        }
    }
}