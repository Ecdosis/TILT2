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
    ArrayList<Polygon> polygons;
    /** word to associate with the merged shape */
    Word word;
    /** line object where the shapes are stored */
    Line line;
    /**
     * Store merges between shapes in <em>one</em> line
     * @param line the line to update
     * @param shapeOffset offset into page-level shapes array for line start
     * @param shapes indices of the shapes
     * @param word the word the merged shape should be associated with
     */
    Merge( Line line, int shapeOffset, int[] shapes, Word word )
    {
        // convert from indices to absolute shapes
        this.polygons = new ArrayList<>();
        for ( int shape : shapes )
            this.polygons.add( line.getShape(shape-shapeOffset) );
        this.word = word;
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
        for ( Polygon shape : polygons )
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
        for ( Polygon shape : polygons )
            delenda.add( shape );
        for ( Polygon s : delenda )
            line.removeShape( s );
        if ( first != -1 )
           line.addShape( first, poly, word );
       else
           throw new ImageException( "Merge: couldn't find shape");
    }
}
