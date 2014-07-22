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
 * The purpose of this package is to represent the visually identified 
 * parts of the page: the lines and word-shapes. The layout is basically 
 * as follows:</p>
 * <p align="center"><img src="doc-files/page.png" width="200"></p>
 * <p>The {@link tilt.image.page.Page package-info} class allows the calculation of useful parameters
 * for later processing: the median line-depth (distance between baselines)
 * and the minimum word-gap. The latter is the basis for word-recognition
 * in the image. See 
 * <a href="http://bltilt.blogspot.com.au/2014/07/getting-word-spacing-right.html">
 * Getting word-spacing right </a></p>
 * <p>There is also the problem of merging adjacent shapes that align to
 * the same word, and splitting shapes that align to different words. These
 * operations are handled by the Split and Merge classes.</p>
 */
 package tilt.image.page;
