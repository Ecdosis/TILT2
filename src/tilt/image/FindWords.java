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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import tilt.image.page.*;
import tilt.image.page.LineRegion;
import java.util.ArrayList;
import tilt.image.geometry.Point;
import tilt.image.geometry.Polygon;
import java.awt.Rectangle;
import tilt.handler.post.Options;
import tilt.handler.post.TextIndex;
import tilt.exception.TiltException;
/**
 * Using the lines lists search for words
 * @author desmond
 */
public class FindWords 
{
    BufferedImage src;
    Page page;
    WritableRaster wr;
    WritableRaster dirty;
    Options opts;
    int shapeID = 1;
    int shapeNo;
    boolean onLine2;
    /**
     * Create a word-finder
     * @param src the src image in pure black and white
     * @param page the page object with postulated lines
     * @param opts the options from the GeoJSON document
     */
    public FindWords( BufferedImage src, Page page, Options opts ) 
    {
        wr = src.getRaster();
        dirty = src.copyData(null);
        Blob.setToWhite(dirty);
        this.page = page;
        this.src = src;
        this.opts = opts;
    }
    /**
     * Actually find the word-shapes
     * @param text the text to find on the page
     */
    public void find( TextIndex text ) throws TiltException
    {
        Line prev;
        Line next;
        Line curr = null;
        ArrayList<Line> lines = page.getLines();
        int lineHt = page.getLineHeight();
        // look along each line segment for word-shapes
        // and assign them to lines
        LineRegion prevLR = null;
        LineRegion lr=null;
        LineRegion nextLR=null;
        for ( int i=0;i<lines.size();i++ )
        {
            prev = curr;
            curr = lines.get(i);
            next = ( i < lines.size()-1)?lines.get(i+1):null;
            prevLR = lr;
            if ( nextLR != null )
                lr = nextLR;
            else
                lr = new LineRegion( src, curr, prev, next, lineHt);
            onLine2 = i == 1;
            Line future = (i<lines.size()-2)?lines.get(i+2):null;
            nextLR = (next==null)?null
                :new LineRegion(src,next,curr,future,lineHt);
            Point[] points = curr.getPoints();
            for ( int j=0;j<points.length-1;j++ )
            {
                shapeNo=0;
                findPartWords( curr, lr, prevLR, nextLR, points[j], points[j+1] );
            }
        }
        WordShapes ws = new WordShapes( text, lines, opts );
        ws.makeWords();
    }
    void printBounds( Polygon pg )
    {
        Rectangle r = pg.getBounds();
        System.out.println("["+r.x+","+r.y+","+r.width+","+r.height+"]");
    }
    int newShapeID()
    {
        return this.shapeID++;
    }
    /**
     * Find part of a word. All blobs near the line will be added.
     * @param curr the current line
     * @param lr the region on the page belonging exclusively to this line
     * @param prevLR the line region of the previous line
     * @param nextLR the line-region of the next line
     * @param p0 the start of the baseline segment
     * @param p1 the end of this baseline segment
     */
    private void findPartWords( Line curr, LineRegion lr, LineRegion prevLR, 
        LineRegion nextLR, Point p0, Point p1 ) throws TiltException
    {
        // compute polygon in which to look for blobs
        Polygon core = lr.getLineBase( p0, p1 );
        // look only inside the core bounds for black pixels
        Rectangle r = core.getBounds();
        Polygon poly = lr.getPoly();
        Rectangle lrBounds = poly.getBounds();
        int endY = r.y+r.height;
        int[] iArray = new int[r.width];
        int[] dArray = new int[1];
        // if the last pixel was black it must be part of the same blob/polygon
        boolean lastPixelWasBlack = false;
        // per pixel row within r
        Point q = new Point(0,0);
        for ( int y=r.y;y<endY;y++ )
        {
            q.y = y;
            wr.getPixels( r.x, y, r.width, 1, iArray );
            // for each pixel in each row...
            for ( int i=0;i<r.width;i++ )
            {
                int x = i+r.x;
                q.x = x;
                if ( iArray[i] == 0 )
                {
                    // ignore black pixels not in polgonal core of line
                    if ( core.contains(q) )
                    {
                        if ( !lastPixelWasBlack )
                        {
                            lastPixelWasBlack = true;
                            // have we already scanned this pixel?
                            dirty.getPixel(x, y, dArray);
                            if ( dArray[0] != 0 )
                            {
                                Polygon shape = page.shapeForPoint(q);
                                if ( shape == null )
                                {
                                    // shape not already defined
                                    Blob b = new Blob(wr,opts,null);
                                    b.save(dirty, wr, q.toAwt());
                                    if ( b.hasHull() )
                                    {
                                        shapeNo++;
                                        shape = b.toPolygon();
//                                      // compute bounds, precise pts
                                        ArrayList<Point> list = shape.toPoints();
//                                        if ( onLine2 && shapeNo==2 )
//                                        {
//                                            System.out.println("Before:"+shape.printList(list) );
//                                            System.out.println(shape.getBounds().toString());
//                                        }
                                        poly = lr.getPoly();
                                        // clip shape to next and prev line regions
                                        if ( !poly.contains(shape) )
                                        {
                                            if ( nextLR != null 
                                                && nextLR.getPoly().intersects(shape) )
                                            {
                                                shape = shape.getIntersection(poly);
                                            }
                                            if ( prevLR != null 
                                                && prevLR.getPoly().intersects(shape) )
                                            {
                                                shape = shape.getIntersection(poly);
                                            }
                                        }
//                                        if ( onLine2 && shapeNo==2 )
//                                        {
//                                            System.out.println("After:"+shape.printList(list) );
//                                            System.out.println(shape.getBounds().toString());
//                                        }
                                        shape.setLine(curr);
                                        shape.toPoints();
                                        shape.setID(newShapeID());
                                        page.addShape(shape);// registers line also
                                    }
                                    else
                                        lastPixelWasBlack = false;
                                }
                            }
                        } 
                    }   
                }
                else
                    lastPixelWasBlack = false;
            }
            // reset at line-end
            lastPixelWasBlack = false;
        }
        Blob.setToWhite( dirty, lrBounds );
    }
}
