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
import java.awt.image.WritableRaster;

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
    static float MIN_H_PROPORTION = 0.1f;
    static float MIN_V_PROPORTION = 0.05f;
    /** accumulate black pixels here */
    WritableRaster dirt; 
    /** unless they are already in here */
    WritableRaster parent;
    /** limits of pixel area */
    int minX,maxX,minY,maxY;
    /** First black pixel*/
    Point firstBlackPixel;
    Blob( WritableRaster parent )
    {
        this.parent = parent;
        this.minY = this.minX = Integer.MAX_VALUE;
    }
    /**
     * Actually commit the blob to the dirty raster
     * @param dirt the raster to store the pixels to be removed
     * @param wr the original image raster
     * @param loc the start-position of the blob
     */
    public void save( WritableRaster dirt, WritableRaster wr, Point loc )
    {
        this.dirt = parent;
        // recompute the blob - a bit inefficient but storing it is far worse
        expandArea( wr, loc );
    }
    /**
     * How many black pixels we have
     * @return 
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
     */
    public static void setToWhite( WritableRaster wr )
    {
        // set all to white
        int[] iArray = new int[1];
        iArray[0] = 255;
        int height = wr.getHeight();
        int width = wr.getWidth();
        for ( int y=0;y<height;y++ )
        {
            for ( int x=0;x<width;x++ )
            {
                wr.setPixel(x,y,iArray);
            }
        }
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
            if ( width/(float)iWidth>=MIN_H_PROPORTION)
                return true;
            else if ( height/(float)iHeight>=MIN_V_PROPORTION)
                return true;
            //else
            //    System.out.println("rejecting blob at "+minX+","+minY
            //    +" width="+width+" height="+height+" edge="+edge);
        }
        //else
        //    System.out.println("rejecting blob at "+minX+","+minY+" width="
        //    +width+" height="+height+" edge="+edge);
        return false;
    }
    /**
     * Set a black pixel in the dirt array, keep track of minima etc
     * @param x the x-coordinate of the black pixel
     * @param y the y-coordinate of the black pixel
     * @param iArray an array of 1 black pixel
     * @return true if the new pixel was in the parent
     */
    private boolean setBlackPixel( int x, int y,int[] iArray )
    {
        parent.getPixel(x,y,iArray);
        if ( iArray[0]!=0 )
        {
            if ( dirt != null )
            {
                iArray[0] = 0;
                dirt.setPixel(x,y,iArray);
            }
            numBlackPixels++;
            if ( x < minX )
                minX = x;
            if ( x > maxX )
                maxX = x;
            if ( y < minY )
                minY = y;
            if ( y > maxY )
                maxY = y;
            return false;
        }
        else
            return true;
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
     * Expand a dark pixel down and to left and right
     * @param wr the raster to expand within
     * @param start the start point
     */
    void expandArea( WritableRaster wr, Point start )
    {
        int[] iArray = new int[1];
        int l = start.x;
        int r = start.x;
        boolean done = false;
        firstBlackPixel = start;
        setBlackPixel(start.x,start.y,iArray);
        // find r in first row
        for ( int x=start.x+1;x<wr.getWidth();x++ )
        {
            wr.getPixel(x,start.y,iArray);
            if ( iArray[0] != 0 )
                break;
            else
            {
                r = x;
                // NB no white pixels in first row
                setBlackPixel(x,start.y,iArray);
            }
        }
        // find subsequent rows
        for ( int y=start.y+1;y<wr.getHeight();y++ )
        {
            boolean lUnset = true;
            boolean rUnset = true;
            int oldL = l;
            int oldR = r;
            // try to extend l to the left
            for ( int x=l;x>=0;x-- )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0]==0 )
                {
                    done = setBlackPixel(x,y,iArray);
                    lUnset = false;
                    l = x;
                }
                else
                    break;
                if ( done )
                    break;
            }
            // extend r to the right
            for ( int x=r;x<wr.getWidth();x++ )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0]==0 )
                {
                    done = setBlackPixel(x,y,iArray);
                    rUnset = false;
                    r = x;
                }
                else
                    break;
                if ( done )
                    break;
            }
            // process middle bit
            for ( int x=oldL+1;x<oldR;x++ )
            {
                wr.getPixel(x,y,iArray);
                if ( iArray[0]==0 )
                {
                    if ( rUnset )
                        r = x;  // keep updating r
                    if ( lUnset )
                    {
                        lUnset = false;
                        l = x;
                    }
                    done = setBlackPixel(x,y,iArray);
                }
                if ( done )
                    break;
            }
            if ( done || (lUnset && rUnset) )
                break;
            else
            {
                // count white pixels
                for ( int x=l;x<=r;x++ )
                {
                    wr.getPixel(x,y,iArray);
                    if ( iArray[0]!=0 )
                        numWhitePixels++;
                }
            }
        }
    }
    /**
     * Get this blob'swidth
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
}
