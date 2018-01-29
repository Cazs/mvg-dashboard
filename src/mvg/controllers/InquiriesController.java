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
import mvg.managers.InquiryManager;
import mvg.model.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InquiriesController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Inquiry> tblEnquiries;
    @FXML
    private TableColumn colId, colClient, colEnquiry, colAddress,
            colDestination,colTripType,colDate,colOther,colAction;
    
    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries view..");
        if( InquiryManager.getInstance().getEnquiries()==null)
        {
            IO.logAndAlert(getClass().getName(), "no enquiries were found in the database.", IO.TAG_ERROR);
            return;
        }

        colId.setMinWidth(100);
        colId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("client_name"));
        CustomTableViewControls.makeEditableTableColumn(colEnquiry, TextFieldTableCell.forTableColumn(), 80, "enquiry", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colAddress, TextFieldTableCell.forTableColumn(), 120, "pickup_location", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colDestination, TextFieldTableCell.forTableColumn(), 120, "destination", "/enquiries");
        CustomTableViewControls.makeEditableTableColumn(colTripType, TextFieldTableCell.forTableColumn(), 80, "trip_type", "/enquiries");
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDate, "date_scheduled");
        CustomTableViewControls.makeEditableTableColumn(colOther, TextFieldTableCell.forTableColumn(), 50, "other", "/enquiries");

        ObservableList<Inquiry> lst_enquiries = FXCollections.observableArrayList();
        lst_enquiries.addAll(InquiryManager.getInstance().getEnquiries().values());
        tblEnquiries.setItems(lst_enquiries);

        Callback<TableColumn<Inquiry, String>, TableCell<Inquiry, String>> cellFactory
                =
                new Callback<TableColumn<Inquiry, String>, TableCell<Inquiry, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Inquiry, String> param)
                    {
                        final TableCell<Inquiry, String> cell = new TableCell<Inquiry, String>()
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
                                    Inquiry inquiry = getTableView().getItems().get(getIndex());

                                    btnQuote.setOnAction(event ->
                                    {
                                        if(inquiry ==null)
                                        {
                                            IO.logAndAlert("Error", "Inquiry object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(inquiry.getCreatorUser() ==null)
                                        {
                                            IO.logAndAlert("Error", "Inquiry creator object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(inquiry.getCreatorUser().getOrganisation() ==null)
                                        {
                                            IO.logAndAlert("Error", "Inquiry creator's organisation object is not set", IO.TAG_ERROR);
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
                                        quote.setEnquiry_id(inquiry.get_id());
                                        quote.setVat(QuoteManager.VAT);
                                        quote.setStatus(Quote.STATUS_PENDING);
                                        quote.setAccount_name(inquiry.getCreatorUser().getOrganisation().getAccount_name());
                                        quote.setRequest(inquiry.getEnquiry());
                                        quote.setClient_id(inquiry.getCreatorUser().getOrganisation_id());
                                        quote.setContact_person_id(inquiry.getCreator());
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
                                                                QuoteManager.getInstance().setSelectedQuote((String)new_quote_id);
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
                                        getTableView().getItems().remove(inquiry);
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
                InquiryManager.getInstance().setSelectedEnquiry(tblEnquiries.getSelectionModel().getSelectedItem()));
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading enquiries data model..");
        try
        {
            InquiryManager.getInstance().reloadDataFromServer();
        } catch (ClassNotFoundException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        refreshModel();
        refreshView();
    }
}
