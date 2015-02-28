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

/**
 * <p>This package is an adaptation of Esko Ukkonen's diff algorithm in 
 * Information and Control 64, 100-118, 1985. Originally designed for the 
 * alignment of two sets of characters or strings, it is here adapted to the 
 * alignment of precise pixel-widths in an image and approximated pixel-widths 
 * in a text. The original algorithm found the optimal alignment between two 
 * texts using the insert, delete and exchange operations via a set of 
 * diagonals. This adaptation uses the same approach, but adds 
 * two new moves: h-extension and v-extension, which allows a number in one
 * set to be aligned with more than one in the other set, so 25, 35 could be 
 * aligned with 60. This reflects the problem in recognising manuscript texts
 * that sometimes two words are joined in the page-image, or one word split
 * apart into two or more pieces. Using this method, and an existing 
 * transcription, shapes of words identified in the image can be aligned with 
 * the correct text, and the shapes themselves merged or split as needed.</p>
 * <p align="center"><img src="doc-files/alignment.png" width="500"></p>
 * <p>This algorithm is central to the correct function of the TILT 
 * application.</p>
 */
package tilt.align;
