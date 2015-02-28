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
 * <h4>Matching up fragments of lines in adjacent columns</h4>
 * <p>Once the y-positions of lines have been identified on the left
 * and right sides we match them up using a basic greedy-type algorithm.
 * The previous version of Matrix used Needleman and WUnsch alignment
 * to join the dots on the left to those on the right. The problem with 
 * this is that links could jump between lines if the overall global
 * alignment was better that way. The new method just looks for 
 * the closest y-positions on the left and the right, and joins them
 * up. Once such a join has been made it will not allow any other 
 * left-to-right joins to cross over it. Also, if the distance between
 * a point on the left and right is greater than some multiple of the
 * the current average line-depth it will not join up any more lines.
 * The points left over on the left become "deletions" and the unattached
 * ones on the right become "insertions".</p>
 */
package tilt.image.matchup;
