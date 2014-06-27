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
    public Border( WritableRaster wr, float average )
    {
        this.wr = wr;
        hUnit = Math.round(wr.getWidth()*RATIO);
        vUnit = Math.round(wr.getHeight()*RATIO);
        int height = wr.getHeight()/vUnit;
        int width = wr.getWidth()/hUnit;
        this.average = Math.round(average*hUnit*vUnit);
        this.border = Math.round(wr.getWidth()*BRD);          
        Rectangle r = new Rectangle( wr.getWidth(), wr.getHeight());
        area = new Area( r );
        // hollow it out
        Rectangle q = new Rectangle(r.x+hUnit, r.y+vUnit, r.width-(hUnit*2), 
            r.height-(vUnit*2));
        area.subtract( new Area(q) );
        // now add in discovered blocks
        // lhs
        for ( int i=0;i<=height;i++ )
        {
            int v = (i*vUnit+vUnit>wr.getHeight())?wr.getHeight():i*vUnit;
            Unit u = new Unit(0,v,Direction.right);
            u.move();
        }
        // rhs
        for ( int i=0;i<=height;i++ )
        {
            int h = wr.getWidth()-hUnit;
            int v = (i*vUnit+vUnit>wr.getHeight())?wr.getHeight():i*vUnit;
            Unit u = new Unit(h,v,Direction.left);
            u.move();
        }
        // top
        for ( int i=0;i<=width;i++ )
        {
            int h = (i*hUnit+hUnit>wr.getWidth())?wr.getWidth():i*hUnit;
            Unit u = new Unit(h,0,Direction.down);
            u.move();
        }
        // bottom
        for ( int i=0;i<=width;i++ )
        {
            int h = (i*hUnit+hUnit>wr.getWidth())?wr.getWidth():i*hUnit;
            Unit u = new Unit(h,wr.getHeight()-vUnit,Direction.up);
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
        seenWhite;
    }
    /**
     * Directions of scan
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
        }
        /**
         * Run the move
         */
        void run()
        {
            // make sure this is zero!
            this.count = 0;
            int hLimit = (x+hUnit<wr.getWidth())?x+hUnit:wr.getWidth();
            int vLimit = (y+vUnit<wr.getHeight())?y+vUnit:wr.getHeight();
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
                    r = new Rectangle( 0, y, x+hUnit, vUnit );
                    break;
                case left:
                    r = new Rectangle( x, y, wr.getWidth()-x, vUnit );
                    break;
                case down:
                    r = new Rectangle( x, 0, hUnit, y+vUnit );
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
            if ( count > average && state == State.seenNothing )
            {
                Rectangle r = makeRectangle();
                if ( r != null )
                    area.add( new Area(r) );
                state = State.seenBlack;
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
            else if ( count > average && state == State.seenBlack )
            {
                Rectangle r = makeRectangle();
                if ( r != null )
                {
                    if ( area.contains(r) )
                        state = State.seenWhite;
                    else
                        area.add( new Area(r) );
                }
            }
            else if ( count < average )
            {
                state = State.seenWhite;
            }
            // else we're done
        }
        /**
         * Set up the initial move and run it
         */
        void move()
        {
            run();
            recompute();
        }
    }
}
