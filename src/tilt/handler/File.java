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

/**
 * Represent a loaded file
 * @author desmond
 */
public class File 
{
    public String data;
    public String name;
    private char UTF8_BOM = 65279;
    public File( String name, String data )
    {
        this.name = name.replaceAll("#", "-");
        this.data = data;
        if ( data.length()>0&&data.charAt(0)==UTF8_BOM )
            this.data = data.substring(1);
    }
    /**
     * Reset the data content
     * @param data the new data
     */
    public void setData( String data )
    {
        this.data = data;
    }
    public boolean isJSON()
    {
        return this.name.endsWith(".json");
    }
    private boolean isBom( char token )
    {
        return token == UTF8_BOM;
    }
    /**
     * Get the name of the file minus extension
     * @return the simple file name
     */
    public String simpleName()
    {
        int dotPos = name.lastIndexOf(".");
        if ( dotPos != -1 )
            return name.substring( 0,dotPos );
        else
            return name;
    }
    @Override
    public String toString()
    {
        return data;
    }
}
