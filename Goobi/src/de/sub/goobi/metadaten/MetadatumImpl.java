package de.sub.goobi.metadaten;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.sql.SQLException;
/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *     		- https://goobi.io
 * 			- https://www.intranda.com
 * 			- https://github.com/intranda/goobi
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang.StringUtils;
import org.geonames.Style;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.goobi.api.display.DisplayCase;
import org.goobi.api.display.Item;
import org.goobi.api.display.enums.DisplayType;
import org.goobi.api.display.helper.NormDatabase;
import org.goobi.api.rest.model.RestMetadata;
import org.goobi.api.rest.model.RestProcess;
import org.goobi.api.rest.request.SearchRequest;
import org.goobi.beans.Process;
import org.goobi.beans.Project;
import org.goobi.production.cli.helper.StringPair;
import org.goobi.vocabulary.Field;
import org.goobi.vocabulary.VocabRecord;
import org.goobi.vocabulary.Vocabulary;

import de.intranda.digiverso.normdataimporter.NormDataImporter;
import de.intranda.digiverso.normdataimporter.dante.DanteImport;
import de.intranda.digiverso.normdataimporter.model.NormData;
import de.intranda.digiverso.normdataimporter.model.NormDataRecord;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.metadaten.search.EasyDBSearch;
import de.sub.goobi.metadaten.search.ViafSearch;
import de.sub.goobi.persistence.managers.MetadataManager;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import ugh.dl.DocStruct;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.MetadataGroupType;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.exceptions.MetadataTypeNotAllowedException;

/**
 * Die Klasse Schritt ist ein Bean für einen einzelnen Schritt mit dessen Eigenschaften und erlaubt die Bearbeitung der Schrittdetails
 * 
 * @author Steffen Hankiewicz
 * @version 1.00 - 10.01.2005
 */

@Data
@Log4j2
public class MetadatumImpl implements Metadatum, SearchableMetadata {
    private Metadata md;
    private int identifier;
    private Prefs myPrefs;
    private Process myProcess;
    private DisplayCase myValues;
    private List<SelectItem> items;
    private List<String> selectedItems;
    private Metadaten bean;
    private DisplayType metadataDisplaytype;

    // include search values

    private String searchValue;
    private String searchOption;

    // name of database
    private String vocabulary; // source
    private String field; // should be unsused

    @Deprecated
    private List<List<NormData>> dataList;
    @Deprecated
    private List<NormData> currentData;

    private List<NormDataRecord> normdataList;
    private NormDataRecord selectedRecord;

    private boolean showNotHits = false;

    private List<String> labelList = new ArrayList<>();
    protected List<SelectItem> possibleItems;
    protected List<String> defaultItems;
    protected String defaultValue;

    private String pagePath;
    private String title;
    private String data;
    private String url;

    private String selectedItem;

    protected List<RestProcess> results;

    // geonames
    private List<Toponym> resultList;
    private int totalResults;

    private Toponym currentToponym;

    // process search
    private SearchRequest searchRequest = new SearchRequest();
    private List<String> possibleFields;

    // viaf data
    private ViafSearch viafSearch = new ViafSearch();
    private EasyDBSearch easydbSearch = new EasyDBSearch();

    // search in vocabulary
    private List<StringPair> vocabularySearchFields;
    private String vocabularyName;
    private List<VocabRecord> records;
    private String vocabularyUrl;
    private VocabRecord selectedVocabularyRecord;

    /**
     * Allgemeiner Konstruktor ()
     */
    public MetadatumImpl(Metadata m, int inID, Prefs inPrefs, Process inProcess, Metadaten bean) {
        this.md = m;
        this.identifier = inID;
        this.myPrefs = inPrefs;
        this.myProcess = inProcess;
        this.bean = bean;
        myValues = new DisplayCase(this.myProcess, this.md.getType());

        metadataDisplaytype = myValues.getDisplayType();
        initializeValues();

    }

    public void searchVocabulary() {

        FacesContext context = FacesContextHelper.getCurrentFacesContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        String contextPath = request.getContextPath();
        String scheme = request.getScheme(); // http
        String serverName = request.getServerName(); // hostname.com
        int serverPort = request.getServerPort(); // 80
        String reqUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
        Client client = ClientBuilder.newClient();
        WebTarget base = client.target(reqUrl);
        WebTarget vocabularyBase = base.path("api").path("vocabulary");
        WebTarget voc = vocabularyBase.path(vocabularyName);

        records= voc.request().post(Entity.json(vocabularySearchFields), new GenericType<List<VocabRecord>>() {
        });
        if (records == null || records.size()==0) {
        	showNotHits = true;
        } else {
        	showNotHits = false;
        }
        
        vocabularyUrl = vocabularyBase.path("records").getUri().toString();
    }

    private void initializeValues() {
        if (myValues.getDisplayType() == DisplayType.select || myValues.getDisplayType() == DisplayType.select1) {
            List<String> selectedItems = new ArrayList<>();
            List<SelectItem> items = new ArrayList<>();
            for (Item i : this.myValues.getItemList()) {
                items.add(new SelectItem(i.getLabel(), i.getValue()));
                if (i.isSelected()) {
                    selectedItems.add(i.getValue());
                }
            }
            setPossibleItems(items);
            setDefaultItems(selectedItems);
            if (selectedItems.size() == 1) {
                setDefaultValue(selectedItems.get(0));
            }
            setSource(myValues.getItemList().get(0).getSource());
            setField(myValues.getItemList().get(0).getField());

        } else {
            if (myValues.getItemList().size() == 1) {
                Item item = myValues.getItemList().get(0);
                if (item.isSelected()) {
                    setDefaultValue(item.getValue());
                }
                setSource(item.getSource());
                setField(item.getField());
            }
        }

        // initialize process search
        initSearch();
        if (metadataDisplaytype == DisplayType.easydb) {
            easydbSearch.prepare();
        } else if (metadataDisplaytype == DisplayType.process) {
            searchRequest.newGroup();
        } else if (metadataDisplaytype == DisplayType.vocabularySearch) {
            vocabularyName = myValues.getItemList().get(0).getSource();
            String fields = myValues.getItemList().get(0).getField();

            String[] fieldNames = fields.split(";");
            vocabularySearchFields = new ArrayList<>();
            for (String fieldname : fieldNames) {
                StringPair sp = new StringPair(fieldname.trim(), "");
                vocabularySearchFields.add(sp);
            }

        } else if (metadataDisplaytype == DisplayType.vocabularyList) {

            FacesContext context = FacesContextHelper.getCurrentFacesContext();

            String vocabularyName = myValues.getItemList().get(0).getSource();

            HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
            String contextPath = request.getContextPath();

            String scheme = request.getScheme(); // http
            String serverName = request.getServerName(); // hostname.com
            int serverPort = request.getServerPort(); // 80

            String reqUrl = scheme + "://" + serverName + ":" + serverPort + contextPath;
            Client client = ClientBuilder.newClient();
            WebTarget base = client.target(reqUrl);
            WebTarget vocabularyBase = base.path("api").path("vocabulary");
            WebTarget voc = vocabularyBase.path(vocabularyName);

            Vocabulary currentVocabulary = voc.request().get(new GenericType<Vocabulary>() {
            });
            currentVocabulary.setUrl(vocabularyBase.path("records").path("" + currentVocabulary.getId()).getUri().toString());
            ArrayList<Item> itemList = new ArrayList<>(currentVocabulary.getRecords().size());
            List<SelectItem> selectItems = new ArrayList<>(currentVocabulary.getRecords().size());
            for (VocabRecord vr : currentVocabulary.getRecords()) {
                for (Field f : vr.getFields()) {
                    if (f.getDefinition().isMainEntry()) {
                        selectItems.add(new SelectItem(f.getValue(), f.getValue()));
                        itemList.add(new Item(f.getValue(), f.getValue(), false, "", ""));
                        break;
                    }
                }
            }
            setPossibleItems(selectItems);
            myValues.setItemList(itemList);
        }

    }

    @Override
    public List<Item> getWert() {
        String value = this.md.getValue();
        if (value != null) {
            for (Item i : myValues.getItemList()) {
                if (i.getValue().equals(value)) {
                    i.setSelected(true);
                } else {
                    i.setSelected(false);
                }
            }
        }
        return this.myValues.getItemList();
    }

    @Override
    public void setWert(String inWert) {
        this.md.setValue(inWert.trim());
    }

    @Override
    public String getTyp() {
        String label = this.md.getType().getLanguage(Helper.getMetadataLanguage());
        if (label == null) {
            label = this.md.getType().getName();
        }
        return label;
    }

    @Override
    public void setTyp(String inTyp) {
        MetadataType mdt = this.myPrefs.getMetadataTypeByName(inTyp);
        this.md.setType(mdt);
    }

    /*
     * ##################################################### ##################################################### ## ## Getter und Setter ##
     * ##################################################### ####################################################
     */

    /******************************************************
     * 
     * new functions for use of display configuration whithin xml files
     * 
     *****************************************************/

    @Override
    public String getOutputType() {
        String type = this.myValues.getDisplayType().name();
        if (type.toLowerCase().startsWith("dante")) {
            return "dante";
        }
        return this.myValues.getDisplayType().name();
    }

    @Override
    public List<SelectItem> getItems() {
        this.items = new ArrayList<>();
        this.selectedItems = new ArrayList<>();
        for (Item i : this.myValues.getItemList()) {
            this.items.add(new SelectItem(i.getLabel()));
            if (i.isSelected()) {
                this.selectedItems.add(i.getLabel());
            }
        }
        return this.items;
    }

    @Override
    public void setItems(List<SelectItem> items) {
        for (Item i : this.myValues.getItemList()) {
            i.setSelected(false);
        }
        String val = "";
        for (SelectItem sel : items) {
            for (Item i : this.myValues.getItemList()) {
                if (i.getLabel().equals(sel.getValue())) {
                    i.setSelected(true);
                    val += i.getValue();
                }
            }
        }
        setWert(val);
    }

    @Override
    public List<String> getSelectedItems() {
        this.selectedItems = new ArrayList<>();
        String values = this.md.getValue();
        if (values != null && values.length() != 0) {
            while (values != null && values.length() != 0) {
                int semicolon = values.indexOf(";");
                if (semicolon != -1) {
                    String value = values.substring(0, semicolon);
                    for (Item i : this.myValues.getItemList()) {
                        if (i.getValue().equals(value)) {
                            this.selectedItems.add(i.getLabel());
                        }
                    }
                    int length = values.length();
                    values = values.substring(semicolon + 1, length);
                } else {
                    for (Item i : this.myValues.getItemList()) {
                        if (i.getValue().equals(values)) {
                            this.selectedItems.add(i.getLabel());
                        }
                    }
                    values = "";
                }
            }
        } else {
            for (Item i : this.myValues.getItemList()) {
                if (i.isSelected()) {
                    values = values + ";" + i.getValue();
                    this.selectedItems.add(i.getLabel());
                }
            }
            if (values != null) {
                setWert(values);
            }
        }
        return this.selectedItems;
    }

    @Override
    public void setSelectedItems(List<String> selectedItems) {

        String val = "";
        for (String sel : selectedItems) {
            for (Item i : this.myValues.getItemList()) {
                if (i.getLabel().equals(sel)) {
                    val += i.getValue() + ";";
                }
            }
        }

        setWert(val);
    }

    @Override
    public String getSelectedItem() {
        String value = this.md.getValue();
        if (value != null && value.length() != 0) {
            for (Item i : this.myValues.getItemList()) {
                if (i.getValue().equals(value)) {
                    return i.getLabel();
                }
            }
        } else {
            for (Item i : this.myValues.getItemList()) {
                if (i.isSelected()) {
                    value = i.getValue();
                    setWert(value);
                    return i.getLabel();
                }
            }
        }
        return "";
    }

    @Override
    public void setSelectedItem(String selectedItem) {

        for (Item i : this.myValues.getItemList()) {
            if (i.getLabel().equals(selectedItem)) {
                setWert(i.getValue());
            }
        }
    }

    @Override
    public void setValue(String value) {
        setWert(value);
    }

    @Override
    public String getValue() {
        return this.md.getValue();
    }

    public List<String> getPossibleDatabases() {
        List<NormDatabase> databaseList = NormDatabase.getAllDatabases();
        List<String> abbrev = new ArrayList<>();
        for (NormDatabase norm : databaseList) {
            abbrev.add(norm.getAbbreviation());
        }
        return abbrev;
    }

    public String getNormdataValue() {
        return md.getAuthorityValue();
    }

    public void setNormdataValue(String normdata) {
        md.setAuthorityValue(normdata);
    }

    public void setNormDatabase(String abbrev) {
        NormDatabase database = NormDatabase.getByAbbreviation(abbrev);
        md.setAuthorityID(database.getAbbreviation());
        md.setAuthorityURI(database.getPath());
    }

    public String getNormDatabase() {
        if (md.getAuthorityURI() != null && md.getAuthorityID() != null) {
            NormDatabase ndb = NormDatabase.getByAbbreviation(md.getAuthorityID());
            return ndb.getAbbreviation();
        } else {
            return null;
        }
    }

    public boolean isNormdata() {
        return md.getType().isAllowNormdata();
    }

    @Override
    public String search() {
        switch (metadataDisplaytype) {
            case dante:
                StringBuilder urlBuilder = new StringBuilder();
                urlBuilder.append("http://api.dante.gbv.de/search?");
                urlBuilder.append("voc=");
                urlBuilder.append(getVocabulary());
                urlBuilder.append("&limit=20");
                urlBuilder.append("&cache=0");
                urlBuilder.append("&type=person,place,concept,groupconcept,corporate");
                urlBuilder.append("&query=");
                urlBuilder.append(getSearchValue());
                normdataList = DanteImport.importNormDataList(urlBuilder.toString(), getLabelList());
                if (normdataList.isEmpty()) {
                    showNotHits = true;
                } else {
                    showNotHits = false;
                }
                break;

            case geonames:
                String credentials = ConfigurationHelper.getInstance().getGeonamesCredentials();
                if (credentials != null) {
                    WebService.setUserName(credentials);
                    ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
                    searchCriteria.setNameEquals(searchValue);
                    searchCriteria.setStyle(Style.FULL);
                    if (StringUtils.isNotBlank(vocabulary)) {
                        Set<String> languageCodes = new HashSet<>();
                        String[] lang = vocabulary.split(";");
                        for (String l : lang) {
                            languageCodes.add(l.trim());
                        }
                        searchCriteria.setCountryCodes(languageCodes);
                    }
                    try {
                        ToponymSearchResult searchResult = WebService.search(searchCriteria);
                        resultList = searchResult.getToponyms();
                        totalResults = searchResult.getTotalResultsCount();
                    } catch (Exception e) {
                        log.error(e);
                    }

                    if (totalResults == 0) {
                        showNotHits = true;
                    } else {
                        showNotHits = false;
                    }
                } else {
                    // deaktiviert
                    Helper.setFehlerMeldung("geonamesList", "Missing data", "mets_geoname_account_inactive");
                }
                break;

            case gnd:
                String val = "";
                if (StringUtils.isBlank(searchOption) && StringUtils.isBlank(searchValue)) {
                    showNotHits = true;
                    return "";
                }
                if (StringUtils.isBlank(searchOption)) {
                    val = "dnb.nid=" + searchValue;
                } else {
                    val = searchValue + " and BBG=" + searchOption;
                }
                URL url = convertToURLEscapingIllegalCharacters("http://normdata.intranda.com/normdata/gnd/woe/" + val);
                String string = url.toString()
                        .replace("Ä", "%C3%84")
                        .replace("Ö", "%C3%96")
                        .replace("Ü", "%C3%9C")
                        .replace("ä", "%C3%A4")
                        .replace("ö", "%C3%B6")
                        .replace("ü", "%C3%BC")
                        .replace("ß", "%C3%9F");
                dataList = NormDataImporter.importNormDataList(string, 3);
                if (dataList.isEmpty()) {
                    showNotHits = true;
                } else {
                    showNotHits = false;
                }
                break;
            case viaf:
                viafSearch.performSearchRequest();
                if (viafSearch.getRecords() == null) {
                    showNotHits = true;
                } else {
                    showNotHits = false;
                }

                break;
            case easydb:
                easydbSearch.search();
                break;
            case process:
                // set our wanted fields
                configureRequest(searchRequest);
                try {
                    this.results = searchRequest.search();
                } catch (SQLException e) {
                    log.error(e);
                }
                break;

            default:
                break;
        }

        return "";
    }

    public String getData() {
        switch (metadataDisplaytype) {
            case dante:
                if (StringUtils.isNotBlank(selectedRecord.getPreferredValue())) {
                    md.setValue(selectedRecord.getPreferredValue());
                }

                for (NormData normdata : selectedRecord.getNormdataList()) {
                    if (normdata.getKey().equals("URI")) {
                        md.setAutorityFile("dante", "https://uri.gbv.de/terminology/dante/", normdata.getValues().get(0).getText());
                    } else if (StringUtils.isBlank(selectedRecord.getPreferredValue()) && CollectionUtils.isEmpty(getLabelList())
                            && normdata.getKey().equals(field)) {
                        String value = normdata.getValues().get(0).getText();
                        md.setValue(filter(value));
                    }
                }

                if (StringUtils.isBlank(selectedRecord.getPreferredValue()) && CollectionUtils.isNotEmpty(getLabelList())) {
                    findPrioritisedMetadata: for (String fieldName : getLabelList()) {
                        for (NormData normdata : currentData) {
                            if (normdata.getKey().equals(fieldName)) {
                                String value = normdata.getValues().get(0).getText();
                                md.setValue(filter(value));
                                break findPrioritisedMetadata;
                            }
                        }
                    }
                }

                normdataList = new ArrayList<>();
                break;
            case geonames:

                md.setValue(currentToponym.getName());
                md.setAutorityFile("geonames", "http://www.geonames.org/", "" + currentToponym.getGeoNameId());
                resultList = new ArrayList<>();
                break;
            case gnd:
                for (NormData normdata : currentData) {
                    if (normdata.getKey().equals("NORM_IDENTIFIER")) {
                        md.setAutorityFile("gnd", "http://d-nb.info/gnd/", normdata.getValues().get(0).getText());
                    } else if (normdata.getKey().equals("NORM_NAME")) {
                        String value = normdata.getValues().get(0).getText();
                        md.setValue(filter(value));
                    }
                }
                dataList = new ArrayList<>();
                break;
            case process:
                initSearch();
                break;
            case viaf:
                viafSearch.getMetadata(md);
                break;
            case easydb:
                easydbSearch.getMetadata(md);
            case vocabularySearch:
                for (Field field : selectedVocabularyRecord.getFields())  {
                    if (field.getDefinition().isMainEntry()) {
                        md.setValue(field.getValue());
                    }
                }
                md.setAutorityFile(vocabulary, vocabularyUrl, vocabularyUrl + "/" + selectedVocabularyRecord.getVocabularyId() + "/" + selectedVocabularyRecord.getId());
            default:
                break;
        }

        return "";
    }

    public String getUrl() {

        if (StringUtils.isBlank(md.getAuthorityValue())) {
            return null;
        } else {
            return md.getAuthorityURI() + md.getAuthorityValue();
        }
    }

    public boolean isShowNoHitFound() {
        return showNotHits;
    }

    public void setSource(String source) {
        vocabulary = source;

        viafSearch.setSource(source);
        easydbSearch.setSearchInstance(source);
    }

    public void setField(String field) {
        if (field.contains(",")) {
            String[] parts = field.split(",");
            for (String part : parts) {
                labelList.add(part.trim());
            }
        } else {
            this.field = field;
        }

        viafSearch.setField(field);
        easydbSearch.setSearchBlock(field);

    }

    public String copy() {
        bean.setCurrentMetadata(md);
        bean.Copy();
        return "";
    }

    public String delete() {
        bean.setCurrentMetadata(md);
        bean.delete();
        return "";
    }

    public String filter(String str) {
        StringBuilder filtered = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char current = str.charAt(i);
            // current != 0x152 && current != 0x156
            if (current != 0x98 && current != 0x9C) {
                filtered.append(current);
            }
        }
        return filtered.toString();
    }

    /**
     * this method is used to disable the edition of the identifier field, the default value is false, so it can be edited
     */

    public boolean isDisableIdentifierField() {
        if (metadataDisplaytype == DisplayType.dante || metadataDisplaytype == DisplayType.easydb) {
            return true;
        }
        return false;
    }

    /**
     * this method is used to disable the edition of the metadata value field, the default value is false
     */

    public boolean isDisableMetadataField() {
        if (metadataDisplaytype == DisplayType.dante || metadataDisplaytype == DisplayType.easydb) {
            return true;
        }
        return false;
    }

    @Override
    public void clearResults() {
        dataList = new ArrayList<>();
        normdataList = new ArrayList<>();
        viafSearch.clearResults();
        easydbSearch.clearResults();
    }

    private URL convertToURLEscapingIllegalCharacters(String string) {
        try {
            String decodedURL = URLDecoder.decode(string, "UTF-8");
            URL url = new URL(decodedURL);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            return uri.toURL();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void initSearch() {
        //        this.searchRequest = new SearchRequest();
        //        List<String> possibleFields = this.getPossibleFields();
        //        SearchQuery query = new SearchQuery(possibleFields.get(0), "", RelationalOperator.EQUAL);
        //        SearchGroup group = new SearchGroup();
        //        group.addFilter(query);
        //        this.searchRequest.addSearchGroup(group);
    }

    @SuppressWarnings("unchecked")
    public void linkProcess(RestProcess rp) {
        Project p = this.getBean().getMyProzess().getProjekt();
        XMLConfiguration xmlConf = ConfigPlugins.getPluginConfig("ProcessPlugin");
        if (xmlConf == null) {
            return;
        }
        HierarchicalConfiguration use = getConfigForProject(p, xmlConf);
        Map<String, List<RestMetadata>> addMetadata = new HashMap<>();
        List<HierarchicalConfiguration> mappings = use.configurationsAt("mapping");
        for (HierarchicalConfiguration mapping : mappings) {
            String from = mapping.getString("[@from]");
            String to = mapping.getString("[@to]");
            List<RestMetadata> fromMeta = rp.getMetadata().get(from);
            if (fromMeta != null) {
                addMetadata.put(to, fromMeta);
            }
        }
        Prefs prefs = this.getBean().getMyPrefs();
        DocStruct ds = this.getBean().getMyDocStruct();
        for (String name : addMetadata.keySet()) {
            try {
                for (RestMetadata rmd : addMetadata.get(name)) {
                    if (!rmd.anyValue()) {
                        continue;
                    }
                    if (name.contains("/")) {
                        String[] split = name.split("/");
                        String group = split[0];
                        String metaName = split[1];
                        MetadataGroupType mgt = prefs.getMetadataGroupTypeByName(group);
                        MetadataGroup addGroup = null;
                        addGroup = new MetadataGroup(mgt);
                        List<Metadata> metaList = addGroup.getMetadataByType(metaName);
                        Metadata md;
                        if (metaList.isEmpty()) {
                            md = new Metadata(prefs.getMetadataTypeByName(metaName));
                            addGroup.addMetadata(md);
                        } else {
                            md = metaList.get(0);
                        }
                        if (rmd.getValue() != null) {
                            md.setValue(rmd.getValue());
                        }
                        if (rmd.getAuthorityID() != null) {
                            md.setAuthorityID(rmd.getAuthorityID());
                        }
                        if (rmd.getAuthorityURI() != null) {
                            md.setAuthorityURI(rmd.getAuthorityURI());
                        }
                        if (rmd.getAuthorityValue() != null) {
                            md.setAuthorityValue(rmd.getAuthorityValue());
                        }
                        ds.addMetadataGroup(addGroup);
                    } else {
                        Metadata md = new Metadata(prefs.getMetadataTypeByName(name));
                        if (rmd.getValue() != null) {
                            md.setValue(rmd.getValue());
                        }
                        if (rmd.getAuthorityID() != null) {
                            md.setAuthorityID(rmd.getAuthorityID());
                        }
                        if (rmd.getAuthorityURI() != null) {
                            md.setAuthorityURI(rmd.getAuthorityURI());
                        }
                        if (rmd.getAuthorityValue() != null) {
                            md.setAuthorityValue(rmd.getAuthorityValue());
                        }
                        ds.addMetadata(md);
                    }
                }
            } catch (MetadataTypeNotAllowedException e) {
                Helper.setFehlerMeldung("Metadata " + name + " not allowed in preferences.", e);
            }
        }
        this.getBean().reloadMetadataList();
    }

    @SuppressWarnings("unchecked")
    private HierarchicalConfiguration getConfigForProject(Project p, XMLConfiguration xmlConf) {
        try {
            HierarchicalConfiguration use = xmlConf.configurationAt("globalConfig");
            List<HierarchicalConfiguration> projectConfs = xmlConf.configurationsAt("projectConfig");
            for (HierarchicalConfiguration projectConf : projectConfs) {
                if (p.getTitel().equals(projectConf.getString("[@project]"))) {
                    use = projectConf;
                    break;
                }
            }
            return use;
        } catch (IllegalArgumentException e) {

        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void configureRequest(SearchRequest req) {

        if (this.getBean() == null) {
            log.error("Metadaten bean null");
            return;
        }
        Project p = this.getBean().getMyProzess().getProjekt();
        XMLConfiguration xmlConf = ConfigPlugins.getPluginConfig("ProcessPlugin");
        if (xmlConf == null) {
            return;
        }
        List<HierarchicalConfiguration> projectConfs = xmlConf.configurationsAt("projectConfig");
        for (HierarchicalConfiguration projectConf : projectConfs) {
            if (p.getTitel().equals(projectConf.getString("[@project]"))) {
                req.setWantedFields(loadWantedFields(projectConf));
                req.setFilterProjects(loadFilterProjects(projectConf));
                return;
            }
        }
        HierarchicalConfiguration global = xmlConf.configurationAt("globalConfig");
        req.setWantedFields(loadWantedFields(global));
        req.setFilterProjects(loadFilterProjects(global));
    }

    private Set<String> loadWantedFields(HierarchicalConfiguration projectConf) {
        Set<String> wantedFields = new HashSet<>();
        String[] wfArr = projectConf.getStringArray("wantedField");
        for (String wf : wfArr) {
            wantedFields.add(wf);
        }
        return wantedFields;
    }

    private List<String> loadFilterProjects(HierarchicalConfiguration projectConf) {
        List<String> projects = new ArrayList<>();
        String[] projArr = projectConf.getStringArray("searchableProject");
        for (String proj : projArr) {
            projects.add(proj);
        }
        return projects;
    }

    public List<String> getPossibleFields() {
        if (this.possibleFields == null) {
            Metadaten mdBean = this.getBean();
            if (mdBean == null) {
                return MetadataManager.getDistinctMetadataNames();
            }
            Project p = mdBean.getMyProzess().getProjekt();
            this.possibleFields = MetadataManager.getDistinctMetadataNames();
            if (StorageProvider.getInstance()
                    .isFileExists(Paths.get(ConfigurationHelper.getInstance().getConfigurationFolder(), "plugin_ProcessPlugin.xml"))) {
                XMLConfiguration xmlConf = ConfigPlugins.getPluginConfig("ProcessPlugin");
                if (xmlConf != null) {
                    HierarchicalConfiguration use = getConfigForProject(p, xmlConf);
                    if (use != null) {
                        List<String> whitelist = Arrays.asList(use.getStringArray("searchableField"));
                        if (!whitelist.isEmpty()) {
                            this.possibleFields = this.possibleFields.stream().filter(x -> whitelist.contains(x)).collect(Collectors.toList());
                        }
                    }
                }
            }
        }
        return this.possibleFields;
    }

    @Override
    public void setSearchInViaf(boolean serachInViaf) {

    }
}
