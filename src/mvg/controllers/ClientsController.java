/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.MVG;
import mvg.auxilary.IO;
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

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * views Controller class
 *
 * @author ghost
 */
public class ClientsController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Client>    tblClients;
    @FXML
    private TableColumn     colClientId,colClientName,colClientPhysicalAddress,
                            colClientPostalAddress,colClientTel,colClientFax,colClientEmail,colClientRegistration,
                            colClientVat,colClientAccount,colClientDatePartnered,colClientWebsite,colClientActive,colClientOther,colAction;

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading clients view..");

        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No users were found in the database.", IO.TAG_ERROR);
            return;
        }
        if( ClientManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getSimpleName(), "No clients were found in the database.", IO.TAG_WARN);
            return;
        }

        colClientId.setMinWidth(100);
        colClientId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        CustomTableViewControls.makeEditableTableColumn(colClientName, TextFieldTableCell.forTableColumn(), 80, "client_name", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientPhysicalAddress, TextFieldTableCell.forTableColumn(), 120, "physical_address", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientPostalAddress, TextFieldTableCell.forTableColumn(), 120, "postal_address", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientTel, TextFieldTableCell.forTableColumn(), 80, "tel", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientFax, TextFieldTableCell.forTableColumn(), 80, "fax", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientEmail, TextFieldTableCell.forTableColumn(), 80, "contact_email", "/clients");
        CustomTableViewControls.makeCheckboxedTableColumn(colClientActive, null, 80, "active", "/clients");
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colClientDatePartnered, "date_partnered");
        CustomTableViewControls.makeEditableTableColumn(colClientWebsite, TextFieldTableCell.forTableColumn(), 100, "website", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientRegistration, TextFieldTableCell.forTableColumn(), 100, "registration_number", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientVat, TextFieldTableCell.forTableColumn(), 100, "vat_number", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientAccount, TextFieldTableCell.forTableColumn(), 100, "account_name", "/clients");
        CustomTableViewControls.makeEditableTableColumn(colClientOther, TextFieldTableCell.forTableColumn(), 50, "other", "/clients");

        ObservableList<Client> lst_clients = FXCollections.observableArrayList();
        lst_clients.addAll(ClientManager.getInstance().getDataset().values());
        tblClients.setItems(lst_clients);

        final ScreenManager screenManager = ScreenManager.getInstance();
        Callback<TableColumn<Client, String>, TableCell<Client, String>> cellFactory
                =
                new Callback<TableColumn<Client, String>, TableCell<Client, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Client, String> param)
                    {
                        final TableCell<Client, String> cell = new TableCell<Client, String>()
                        {
                            final Button btnNotification = new Button("Send Notification");
                            final Button btnRemove = new Button("Delete");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                btnNotification.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnNotification.getStyleClass().add("btnAdd");
                                btnNotification.setMinWidth(100);
                                btnNotification.setMinHeight(35);
                                HBox.setHgrow(btnNotification, Priority.ALWAYS);

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
                                    HBox hBox = new HBox(btnNotification, btnRemove);
                                    Client client = getTableView().getItems().get(getIndex());

                                    btnNotification.setOnAction(event ->
                                        NotificationManager.getInstance().newNotificationWindow(client, new Callback()
                                        {
                                            @Override
                                            public Object call(Object arg)
                                            {
                                                ScreenManager.getInstance().showLoadingScreen(param ->
                                                {
                                                    new Thread(new Runnable()
                                                    {
                                                        @Override
                                                        public void run()
                                                        {
                                                            try
                                                            {
                                                                //load User data to memory
                                                                UserManager.getInstance().initialize();

                                                                //TODO: set screen to notifications screen
                                                                if (ScreenManager.getInstance()
                                                                        .loadScreen(Screens.DASHBOARD
                                                                                .getScreen(), MVG.class
                                                                                .getResource("views/" + Screens.DASHBOARD
                                                                                        .getScreen())))
                                                                {
                                                                    ScreenManager.getInstance()
                                                                            .setScreen(Screens.DASHBOARD.getScreen());
                                                                } else IO.log(getClass()
                                                                        .getName(), IO.TAG_ERROR, "could not load dashboard screen.");
                                                            } catch (IOException e)
                                                            {
                                                                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                                            }
                                                        }
                                                    }).start();
                                                    return null;
                                                });
                                                return null;
                                            }
                                        })
                                    );

                                    btnRemove.setOnAction(event ->
                                    {
                                        getTableView().getItems().remove(client);
                                        getTableView().refresh();
                                        //TODO: remove from server
                                        IO.log(getClass().getName(), IO.TAG_INFO, "successfully removed client: " + client.get_id());
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

        tblClients.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                ClientManager.getInstance().setSelected(tblClients.getSelectionModel().getSelectedItem()));
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading clients data model..");
        ClientManager.getInstance().initialize();
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

    @FXML
    public void createClientClick()
    {
        //TODO:
        /*try
        {
            if(ScreenManager.getInstance().loadScreen(Screens.NEW_CLIENT.getScreen(), mvg.FadulousBMS.class.getResource("views/"+Screens.NEW_CLIENT.getScreen())))
                ScreenManager.getInstance().setScreen(Screens.NEW_CLIENT.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load client creation screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }*/
    }

    public static RadialMenuItem[] getDefaultContextMenu()
    {
        //RadialMenuItem level1Item = new RadialMenuItemCustom(ScreenManager.MENU_SIZE, "level 1 item 1", null, null, null);//RadialMenuItem(menuSize, "level 1 item", null, null);
        //return ScreenController.getDefaultContextMenu();
        return null;
    }
}
