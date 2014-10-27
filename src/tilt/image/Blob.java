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

package tilt.image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.net.InetAddress;
import javax.imageio.ImageIO;
import java.util.Stack;
import java.util.ArrayList;
import tilt.Utils;
import tilt.handler.TextIndex;
import tilt.image.convexhull.*;
import org.json.simple.*;
import tilt.image.page.Polygon;


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
    /** minimum ratio of black to white pixels in area */
    static float MIN_BLACK_PC = 0.5f;
    /** minimum proportion of width or height taken up by blob */
    static float MIN_H_PROPORTION = 0.05f;
    static float MIN_V_PROPORTION = 0.05f;
    static float ODD_SHAPE = 4.0f;
    /** accumulate black pixels here */
    WritableRaster dirt; 
    /** unless they are already in here */
    WritableRaster parent;
    /** limits of pixel area */
    int minX,maxX,minY,maxY;
    /** First black pixel*/
    Point firstBlackPixel;
    Stack<Point> stack;
    ArrayList<Point> hull;
    Blob( WritableRaster parent )
    {
        this.parent = parent;
        this.minY = this.minX = Integer.MAX_VALUE;
        this.stack = new Stack();
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
     * Is this blob entirely with the narrow edge around the margin?
     * @param edge width of the edge
     * @param width width of the blob
     * @param height height of the blob
     * @return true if it is
     */
    private boolean nearEdge( int edge, float width, float height )
    {
        return ( ((width/height)>2.0f && (minY<=edge||maxY>=parent.getHeight()-edge))
            || ((height/width)>2.0f && (minX<=edge||maxX>=parent.getWidth()-edge)) );
    }
    /**
     * Does this blob constitute a big enough area?
     * @param iWidth the overall image width
     * @param iHeight the overall image height
     * @return true if this blob is big enough to remove
     */
    public boolean isValid( int iWidth, int iHeight )
    {
        int totalPixels = numWhitePixels+numBlackPixels;
        float ratio= (float)numBlackPixels/(float)totalPixels;
        float width = (float)(maxX+1-minX);
        float height = (float)(maxY+1-minY);
        int edge = Math.round(iWidth/100.0f);
        if ( nearEdge(edge,width,height) )
            return true;
        else if ( ratio > MIN_BLACK_PC )
        {
            float hprop = width/(float)iWidth;
            float vprop = height/(float)iHeight;
            if ( hprop >= MIN_H_PROPORTION )
                return true;
            else if ( vprop >=MIN_V_PROPORTION )
                return true;
//            if ( htWtRatio>= 2.0 )
//                System.out.println("vprop="+vprop);
//            if ( wtHtRatio>= 2.0 )
//                System.out.println("hprop="+hprop);
        }
        //else
        //    System.out.println("rejecting blob at "+minX+","+minY+" width="
        //    +width+" height="+height+" edge="+edge);
        return false;
    }
    public boolean isOddShaped( int iWidth, int iHeight )
    {
        float width = (float)(maxX+1-minX);
        float height = (float)(maxY+1-minY);
        float htWtRatio= (float)height/(float)width;
        float wtHtRatio = (float)width/(float)height;
        float hprop = width/(float)iWidth;
        float vprop = height/(float)iHeight;
        if ( (hprop >= MIN_H_PROPORTION) && (wtHtRatio > ODD_SHAPE) )
            return true;
        else if ( (vprop >=MIN_V_PROPORTION) && (htWtRatio > ODD_SHAPE) )
            return true;
        else
            return false;
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
    /**
     * Check if there is one dirty dot not already seen.
     * @param wr the raster to search
     * @param start the first blackdot
     * @param iArray the array containing the dirty dot
     */
    void checkDirtyDot( WritableRaster wr, Point start,  int[] iArray )
    {
        hull = new ArrayList<>();
        // do it without recursion
        stack.push( start );
        while ( !stack.empty() )
        {
            Point p = stack.pop();
            wr.getPixel( p.x, p.y, iArray );
            if ( iArray[0] == 0 )
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
        return (maxX-minX)+1;
    }
    /**
     * Get this blob's height
     * @return an int > 0
     */
    int getHeight()
    {
        return (maxY-minY)+1;
    }
    /**
     * Test with small image
     * @param args ignored
     */
    public static void main(String[] args )
    {
        try
        {
            String s="[[0.0,0.0],[100.0,0.0],[100.0,100.0],[0.0,100.0]]";
            Object obj=JSONValue.parse(s);
            JSONArray coords=(JSONArray)obj;
            JSONObject opts = new JSONObject();
            opts.put("coords",coords);
            opts.put("blur",0);
            Picture p = new Picture( "http://ecdosis.net/test.png", opts, 
                new TextIndex("",""), 
                InetAddress.getByName("127.0.0.1") );
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
                    Blob b = new Blob(darkRegions);
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
            GrahamScan gc = new GrahamScan( points );
            Iterable<Point2D> pts = gc.hull();
            return Utils.pointsToPolygon(pts);
        }
        else
            return null;
    }
}
