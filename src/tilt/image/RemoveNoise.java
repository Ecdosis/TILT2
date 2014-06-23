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
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
/**
 * Remove big black blobs from two-tone pictures.
 * @author desmond
 */
public class RemoveNoise 
{
    BufferedImage src;
    WritableRaster darkRegions;
    static float BORDER = 0.1f;
    ArrayList<Blob> rejects;
    public RemoveNoise( BufferedImage src )
    {
        this.src = src;
        darkRegions = src.copyData(null);
        Blob.setToWhite( darkRegions );
        this.rejects = new ArrayList<Blob>();
    }
    /**
     * Clear pixels in the corresponding image that are marked "dark" 
     * @param wr the writable raster to clean
     */
    private void clearPixels( WritableRaster wr )
    {
        int[] iArray = new int[1];
        Point p = new Point(0,0);
        for ( int y=0;y<wr.getHeight();y++ )
        {
            for ( int x=0;x<wr.getWidth();x++ )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    p.x = x;
                    p.y = y;
                    darkRegions.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        iArray[0] = 255;
                        wr.setPixel(x,y,iArray );
                    }
                }
            }
        }
    }
    /**
     * Is there a matching black pixel at these coordinates in darkRegions?
     * @param loc the point to test
     * @return true if it is already dark there
     */
    private boolean isDirty( Point loc )
    {
        int[] iArray =new int[1];
        darkRegions.getPixel(loc.x,loc.y,iArray);
        return iArray[0]==0;
    }
    /**
     * Add the pixels of a blob to the darkRegions
     * @param b the blob to add
     */
    private void mergeIntoDarkness( Blob b )
    {
        WritableRaster other = b.dirt;
        int[] iArray = new int[1];
        for ( int y=0;y<other.getHeight();y++ )
        {
            for ( int x=0;x<other.getWidth();x++ )
            {
                other.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                    darkRegions.setPixel(x,y,iArray);
            }
        }
    }
    /**
     * Add any formerly rejected blobs that are surrounded by accepted blobs
     * @param wr the raster of the image
     */
    private void calcWhiteArea( WritableRaster wr )
    {
        Point lastLeft=null,lastRight=null;
        Point currentLeft = new Point(0,0);
        Point currentRight = new Point(0,0);
        int width = darkRegions.getWidth();
        int height = darkRegions.getHeight();
        ArrayList<Point> rhs = new ArrayList<Point>();
        int[] iArray = new int[1];
        // build enclosing polygon
        Polygon whiteRegion = new Polygon();
        for ( int y=0;y<height;y++ )
        {
            int leftMost = -1;
            int rightMost = width;
            for ( int x=0;x<width;x++ )
            {
                darkRegions.getPixel(x,y,iArray);
                if ( iArray[0] == 0 )
                {
                    rightMost = leftMost = x;
                    break;
                }
            }
            if ( leftMost > 0 )
            {
                for ( int x=width-1;x>leftMost;x-- )
                {
                    darkRegions.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        rightMost = x;
                        break;
                    }
                }
            }
            if ( leftMost > 0 )
            {
                if ( lastLeft==null || lastLeft.x != leftMost )
                {
                    if ( lastLeft != null )
                        whiteRegion.addPoint(lastLeft.x,lastLeft.y);
                    lastLeft = new Point( (leftMost==0)?0:leftMost-1, y );
                }
                currentLeft.x = leftMost;
                currentLeft.y = y;
            }
            if ( rightMost < width )
            {
                if ( lastRight==null || lastRight.x != rightMost )
                {
                    if ( lastRight != null )
                        rhs.add(lastRight);
                    lastRight = new Point( (rightMost==width-1)?width-1
                        :rightMost+1, y );
                }
                currentRight.x = rightMost;
                currentRight.y = y;
            }
        }
        // join up the bottom
        if ( currentLeft.y != lastLeft.y )
            whiteRegion.addPoint(currentLeft.x,currentLeft.y);
        if ( currentRight.y != lastRight.y )
            whiteRegion.addPoint(currentRight.x,currentRight.y);
        // add the rhs
        for ( int i=rhs.size()-1;i>=0;i-- )
        {
            Point r = rhs.get(i);
            whiteRegion.addPoint( r.x,r.y );
        }
        for ( int i=0;i<rejects.size();i++ )
        {
            Blob b = rejects.get(i);
            if ( whiteRegion.contains(b.topLeft()) 
                || whiteRegion.contains(b.botRight()) )
            {
                //System.out.println("Adding back rejected blob "+b.minX+","+b.minY);
                b.save(darkRegions,wr,b.firstBlackPixel);
            }
        }
    }
    /**
     * Identify large areas of adjacent black pixels and remove them.
     */
    public void clean()
    {
        WritableRaster wr = src.getRaster();
        Point loc= new Point(0,0);
        int[] iArray = new int[1];
        int topYLimit = Math.round(BORDER*wr.getHeight());
        int botYLimit = wr.getHeight()-topYLimit;
        int leftXLimit = Math.round(BORDER*wr.getWidth());
        int rightXLimit = wr.getWidth()-leftXLimit;
        for ( int y=0;y<wr.getHeight();y++ )
        {
            loc.y = y;
            for ( int x=0;x<wr.getWidth();x++ )
            {
                if ( y <= topYLimit || y >= botYLimit 
                    || x <= leftXLimit || x >= rightXLimit )
                {
                    loc.x = x;
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0] == 0 )
                    {
                        if ( !isDirty(loc) )
                        {
                            Blob b = new Blob(darkRegions);
                            b.expandArea( wr, loc );
                            if ( b.isValid(wr.getWidth(),wr.getHeight()) )
                            {
                                b.save(darkRegions,wr,loc);
                                mergeIntoDarkness( b );
                                //System.out.println("Added blob x="+x+" y="+y
                                //    +" width="+b.getWidth()+" height="+b.getHeight());
                            }
                            else
                                rejects.add( b );
                        }
                    }
                }
            }
        }
        calcWhiteArea( wr );
        clearPixels( wr );
    }
}
