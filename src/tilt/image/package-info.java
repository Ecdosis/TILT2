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
 * This package handles everything directly related to the manipulation of 
 * images, and preparing them for text-to-image linking. The 
 * <ul><li>{@link tilt.image.Blob} class identifies connected pixels in 
 * B&amp;W images.</li> <li>The {@link tilt.image.Border} class identifies 
 * blobs close to the edges that can be safely deleted.</li><li>The {@link
 * tilt.image.FastConvexHull} algorithm selects those points that are
 * outliers of a set of points representing something, and draws a
 * <em>convex</em> polygon around them.</li><li>The {@link
 * tilt.image.FindLines} class identifies lines in various kinds of textual
 * images.</li><li>The {@link tilt.image.Picture} class manages the
 * picture, originally fetch from a URL, in its many formats: B&amp; W,
 * original, with lines etc.</li><li>{@link tilt.image.Picture}s are stored 
 * in the {@link tilt.image.PictureRegistry}, which manages the temporary 
 * files associated with each image type. Images are regularly deleted when 
 * no longer required and are only loaded into memory when needed.</li><li>The
 * {@link tilt.image.RemoveNoise} class supervises the removal of noise in
 * the borders and anomalous blobs in the inner border.</li> <li>The {@link
 * tilt.image.XCompare} class is a comparison method needed by {@link
 * tilt.image.FastConvexHull}.</li></ul>
 */
package tilt.image;
