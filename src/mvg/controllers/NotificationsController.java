/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import mvg.auxilary.IO;
import mvg.auxilary.PDF;
import mvg.auxilary.PDFViewer;
import mvg.managers.*;
import mvg.model.*;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * views Controller class
 *
 * @author ghost
 */
public class NotificationsController extends ScreenController implements Initializable
{
    @FXML
    private TableView<Notification>    tblNotifications;
    @FXML
    private TableColumn     colId, colClient, colSubject, colMessage, colDateLogged, colStatus, colCreator, colExtra,colAction;

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading notifications view..");

        if(UserManager.getInstance().getDataset()==null)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "no employees were found in the database.");
            return;
        }
        if(NotificationManager.getInstance().getDataset()==null)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "no notifications were found in the database.");
            return;
        }
        if(ClientManager.getInstance().getDataset()==null)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "no clients were found in the database.");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("_id"));
        colClient.setMinWidth(120);
        colClient.setCellValueFactory(new PropertyValueFactory<>("client_name"));
        colSubject.setMinWidth(120);
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        colMessage.setMinWidth(120);
        colMessage.setCellValueFactory(new PropertyValueFactory<>("message"));
        CustomTableViewControls.makeLabelledDatePickerTableColumn(colDateLogged, "date_logged", false);
        CustomTableViewControls.makeDynamicToggleButtonTableColumn(colStatus,100, "status", new String[]{"0","PENDING","1","READ"}, false,"/notifications");
        colCreator.setCellValueFactory(new PropertyValueFactory<>("creator"));
        CustomTableViewControls.makeEditableTableColumn(colExtra, TextFieldTableCell.forTableColumn(), 100, "other", "/notifications");

        Callback<TableColumn<Notification, String>, TableCell<Notification, String>> cellFactory
                =
                new Callback<TableColumn<Notification, String>, TableCell<Notification, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<Notification, String> param)
                    {
                        final TableCell<Notification, String> cell = new TableCell<Notification, String>()
                        {
                            final Button btnRemove = new Button("Delete");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                /*btnView.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnView.getStyleClass().add("btnDefault");
                                btnView.setMinWidth(100);
                                btnView.setMinHeight(35);
                                HBox.setHgrow(btnView, Priority.ALWAYS);*/
                                
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
                                    HBox hBox = new HBox(btnRemove);

                                    btnRemove.setOnAction(event ->
                                    {
                                        Notification notification = getTableView().getItems().get(getIndex());
                                        getTableView().getItems().remove(notification);
                                        getTableView().refresh();
                                        //TODO: remove from server
                                        IO.log(getClass().getName(), IO.TAG_INFO, "successfully removed notification: " + notification.get_id());
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
        colAction.setCellFactory(cellFactory);

        tblNotifications.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) ->
                NotificationManager.getInstance().setSelected(tblNotifications.getSelectionModel().getSelectedItem()));
        //set list of Notifications
        tblNotifications.setItems(FXCollections.observableArrayList(NotificationManager.getInstance().getDataset().values()));
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading notifications data model..");

        UserManager.getInstance().initialize();
        ClientManager.getInstance().initialize();
        NotificationManager.getInstance().initialize();
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

    /*public static RadialMenuItem[] getDefaultContextMenu()
    {
        //RadialMenuItem level1Item = new RadialMenuItemCustom(ScreenManager.MENU_SIZE, "level 1 item 1", null, null, null);//RadialMenuItem(menuSize, "level 1 item", null, null);
        return ScreenController.getDefaultContextMenu();
    }*/
}