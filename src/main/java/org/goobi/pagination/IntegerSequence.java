/**
 * This file is part of the Goobi Application - a Workflow tool for the support of
 * mass digitization.
 *
 * Visit the websites for more information.
 *             - https://goobi.io
 *             - https://www.intranda.com
 *
 * Copyright 2011, Center for Retrospective Digitization, Göttingen (GDZ),
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place,
 * Suite 330, Boston, MA 02111-1307 USA
 */

package org.goobi.pagination;

import java.util.ArrayList;

public class IntegerSequence extends ArrayList<Integer> {
    private static final long serialVersionUID = 8122111085741961640L;

    public IntegerSequence() {
    }

    public IntegerSequence(int start, int end) {
        generateElements(start, end, 1);
    }

    public IntegerSequence(int start, int end, int increment) {
        generateElements(start, end, increment);
    }

    private void generateElements(int start, int end, int increment) {
        if (start > end) {
            throw new IllegalArgumentException("Sequence end value cannot be smaller than start value.");
        }

        this.ensureCapacity(end - start);
        for (int i = start; i <= end; i = (i + increment)) {
            this.add(Integer.valueOf(i));
        }
    }
}
