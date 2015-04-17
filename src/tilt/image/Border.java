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
import java.awt.image.WritableRaster;
import java.awt.geom.Area;
import java.awt.Rectangle;
/**
 * Compute the best border for an image
 * @author desmond
 */
public class Border 
{
    /** hollow border region */
    Area area;
    /** average number of pixels in a block */
    float average;
    /** maximum number of pixels in a "white" block */
    float whiteLevel;
    /** size of blocks relative to image */
    static float RATIO = 0.02f;
    /** fraction of image to scan for border */
    static float BRD = 0.2f;
    /** dimensions of a block */
    int hUnit,vUnit;
    /** the parent image's raster */
    WritableRaster wr;
    /** the border width in pixels (same all around) */
    int border;
    /**
     * Create a border object
     * @param wr the raster of the image
     * @param average the average black pixel density per pixel
     */
    public Border( WritableRaster wr, float average, Rectangle cropRect )
    {
        this.wr = wr;
        hUnit = Math.round(cropRect.width*RATIO);
        vUnit = Math.round(cropRect.height*RATIO);
        int height = cropRect.height/vUnit;
        int width = cropRect.width/hUnit;
        this.whiteLevel = Math.round(average*hUnit*vUnit)/3;
        if ( this.whiteLevel==0 )
            this.whiteLevel = 1;
        this.average = Math.round(average*hUnit*vUnit);
        this.border = Math.round(cropRect.width*BRD);          
        area = new Area( cropRect );
        // hollow it out
        Rectangle q = new Rectangle(cropRect.x+hUnit, cropRect.y+vUnit, 
            cropRect.width-(hUnit*2), cropRect.height-(vUnit*2));
        area.subtract( new Area(q) );
        // now add in discovered blocks
        // lhs
        for ( int i=1;i<height-1;i++ )
        {
            int v = cropRect.y+i*vUnit;
            Unit u = new Unit(hUnit,v,Direction.right);
            u.move();
        }
        // rhs
        for ( int i=1;i<height-1;i++ )
        {
            int h = cropRect.x+cropRect.width-hUnit*2;
            int v = cropRect.y+i*vUnit;
            Unit u = new Unit(h,v,Direction.left);
            u.move();
        }
        // top
        for ( int i=1;i<width-1;i++ )
        {
            int h = cropRect.x+i*hUnit;
            Unit u = new Unit(h,vUnit,Direction.down);
            u.move();
        }
        // bottom
        for ( int i=1;i<width-1;i++ )
        {
            int h = cropRect.x+i*hUnit;
            Unit u = new Unit(h,cropRect.height-vUnit*2,Direction.up);
            u.move();
        }
    }
    /**
     * States of scanning from the border inwards: 
     * seenNothing->seenBlack->seenWhite 
     */
    enum State
    {
        seenNothing,
        seenBlack,
        seenOneWhite,
        seenTwoWhite;
    }
    /**
     * Directions of scan from the edges inwards
     */
    enum Direction
    {
        left,
        right,
        down,
        up;
    };
    /**
     * A block of pixels from the border inwards
     */
    class Unit
    {
        /** leftmost pixel */
        int x;
        /** topmost pixel */
        int y;
        /** direction to move */
        Direction dir;
        /** state of match */
        State state;
        int count;
        Area accumulation;
        /**
         * Create a move object
         * @param x its x-position (left)
         * @param y its y-position (top)
         * @param dir direction to go in
         */
        Unit( int x, int y, Direction dir )
        {
            this.x = x;
            this.y = y;
            this.dir = dir;
            this.state = State.seenNothing;
            this.accumulation = new Area();
        }
        /**
         * Run the move
         */
        void run()
        {
            // make sure this is zero!
            this.count = 0;
            int hLimit = x+hUnit;
            int vLimit = y+vUnit;
            int[] iArray = new int[1];
            for ( int v=y;v<vLimit;v++ )
            {
                for ( int h=x;h<hLimit;h++ )
                {
                    wr.getPixel(h,v,iArray);
                    if ( iArray[0]==0 )
                        count++;
                }
            }
        }
        Rectangle makeRectangle()
        {
            Rectangle r=null;
            switch ( dir )
            {
                case right:
                    r = new Rectangle( x, y, x+hUnit, vUnit );
                    break;
                case left:
                    r = new Rectangle( x, y, wr.getWidth()-x, vUnit );
                    break;
                case down:
                    r = new Rectangle( x, y, hUnit, y+vUnit );
                    break;
                case up:
                    r = new Rectangle( x, y, hUnit, wr.getHeight()-y );
                    break;
            }
            return r;
        }
        /**
         * We've finished this move: set up for next move or stop
         */
        void recompute()
        {
            Rectangle r = makeRectangle();
            Area currentArea = new Area(r);
            accumulation.add( currentArea ); 
            if ( count > average )
            {
                if ( state == State.seenNothing )
                    state = State.seenBlack;
                else 
                    state = State.seenTwoWhite;
            }
            else if ( count <= whiteLevel )
            {
                if ( state == State.seenBlack )
                    state = State.seenOneWhite;
                else if ( state==State.seenOneWhite )
                {
                    state = State.seenTwoWhite;
                    accumulation.subtract( currentArea );
                    area.add( accumulation );
                }
            }
            // else carry on but don't accept it as white
            if ( state != State.seenTwoWhite )
            {
                // we're still good to go
                switch ( dir )
                {
                    case left:
                        if ( wr.getWidth()-(x-hUnit) < border )
                        {
                           this.x -= hUnit;
                           move(); 
                        }
                        break;
                    case right:
                        if ( (x+hUnit) < border )
                        {
                            this.x += hUnit;
                            move();
                        }
                        break;
                    case up:
                        if ( wr.getHeight()-(y-vUnit) < border )
                        {
                            this.y -= vUnit;
                            move();
                        }
                        break;
                    case down:
                        if ( this.y+vUnit < border )
                        {
                            this.y += vUnit;
                            move();
                        }
                        break;
                }
            }
        }
        /**
         * Set up the initial move and run it
         */
        void move()
        {
            Rectangle r = makeRectangle();
            // has this rectangle has already been processed by another unit?
            if ( !area.contains(r) )
            {
                run();
                recompute();
            }
        }
    }
}
