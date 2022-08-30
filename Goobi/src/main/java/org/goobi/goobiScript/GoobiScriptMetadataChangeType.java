/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *             - https://goobi.io
 *             - https://www.intranda.com
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
package org.goobi.goobiScript;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.production.enums.GoobiScriptResultType;
import org.goobi.production.enums.LogType;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.extern.log4j.Log4j2;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;

@Log4j2
public class GoobiScriptMetadataChangeType extends AbstractIGoobiScript implements IGoobiScript {
    // action:metadataTypeChange position:work oldType:singleDigCollection newType:DDC

    @Override
    public String getAction() {
        return "metadataChangeType";
    }

    @Override
    public String getSampleCall() {
        StringBuilder sb = new StringBuilder();
        addNewActionToSampleCall(sb, "This GoobiScript allows to change the type of an existing metadata.");
        addParameterToSampleCall(sb, "oldType", "old",
                "Define the current type that shall be changed. Use the internal name here (e.g. `TitleDocMain`), not the translated display name (e.g. `Main title`).");
        addParameterToSampleCall(sb, "newType", "new", "Define the type that shall be used as new type. Use the internal name here as well.");
        addParameterToSampleCall(sb, "position", "work",
                "Define where in the hierarchy of the METS file the searched term shall be replaced. Possible values are: `work` `top` `child` `any` `physical`");
        addParameterToSampleCall(sb, "ignoreErrors", "true",
                "Define if the further processing shall be cancelled for a Goobi process if an error occures (`false`) or if the processing should skip errors and move on (`true`).\\n# This is especially useful if the the value `any` was selected for the position.");
        return sb.toString();
    }

    @Override
    public List<GoobiScriptResult> prepare(List<Integer> processes, String command, Map<String, String> parameters) {
        super.prepare(processes, command, parameters);

        if (StringUtils.isBlank(parameters.get("oldType"))) {
            Helper.setFehlerMeldungUntranslated("goobiScriptfield", "Missing parameter: ", "oldType");
            return new ArrayList<>();
        }

        if (StringUtils.isBlank(parameters.get("newType"))) {
            Helper.setFehlerMeldungUntranslated("goobiScriptfield", "Missing parameter: ", "newType");
            return new ArrayList<>();
        }

        if (parameters.get("position") == null || parameters.get("position").equals("")) {
            Helper.setFehlerMeldungUntranslated("goobiScriptfield", "Missing parameter: ", "position");
            return new ArrayList<>();
        }

        // add all valid commands to list
        List<GoobiScriptResult> newList = new ArrayList<>();
        for (Integer i : processes) {
            GoobiScriptResult gsr = new GoobiScriptResult(i, command, parameters, username, starttime);
            newList.add(gsr);
        }
        return newList;
    }

    @Override
    public void execute(GoobiScriptResult gsr) {
        Map<String, String> parameters = gsr.getParameters();
        Process p = ProcessManager.getProcessById(gsr.getProcessId());
        gsr.setProcessTitle(p.getTitel());
        gsr.setResultType(GoobiScriptResultType.RUNNING);
        gsr.updateTimestamp();
        try {
            Fileformat ff = p.readMetadataFile();
            // first get the top element
            DocStruct ds = ff.getDigitalDocument().getLogicalDocStruct();
            DocStruct physical = ff.getDigitalDocument().getPhysicalDocStruct();

            // find the right elements to adapt
            List<DocStruct> dsList = new ArrayList<>();
            switch (parameters.get("position")) {

                // just the anchor element
                case "top":
                    if (ds.getType().isAnchor()) {
                        dsList.add(ds);
                    } else {
                        gsr.setResultMessage("Error while adapting metadata for top element, as top element is no anchor");
                        gsr.setResultType(GoobiScriptResultType.ERROR);
                        return;
                    }
                    break;

                    // fist the first child element
                case "child":
                    if (ds.getType().isAnchor()) {
                        dsList.add(ds.getAllChildren().get(0));
                    } else {
                        gsr.setResultMessage("Error while adapting metadata for child, as top element is no anchor");
                        gsr.setResultType(GoobiScriptResultType.ERROR);
                        return;
                    }
                    break;

                    // any element in the hierarchy
                case "any":
                    dsList.add(ds);
                    dsList.addAll(ds.getAllChildrenAsFlatList());
                    if (physical != null) {
                        dsList.add(physical);
                    }
                    break;
                case "physical":
                    if (physical != null) {
                        dsList.add(physical);
                    }
                    break;

                    // default "work", which is the first child or the main top element if it is not an anchor
                default:
                    if (ds.getType().isAnchor()) {
                        dsList.add(ds.getAllChildren().get(0));
                    } else {
                        dsList.add(ds);
                    }
                    break;
            }

            // check if errors shall be ignored
            boolean ignoreErrors = getParameterAsBoolean("ignoreErrors");

            String oldMetadataType = parameters.get("oldType");
            String newMetadataType = parameters.get("newType");

            boolean changed = changeMetadataType(dsList, oldMetadataType, newMetadataType, p.getRegelsatz().getPreferences(), ignoreErrors);
            if (changed) {
                p.writeMetadataFile(ff);
                Thread.sleep(2000);
                Helper.addMessageToProcessLog(p.getId(), LogType.DEBUG,
                        "Metadata type changed using GoobiScript: from " + oldMetadataType + " to " + newMetadataType, username);
            }
            log.info("Metadata type changed using GoobiScript for process with ID {} from {} to {} ", p.getId(), oldMetadataType, newMetadataType);

            gsr.setResultMessage("Metadata changed successfully.");
            gsr.setResultType(GoobiScriptResultType.OK);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e1) {
            log.error("Problem while changing the metadata using GoobiScript for process with id: " + p.getId(), e1);
            gsr.setResultMessage("Error while changing metadata: " + e1.getMessage());
            gsr.setResultType(GoobiScriptResultType.ERROR);
            gsr.setErrorText(e1.getMessage());
        }

        gsr.updateTimestamp();
    }

    /**
     * Change the type of all occurrences of a metadata
     * 
     * @param dsList as List of structural elements to use
     * @param oldMetadataType old type
     * @param newMetadataType new type
     * @param prefs ruleset
     * @return true, if the type was changed, false otherwise
     * 
     * @throws MetadataTypeNotAllowedException if the new type does not exist or is not allowed
     */
    private boolean changeMetadataType(List<DocStruct> dsList, String oldMetadataType, String newMetadataType, Prefs prefs, boolean ignoreErrors)
            throws MetadataTypeNotAllowedException {
        boolean metadataChanged = false;

        for (DocStruct ds : dsList) {
            // search for all metadata with type of oldMetadataType
            List<? extends Metadata> mdList = ds.getAllMetadataByType(prefs.getMetadataTypeByName(oldMetadataType));
            if (mdList == null || mdList.isEmpty()) {
                continue;
            }
            MetadataType type = prefs.getMetadataTypeByName(newMetadataType);

            // for each metadata create new metadata with new type
            for (Metadata oldMd : mdList) {
                try {
                    Metadata newMd = new Metadata(type);
                    // copy value from existing metadata
                    newMd.setValue(oldMd.getValue());
                    newMd.setAutorityFile(oldMd.getAuthorityID(), oldMd.getAuthorityURI(), oldMd.getAuthorityValue());
                    // add all new metadata
                    ds.addMetadata(newMd);
                    // delete oldMetadata from ds
                    ds.removeMetadata(oldMd);
                    metadataChanged = true;
                } catch (MetadataTypeNotAllowedException e) {
                    if (!ignoreErrors) {
                        throw e;
                    }
                }
            }
        }
        return metadataChanged;
    }

}
