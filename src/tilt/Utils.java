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

/**
 * Some routines that need sharing by all
 * @author desmond
 */
public class Utils 
{
    /**
     * Convert a URL into a form suitable as a parameter
     * @param value the raw, unescapedURL
     * @return an escaped URL with / and  space escaped
     */
    public static String escape( String value )
    {
        StringBuilder sb = new StringBuilder();
        {
            for ( int i=0;i<value.length();i++ )
            if ( value.charAt(i) == ' ' )
                sb.append("%20");
            else if ( value.charAt(i) == '/' )
               sb.append("%2F");
            else
                sb.append( value.charAt(i) );
        }
        return sb.toString();
    }
    /**
     * Chop off the first component of a urn
     * @param urn the urn to chop
     * @return the first urn component
     */
    public static String first( String urn )
    { 
        int slashPos1 = -1;
        if ( urn.startsWith("/") )
            slashPos1 = urn.indexOf( "/" );
        int slashPos2 = urn.indexOf( "/", slashPos1+1 );
        if ( slashPos1 != -1 && slashPos2 != -1 )
            return urn.substring(slashPos1+1, slashPos2 );
        else if ( slashPos1 != -1 && slashPos2 == -1 )
            return urn.substring( slashPos1+1 );
        else if ( slashPos1 == -1 && slashPos2 != -1 )
            return urn.substring( 0,slashPos2 );
        else
            return urn;
    }
    /**
     * Extract the second component of a urn
     * @param urn the urn to extract from
     * @return the second urn component
     */
    public static String second( String urn )
    { 
        int start=-1,end=-1;
        for ( int state=0,i=0;i<urn.length();i++ )
        {
            char token = urn.charAt(i);
            switch ( state )
            {
                case 0:// always pass first char
                    state = 1;
                    break;
                case 1: 
                    if ( token == '/' )
                        state = 2;
                    break;
                case 2:
                    start=i;
                    if ( token == '/' )
                    {
                        state = -1;
                        end = i;
                    }
                    else
                        state = 3;
                    break;
                case 3:
                    if ( token == '/' )
                    {
                        end = i;
                        state = -1;
                    }
                    break;
            }
            if ( state == -1 )
                break;
        }
        if ( end == -1 )
            end = urn.length();
        if ( start == -1 )
            start = urn.length();
        return urn.substring( start, end );
    }
    /**
     * Pop off the frontmost part of the path
     * @param path the path to pop
     * @return the popped path
     */
    public static String pop( String path )
    {
        while ( path.length()>0 && path.startsWith("/") )
            path = path.substring(1);
        int pos = path.indexOf("/");
        if ( pos != -1 )
            path = path.substring( pos+1 );
        return path;
    }
    /**
     * Just escape quotes for a string to be jsonified
     * @param input the string to escape
     * @param replaceWith replace double quotes with this
     * @return 
     */
    public static String escapeQuotes( String input, String replaceWith )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<input.length();i++ )
        {
            char token = input.charAt(i);
            switch ( token )
            {
                case '"':
                    sb.append(replaceWith);
                    break;
                default:
                    sb.append(token);
                    break;
            }
        }
        return sb.toString();
    }
}
