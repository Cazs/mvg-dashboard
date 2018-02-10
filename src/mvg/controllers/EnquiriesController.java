package mvg.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import mvg.auxilary.IO;
import mvg.managers.*;
import mvg.managers.EnquiryManager;
import mvg.model.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class EnquiriesController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Enquiry> tblEnquiries;
    @FXML
    private TableColumn colId, colClient, colEnquiry, colAddress,
            colDestination,colTripType,colCreator,colDate,colDateLogged,colOther,colAction;
    
    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries view..");

        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No users were found in the database.", IO.TAG_ERROR);
            return;
        }
        if( EnquiryManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No enquiries were found in the database.", IO.TAG_WARN);
            return;
        }

        colId.setMinWidth(100);
        colId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("client_name"));
        colEnquiry.setCellValueFactory(new PropertyValueFactory<>("enquiry"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("pickup_location"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        colTripType.setCellValueFactory(new PropertyValueFactory<>("trip_type"));
        /*CustomTableViewControls.makeEditableTableColumn(colEnquiry, TextFieldTableCell.forTableColumn(), 80, "enquiry", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colAddress, TextFieldTableCell.forTableColumn(), 120, "pickup_location", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colDestination, TextFieldTableCell.forTableColumn(), 120, "destination", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colTripType, TextFieldTableCell.forTableColumn(), 80, "trip_type", "/enquiries");*/
        colCreator.setMinWidth(100);
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creator"));
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDate, "date_scheduled");
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDateLogged, "date_logged", false);
        CustomTableViewControls.makeEditableTableColumn(colOther, TextFieldTableCell.forTableColumn(), 50, "other", "/enquiries");

        ObservableList<Enquiry> lst_enquiries = FXCollections.observableArrayList();
        lst_enquiries.addAll(EnquiryManager.getInstance().getDataset().values());
        tblEnquiries.setItems(lst_enquiries);

        Callback<TableColumn<Enquiry, String>, TableCell<Enquiry, String>> cellFactory
                =
                new Callback<TableColumn<Enquiry, String>, TableCell<Enquiry, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Enquiry, String> param)
                    {
                        final TableCell<Enquiry, String> cell = new TableCell<Enquiry, String>()
                        {
                            final Button btnQuote = new Button("New Quote");
                            final Button btnRemove = new Button("Delete");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                btnQuote.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnQuote.getStyleClass().add("btnApply");
                                btnQuote.setMinWidth(100);
                                btnQuote.setMinHeight(35);
                                HBox.setHgrow(btnQuote, Priority.ALWAYS);

                                btnRemove.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnRemove.getStyleClass().add("btnBack");
                                btnRemove.setMinWidth(100);
                                btnRemove.setMinHeight(35);
                                HBox.setHgrow(btnRemove, Priority.ALWAYS);

                                if (empty)
                                {
                                    setGraphic(null);
                                    setText(null);
                                } else
                                {
                                    HBox hBox = new HBox(btnQuote, btnRemove);
                                    Enquiry enquiry = getTableView().getItems().get(getIndex());

                                    btnQuote.setOnAction(event ->
                                    {
                                        if(enquiry ==null)
                                        {
                                            IO.logAndAlert("Error", "Enquiry object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(enquiry.getCreatorUser() ==null)
                                        {
                                            IO.logAndAlert("Error", "Enquiry creator object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(enquiry.getCreatorUser().getOrganisation() ==null)
                                        {
                                            IO.logAndAlert("Error", "Enquiry creator's organisation object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(SessionManager.getInstance().getActive()==null)
                                        {
                                            IO.logAndAlert("Error " + getClass().getName(), "Invalid active Session.", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(SessionManager.getInstance().getActive().isExpired())
                                        {
                                            IO.logAndAlert("Error", "Active Session has expired.", IO.TAG_ERROR);
                                            return;
                                        }

                                        Quote quote = new Quote();
                                        quote.setEnquiry_id(enquiry.get_id());
                                        quote.setVat(QuoteManager.VAT);
                                        quote.setStatus(Quote.STATUS_PENDING);
                                        quote.setAccount_name(enquiry.getCreatorUser().getOrganisation().getAccount_name());
                                        quote.setRequest(enquiry.getEnquiry());
                                        quote.setClient_id(enquiry.getCreatorUser().getOrganisation_id());
                                        quote.setContact_person_id(enquiry.getCreator());
                                        quote.setCreator(SessionManager.getInstance().getActiveUser().getUsr());
                                        quote.setRevision(1.0);

                                        try
                                        {
                                            QuoteManager.getInstance().createQuote(quote, null, new Callback()
                                            {
                                                @Override
                                                public Object call(Object new_quote_id)
                                                {
                                                    ScreenManager.getInstance().showLoadingScreen(arg ->
                                                    {
                                                        new Thread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                //set selected Quote
                                                                if(QuoteManager.getInstance().getDataset()!=null)
                                                                    QuoteManager.getInstance().setSelected(QuoteManager.getInstance().getDataset().get(new_quote_id));
                                                                try
                                                                {
                                                                    if(ScreenManager.getInstance().loadScreen(Screens.VIEW_QUOTE.getScreen(),mvg.MVG.class.getResource("views/"+Screens.VIEW_QUOTE.getScreen())))
                                                                    {
                                                                        Platform.runLater(() -> ScreenManager.getInstance().setScreen(Screens.VIEW_QUOTE.getScreen()));
                                                                    }
                                                                    else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load Quotes viewer screen.");
                                                                } catch (IOException e)
                                                                {
                                                                    e.printStackTrace();
                                                                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                                                }
                                                            }
                                                        }).start();
                                                        return null;
                                                    });
                                                    return null;
                                                }
                                            });
                                        } catch (IOException e)
                                        {
                                            e.printStackTrace();
                                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                        }
                                    });

                                    btnRemove.setOnAction(event ->
                                    {
                                        //Quote quote = getTableView().getItems().get(getIndex());
                                        getTableView().getItems().remove(enquiry);
                                        getTableView().refresh();
                                        //TODO: remove from server
                                        //IO.log(getClass().getName(), IO.TAG_INFO, "successfully removed quote: " + quote.get_id());
                                    });

                                    hBox.setFillHeight(true);
                                    HBox.setHgrow(hBox, Priority.ALWAYS);
                                    hBox.setSpacing(5);
                                    setGraphic(hBox);
                                    setText(null);
                                }
                            }
                        };
                        return cell;
                    }
                };

        colAction.setCellValueFactory(new PropertyValueFactory<>(""));
        colAction.setCellFactory(cellFactory);

        tblEnquiries.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                EnquiryManager.getInstance().setSelected(tblEnquiries.getSelectionModel().getSelectedItem()));
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries data model..");
            ClientManager.getInstance().initialize();
            EnquiryManager.getInstance().initialize();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        refreshModel();
        refreshView();
    }
}
