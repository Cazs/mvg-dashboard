/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import javafx.collections.ObservableList;
import javafx.util.Callback;
import mvg.MVG;
import mvg.auxilary.*;
import mvg.managers.*;
import mvg.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jfxtras.labs.scene.control.radialmenu.RadialMenuItem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * views Controller class
 *
 * @author ghost
 */
public class TripsController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Trip> tblTrips;
    @FXML
    private TableColumn colId, colClient, colAddress, colDestination, colRequest, colTotal,
            colContactPerson, colDateLogged, colDateAssigned, colDateScheduled, colCreator, colExtra, colStatus, colAction;
    public static final String TAB_ID = "tripsTab";

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading trips view..");

        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No users were found in the database.", IO.TAG_ERROR);
            return;
        }
        if (TripManager.getInstance().getDataset() == null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No trips were found in the database.", IO.TAG_WARN);
            return;
        }
        if (TripManager.getInstance().getDataset().values() == null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No trips were found in the database.", IO.TAG_WARN);
            return;
        }
        colId.setMinWidth(100);
        colId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        colRequest.setCellValueFactory(new PropertyValueFactory<>("trip_description"));
        colClient.setCellValueFactory(new PropertyValueFactory<>("client_name"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));
        colDestination.setCellValueFactory(new PropertyValueFactory<>("destination"));
        colContactPerson.setCellValueFactory(new PropertyValueFactory<>("contact_person"));
        //TODO: contact_personProperty
        CustomTableViewControls.makeDynamicToggleButtonTableColumn(colStatus,90, "status", new String[]{"0","PENDING","1","APPROVED"}, false,"/trips");
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDateLogged, "date_logged", false);
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDateAssigned, "date_assigned");
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDateScheduled, "date_scheduled", false);
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creator_name"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        ObservableList<Trip> lst_trips = FXCollections.observableArrayList();
        lst_trips.addAll(TripManager.getInstance().getDataset().values());
        tblTrips.setItems(lst_trips);

        Callback<TableColumn<Trip, String>, TableCell<Trip, String>> cellFactory
                =
                new Callback<TableColumn<Trip, String>, TableCell<Trip, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Trip, String> param)
                    {
                        final TableCell<Trip, String> cell = new TableCell<Trip, String>()
                        {
                            final Button btnApprove = new Button("Approve");
                            final Button btnAssign = new Button("Assign Drivers");
                            final Button btnInvoice = new Button("Generate Invoice");
                            final Button btnRemove = new Button("Delete");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                File fCss = new File(IO.STYLES_ROOT_PATH+"home.css");
                                btnAssign.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                btnAssign.getStyleClass().add("btnDefault");
                                btnAssign.setMinWidth(100);
                                btnAssign.setMinHeight(35);
                                HBox.setHgrow(btnAssign, Priority.ALWAYS);

                                btnApprove.setMinWidth(100);
                                btnApprove.setMinHeight(35);
                                btnApprove.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                if(SessionManager.getInstance().getActiveUser()!=null)
                                {
                                    //disable [Approve] button if not authorised
                                    if (SessionManager.getInstance().getActiveUser().getAccessLevel()>=AccessLevels.SUPERUSER.getLevel())
                                    {
                                        btnApprove.getStyleClass().add("btnAdd");
                                        btnApprove.setDisable(false);
                                    } else
                                    {
                                        btnApprove.getStyleClass().add("btnDisabled");
                                        btnApprove.setDisable(true);
                                    }
                                } else IO.logAndAlert("Error", "No valid active user session found, please log in.", IO.TAG_ERROR);

                                HBox.setHgrow(btnApprove, Priority.ALWAYS);

                                btnInvoice.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));
                                btnInvoice.getStyleClass().add("btnDefault");
                                btnInvoice.setMinWidth(100);
                                btnInvoice.setMinHeight(35);
                                HBox.setHgrow(btnInvoice, Priority.ALWAYS);

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
                                    HBox hBox = new HBox(btnApprove, btnAssign, btnInvoice, btnRemove);
                                    hBox.setMaxWidth(Double.MAX_VALUE);
                                    HBox.setHgrow(hBox, Priority.ALWAYS);
                                    Trip trip = getTableView().getItems().get(getIndex());

                                    btnApprove.setOnAction(event ->
                                            TripManager.approveTrip(trip, param1 ->
                                            {
                                                //Refresh UI
                                                new Thread(() ->
                                                {
                                                    refreshModel();
                                                    Platform.runLater(() -> refreshView());
                                                }).start();
                                                return null;
                                            }));

                                    btnAssign.setOnAction(event ->
                                            assignDrivers(trip));

                                    btnInvoice.setOnAction(event ->
                                            generateInvoice(trip));

                                    btnRemove.setOnAction(event ->
                                    {
                                        //197.242.144.30
                                        //Quote quote = getTableView().getItems().get(getIndex());
                                        //getTableView().getItems().remove(quote);
                                        //getTableView().refresh();
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
        colAction.setMinWidth(500);

        tblTrips.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                TripManager.getInstance().setSelected(tblTrips.getSelectionModel().getSelectedItem()));
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading trips data model..");

        EnquiryManager.getInstance().initialize();
        ResourceManager.getInstance().initialize();
        ClientManager.getInstance().initialize();
        QuoteManager.getInstance().initialize();
        TripManager.getInstance().initialize();
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

    private static void generateInvoice(Trip trip)
    {
        if (trip != null)
        {
            if(trip.getStatus()==MVGObject.STATUS_APPROVED)//if Trip has been approved
            {
                if (trip.getAssigned_drivers() != null)//if Trip has drivers assigned to it then generate invoice
                {
                    Stage stage = new Stage();
                    stage.setTitle("Amount received for Trip["+trip.get_id()+"]");
                    stage.setResizable(false);

                    VBox container = new VBox();

                    TextField txt_receivable = new TextField();
                    HBox hbx_receivable = new HBox(new Label("Amount Receivable: "), txt_receivable);

                    container.getChildren().add(hbx_receivable);

                    /*container.getChildren().add(new Label("Choose Quote Revisions"));
                    HashMap<String, Quote> quote_revs = new HashMap<>();
                    for(Quote quote_rev: trip.getQuote().getSortedSiblings("revision"))
                    {
                        CheckBox checkBox = new CheckBox("Revision "+quote_rev.getRevision());
                        checkBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                        {
                            //add Quote to map on checkbox check, remove otherwise
                            if(newValue)
                                quote_revs.put(quote_rev.get_id(), quote_rev);
                            else quote_revs.remove(quote_rev.get_id());
                        });
                        container.setSpacing(10);
                        container.getChildren().add(checkBox);
                    }*/

                    Button btnSubmit = new Button("Submit");
                    btnSubmit.setOnAction(event1 ->
                            ScreenManager.getInstance().showLoadingScreen(param ->
                            {
                                new Thread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            /*//get Quote revisions
                                            String str_quote_revs="";
                                            for(Quote quote: quote_revs.values())
                                                str_quote_revs+=(str_quote_revs==""?quote.getRevision():";"+quote.getRevision());//comma separated revision numbers*/

                                            //get latest revision
                                            Quote[] quote_revs = trip.getQuote().getSortedSiblings("revision");//get all revisions ordered by revision number
                                            if(quote_revs!=null)
                                            {
                                                Quote latest_quote_revision = quote_revs[quote_revs.length - 1];

                                                InvoiceManager.getInstance().createInvoice(trip, String
                                                        .valueOf(latest_quote_revision.getRevision()), Double
                                                        .parseDouble(txt_receivable.getText()), new_invoice_id ->
                                                {
                                                    Platform.runLater(() ->
                                                    {
                                                        if(stage!=null)
                                                            if(stage.isShowing())
                                                                stage.hide();
                                                    });
                                                    return null;
                                                });

                                                //TODO: show Invoices tab
                                                if (ScreenManager.getInstance()
                                                        .loadScreen(Screens.DASHBOARD
                                                                .getScreen(), MVG.class.getResource("views/" + Screens.DASHBOARD
                                                                .getScreen())))
                                                {
                                                    Platform.runLater(() -> ScreenManager
                                                            .getInstance()
                                                            .setScreen(Screens.DASHBOARD
                                                                    .getScreen()));
                                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load invoices list screen.");
                                            }
                                        } catch (NumberFormatException e)
                                        {
                                            IO.log(TripsController.class.getName(), IO.TAG_ERROR, "Invalid amount receivable: " + e.getMessage());
                                        } catch (IOException e)
                                        {
                                            IO.log(TripsController.class.getName(), IO.TAG_ERROR, e.getMessage());
                                        }
                                    }
                                }).start();
                                return null;
                            }));
                    container.getChildren().add(btnSubmit);
                    stage.setScene(new Scene(container));
                    stage.show();
                    stage.centerOnScreen();
                } else
                    IO.logAndAlert("Error", "Selected trip has no assigned drivers, please assign drivers first then try again.", IO.TAG_ERROR);
            } else
                IO.logAndAlert("Error", "Selected trip has not been APPROVED yet, please sign it first and try again.", IO.TAG_ERROR);
        } else IO.logAndAlert("Error", "Selected trip is invalid.", IO.TAG_ERROR);
    }

    private static void assignDrivers(Trip trip)
    {
        if (trip != null)
        {
            if(trip.getStatus()>=MVGObject.STATUS_APPROVED)
            {
                Stage stage = new Stage();
                stage.setTitle("Assign drivers for Trip["+trip.get_id()+"]");
                stage.setResizable(false);

                VBox container = new VBox();

                container.getChildren().add(new Label("Possible Drivers"));

                //get possible drivers - load all Users in the system
                //TODO: add User job_title attribute to fix the above issue
                //TODO: check drivers that have already been assigned.
                final HashMap<String, User> drivers = new HashMap<>();
                for(User driver: UserManager.getInstance().getDataset().values())
                {
                    CheckBox checkBox = new CheckBox(driver.getName());

                    //check checkbox if already assigned
                    if(trip.getAssigned_drivers()!=null)
                        for(User trip_drv :trip.getAssigned_drivers())
                            if(trip_drv.getUsr().equals(driver.getUsr()))
                                checkBox.setSelected(true);

                    checkBox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    {
                        //add driver to map on check, remove otherwise
                        if(newValue)
                            drivers.put(driver.get_id(), driver);
                        else drivers.remove(driver.get_id());
                    });
                    container.setSpacing(10);
                    container.getChildren().add(checkBox);
                }

                Button btnSubmit = new Button("Submit");
                btnSubmit.setOnAction(event1 ->
                        ScreenManager.getInstance().showLoadingScreen(param ->
                        {
                            new Thread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        if(drivers!=null)
                                        {
                                            User[] drivers_arr = new User[drivers.size()];
                                            drivers.values().toArray(drivers_arr);

                                            TripManager.assignTripDrivers(trip, drivers_arr, param1 ->
                                            {
                                                Platform.runLater(() ->
                                                {
                                                    if(stage!=null)
                                                        if(stage.isShowing())
                                                            stage.hide();
                                                });
                                                return null;
                                            });

                                            if (ScreenManager.getInstance()
                                                    .loadScreen(Screens.DASHBOARD
                                                            .getScreen(), MVG.class.getResource("views/" + Screens.DASHBOARD
                                                            .getScreen())))
                                            {
                                                Platform.runLater(() -> ScreenManager
                                                        .getInstance()
                                                        .setScreen(Screens.DASHBOARD
                                                                .getScreen()));
                                            } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load invoices list screen.");
                                        }
                                    } catch (NumberFormatException e)
                                    {
                                        IO.log(TripsController.class.getName(), IO.TAG_ERROR, "Invalid amount receivable: " + e.getMessage());
                                    } catch (IOException e)
                                    {
                                        IO.log(TripsController.class.getName(), IO.TAG_ERROR, e.getMessage());
                                    }
                                }
                            }).start();
                            return null;
                        }));
                container.getChildren().add(btnSubmit);
                stage.setScene(new Scene(new ScrollPane(container)));
                stage.show();
                stage.centerOnScreen();
            } else
                IO.logAndAlert("Error", "Selected trip has not been APPROVED yet, please sign it first and try again.", IO.TAG_ERROR);
        } else IO.logAndAlert("Error", "Selected trip is invalid.", IO.TAG_ERROR);
    }

    public static RadialMenuItem[] getContextMenu()
    {
        RadialMenuItem[] context_menu = new RadialMenuItem[7];

        //View Trip Menu item
        context_menu[0] = new RadialMenuItemCustom(30, "Approve Trip", null, null, null);

        //Sign Trip menu item
        context_menu[1] = new RadialMenuItemCustom(30, "Assign Trip Drivers", null, null, event ->
        {
            if(TripManager.getInstance().getSelected()==null)
            {
                IO.logAndAlert("Error", "Selected Trip object is not set.", IO.TAG_ERROR);
                return;
            }
            TripManager.approveTrip((Trip)TripManager.getInstance().getSelected(), null);
        });

        //View signed Trip menu item
        context_menu[2] = new RadialMenuItemCustom(30, "Request Trip Approval", null, null, event ->
        {
            if(TripManager.getInstance().getSelected()==null)
            {
                IO.logAndAlert("Error", "Selected Trip object is not set.", IO.TAG_ERROR);
                return;
            }
        });

        //Generate Trip Invoice menu item
        context_menu[3] = new RadialMenuItemCustom(30, "Generate Invoice", null, null, event ->
        {
            if(TripManager.getInstance().getSelected()==null)
            {
                IO.logAndAlert("Error", "Selected Trip object is not set.", IO.TAG_ERROR);
                return;
            }
        });
        return context_menu;
    }
}
