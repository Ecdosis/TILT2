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
    Background background;
    int backgroundAverage;
    public Preflight( BufferedImage ci, Options opts )
    {
        this.opts = opts;
        this.src = ci;
        this.background = new Background();
    }
    /**
     * Apply the optional reductions
     * @return 
     */
    public BufferedImage reduce()
    {
        BufferedImage after = src;
        boolean blueGreenFilter = opts.getBoolean(Options.Keys.blueGreenFilter);
        int maxWidth = opts.getInt(Options.Keys.maximumWidth);
        if ( src.getWidth() > maxWidth )
        {
            float scale = (float)maxWidth/(float)src.getWidth();
            int w = Math.round(src.getWidth()*scale);
            int h = Math.round(src.getHeight()*scale);
            after = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            at.scale(scale, scale);
            AffineTransformOp scaleOp = 
               new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            after = scaleOp.filter(src, after);
        }
        if ( blueGreenFilter )
        {
            int w = after.getWidth();
            int h = after.getHeight();
            for (int x=0; x<w; x++)
            {
                for (int y=0;y<h;y++ )
                {
                    int current = after.getRGB(x,y);
                    int blueCol = current & 0x000000FF;
                    int redCol = current & 0x00FF0000;
                    redCol >>= 16;
                    int greenCol = current & 0x0000FF00;
                    greenCol >>= 8;
                    if ( greenCol > redCol || blueCol > redCol )
                    {
                        // we have "blue"
                        current &= 0xFF000000;
                        current |= backgroundAverage;
                    }
                    else if ( background.isBright(current) )
                        backgroundAverage = background.update(current);
                    after.setRGB(x, y, current);
                }
            }
        }
        return after;
    }
}
