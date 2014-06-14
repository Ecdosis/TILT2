/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import tilt.handler.*;
import tilt.exception.*;

/**
 * TILT2 is a Java service that creates word-shapes of page images
 * @author desmond
 */
public class JettyServer extends AbstractHandler
{
    static String host;
    static int wsPort;
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
            if ( method.equals("GET") )
                new TiltGetHandler().handle( request, response, target );
            else if ( method.equals("PUT") )
                new TiltPutHandler().handle( request, response, target );
            else if ( method.equals("DELETE") )
                new TiltDeleteHandler().handle( request, response, target );
            else if ( method.equals("POST") )
                new TiltPostHandler().handle( request, response, target );
            else
                throw new TiltException("Unknown http method "+method);
        }
        catch ( TiltException te )
        {
            System.out.println(te.getMessage());
            te.printStackTrace(System.out);
        }
    }
    static boolean readArgs(String[] args)
    {
        boolean sane = true;
        wsPort = 8080;
        host = "localhost";
        for ( int i=0;i<args.length;i++ )
        {
            if ( args[i].charAt(0)=='-' && args[i].length()==2 )
            {
                if ( args.length>i+1 )
                {
                    if ( args[i].charAt(1) == 'h' )
                        host = args[i+1];
                    else if ( args[i].charAt(1) == 'w' )
                        wsPort = Integer.parseInt(args[i+1]);
                    else
                        sane = false;
                } 
                else
                    sane = false;
            }
            if ( !sane )
                break;
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
