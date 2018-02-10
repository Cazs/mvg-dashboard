/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.auxilary.IO;
import mvg.auxilary.PDF;
import mvg.auxilary.PDFViewer;
import mvg.managers.*;
import mvg.model.*;
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
import jfxtras.labs.scene.control.radialmenu.RadialMenuItem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * views Controller class
 *
 * @author ghost
 */
public class InvoicesController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Invoice>    tblInvoices;
    @FXML
    private TableColumn     colInvoiceId,colTripId,colClient,colTotal,colReceivable,colDateGenerated,colAccount,colStatus,
                            colCreator,colExtra,colAction;

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading invoices view..");

        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No users were found in the database.", IO.TAG_ERROR);
            return;
        }
        if(InvoiceManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No invoices were found in the database.", IO.TAG_WARN);
            return;
        }
        colInvoiceId.setMinWidth(140);
        colInvoiceId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        colTripId.setMinWidth(50);
        colTripId.setCellValueFactory(new PropertyValueFactory<>("trip_id"));
        colClient.setMinWidth(80);
        colClient.setCellValueFactory(new PropertyValueFactory<>("client"));
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDateGenerated, "date_logged", false);
        CustomTableViewControls.makeDynamicToggleButtonTableColumn(colStatus,100, "status", new String[]{"0","PENDING","1","APPROVED"}, false,"/invoices");
        colCreator.setMinWidth(70);
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creator"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colAccount.setCellValueFactory(new PropertyValueFactory<>("account"));
        CustomTableViewControls.makeEditableTableColumn(colReceivable, TextFieldTableCell.forTableColumn(), 80, "receivable", "/invoices");
        CustomTableViewControls.makeTripManagerAction(colAction, 600, null);
        CustomTableViewControls.makeEditableTableColumn(colExtra, TextFieldTableCell.forTableColumn(), 80, "other", "/invoices");
        colAction.setMinWidth(460);

        ObservableList<Invoice> lst_invoices = FXCollections.observableArrayList();
        lst_invoices.addAll(InvoiceManager.getInstance().getDataset().values());
        tblInvoices.setItems(lst_invoices);

        final ScreenManager screenManager = ScreenManager.getInstance();
        Callback<TableColumn<Invoice, String>, TableCell<Invoice, String>> cellFactory
                =
                new Callback<TableColumn<Invoice, String>, TableCell<Invoice, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Invoice, String> param)
                    {
                        final TableCell<Invoice, String> cell = new TableCell<Invoice, String>()
                        {
                            final Button btnApproval = new Button("Request Approval");
                            final Button btnViewQuote = new Button("View linked Quote");
                            final Button btnPDF = new Button("View as PDF");
                            final Button btnEmail = new Button("eMail Invoice");
                            final Button btnRemove = new Button("Delete");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                File fCss = new File(IO.STYLES_ROOT_PATH+"home.css");
                                btnApproval.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                btnApproval.getStyleClass().add("btnAdd");
                                btnApproval.setMinWidth(100);
                                btnApproval.setMinHeight(35);
                                HBox.setHgrow(btnApproval, Priority.ALWAYS);

                                btnViewQuote.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                btnViewQuote.getStyleClass().add("btnDefault");
                                btnViewQuote.setMinWidth(100);
                                btnViewQuote.setMinHeight(35);
                                HBox.setHgrow(btnViewQuote, Priority.ALWAYS);

                                btnPDF.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                btnPDF.getStyleClass().add("btnDefault");
                                btnPDF.setMinWidth(100);
                                btnPDF.setMinHeight(35);
                                HBox.setHgrow(btnPDF, Priority.ALWAYS);

                                btnEmail.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                btnEmail.getStyleClass().add("btnDefault");
                                btnEmail.setMinWidth(100);
                                btnEmail.setMinHeight(35);
                                HBox.setHgrow(btnEmail, Priority.ALWAYS);

                                if(!empty)
                                {
                                    if (getTableView().getItems().get(getIndex())==null)
                                    {
                                        IO.logAndAlert("Error " + getClass().getName(), "Invoice object is not set", IO.TAG_ERROR);
                                        return;
                                    }
                                    if (getTableView().getItems().get(getIndex()).getStatus()==MVGObject.STATUS_APPROVED)
                                    {
                                        btnEmail.getStyleClass().add("btnAdd");
                                        btnEmail.setDisable(false);
                                    } else
                                    {
                                        btnEmail.getStyleClass().add("btnDisabled");
                                        btnEmail.setDisable(true);
                                    }
                                }

                                btnRemove.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
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
                                    HBox hBox = new HBox(btnApproval, btnViewQuote, btnPDF, btnEmail, btnRemove);
                                    Invoice invoice = getTableView().getItems().get(getIndex());

                                    btnApproval.setOnAction(event ->
                                    {
                                        //send email requesting approval of Invoice
                                        try
                                        {
                                            InvoiceManager.getInstance().setSelected(invoice);
                                            if(InvoiceManager.getInstance().getSelected()!=null)
                                                InvoiceManager.getInstance().requestInvoiceApproval((Invoice) InvoiceManager.getInstance().getSelected(), null);
                                            else IO.logAndAlert("Error", "Selected Invoice is invalid.", IO.TAG_ERROR);
                                        } catch (IOException e)
                                        {
                                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                        }
                                    });

                                    btnViewQuote.setOnAction(event ->
                                    {
                                        if(invoice.getTrip()==null)
                                        {
                                            IO.logAndAlert("Error " + getClass().getName(), "Job object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(invoice.getTrip().getQuote()==null)
                                        {
                                            IO.logAndAlert("Error " + getClass().getName(), "Job->Quote object is not set", IO.TAG_ERROR);
                                            return;
                                        }
                                        screenManager.showLoadingScreen(param ->
                                        {
                                            new Thread(new Runnable()
                                            {
                                                @Override
                                                public void run()
                                                {
                                                    QuoteManager.getInstance().setSelected(invoice.getTrip().getQuote());
                                                    try
                                                    {
                                                        if(screenManager.loadScreen(Screens.VIEW_QUOTE.getScreen(), mvg.MVG.class.getResource("views/"+Screens.VIEW_QUOTE.getScreen())))
                                                        {
                                                            Platform.runLater(() -> screenManager.setScreen(Screens.VIEW_QUOTE.getScreen()));
                                                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load quotes viewer screen.");
                                                    } catch (IOException e)
                                                    {
                                                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                                    }
                                                }
                                            }).start();
                                            return null;
                                        });
                                        /*QuoteManager.getInstance().setSelectedQuote(invoice.getTrip().getQuote());
                                        try
                                        {
                                            if(screenManager.loadScreen(Screens.VIEW_QUOTE.getScreen(),mvg.mvg.class.getResource("views/"+Screens.VIEW_QUOTE.getScreen())))
                                                screenManager.setScreen(Screens.VIEW_QUOTE.getScreen());
                                            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load quote viewing screen.");
                                        } catch (IOException e)
                                        {
                                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                        }*/
                                    });

                                    btnRemove.setOnAction(event ->
                                    {
                                        //197.242.144.30
                                        //Quote quote = getTableView().getItems().get(getIndex());
                                        //getTableView().getItems().remove(quote);
                                        //getTableView().refresh();
                                        //TODO: remove from server
                                        //IO.log(getClass().getName(), IO.TAG_INFO, "successfully removed quote: " + quote.get_id());
                                    });

                                    btnPDF.setOnAction(event ->
                                    {
                                        if(invoice.getTrip()==null)
                                        {
                                            IO.logAndAlert("Error","Invalid Job object.", IO.TAG_ERROR);
                                            return;
                                        }
                                        if(invoice.getTrip().getQuote()==null)
                                        {
                                            IO.logAndAlert("Error","Invalid Quote associated with this Invoice["+invoice.get_id()+"].", IO.TAG_ERROR);
                                            return;
                                        }
                                        try
                                        {
                                            String path = PDF.createInvoicePdf(invoice);
                                            if(path!=null)
                                            {
                                                PDFViewer pdfViewer = PDFViewer.getInstance();
                                                pdfViewer.setVisible(true);
                                                pdfViewer.doOpen(path);
                                            }else IO.logAndAlert("Error", "Could not get valid path of generated Invoice PDF document.", IO.TAG_ERROR);
                                        } catch (IOException ex)
                                        {
                                            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
                                        }
                                    });

                                    btnEmail.setOnAction(event ->
                                    {
                                        try
                                        {
                                            InvoiceManager.getInstance().emailMVGObject(invoice, PDF.createInvoicePdf(invoice), null);
                                        } catch (IOException e)
                                        {
                                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                        }
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

        tblInvoices.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                InvoiceManager.getInstance().setSelected(tblInvoices.getSelectionModel().getSelectedItem()));
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
        new Thread(() ->
        {
            refreshModel();
            Platform.runLater(() -> refreshView());
        }).start();
    }

    @Override
    public void refreshModel()
    {
        UserManager.getInstance().initialize();
        ClientManager.getInstance().initialize();
        QuoteManager.getInstance().initialize();
        TripManager.getInstance().initialize();
        InvoiceManager.getInstance().initialize();
    }

    public static RadialMenuItem[] getDefaultContextMenu()
    {
        //RadialMenuItem level1Item = new RadialMenuItemCustom(ScreenManager.MENU_SIZE, "level 1 item 1", null, null, null);//RadialMenuItem(menuSize, "level 1 item", null, null);
        //return ScreenController.getDefaultContextMenu();
        return null;
    }
}
