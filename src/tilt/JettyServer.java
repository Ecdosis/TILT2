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

import calliope.core.database.Connector;
import calliope.core.database.Repository;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import tilt.constants.Service;
import tilt.handler.*;
import tilt.exception.*;

/**
 * This launches the Jetty service
 * @author desmond
 */
public class JettyServer extends AbstractHandler
{
    /**
     * Main entry point
     * @param target the URN part of the URI
     * @param baseRequest 
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException 
     */
    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setStatus(HttpServletResponse.SC_OK);
        String method = request.getMethod();
        baseRequest.setHandled( true );
        try
        {
            String service = Utils.first(target);
            if ( service.equals(Service.TILT) )
            {
                String urn = Utils.pop(target);
                if ( method.equals("GET") )
                    new TiltGetHandler().handle( request, response, urn );
                else if ( method.equals("PUT") )
                    new TiltPutHandler().handle( request, response, urn );
                else if ( method.equals("DELETE") )
                    new TiltDeleteHandler().handle( request, response, urn );
                else if ( method.equals("POST") )
                    new TiltPostHandler().handle( request, response, urn );
                else
                    throw new TiltException("Unknown http method "+method);
            }
            else
                throw new TiltException("Unknown service"+service);
        }
        catch ( TiltException te )
        {
            StringBuilder sb = new StringBuilder();
            sb.append("<p>");
            sb.append(te.getMessage());
            sb.append("</p>");
            response.getOutputStream().println(sb.toString());
            te.printStackTrace(System.out);
        }
    }
    /**
     * Read commandline arguments for launch
     * @param args options on the commandline
     * @return true if they checked out
     */
    static boolean readArgs(String[] args)
    {
        boolean sane = true;
        TiltWebApp.user ="admin";
        TiltWebApp.password = "jabberw0cky";
        TiltWebApp.dbPort = 27017;
        TiltWebApp.repository = Repository.MONGO;
        TiltWebApp.wsPort = 8082;
        TiltWebApp.host = "localhost";
        TiltWebApp.database = "calliope";
        try
        {
            for ( int i=0;i<args.length;i++ )
            {
                if ( args[i].charAt(0)=='-' && args[i].length()==2 )
                {
                    if ( args.length>i+1 )
                    {
                        if ( args[i].charAt(1) == 'h' )
                            TiltWebApp.host = args[i+1];
                        else if ( args[i].charAt(1) == 'w' )
                            TiltWebApp.wsPort = Integer.parseInt(args[i+1]);
                        else if ( args[i].charAt(1) == 'u' )
                            TiltWebApp.user = args[i+1];
                        else if ( args[i].charAt(1) == 'p' )
                            TiltWebApp.password = args[i+1];
                        else if ( args[i].charAt(1) == 'b' )
                            TiltWebApp.dbPort = Integer.parseInt(args[i+1]);
                        else if ( args[i].charAt(1) == 'd' )
                            TiltWebApp.database = args[i+1];
                        else if ( args[i].charAt(1) == 'r' )
                            TiltWebApp.repository = 
                                Repository.valueOf(args[i+1].toUpperCase());
                        else
                            sane = false;
                    } 
                    else
                        sane = false;
                }
                if ( !sane )
                    break;
            }
            if ( sane )
            {
                Connector.init( TiltWebApp.repository, TiltWebApp.user, 
                    TiltWebApp.password, TiltWebApp.host, TiltWebApp.database, 
                    8083, TiltWebApp.wsPort, "/var/www" );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            sane = false;
        }
        return sane;
    }
    /**
     * Launch the AeseServer
     * @throws Exception 
     */
    private static void launchServer() throws Exception
    {
        JettyServerThread p = new JettyServerThread();
        p.start();
    }
    /**
     * Tell user how to invoke it on commandline
     */
    private static void usage()
    {
        System.out.println( "java -jar tilt2.jar [-h host] [-d db-port] " );
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        try
        {
            if ( readArgs(args) )
                launchServer();
            else
                usage();
        }
        catch ( Exception e )
        {
            System.out.println(e.getMessage());
        }
    }
}
