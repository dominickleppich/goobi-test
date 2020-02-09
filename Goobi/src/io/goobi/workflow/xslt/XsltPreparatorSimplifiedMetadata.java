package io.goobi.workflow.xslt;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi
 * 			- http://digiverso.com
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.helper.StringPair;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ExportFileException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.metadaten.MetadatenHelper;
import de.sub.goobi.metadaten.MetadatenImagesHelper;
import de.sub.goobi.persistence.managers.MetadataManager;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.WriteException;

/**
 * This class provides a simplified export of all metadata into a xml file
 * 
 * @author Steffen Hankiewicz
 * 
 */
public class XsltPreparatorSimplifiedMetadata implements IXsltPreparator {
	private static final Logger logger = LogManager.getLogger(XsltPreparatorSimplifiedMetadata.class);

	private static Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");

	private static final SimpleDateFormat dateConverter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	/**
	 * This method exports the METS metadata as xml to a given directory
	 * 
	 * @param p           the process to export
	 * @param destination the destination to write the file
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ExportFileException
	 */

	public void startExport(Process p, String destination) throws FileNotFoundException, IOException {
		startExport(p, new FileOutputStream(destination), null);
	}

	public void startExport(Process p, Path dest) throws FileNotFoundException, IOException {
		startExport(p, new FileOutputStream(dest.toFile()), null);
	}

	/**
	 * This method exports the METS metadata as xml to a given stream.
	 * 
	 * @param process the process to export
	 * @param os      the OutputStream to write the contents to
	 * @throws IOException
	 * @throws ExportFileException
	 */
	@Override
	public void startExport(Process process, OutputStream os, String xslt) throws IOException {
		try {
			Document doc = createDocument(process, true);

			XMLOutputter outp = new XMLOutputter();
			outp.setFormat(Format.getPrettyFormat());

			outp.output(doc, os);
			os.close();

		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * This method creates a new xml document with process metadata
	 * 
	 * @param process the process to export
	 * @return a new xml document
	 * @throws ConfigurationException
	 */
	public Document createDocument(Process process, boolean addNamespace) {

		Element processElm = new Element("process");
		Document doc = new Document(processElm);

		processElm.setAttribute("processID", String.valueOf(process.getId()));

		Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");
		processElm.setNamespace(xmlns);
		// namespace declaration
		if (addNamespace) {
			Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			processElm.addNamespaceDeclaration(xsi);
			Attribute attSchema = new Attribute("schemaLocation", "http://www.goobi.io/logfile" + " XML-logfile.xsd",
					xsi);
			processElm.setAttribute(attSchema);
		}

		// process information
		ArrayList<Element> processElements = new ArrayList<>();
		Element processTitle = new Element("title", xmlns);
		processTitle.setText(process.getTitel());
		processElements.add(processTitle);

		Element project = new Element("project", xmlns);
		project.setText(process.getProjekt().getTitel());
		processElements.add(project);

		Element date = new Element("time", xmlns);
		date.setAttribute("type", "creation date");
		date.setText(String.valueOf(process.getErstellungsdatum()));
		processElements.add(date);

		Element ruleset = new Element("ruleset", xmlns);
		ruleset.setText(process.getRegelsatz().getDatei());
		processElements.add(ruleset);

		// add all important mets content
		Element mets = new Element("metsNode", xmlns);
		try {
			Prefs myPrefs = process.getRegelsatz().getPreferences();
			Fileformat ff = process.readMetadataFile();
			if (ff != null) {
				DigitalDocument dd = ff.getDigitalDocument();
				DocStruct logicalTopstruct = dd.getLogicalDocStruct();
				mets.setAttribute("type", logicalTopstruct.getType().getNameByLanguage(Helper.getMetadataLanguage()));
				addMetadataAndChildElements(logicalTopstruct, mets);
			}
		} catch (Exception e) {
			logger.error("Error while creating a pdf file", e);
		}
		processElements.add(mets);
		processElm.setContent(processElements);
		return doc;
	}

	/**
	 * add a node for each docstruct and add all metadata
	 * 
	 * @param parentStruct the parent structure element to analyze
	 * @param parentNode the parent node where to add the subnodes to
	 */
	private void addMetadataAndChildElements(DocStruct parentStruct, Element parentNode) {
		if (parentStruct.getAllMetadata() != null) {
			for (Metadata md : parentStruct.getAllMetadata()) {
				if (md.getValue() != null && md.getValue().length() > 0) {
					Element metadata = new Element("metadata", xmlns);
					metadata.setAttribute("name", md.getType().getNameByLanguage(Helper.getMetadataLanguage()));
					metadata.addContent(md.getValue());
					parentNode.addContent(metadata);
				}
			}
		}
		if (parentStruct.getAllChildren()!=null) {
			for (DocStruct ds : parentStruct.getAllChildren()) {
				Element node = new Element("metsNode", xmlns);
				node.setAttribute("type", ds.getType().getNameByLanguage(Helper.getMetadataLanguage()));
				addMetadataAndChildElements(ds, node);
				parentNode.addContent(node);
			}
		}
	}

	/**
	 * This method exports the production metadata for a list of processes as a
	 * single file to a given stream.
	 * 
	 * @param processList
	 * @param outputStream
	 * @param xslt
	 */
	public void startExport(List<Process> processList, OutputStream outputStream, String xslt) {
		Document answer = new Document();
		Element root = new Element("processes");
		answer.setRootElement(root);
		Namespace xmlns = Namespace.getNamespace("http://www.goobi.io/logfile");

		Namespace xsi = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		root.addNamespaceDeclaration(xsi);
		root.setNamespace(xmlns);
		Attribute attSchema = new Attribute("schemaLocation", "http://www.goobi.io/logfile" + " XML-logfile.xsd", xsi);
		root.setAttribute(attSchema);
		for (Process p : processList) {
			Document doc = createDocument(p, false);
			Element processRoot = doc.getRootElement();
			processRoot.detach();
			root.addContent(processRoot);
		}

		XMLOutputter outp = new XMLOutputter();
		outp.setFormat(Format.getPrettyFormat());
		try {
			outp.output(answer, outputStream);
		} catch (IOException e) {
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					outputStream = null;
				}
			}
		}
	}
}
