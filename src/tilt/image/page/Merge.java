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

package tilt.image.page;
import java.awt.Polygon;
import java.awt.Point;
import java.util.ArrayList;
import tilt.Utils;
import tilt.image.FastConvexHull;
import tilt.exception.ImageException;
/**
 * Store and merge two or more shapes
 * @author desmond
 */
public class Merge 
{
    /** store shapes explicitly not by index */
    ArrayList<Polygon> shapes;
    /** word-offset to be applied to merged shape */
    int wordOffset;
    /** line object where the shapes are stored */
    Line line;
    /**
     * Store merges between shapes in a line
     * @param line the line to update
     * @param shapes indices of the shapes
     * @param wordOffset the offset that the merged shape should have
     */
    Merge( Line line, int[] shapes, int wordOffset )
    {
        // convert from indices to absolute shapes
        this.shapes = new ArrayList<>();
        for ( int shape : shapes )
            this.shapes.add( line.getShape(shape) );
        this.wordOffset = wordOffset;
        this.line = line;
    }
    /**
     * Actually carry out the merging of several polygons into one
     * @throws ImageException 
     */
    void execute() throws ImageException
    {
        int first = -1;
        // gather points from all shapes
        ArrayList<Point> pts = new ArrayList<>();
        for ( Polygon shape : shapes )
        {
            if ( first == -1 )
                first = line.getShapeIndex(shape);
            ArrayList<Point> points = Utils.polygonToPoints( shape );
            pts.addAll( points );
        }
        // create merged polygon
        ArrayList<Point> pg = FastConvexHull.execute( pts );
        Polygon poly = new Polygon();
        for ( Point pt : pg )
            poly.addPoint( pt.x, pt.y );
        // update list
        ArrayList<Polygon> delenda = new ArrayList<>();
        for ( Polygon shape : shapes )
            delenda.add( shape );
        for ( Polygon s : delenda )
            line.removeShape( s );
        if ( first != -1 )
           line.addShape( first, poly );
       else
           throw new ImageException( "Merge: couldn't find shape");
    }
}
