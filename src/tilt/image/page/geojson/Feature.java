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
package tilt.image.page.geojson;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
/**
 * An abstract feature in a GeoJson file
 * @author desmond
 */
public abstract class Feature 
{
    HashMap<String,Object> props;
    public Feature()
    {
        props = new HashMap<>();
    }
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"Feature\",\n");
        sb.append(getGeometry());
        if ( props.size()> 0 )
            sb.append("\t\t\t,\n");
        sb.append(getProps());
        sb.append("\t\t\t},\n");
        return sb.toString();
    }
    protected abstract String getGeometry();
    protected String getProps()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t\t\"properties\": {\n");
        if ( props.size() > 0 )
        {
            Set<String> keys = props.keySet();
            Iterator<String> iter = keys.iterator();
            while ( iter.hasNext() )
            {
                String key = iter.next();
                sb.append("\t\t\t\t");
                sb.append("\"");
                sb.append(key);
                sb.append("\": ");
                Object value = props.get(key);
                if ( value instanceof String )
                    sb.append("\"");
                sb.append(value);
                if ( value instanceof String )
                    sb.append("\"");
                if ( iter.hasNext() )
                    sb.append(",");
                sb.append("\n");
            }
        }
        sb.append("\t\t\t}\n");
        return sb.toString();
    }
    public void setProperty( String key, String value )
    {
        props.put( key, value );
    }
}
