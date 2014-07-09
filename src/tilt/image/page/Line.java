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
import java.awt.Polygon;
import java.awt.Color;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.HashSet;
import tilt.Utils;
/**
 * Represent a discovered line in an image
 * @author desmond
 */
public class Line implements Comparable<Line>
{
    static double DEFAULT_WORD_GAP = 24.0;
    boolean open;
    /** array of actual image coordinates to draw the line between */
    ArrayList<Point> points;
    /** array of shapes representing words */
    ArrayList<Polygon> shapes;
    /** scaled vertical endpoint */
    int end;
    /** shapes shared with some other lines */
    HashMap<Line,ArrayList<Polygon>> shared;
    /** total number of shapes, shared and non-shared */
    int total;
    /** median y-value */
    int medianY;
    int averageY;
    static float SHARED_RATIO= 0.75f;
    public Line()
    {
        points = new ArrayList<>();
        shapes = new ArrayList<>();
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
            int top = rightMost.y;
            int bottom = top+vScale;
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
            list = new ArrayList<Shape>();
            list.add( s );
            shared.put( other, list );
        }
        total++;
    }
    /** 
     * Do almost all the shapes of this line belong elsewhere?
     * @return true if these shapes are made for walkin'
     */
    boolean wantsMerge()
    {
        float proportion = Math.round(SHARED_RATIO*total);
        Set<Line> keys = shared.keySet();
        Iterator<Line> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            Line l = iter.next();
            ArrayList<Polygon> list = shared.get( l );
            if ( list.size() >= proportion )
                return true;
        }
        return false;
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
     * @paramn the number of the line
     */
    public void print( Graphics g, WritableRaster wr, int n )
    {
        Color tRed = new Color(255, 0,0, 128 );
        Color tBlue = new Color(0, 255,0, 128 );
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
            if ( i%2==1)
                g.setColor( tRed );
            else
                g.setColor( tBlue );
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
            ArrayList<Integer> yValues = new ArrayList<Integer>();
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
        if ( averageY == 0 )
        {
            int total = 0;
            for ( int i=0;i<points.size();i++ )
                total += points.get(i).y;
            averageY = total/points.size();
        }
        return averageY;
    }
    /**
     * Work out what the median gap between words is
     * @param vGap the median gap between baselines
     * @param wr the raster of image black and white pixels
     * @return an int
     */
    public int getMedianHGap( int vGap, WritableRaster wr )
    {
        HashSet<Integer> gaps = new HashSet<Integer>();
        int[] iArray = new int[vGap];
        int hGap = 0;
        for ( int i=0;i<points.size()-1;i++ )
        {
            Point p1 = points.get( i );
            Point p2 = points.get( i+1 );
            int y = (p1.y+p2.y)/2;
            y = (y-(vGap/2)<0)?0:y-(vGap/2);
            for ( int x=p1.x;x<p2.x;x++ )
            {
                int colTotal = 0;
                wr.getPixels(x,y,1,vGap,iArray);
                for ( int j=0;j<vGap;j++ )
                    if ( iArray[j]== 0 )
                        colTotal++;
                if ( colTotal == 0 )
                    hGap++;
                else if ( hGap > 0 )
                {
                    gaps.add( new Integer(hGap) );
                    hGap = 0;
                }
            }
        }
        if ( gaps.size()> 0 )
        {
            Integer[] array = new Integer[gaps.size()];
            gaps.toArray(array);
            Arrays.sort(array);
            return array[array.length/2];
        }
        else
            return (int)DEFAULT_WORD_GAP;
    }
    /**
     * Merge polygons that are close together
     * @param scale proportion to scale inter-blob spacing
     * @param debugif true print out diagnostic information
     */
    public void mergeWords( double scale, boolean debug )
    {
        Polygon prev = null;
        HashSet<Integer> gaps = new HashSet<>();
        ArrayList<Polygon> newShapes = new ArrayList<>();
        double averageGap;
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon pg = shapes.get(i);
            if ( prev != null )
            {
                int gap = Utils.distanceBetween( prev, pg );
                if ( debug )
                    System.out.print(" width="+pg.getBounds().width+" gap="+gap);
                gaps.add( gap );
                prev = pg;
            }
            else
                prev = pg;
        }
        System.out.println("");
        // compute average gap
        double totalGaps = 0.0f;
        Integer[] gapArray = new Integer[gaps.size()];
        gaps.toArray( gapArray );
        for ( int i=0;i<gapArray.length;i++ )
        {
            int gap = gapArray[i].intValue();
            totalGaps += gap;
        }
        if ( gaps.size()>0 )
            averageGap = totalGaps/gaps.size();
        else
            averageGap = DEFAULT_WORD_GAP;
        averageGap *= scale;
        prev = null;
        for ( int i=0;i<shapes.size();i++ )
        {
            Polygon pg = shapes.get(i);
            if ( prev != null )
            {
                // gaps will have changed due to merging
                int current = Utils.distanceBetween( prev, pg );
                if ( current <= averageGap  )
                {
                    prev = Utils.mergePolygons(prev,pg);
                }
                else
                {
                    newShapes.add( prev );
                    prev = pg;
                }
            }
            else
                prev = pg;
        }
        if ( prev != null )
            newShapes.add( prev );
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
}
