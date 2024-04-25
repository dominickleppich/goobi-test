package de.sub.goobi.statistik;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi-workflow
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.util.Iterator;
import java.util.List;

import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.beans.Usergroup;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DefaultPieDataset;

public class StatistikBenutzergruppen {

    public static Dataset getDiagramm(List<Process> inProzesse) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Process proz : inProzesse) {
            Step step = proz.getAktuellerSchritt();
            /* wenn wirklich ein aktueller Schritt zurückgegeben wurde */
            if (step != null) {
                /* von dem Schritt alle verantwortlichen Benutzergruppen ermitteln und im Diagramm erfassen */
                for (Iterator<Usergroup> iter2 = step.getBenutzergruppenList().iterator(); iter2.hasNext();) {
                    Usergroup group = iter2.next();
                    if (dataset.getIndex(group.getTitel()) != -1) {
                        dataset.setValue(group.getTitel(), dataset.getValue(group.getTitel()).intValue() + 1d);
                    } else {
                        dataset.setValue(group.getTitel(), 1);
                    }
                }

            }
        }
        return dataset;
    }

}
