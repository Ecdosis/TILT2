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
import tilt.align.Matchup;
import tilt.handler.TextIndex;
import tilt.image.page.Page;

/**
 * Link shapes to words
 * @author desmond
 */
public class Linker 
{
    TextIndex text;
    Page page;
    public Linker( TextIndex text, Page page )
    {
        this.text = text;
        this.page = page;
        float ppc = page.pixelsPerChar( text.numChars() );
        int[] shapeWidths = page.getShapeWidths();
        int[] wordWidths = text.getWordWidths( ppc );
        Matchup matcher = new Matchup( wordWidths, shapeWidths );
        //matcher.align();
    }
}
