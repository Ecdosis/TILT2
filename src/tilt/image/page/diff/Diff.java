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

package tilt.image.page.diff;

/**
 * Store the difference between one version of text and another
 * @author desmond
 */
public class Diff
{
    /** offset in the OLD (B) version */
    public int oldOffset;
	/** offset in the NEW (A) version */
    public int newOffset;
	/** its old version length */
    public int oldLen;
	/** its new version length */
    public int newLen;
    /** kind of Diff */
    public DiffKind kind;
    /**
     * Create a Diff
     * @param oldOffset its offset in the old (B) version
     * @param newOffset its offset in the new (A) version
     * @param oldLen the length of the old text in this diff
     * @param newLen the length of the new text in this diff
     */
    Diff( int oldOffset, int newOffset, int oldLen, int newLen, DiffKind kind )
    {
        this.oldOffset = oldOffset;
        this.newOffset = newOffset;
        this.oldLen = oldLen;
        this.newLen = newLen;
        this.kind = kind;
    }
    /**
    * Get the end of the diff's old version
    * @return an integer
    */
    public int oldEnd()
    {
        return oldOffset+ oldLen;
    }
   /**
    * Get the end of the diff's new version
    * @return an integer
    */
    public int newEnd()
    {
        return newOffset+newLen;
    }
   /**
    * Get the length of the diff's old version
    * @return an integer
    */
    public int oldLen()
    {
        return oldLen;
    }
   /**
    * Get the length of the diff's new version
    * @return an integer
    */
    public int newLen()
    {
        return newLen;
    }
   /**
    * Get the old offset of the Diff
    * @return an integer
    */
    public int oldOff()
    {
        return oldOffset;
    }
   /**
    * Get the new offset of the Diff
    * @return an integer
    */
    public int newOff()
    {
        return newOffset;
    }
   /**
    * Get the Diff's kind
    * @return a DiffKind
    */
    public DiffKind getKind()
    {
        return this.kind;
    }
    public String toString()
    {
        return "["+oldOffset+","+newOffset+","+oldLen+","+newLen+":"+kind+"]";
    }
}
