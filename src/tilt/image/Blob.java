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
 *  (c) copyright Desmond Schmidt 2015
 */

package tilt.image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.net.InetAddress;
import javax.imageio.ImageIO;
import java.util.Stack;
import java.util.ArrayList;
import tilt.handler.post.TextIndex;
import tilt.image.convexhull.*;
import org.json.simple.*;
import tilt.image.geometry.Polygon;
import tilt.handler.post.Options;


/**
 * An amorphous region of connected black pixels in an image
 * @author desmond
 */
public class Blob 
{
    /** number of black pixels in the area */
    int numBlackPixels;
    /** number of white pixels in area */
    int numWhitePixels;
    /** accumulate black pixels here */
    WritableRaster dirt; 
    /** unless they are already in here */
    WritableRaster parent;
    /** localised grey average */
    WritableRaster blackLevelImage;
    /** limits of pixel area */
    int minX,maxX,minY,maxY;
    /** First black pixel*/
    Point firstBlackPixel;
    Stack<Point> stack;
    ArrayList<Point> hull;
    Options opts;
    public Blob( WritableRaster parent, Options opts, WritableRaster bli )
    {
        this.parent = parent;
        this.minY = this.minX = Integer.MAX_VALUE;
        this.stack = new Stack();
        this.opts = opts;
        this.blackLevelImage = bli;
    }
    /**
     * Actually commit the blob to the dirty raster
     * @param dirt the raster to store the pixels to be removed
     * @param wr the original image raster
     * @param loc the start-position of the blob
     */
    public void save( WritableRaster dirt, WritableRaster wr, Point loc )
    {
        this.dirt = dirt;
        // recompute the blob - a bit inefficient but storing it is far worse
        expandArea( wr, loc );
    }
    /**
     * How many black pixels we have
     * @return the sizeof the blob in overall black pixels
     */
    public int size()
    {
        return numBlackPixels;
    }
    /**
     * Top left coordinate of blob
     * @return a Point
     */
    Point topLeft()
    {
        return new Point( minX,minY );
    }
    /**
     * Bottom right coordinate of blob
     * @return a Point
     */
    Point botRight()
    {
        return new Point( maxX,maxY );
    }
    /**
     * Set a raster to white (now only called by Picture)
     * @param wr the raster to white out
     * @return the average number of black pixels per pixel
     */
    public static float setToWhite( WritableRaster wr )
    {
        int width = wr.getWidth();
        int height = wr.getHeight();
        Rectangle r = new Rectangle( 0, 0, width, height );
        return setToWhite( wr, r );
    }
    public static float setToWhite( WritableRaster wr, Rectangle bounds )
    {
        int blackPixels = 0;
        int yEnd = bounds.height+bounds.y;
        int xEnd = bounds.width+bounds.x;
        // set all to white
        int[] iArray = new int[bounds.width];
        int[] dirty = new int[bounds.width];
        for ( int x=0;x<bounds.width;x++ )
            iArray[x] = 255;
        for ( int y=bounds.y;y<yEnd;y++ )
        {
            wr.getPixels(bounds.x,y,bounds.width,1,dirty);
            for ( int i=0;i<dirty.length;i++ )
                if ( dirty[i]==0 )
                    blackPixels++;
            wr.setPixels(bounds.x,y,bounds.width,1,iArray);
        }
        return (float)blackPixels/(float)(wr.getWidth()*wr.getHeight());
    }
    /**
     * Actually set a black in the given raster
     * @param wr the raster to set a black pixel in
     * @param loc the location of the pixel
     */
    public static void addBlackPixel( WritableRaster wr, Point loc )
    {
        int[] iArray = new int[1];
        wr.setPixel(loc.x,loc.y,iArray);
    }
    /**
     * Set a black pixel in the dirt array, keep track of minima etc
     * @param x the x-coordinate of the black pixel
     * @param y the y-coordinate of the black pixel
     * @param iArray an array of 1 black pixel
     */
    private void setBlackPixel( int x, int y,int[] iArray )
    {
        if ( dirt != null )
            dirt.setPixel(x,y,iArray);
        else    // accumulate
            parent.setPixel( x, y, iArray );
        numBlackPixels++;
        if ( x < minX )
            minX = x;
        if ( x > maxX )
            maxX = x;
        if ( y < minY )
            minY = y;
        if ( y > maxY )
            maxY = y;
    }
    /**
     * Is this pixel already in the dirty set?
     * @param loc the point to test
     * @return true if the pixel in that position is already part of this blob
     */
    boolean contains( Point loc )
    {
        int[] iArray=new int[1];
        dirt.getPixel(loc.x,loc.y,iArray);
        return iArray[0]==0;
    }
    int getBlackLevel(Point p)
    {
        if ( blackLevelImage == null )
            return 0;
        else
        {
            int[] iArray = new int[1];
            blackLevelImage.getPixel(p.x, p.y, iArray);
            return iArray[0];
        }
    }
    int getTotalPixels()
    {
        return numWhitePixels+numBlackPixels;
    }
    int getBlackPixels()
    {
        return numBlackPixels;
    }
    /**
     * Check if there is one dirty dot not already seen.
     * @param wr the raster to search
     * @param start the first blackdot
     * @param iArray the array containing the dirty dot
     */
    void checkDirtyDot( WritableRaster wr, Point start, int[] iArray )
    {
        hull = new ArrayList<>();
        // do it without recursion
        stack.push( start );
        while ( !stack.empty() )
        {
            Point p = stack.pop();
            wr.getPixel( p.x, p.y, iArray );
            if ( iArray[0] <= getBlackLevel(p) )
            {
                if ( dirt != null )
                    dirt.getPixel( p.x, p.y, iArray );
                else
                    parent.getPixel( p.x, p.y, iArray );
                if ( iArray[0] != 0 )
                {
                    iArray[0] = 0;
                    // set one pixel known to be black 
                    setBlackPixel( p.x, p.y, iArray );
                    // top
                    if ( p.y > 0 )
                        stack.push( new Point(p.x,p.y-1) );
                    // bottom
                    if ( p.y < wr.getHeight()-1 )
                        stack.push( new Point(p.x, p.y+1) );
                    // left
                    if ( p.x > 0 )
                        stack.push( new Point(p.x-1, p.y) );
                    // right
                    if ( p.x < wr.getWidth()-1 )
                        stack.push( new Point(p.x+1, p.y) );
                    // bot-left
                    if ( p.y < wr.getHeight()-1 && p.x > 0 )
                        stack.push( new Point(p.x-1, p.y+1) );
                    // bot-right
                    if ( p.y < wr.getHeight()-1 && p.x < wr.getWidth()-1 )
                        stack.push( new Point(p.x+1, p.y+1) );
                    // top-left
                    if ( p.y > 0 && p.x > 0 )
                        stack.push( new Point(p.x-1, p.y-1) );
                    // top-right
                    if ( p.y > 0 && p.x < wr.getWidth()-1 )
                        stack.push( new Point(p.x+1, p.y-1) );
                }
            }
            else
                hull.add( p );
        }
    }
    /**
     * Expand a dark pixel down and to left and right
     * @param wr the raster to expand within
     * @param start the start point
     */
    public void expandArea( WritableRaster wr, Point start )
    {
        int[] iArray = new int[1];
        numBlackPixels = 0;
        stack.clear();
        checkDirtyDot(wr,start,iArray);
        firstBlackPixel = start;
    }
    /**
     * Get this blob's width
     * @return an int > 0
     */
    int getWidth()
    {
        return (minX==Integer.MAX_VALUE)?0:(maxX-minX)+1;
    }
    /**
     * Get this blob's height
     * @return an int > 0
     */
    int getHeight()
    {
        return (minX==Integer.MAX_VALUE)?0:(maxY-minY)+1;
    }
    /**
     * Is a point inside a circle?
     * @param centre the centre of the circle
     * @param loc the point to test
     * @param radius the radius of the circle
     * @return true if inside on or the circle
     */
    boolean inCircle( Point centre, Point loc, int radius )
    {
        int xDelta = loc.x-centre.x;
        int yDelta = loc.y-centre.y;
        return xDelta*xDelta + yDelta*yDelta < radius*radius;
    }
    /**
     * Is this blob surrounded by a wide band of white?
     * @param wr the raster to search in
     * @param standoff the amount of vertical or horizontal white standoff
     * @return true if it is
     */
    boolean hasWhiteStandoff( WritableRaster wr, int standoff )
    {
        return hasWhiteStandoff(wr,standoff,standoff);
    }
    /**
     * Is this blob surrounded by a wide band of white?
     * @param wr the raster to search in
     * @param hStandoff the amount of white horizontal standoff
     * @param vStandoff the amount of vertical white standoff
     * @return true if it is
     */
    boolean hasWhiteStandoff( WritableRaster wr, int hStandoff, int vStandoff )
    {
        // 1. original blob bounds
        Rectangle inner = new Rectangle(topLeft().x,topLeft().y,
            getWidth(),getHeight());
        Rectangle outer = (Rectangle)inner.clone();
        // 2. outset rect by standoff
        if ( outer.x >= hStandoff )
            outer.x -= hStandoff;
        else
            outer.x = 0;
        if ( outer.y-vStandoff > 0 )
            outer.y -= vStandoff;
        else
            outer.y = 0;
        if ( outer.x+outer.width+hStandoff*2 < wr.getWidth() )
            outer.width += hStandoff*2;
        else
            outer.width = wr.getWidth()-outer.x;
        if ( outer.y+outer.height+vStandoff*2 < wr.getHeight() )
            outer.height += vStandoff*2;
        else
            outer.height = wr.getHeight()-outer.y;
        // 3. test for black pixels in that area
        int[] iArray = new int[1];
        int maxY = outer.y+outer.height;
        int maxX = outer.x+outer.width;
        int nBlacks = 0;
        int maxRogues = opts.getInt(Options.Keys.maxRoguePixels);
        for ( int y=outer.y;y<maxY;y++ )
        {
            for ( int x=outer.x;x<maxX;x++ )
            {
                Point loc = new Point(x,y);
                if ( !inner.contains(loc) )
                {
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        if ( nBlacks == maxRogues )
                        {
                            return false;
                        }
                        else
                            nBlacks++;
                    }
                }
            }
        }
        return true;
    }
    /**
     * Test with small image
     * @param args ignored
     */
    public static void main(String[] args )
    {
        try
        {
            Options opts = new Options(new JSONObject());
            String url = "http://ecdosis.net/test.png";
            Double[][] cc = {{0.0,0.0},{100.0,0.0},{100.0,100.0},{0.0,100.0}};
            Picture p = new Picture( opts, url, new TextIndex("",""), 
                cc, InetAddress.getByName("127.0.0.1") );
            p.convertToTwoTone();
            BufferedImage bandw = ImageIO.read(p.twotone);
            WritableRaster wr = bandw.getRaster();
            Blob largest = null;
            int max = 0;
            int[] iArray = new int[1];
            WritableRaster darkRegions = bandw.copyData(null);
            Blob.setToWhite( darkRegions );
            for ( int y=0;y<wr.getHeight();y++ )
            {
                for ( int x=0;x<wr.getWidth();x++ )
                {
                    Blob b = new Blob(darkRegions,opts,null);
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        b.expandArea(wr, new Point(x,y) );
                        if ( b.size()>max )
                        {
                            System.out.println("Found new blob at "+x
                                +","+y+" size="+b.size());
                            largest = b;
                            max = b.size();
                        }
                    }
                }
            }
            if ( largest != null )
            {
                WritableRaster dirt = bandw.copyData(null);
                Blob.setToWhite( dirt );
                largest.save(dirt,wr,largest.firstBlackPixel);
                System.out.println(largest.toString());
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
    }
    /**
     * Convert to string for debug
     * @return a String representation of the blog in situ (big)
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        int[] iArray = new int[1];
        for ( int y=0;y<dirt.getHeight();y++ )
        {
            for ( int x=0;x<dirt.getWidth();x++ )
            {
                dirt.getPixel(x,y,iArray);
                if ( iArray[0]==0 )
                    sb.append("*");
                else
                    sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    /**
     * Convert a blob to a Polygon if you can
     * @return a Polygon that is a convex hull of the blob or null
     */
    Polygon toPolygon()
    {
        if ( dirt != null && firstBlackPixel != null )
        {
            if ( hull == null )
                expandArea( dirt, firstBlackPixel );
            Point2D[] points = new Point2D[hull.size()];
            for ( int i=0;i<hull.size();i++ )
            {
                Point p = hull.get(i);
                points[i] = new Point2D( p.x, p.y );
            }
            if ( points.length > 1 )
            {
                GrahamScan gs = new GrahamScan( points );
                return gs.toPolygon();
            }
        }
        return null;
    }
    /**
     * Does this blob have a hull or is it just a dot?
     * @return true if it has a valid hull
     */
    public boolean hasHull()
    {
        return this.hull != null && this.hull.size()>1;
    }
}
