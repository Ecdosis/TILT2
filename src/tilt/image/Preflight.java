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
import tilt.handler.post.Options;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

/**
 * Apply options such as colour, size reduction
 * @author desmond
 */
public class Preflight 
{
    Options opts;
    BufferedImage src;
    public Preflight( BufferedImage ci, Options opts )
    {
        this.opts = opts;
        this.src = ci;
    }
    /**
     * Apply the optional reductions
     * @return 
     */
    public BufferedImage reduce()
    {
        BufferedImage after = src;
        float blueFactor = opts.getFloat(Options.Keys.blueFactor);
        int maxWidth = opts.getInt(Options.Keys.maximumWidth);
        if ( src.getWidth() > maxWidth )
        {
            int w = src.getWidth();
            int h = src.getHeight();
            after = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            double scale = (double)maxWidth/(double)w;
            at.scale(scale, scale);
            AffineTransformOp scaleOp = 
               new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            after = scaleOp.filter(src, after);
        }
        if ( blueFactor < 1.0f )
        {
            int w = after.getWidth();
            int h = after.getHeight();
            for (int x=0; x<w; x++)
            {
                for (int y=0;y<h;y++ )
                {
                    int pixelCol = src.getRGB(x,y);
                    pixelCol &= 0xFFFFFF00;
                    pixelCol += Math.round(blueFactor*255);
                    //mask out the non green,non-alpha color.
                    //A is 0xFF000000
                    //R is 0x00FF0000
                    //G is 0x0000FF00
                    //B is 0x000000FF        
                    after.setRGB(x, y, pixelCol);
                }
            }
        }
        return after;
    }
}
