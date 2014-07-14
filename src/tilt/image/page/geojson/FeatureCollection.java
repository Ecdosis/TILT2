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
import java.util.ArrayList;
/**
 * The outermost object in a GeoJson file
 * @author desmond
 */
public class FeatureCollection 
{
    ArrayList<Feature> features;
    FeatureCollection()
    {
        features = new ArrayList<>();
    }
    /**
     * Convert the whole collection to a string
     * @return a GeoJson document
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"type\": \"FeatureCollection\",\n");
        if ( features.size() > 0 )
        {
            sb.append("\t\"features\": [\n");
            for ( int i=0;i<features.size();i++ )
            {
                sb.append("\t\t");
                sb.append(features.get(i).toString());
                if ( i < features.size()-1 )
                    sb.append(",");
                sb.append("\n");
            }
            sb.append("\t]\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
