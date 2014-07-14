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
import java.awt.Point;
/**
 * A scaled polygon within a page enclosing a word
 * @author desmond
 */
public class Polygon extends Feature
{
    ArrayList<Point> points;
    int imageWidth;
    int imageHeight;
    /**
     * Create a GeoJson Polygon
     * @param points an array of point form an awt Ppolygon
     * @param imageWidth the full image width
     * @param imageHeight the full image height
     */
    Polygon( ArrayList<Point> points, int imageWidth, int imageHeight )
    {
        super();
        this.points = points;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }
    /**
     * Get the geometry object for a GeoJson Feature
     * @return a JSON object string
     */
    protected String getGeometry()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\t\t\t\"geometry\": {\n");
        sb.append("\t\t\t\t\"type\": \"Polygon\",\n");
        sb.append("\t\t\t\t\"coordinates\": [\n");
        for ( int i=0;i<points.size();i++ )
        {
            Point p = points.get(i);
            sb.append("[ ");
            sb.append(getScaledX(p));
            sb.append(", ");
            sb.append(getScaledY(p));
            sb.append("], ");
        }
        sb.append("\t\t\t\t]\n");
        sb.append("\t\t\t}\n");
        return sb.toString();
    }
    /**
     * Get the X-coordinate in the image expressed as a fraction of image width
     * @param p the awt point
     * @return a fraction of the image width representing the point
     */
    private float getScaledX( Point p )
    {
        return (float)p.x/(float)imageWidth;
    }
    /**
     * Get the Y-coordinate in the image expressed as a fraction of image height
     * @param p the awt point
     * @return a fraction of the image height representing the point
     */
    private float getScaledY( Point p )
    {
        return (float)p.y/(float)imageHeight;
    }
}
