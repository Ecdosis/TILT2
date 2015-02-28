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
package tilt.exception;

/**
 *  Something went wrong with an image
 *  @author desmond
 */
public class AlignException extends TiltException
{
    /**
     * Create a new AlignExeption from scratch
     * @param message the message it is to contain
     */
    public AlignException( String message )
    {
        super( message );
    }
    /**
     * For wrapping another exception
     * @param e the other exception
     */
    public AlignException( Exception e )
    {
        super( e );
    }
}
