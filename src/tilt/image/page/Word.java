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

package tilt.image.page;

/**
 * Maintain word-related information
 * @author desmond
 */
public class Word 
{
    /** offset of word in HTML text */
    int offset;
    /** the actual word */
    String text;
    /** pixel-length */
    int width;
    /**
     * Create a word object
     * @param offset the offset of the word in the HTML
     * @param text the word itself 
     * @param width its estimated pixel-width
     */
    public Word( int offset, String text, int width )
    {
        this.offset = offset;
        this.width = width;
        this.text = text;
    }
    /**
     * Split a word into two halves
     * @param at the position before which to split it
     * @return the new word (this word is changed)
     */
    Word split( int at )
    {
        float ppc = (float)this.width/(float)this.text.length();
        Word newWord = new Word( this.offset+at, this.text.substring(at), 
            Math.round(ppc*this.text.length()-at) );
        this.width = Math.round(ppc*at);
        this.text = this.text.substring(0,at);
        return newWord;
    }
    /**
     * Get this word's pixel width
     * @return its estimated width in pixels
     */
    int width()
    {
        return width;
    }
    /**
     * Get this word's length in characters
     * @return an int
     */
    int length()
    {
        return text.length();
    }
    /**
     * Get its offset in the HTML text
     * @return an int
     */
    int offset()
    {
        return this.offset;
    }
    /**
     * Convert to a string for display
     * @return the text of the word
     */
    public String toString()
    {
        return text;
    }
}
