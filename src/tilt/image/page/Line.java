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
import java.util.ArrayList;
import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.awt.Graphics;
import java.awt.Shape;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import org.json.simple.*;
import java.awt.geom.Area;
import tilt.Utils;
import tilt.exception.AlignException;
import tilt.image.convexhull.Point2D;
/**
 * Represent a discovered line in an image
 * @author desmond
 */
public class Line implements Comparable<Line>
{
    boolean open;
    /** array of actual image coordinates to draw the line between */
    ArrayList<Point> points;
    /** array of shapes representing words */
    ArrayList<Polygon> shapes;
    /** scaled vertical endpoint */
    int end;
    /** shapes shared with some other lines */
    HashMap<Line,ArrayList<Polygon>> shared;
    /** Words in text alignment, one per shape */
    ArrayList<Word> words;
    /** total number of shapes, shared and non-shared */
    int total;
    /** median y-value */
    int medianY;
    int averageY;
    static float SHARED_RATIO= 0.75f;
    public static final int NO_OFFSET = -1;
    public Line()
    {
        points = new ArrayList<>();
        shapes = new ArrayList<>();
        words = new ArrayList<>();
        shared = new HashMap<>();
        open = true;
    }
    /**
     * Add a point to a line
     * @param loc the absolute position of the new point
     * @param end the new end in scaled pixels
     */
    void addPoint( Point loc, int end )
    {
        points.add( loc );
        this.end = end;
        this.open = true;
    }
    /**
     * Get the scaled endpoint of the line
     * @return an int
     */
    int getEnd()
    {
        return end;
    }
    /**
     * Update the line by adding anew point, update end
     * @param loc the new location of the line-end in real coordinates
     * @param end the unscaled vertical position
     */
    void update( Point loc, int end )
    {
        points.add( loc );
        this.end = end;
    }
    /**
     * Get the points of the line
     * @return an ordinary array of Points
     */
    public Point[] getPoints()
    {
        Point[] line = new Point[points.size()];
        points.toArray(line);
        return line;
    }
    /**
     * Add gaps between shapes to the map
     * @param map a map of shape gaps to frequency of that gap
     */
    void addGaps( HashMap<Integer,Integer> map )
    {
        Polygon prev = null;
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon pg = shapes.get(i);
            if ( prev != null )
            {
                double gap = Utils.distanceBetween( prev, pg );
                Integer key = new Integer((int)Math.round(gap));
                int current = 0;
                if ( map.containsKey(key) )
                    current = map.get(key).intValue();
                current++;
                map.put( key, new Integer(current) );
                prev = pg;
            }
            else
                prev = pg;
        }
    }
    /**
     * Draw the line on the image for debugging
     * @param g the graphics environment to draw in
     */
    public void draw( Graphics g )
    {
        Point p1,p2=null;
        for ( int i=0;i<points.size();i++ )
        {
            p1 = p2;
            p2 = points.get(i);
            if ( p1 != null )
                g.drawLine(p1.x,p1.y,p2.x,p2.y);
        }
    }
    /**
     * This line is not matched in the next column. Terminate it!
     * @param hScale width of a rectangle
     */
    public void close( int hScale )
    {
        if ( points.size()>0 )
        {
            Point p = points.get(points.size()-1);
            points.add( new Point(p.x+hScale,p.y) );
        }
        this.open = false;
    }
    /**
     * Move the leftmost x position up to the first pixel that is black
     * @param wr the raster of the image
     * @param hScale the width of a rectangle or slice
     * @param vScale the height of a rectangle or slice
     * @param black value of a black pixel
     */
    public void refineLeft( WritableRaster wr, int hScale, int vScale, int black )
    {
        if ( points.size()>0 )
        {
            int[] iArray = new int[1];
            Point leftMost= points.get(0);
            int top = leftMost.y;
            int bottom = top+vScale;
            int right = leftMost.x+hScale;
            int left = (leftMost.x-hScale > 0)?leftMost.x-hScale:0;
            int nPixels = 0;
            for ( int x=left;x<right;x++ )
            {
                for ( int y=top;y<bottom;y++ )
                {
                    wr.getPixel( x,y, iArray );
                    if ( iArray[0] == black )
                    {
                        nPixels++;
                        if ( nPixels == 2 )
                        {
                            leftMost.x = x;
                            break;
                        }
                    }
                }
                if ( nPixels == 2 )
                    break;
            }
        }
    }
    /**
     * Move the rightmost x position inwards to the first pixel that is black
     * @param wr the raster of the image
     * @param hScale the width of a rectangle or slice
     * @param vScale the height of a rectangle or slice
     * @param black value of a black pixel
     */
    public void refineRight( WritableRaster wr, int hScale, int vScale, int black )
    {
        if ( points.size()>0 )
        {
            int[] iArray = new int[1];
            Point rightMost= points.get(points.size()-1);
            int top = rightMost.y-(vScale/2);
            int bottom = top+(vScale/2);
            int right = rightMost.x;
            int left = (rightMost.x-hScale>0)?rightMost.x-hScale:0;
            int nPixels = 0;
            for ( int x=right;x<=left;x-- )
            {
                int y;
                for ( y=top;y<bottom;y++ )
                {
                    wr.getPixel( x,y, iArray );
                    if ( iArray[0] == black )
                    {
                        nPixels++;
                        if ( nPixels == 2 )
                        {
                            rightMost.x = x;
                            break;
                        }
                    }
                }
                if ( nPixels == 2 )
                    break;
            }
        }
    }
    /**
     * Add a shape (polygon or rectangle) to the line. Keep it sorted.
     * @param s the shape to add
     */
    public void add( Polygon s )
    {
        int i;
        Rectangle sBounds = s.getBounds();
        for ( i=0;i<shapes.size();i++ )
        {
            Shape t = shapes.get(i);
            Rectangle bounds = t.getBounds();
            if ( bounds.x >= sBounds.x )
            {
                shapes.add( i, s );
                break;
            }
        }
        if ( i==shapes.size() )
            shapes.add( s );
        total++;
    }
    /**
     * Recall which lines shared shapes with us (for later merging)
     * @param other the other line
     * @param s the shape we share
     */
    public void addShared( Line other, Shape s )
    {
        ArrayList list = shared.get( other );
        if ( list != null )
        {
            list.add( s );
        }
        else
        {
            list = new ArrayList<>();
            list.add( s );
            shared.put( other, list );
        }
        total++;
    }
    /**
     * Get the centroids for the line
     * @return an array of centroids for all the polygons on the line
     */
    Point[] getCentroids()
    {
        Point[] centroids = new Point[shapes.size()];
        for ( int i=0;i<centroids.length;i++ )
            centroids[i] = shapes.get(i).getCentroid();
        return centroids;
    }
    /**
     * Merge this line with its most similar colleague
     * @param map update this shape to line map for moved shapes
     */
    void merge( HashMap<Polygon,Line> map )
    {
        float proportion = Math.round(SHARED_RATIO*total);
        Set<Line> keys = shared.keySet();
        Iterator<Line> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            Line l = iter.next();
            ArrayList<Polygon> list = shared.get( l );
            if ( list.size() >= proportion )
            {
                // move all OUR shapes over to the dominant line
                for ( int j=0;j<shapes.size();j++ )
                {
                    Polygon pg = shapes.get(j);
                    map.put( pg, l );
                    l.add( pg );
                }
                shapes.clear();
                break;
            }
        }
    }
    /**
     * Print the shapes onto the original image
     * @param g the graphics environment
     * @param wr the raster to write on
     * @param n the number of the line
     */
    public void print( Graphics g, WritableRaster wr, int n )
    {
        Color tColour;
        if ( n%3==0 )
            tColour = new Color( 255, 0, 0, 128 );
        else if ( n%3==1 )
            tColour = new Color( 0, 255,0,128);
        else
            tColour = new Color(0,0,255,128);
        Color old = g.getColor();
        if ( shapes.size()>0 )
        {
            Rectangle bounds = shapes.get(0).getBounds();
            g.setColor( Color.black );
            g.drawString( Integer.toString(n), bounds.x-20, bounds.y-10 );
        }
        for ( int i=0;i<shapes.size();i++ )
        {
            Shape s = shapes.get(i);
            g.setColor( tColour );
            if ( s instanceof Polygon )
                g.fillPolygon( (Polygon)s );
            else if ( s instanceof Rectangle )
            {
                Rectangle r = (Rectangle) s;
                g.fillRect( r.x, r.y, r.width, r.height );
            }
        }
        g.setColor( old );
    }
    /**
     * Get the median Y value of the line
     * @return an int,being the median yValue of the points in the line
     */
    public int getMedianY()
    {
        if ( medianY == 0 )
        {
            ArrayList<Integer> yValues = new ArrayList<>();
            for ( int i=0;i<points.size();i++ )
            {
                yValues.add( new Integer(points.get(i).y) );
            }
            if ( yValues.size() > 0 )
            {
                Integer[] array = new Integer[yValues.size()];
                yValues.toArray( array );
                Arrays.sort( array );
                medianY = array[array.length/2];
            }
        }
        return medianY;
    }
    /**
     * Get the average Y value of the line
     * @return an int,being the average yValue of the points in the line
     */
    public int getAverageY()
    {
        int ltotal = 0;
        for ( int i=0;i<points.size();i++ )
            ltotal += points.get(i).y;
        averageY = ltotal/points.size();
        return averageY;
    }
    /**
     * Merge polygons that are close together
     * @param minWordGap average word gap on the page
     */
    public void mergeWords( int minWordGap )
    {
        ArrayList<Integer> positions = new ArrayList<>();
        Polygon prev = null;
        ArrayList<Polygon> newShapes = new ArrayList<>();
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon pg = shapes.get(i);
            if ( prev != null )
            {
                // gaps will change due to merging
                int current = (int)Math.round(Utils.distanceBetween(prev,pg) );
                if ( current >= minWordGap  )
                    positions.add( new Integer(i) );
            }
            prev = pg;
        }
        positions.add( shapes.size());
        // now actually merge the words
        for ( int last=0,i=0;i<positions.size();i++ )
        {
            int pos = positions.get(i).intValue();
            prev = null;
            for ( int j=last;j<pos;j++)
            {
                Polygon pg = shapes.get(j);
                if ( prev != null )
                    prev = Utils.mergePolygons(prev,pg);
                else
                    prev = pg;
            }
            if ( prev != null )
                newShapes.add( prev );
            last = pos;
        }
        this.shapes = newShapes;
    }
    /**
     * Compute the horizontal overlap between two lines
     * @param other the other line
     * @return the fraction of the two lines together which overlaps
     */
    float overlap( Line other )
    {
        if ( this.points.isEmpty() || other.points.isEmpty() )
            return 0.0f;
        else
        {
            int left1 = this.points.get(0).x;
            int left2= other.points.get(0).x;
            int right1 = this.points.get(points.size()-1).x;
            int right2 = other.points.get(other.points.size()-1).x;
            if ( right1 < left2 || right2 < left1 )
                return 0.0f;
            // we completely enclose other
            else if (left2 > left1 && right2 < right1)
                return (float)(right2-left2)/(float)(right1-left1);
            // other completely encloses us
            else if ( left1>left2&&right1<right2 )
                return (float)(right1-left1)/(float)(right2-left2);
            // we precede other but overlap
            else if ( right1 > left2 )
            {
                int overlap = right1-left2;
                int total = right2-left1;
                return (float)overlap/(float)total;
            }
            // other precedes us but overlaps
            else if ( right2 > left1  )
            {
                int overlap = right2-left1;
                int total = right1-left2;
                return (float)overlap/(float)total;
            }
            // shouldn't happen
            else
                return 0.0f;
        }
    }
    /**
     * A line is before another based on first point position in x direction.
     * @param other the other line
     * @return 1 if we are after other, 0 if at the same position else -1
     */
    @Override
    public int compareTo( Line other )
    {
        if ( this.points.isEmpty() )
        {
            if ( other.points.isEmpty() )
                return 0;
            else    // we are less than other
                return -1;
        }
        else if ( other.points.isEmpty() )
        {
            if ( this.points.isEmpty() )
                return 0;
            else    // other is less than us
                return 1;
        }
        else
        {
            return this.points.get(0).x-other.points.get(0).x;
        }
    }
    /**
     * Insert sort the points on the x-coordinate
     */
    void sortPoints()
    {
        for ( int i=0;i<points.size();i++ )
        {
            Point p = points.get(i);
            int j = i;
            while ( j > 0 && points.get(j-1).x > p.x )
            {
                points.set(j,points.get(j-1));
                j--;
            }
            points.set(j,p);
        }
    }
    /**
     * Get the overall bounding box of this line
     * @return a rectangle
     */
    public Rectangle getBounds()
    {
        Area region = new Area();
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon shape = shapes.get(i);
            region.add(new Area(shape) );
        }
        return region.getBounds();
    }
    /**
     * Convert the line to a GeoJSON object for export
     * @param pageWidth the width of the image in pixels
     * @param pageHeight the height of the image in pixels
     * @return a GeoJSON object for this line
     */
    public JSONObject toGeoJSON( int pageWidth, int pageHeight )
    {
        JSONObject collection = new JSONObject();
        collection.put("type","FeatureCollection");
        collection.put("name","Line");
        Rectangle bounds = getBounds();
        JSONArray bbox = new JSONArray();
        bbox.add(bounds.x);
        bbox.add(bounds.y);
        bbox.add(bounds.x+bounds.width);
        bbox.add(bounds.y+bounds.height);
        collection.put("bbox",bbox);
        JSONArray features = new JSONArray();
        for ( int i=0;i<shapes.size();i++ )
        {
            JSONObject feature = new JSONObject();
            feature.put("type","Feature");
            Point2D[] pts = Utils.polygonToPoints(shapes.get(i));
            JSONObject geometry = new JSONObject();
            JSONArray coordinates = new JSONArray();
            for ( int j=0;j<pts.length;j++ )
            {
                geometry.put("type","Polygon");
                JSONArray point = new JSONArray();
                point.add( pts[j].x()/(double)pageWidth );
                point.add( pts[j].y()/(double)pageHeight );
                coordinates.add( point );
            }
            if ( pts.length>0 && !pts[0].equals(pts[pts.length-1]) )
                coordinates.add( coordinates.get(0) );
            geometry.put( "coordinates", coordinates );
            feature.put( "geometry", geometry );
            // add feature properties like text-offset here
            if ( words.size() >i )
            {
                Word val = words.get( i );
                if ( val != null )
                {
                    JSONObject properties = new JSONObject();
                    properties.put( "offset", val.offset() );
                    if ( val.hyphenPos()>0 )
                        properties.put( "hyphen", val.hyphenPos());
                    feature.put( "properties", properties );
                }
            }
            features.add( feature );
        }
        collection.put( "features", features );
        return collection;
    }
    /**
     * Get the widths of the polygons on the line
     * @return an array of word-widths on the line
     */
    int[] getWidths()
    {
        int[] widths = new int[shapes.size()];
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon pg = shapes.get(i);
            Rectangle bounds = pg.getBounds();
            widths[i] = bounds.width;
        }
        return widths;
    }
    /**
     * Get the number of shapes on a line
     * @return an int
     */
    public int countShapes()
    {
        return shapes.size();
    }
    public int countPoints()
    {
        return this.points.size();
    }
    /**
     * Fill in the pixel widths of each shape in the line
     * @param widths the array to partly fill in
     * @param start the start index into widths
     * @return the number of entries filled in
     */
    int getShapeWidths( int[] widths, int start )
    {
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon pg = shapes.get(i);
            widths[start+i] = pg.getBounds().width;
        }
        return shapes.size();
    }
    /**
     * Does the index for this shape exist?
     * @param index the index to query
     * @return true if a shape at that index in shapes exists else false
     */
    boolean hasShape( int index )
    {
        return index < shapes.size();
    }
    /**
     * Set the word offset of the indexed shape
     * @param index the index of the shape in shapes
     * @param word the word to associate with the shape at this index
     */
    public void setShapeWord( int index, Word word ) throws AlignException
    {
        if ( index < words.size() )
            throw new AlignException("Add words in correct order");
        while ( index > words.size() )
            words.add( null );
        if ( index >= shapes.size() )
            throw new AlignException("More offsets than shapes");
        words.add( word );
    }
    /**
     * Split one shape into several at the correct places
     * @param index the index into shapes
     * @param offsets the word-offsets to set for each new shape
     * @param wr the raster of the cleaned image
     * @return the number of split shapes 
     */
    public int splitShape( int index, int[] offsets, WritableRaster wr)
    {
        return 0;
    }
    /**
     * Get a shape by its index
     * @param index the index
     * @return a Polygon
     */
    Polygon getShape( int index )
    {
        return shapes.get( index );
    }
    /**
     * Get the index of the specified shape
     * @param pg the shape to look for
     * @return its 0-based index on the line
     */
    int getShapeIndex( Polygon pg )
    {
        return shapes.indexOf( pg );
    }
    /**
     * Remove a shape from the line
     * @param pg the shape to remove
     */
    void removeShape( Polygon pg )
    {
        int index = shapes.indexOf(pg);
        if ( index < words.size() )
            words.remove( index );
        shapes.remove( index );
    }
    /**
     * Add a shape to the line
     * @param index the index at which to add (or ==shapes.size() to add)
     * @param pg the shape to add
     * @param word the word to associate with it (or null)
     */
    void addShape( int index, Polygon pg, Word word )
    {
        shapes.add( index, pg );
        if ( words.size() >= index )
            words.add( index, word );
    }
    /**
     * Reset the words and shapes arrays for a realignment
     */
    public void reset()
    {
        words.clear();
    }
    public void resetShapes()
    {
        shapes.clear();
    }
    /**
     * Remove some points from the line. Must be before we recognise words
     * @param removals the points to cull
     */
    public void removePoints( ArrayList<Point>removals )
    {
        for ( Point p: removals )
            points.remove( p );
    }
    public void mergeWith( Line other )
    {
        points.addAll(other.points);
        sortPoints();
    }       
}
