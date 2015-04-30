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
     */
    public void find() throws TiltException
    {
        Line prev;
        Line next;
        Line curr = null;
        ArrayList<Line> lines = page.getLines();
        int lineHt = page.getLineHeight();
        // look along each line segment for word-shapes
        // and assign them to lines
        for ( int i=0;i<lines.size();i++ )
        {
            prev = curr;
            curr = lines.get(i);
            if ( i < lines.size()-1 )
                next = lines.get(i+1);
            else
                next = null;
            LineRegion lr = new LineRegion( src, curr, prev, next, lineHt);
            Line future = (i<lines.size()-2)?lines.get(i+2):null;
            LineRegion nextLR = new LineRegion(src,next,curr,future, lineHt);
            Point[] points = curr.getPoints();
            for ( int j=0;j<points.length-1;j++ )
            {
                findPartWords( curr, lr, nextLR, points[j], points[j+1] );
            }
        }
//        // page.mergeLines();
//        page.pruneShortLines();
//        // now merge the word-shapes into real words
//        page.getWordGap(); 
//        page.joinWords();
    }
    /**
     * Find part of a word. All blobs near the line will be added.
     * @param curr the current line
     * @param lr the region on the page belonging exclusively to this line
     * @param nextLR the line-region of the next line
     * @param p0 the start of the baseline segment
     * @param p1 the end of this baseline segment
     */
    private void findPartWords( Line curr, LineRegion lr, LineRegion nextLR,
        Point p0, Point p1 ) throws TiltException
    {
        // compute polygon in which to look for blobs
        Polygon core = lr.getLineBase( p0, p1 );
        // look only inside the core bounds for black pixels
        Rectangle r = core.getBounds();
        int endY = r.y+r.height;
        int[] iArray = new int[r.width];
        // if the last pixel was black it must be part of the same blob/polygon
        boolean lastPixelWasBlack = false;
        for ( int y=r.y;y<endY;y++ )
        {
            wr.getPixels( r.x, y, r.width, 1, iArray );
            for ( int i=0;i<r.width;i++ )
            {
                int x = i+r.x;
                if ( iArray[i] == 0 )
                {
                    if ( !lastPixelWasBlack )
                    {
                        lastPixelWasBlack = true;
                        Point q = new Point(x,y);
                        if ( core.contains(q) )
                        {
                            Polygon shape = page.shapeForPoint(q);
                            if ( shape == null )
                            {
                                Blob b = new Blob(wr,opts,null);
                                b.save(dirty, wr, q.toAwt());
                                shape = b.toPolygon();
                                // if shape exceeds the current line region:
                                if ( !lr.getPoly().contains(shape) )
                                {
                                    if ( nextLR != null 
                                        && nextLR.getPoly().intersects(shape) )
                                        shape = shape.getIntersection(lr.getPoly());
                                    else    // let it all hang out...
                                        lr.setPoly(lr.getPoly().getUnion(shape));
                                    // the previous line is already dealt with
                                }
                                shape.setLine(curr);
                                page.addShape(shape);// registers line also
                            }
                        } // else ignore it
                    }   
                }
                else
                    lastPixelWasBlack = false;
            }
            // reset at line-end
            lastPixelWasBlack = false;
        }
    }
    
}
