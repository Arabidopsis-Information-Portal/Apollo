package org.bbop.apollo.gwt.client;

import com.google.gwt.cell.client.*;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.bbop.apollo.gwt.client.dto.OrganismInfo;
import org.bbop.apollo.gwt.client.event.OrganismChangeEvent;
import org.bbop.apollo.gwt.client.event.OrganismChangeEventHandler;
import org.bbop.apollo.gwt.client.resources.TableResources;
import org.bbop.apollo.gwt.client.rest.OrganismRestService;
import org.gwtbootstrap3.client.ui.*;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.constants.IconType;

import java.util.Comparator;
import java.util.List;

/**
 * Created by ndunn on 12/17/14.
 */
public class OrganismPanel extends Composite {


    interface OrganismBrowserPanelUiBinder extends UiBinder<Widget, OrganismPanel> {
    }

    private OrganismInfo selectedOrganismInfo;

    private static OrganismBrowserPanelUiBinder ourUiBinder = GWT.create(OrganismBrowserPanelUiBinder.class);
    @UiField
    org.gwtbootstrap3.client.ui.TextBox organismName;
//    @UiField
//    InputGroupAddon trackCount;
    @UiField
    InputGroupAddon annotationCount;
    @UiField
    org.gwtbootstrap3.client.ui.TextBox sequenceFile;

    DataGrid.Resources tablecss = GWT.create(TableResources.TableCss.class);
    @UiField(provided = true)
    DataGrid<OrganismInfo> dataGrid = new DataGrid<OrganismInfo>(10, tablecss);
    @UiField
    Button newButton;
    @UiField
    Button createButton;
    @UiField
    Button cancelButton;
    @UiField
    Button deleteButton;
    @UiField
    InputGroupAddon validDirectory;

    private ListDataProvider<OrganismInfo> dataProvider = new ListDataProvider<>();
    private final SingleSelectionModel<OrganismInfo> singleSelectionModel = new SingleSelectionModel<>();

    public OrganismPanel() {
        initWidget(ourUiBinder.createAndBindUi(this));

        TextColumn<OrganismInfo> organismNameColumn = new TextColumn<OrganismInfo>() {
            @Override
            public String getValue(OrganismInfo employee) {
                return employee.getName();
            }
        };
        organismNameColumn.setSortable(true);

        Column<OrganismInfo, Number> annotationsNameColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumFeatures();
            }
        };
        annotationsNameColumn.setSortable(true);
        Column<OrganismInfo, Number> sequenceColumn = new Column<OrganismInfo, Number>(new NumberCell()) {
            @Override
            public Integer getValue(OrganismInfo object) {
                return object.getNumSequences();
            }
        };
        sequenceColumn.setSortable(true);

        Annotator.eventBus.addHandler(OrganismChangeEvent.TYPE, new OrganismChangeEventHandler() {
            @Override
            public void onOrganismChanged(OrganismChangeEvent organismChangeEvent) {
                dataProvider.setList(organismChangeEvent.organismInfoList);
            }
        });

        dataGrid.setLoadingIndicator(new HTML("Calculating Annotations ... "));

        dataGrid.addColumn(organismNameColumn, "Name");
        dataGrid.addColumn(annotationsNameColumn, "Annotations");
        dataGrid.addColumn(sequenceColumn, "Sequences");


        singleSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                selectedOrganismInfo = singleSelectionModel.getSelectedObject();
                setSelectedInfo(selectedOrganismInfo);
                cancelButton.setVisible(false);
                createButton.setVisible(false);
            }
        });
        dataGrid.setSelectionModel(singleSelectionModel);

        dataProvider.addDataDisplay(dataGrid);

        List<OrganismInfo> trackInfoList = dataProvider.getList();

        ColumnSortEvent.ListHandler<OrganismInfo> sortHandler = new ColumnSortEvent.ListHandler<OrganismInfo>(trackInfoList);
        dataGrid.addColumnSortHandler(sortHandler);
        sortHandler.setComparator(organismNameColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        sortHandler.setComparator(annotationsNameColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumFeatures() - o2.getNumFeatures();
            }
        });
        sortHandler.setComparator(sequenceColumn, new Comparator<OrganismInfo>() {
            @Override
            public int compare(OrganismInfo o1, OrganismInfo o2) {
                return o1.getNumSequences() - o2.getNumSequences();
            }
        });

    }

    public void setSelectedInfo(OrganismInfo organismInfo){
        if(organismInfo==null) {
           setNoSelection()   ;
            return;
        }
        organismName.setText(organismInfo.getName());
        organismName.setEnabled(true);
        sequenceFile.setText(organismInfo.getDirectory());
        sequenceFile.setEnabled(true);
        annotationCount.setText(organismInfo.getNumFeatures().toString());
        deleteButton.setVisible(true);
        deleteButton.setEnabled(true);
        if(organismInfo.getValid()==null){
            validDirectory.setIcon(IconType.QUESTION);
            validDirectory.setColor("Red");
        }
        else
        if(organismInfo.getValid()){
            validDirectory.setIcon(IconType.CHECK);
            validDirectory.setColor("Green");
        }
        else{
            validDirectory.setIcon(IconType.XING);
            validDirectory.setColor("Red");
        }
        validDirectory.setVisible(true);
    }

    private void setNoSelection() {
        organismName.setText("");
        organismName.setEnabled(false);
        sequenceFile.setText("");
        sequenceFile.setEnabled(false);
        annotationCount.setText("");
        validDirectory.setVisible(false);
        deleteButton.setVisible(false);
    }


    private class UpdateInfoListCallback implements  RequestCallback{

        private boolean clearSelections = false ;

        public UpdateInfoListCallback(){
            this(false);
        }

        public UpdateInfoListCallback(boolean clearSelections){
            this.clearSelections = clearSelections ;
        }

        @Override
        public void onResponseReceived(Request request, Response response) {
            GWT.log("recived response: "+response.getText());
            List<OrganismInfo> organismInfoList = OrganismRestService.convertJSONStringToOrganismInfoList(response.getText());
            GWT.log("converted responsde : " + organismInfoList.size());
            dataGrid.setSelectionModel(singleSelectionModel);
            GWT.log("clearing selection") ;
            if(clearSelections){
                clearSelections();
            }
            setDefaultButtonState();
            OrganismChangeEvent organismChangeEvent = new OrganismChangeEvent(organismInfoList);
            Annotator.eventBus.fireEvent(organismChangeEvent);
        }

        @Override
        public void onError(Request request, Throwable exception) {
            Window.alert("problem handling organism: "+exception);
        }
    }

    public void clearSelections(){
        selectedOrganismInfo = null ;
        singleSelectionModel.clear();
        organismName.setText("");
        sequenceFile.setText("");
        validDirectory.setVisible(false);
        newButton.setEnabled(false);
    }

    @UiHandler("newButton")
    public void handleAddNewOrganism(ClickEvent clickEvent) {
//        clearSelections();
//        dataGrid.setSelectionModel(new NoSelectionModel<OrganismInfo>());
        setNewOrganismButtonState();
//        selectedOrganismInfo.setName(organismName.getText());
//        updateOrganismInfo();
    }

    @UiHandler("createButton")
    public void handleSaveNewOrganism(ClickEvent clickEvent) {
        setDefaultButtonState();
        OrganismInfo organismInfo = new OrganismInfo();
        organismInfo.setName(organismName.getText());
        organismInfo.setDirectory(sequenceFile.getText());
        createButton.setEnabled(false);
        createButton.setText("Processing");
        OrganismRestService.createOrganism(new UpdateInfoListCallback(true), organismInfo);
    }

    @UiHandler("cancelButton")
    public void handleCancelNewOrganism(ClickEvent clickEvent) {
        organismName.setText("");
        sequenceFile.setText("");
        dataGrid.setSelectionModel(singleSelectionModel);
        newButton.setEnabled(true);
        setSelectedInfo(selectedOrganismInfo);
        setDefaultButtonState();
    }

    @UiHandler("deleteButton")
    public void handleDeleteOrganism(ClickEvent clickEvent) {
        if(selectedOrganismInfo==null) return ;

        if(Window.confirm("Delete organism: "+selectedOrganismInfo.getName())){
            deleteButton.setEnabled(false);
            OrganismRestService.deleteOrganism(new UpdateInfoListCallback(true),selectedOrganismInfo);
        }
    }


    @UiHandler("organismName")
    public void handleOrganismNameChange(ChangeEvent changeEvent) {
        if(selectedOrganismInfo==null) return ;
        selectedOrganismInfo.setName(organismName.getText());
        updateOrganismInfo();
    }

    @UiHandler("sequenceFile")
    public void handleOrganismDirectory(ChangeEvent changeEvent) {
        if(selectedOrganismInfo==null) return ;
        selectedOrganismInfo.setDirectory(sequenceFile.getText());
        updateOrganismInfo();
    }


    private void updateOrganismInfo() {
        OrganismRestService.updateOrganismInfo(selectedOrganismInfo);
    }


    public List<OrganismInfo> reload() {
        List<OrganismInfo> trackInfoList = dataProvider.getList();
        OrganismRestService.loadOrganisms(trackInfoList);

        return trackInfoList;
    }

    public void setDefaultButtonState(){
        if(singleSelectionModel.getSelectedSet().size()>0){
            newButton.setEnabled(true);
            newButton.setVisible(true);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(true);
        }
        else{
            newButton.setEnabled(true);
            newButton.setVisible(true);
            createButton.setVisible(false);
            cancelButton.setVisible(false);
            deleteButton.setVisible(false);
        }
    }

    public void setNewOrganismButtonState(){
        createButton.setText("Create Organism");
        newButton.setEnabled(false);
        newButton.setVisible(true);
        createButton.setVisible(true);
        createButton.setEnabled(true);
        cancelButton.setVisible(true);
        cancelButton.setEnabled(true);
        deleteButton.setVisible(false);

        organismName.setText("");
        sequenceFile.setText("");
        validDirectory.setVisible(false);
        organismName.setEnabled(true);
        sequenceFile.setEnabled(true);
    }

    public void setThinkingInterface(){
    }

    public void unsetThinkingInterface(){
    }
}