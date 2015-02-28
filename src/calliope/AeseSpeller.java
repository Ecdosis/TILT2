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

/**
 * This package requires that the server installs aspell, dictionaries
 * and the aesespeller library. It has to be called calliope or we need 
 * to rebuild the supporting C library and rerun javah.
 */
package calliope;

/**
 * Look words up in the aspell dictionary
 * @author desmond
 */
public class AeseSpeller 
{
    String lang;
    static 
    {  
        System.loadLibrary("AeseSpeller");
        System.loadLibrary("aspell");
    }
    public AeseSpeller( String lang ) throws Exception
    {
        this.lang = lang;
        if ( !initialise(lang) )
            throw new Exception("failed to initialise "+lang );
    }
    /** must call this to initialise at least one language */
    native boolean initialise( String lang );
    /** must cleanup or waste memory */
    public native void cleanup();
    /** tells you if word is in that dictionary */
    public native boolean hasWord( String word, String lang );
    /** lists valid language codes */
    public native String[] listDicts();
    /**
     * For testing
     * @param args 
     */
    public static void main( String[] args )
    {
        try
        {
            AeseSpeller as = new AeseSpeller("en_GB");
            if ( as.hasWord( "housing", "en_GB") )
                System.out.println("Dictionary (en_GB) has housing");
            if ( as.hasWord( "pratiche", "it") )
                System.out.println("Dictionary (it) has practiche");
            if ( as.hasWord( "progetto", "it") )
                System.out.println("Dictionary (it) has progetto");
            if ( as.hasWord( "automobile", "en_GB") )
                System.out.println("Dictionary (en_GB) has automobile");
            String[] dicts = as.listDicts();
            for ( int i=0;i<dicts.length;i++ )
            {
                System.out.println(dicts[i]);
            }
            if ( dicts.length==0 )
                System.out.println("no dicts!");
            as.cleanup();
        }
        catch ( Exception e )
        {
            System.out.println(e.getMessage());
        }
    }
}
