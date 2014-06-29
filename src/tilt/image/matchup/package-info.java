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
 * <p>Once fragments of lines have been identified, based on the peak values 
 * of rectangles of summed pixels as you move down the page on a column by 
 * column basis, there is a residual problem of how to best link up 
 * the line-fragments in one column with those of the next column. Lines can 
 * peter out or start anew, but the existing lines need to be linked together 
 * even if they gradually tilt down the page or curve up or down.</p>
 * <p align="center"><img src="doc-files/grid.png" width="350"></p>
 * <p>The way TILT resolves this problem is to use the dynamic 
 * programming method as used by biologists to optimally align sequences of 
 * amino acids. Although this is an O(N<sup>2</sup>) algorithm, on the scale 
 * of a single page performance is perfectly adequate and it produces an 
 * optimal result every time. The method is to compute a matrix based on the 
 * two adjacent lines. Instead of matching/mismatching amino acids we use
 * the vertical positions of the lines given by the numbers of the rectangles
 * as you move down the page. (To obtain the true pixel-positions they just 
 * need to be multiplied by the vertical scale.) This works for any two 
 * sequences of integers:</p>
 * <p align="center"><img src="doc-files/matrix.png"></p>
 * <p>Each square in the {@link tilt.image.matchup.Matrix} is computed from 
 * the minimum cost of an insertion, deletion or exchange from the squares 
 * immediately above, to the left or diagonally left and up. In cases where 
 * there is no such row or column, the score is made suitably large. In cases 
 * of exchange the cost is 0 if the two numbers are the same, otherwise it is 
 * given by their absolute difference. The cost of an insertion or deletion is 
 * 1. Since the value in each square is computed from preceding ones the final 
 * square in the matrix in the bottom-right corner represents the cost of 
 * getting there, not the cost of <em>leaving</em> it. The final move may 
 * likewise be an insertion, deletion or exchange. To compute it thus requires 
 * an additional row and column, and one of the three emboldened squares in 
 * the bottom right of this extended region will contain the minimal cost of 
 * matching the two columns of integers, depending on whether the final move 
 * was an insertion, deletion or exchange. So in the drawing above the optimal 
 * cost is 4, resulting from an exchange of a 9 and 10.</p>
 * <p>The {@link tilt.image.matchup.Matrix#traceBack() traceback} method then
 * recovers the optimal alignment from the matrix by starting from the final
 * square in this extended region. Sometimes there is ambiguity, but the 
 * traceback method assumes that the exchange {@link tilt.image.matchup.Move} 
 * is preferred when available. The traceback continues until we reach the 
 * origin of the Matrix. The result of this traceback process is shown in the 
 * second half of the drawing above.</p>
 */
package tilt.image.matchup;
