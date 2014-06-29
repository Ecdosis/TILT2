/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tilt.image.page;
import java.util.ArrayList;
import java.awt.Point;
import java.awt.Graphics;
import java.awt.image.WritableRaster;
/**
 * Represent a discovered line in an image
 * @author desmond
 */
public class Line 
{
    boolean open;
    /** array of actual image coordinates to draw the line between */
    ArrayList<Point> points;
    /** scaled vertical endpoint */
    int end;
    public Line()
    {
        points = new ArrayList<Point>();
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
}
