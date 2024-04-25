/*
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

//
package org.goobi.pagination;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.sub.goobi.AbstractTest;

public class IntegerSequenceTest extends AbstractTest {

    @Test
    public void sequenceWithoutRangeIsEmpty() {

        IntegerSequence seq = new IntegerSequence();
        assertTrue(seq.isEmpty());

    }

    @Test(expected = IllegalArgumentException.class)
    public void throwIllegalArgumentExceptionWhenStartIsBelowEnd() {

        new IntegerSequence(5, 1);

    }

    @Test
    public void sequenceFromOneToFiveHasFiveElements() {

        IntegerSequence seq = new IntegerSequence(1, 5);
        assertEquals(5, seq.size());

    }

    @Test
    public void sequenceIsStrictlyIncreasing() {

        IntegerSequence seq = new IntegerSequence(1, 5);

        int last = 0;
        for (int i : seq) {
            assertTrue(i > last);
            last = i;
        }

    }

    @Test
    public void differenceBetweenElementsEqualsIncrement() {

        IntegerSequence seq = new IntegerSequence(1, 5, 2);

        int last = -1;
        for (int i : seq) {
            assertTrue("Difference between elements should be equal to increment (2)", (last + 2) == i);
            last = i;
        }

    }

}
