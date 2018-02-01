package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import mvg.auxilary.*;
import mvg.model.Client;
import mvg.model.CustomTableViewControls;
import mvg.model.Notification;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ghost on 2018/01/29.
 */
public class NotificationManager extends MVGObjectManager
{
    private HashMap<String, Notification> notifications;
    private Notification selected;
    private Gson gson;
    private static NotificationManager notificationManager = new NotificationManager();
    public static final String TAG = "NotificationManager";
    public static final String ROOT_PATH = "cache/notifications/";
    public String filename = "";
    private long timestamp;

    private NotificationManager()
    {
    }

    public static NotificationManager getInstance()
    {
        return notificationManager;
    }

    public HashMap<String, Notification> getNotifications(){return notifications;}

    public void setSelected(Notification notification)
    {
        this.selected=notification;
    }

    public Notification getSelected()
    {
        return this.selected;
    }

    @Override
    public void initialize()
    {
        loadDataFromServer();
    }

    public void loadDataFromServer()
    {
        try
        {
            if(notifications==null)
                reloadDataFromServer();
            else IO.log(getClass().getName(), IO.TAG_INFO, "notifications object has already been set.");
        } catch (MalformedURLException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
            IO.showMessage("URL Error", ex.getMessage(), IO.TAG_ERROR);
        }catch (ClassNotFoundException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            IO.showMessage("ClassNotFoundException", e.getMessage(), IO.TAG_ERROR);
        }catch (IOException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
            IO.showMessage("I/O Error", ex.getMessage(), IO.TAG_ERROR);
        }
    }

    public void reloadDataFromServer() throws ClassNotFoundException, IOException
    {
        SessionManager smgr = SessionManager.getInstance();
        if(smgr.getActive()!=null)
        {
            if(!smgr.getActive().isExpired())
            {
                gson = new GsonBuilder().create();
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", smgr.getActive().getSessionId()));

                //Get Timestamp
                String timestamp_json = RemoteComms.sendGetRequest("/timestamp/notifications_timestamp", headers);
                Counters cntr_timestamp = gson.fromJson(timestamp_json, Counters.class);
                if (cntr_timestamp != null)
                {
                    timestamp = cntr_timestamp.getCount();
                    filename = "notifications_" + timestamp + ".dat";
                    IO.log(this.getClass().getName(), IO.TAG_INFO, "Server Timestamp: " + timestamp);
                } else
                {
                    IO.logAndAlert(this.getClass().getName(), "could not get valid timestamp", IO.TAG_ERROR);
                    return;
                }

                if (!isSerialized(ROOT_PATH + filename))
                {
                    String notifications_json_object = RemoteComms.sendGetRequest("/notifications", headers);
                    NotificationServerObject notificationServerObject = gson.fromJson(notifications_json_object, NotificationServerObject.class);
                    if(notificationServerObject!=null)
                    {
                        if(notificationServerObject.get_embedded()!=null)
                        {
                            Notification[] notifications_arr = notificationServerObject.get_embedded().getNotifications();

                            notifications = new HashMap<>();
                            for (Notification notification : notifications_arr)
                                notifications.put(notification.get_id(), notification);
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Notifications in database.");
                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "NotificationServerObject (containing Notification objects & other metadata) is null");

                    IO.log(getClass().getName(), IO.TAG_INFO, "reloaded collection of notifications.");
                    this.serialize(ROOT_PATH + filename, notifications);
                } else
                {
                    IO.log(this.getClass().getName(), IO.TAG_INFO, "binary object [" + ROOT_PATH + filename + "] on local disk is already up-to-date.");
                    notifications = (HashMap<String, Notification>) this.deserialize(ROOT_PATH + filename);
                }
            } else IO.logAndAlert("Error: Active session has expired.", "Session Expired", IO.TAG_ERROR);
        } else IO.logAndAlert("Error: No active sessions.", "Session Expired", IO.TAG_ERROR);
    }

    public void newNotificationWindow(Client client, Callback callback)
    {
        if(client==null)
        {
            IO.logAndAlert("Error", "Client object is not set.", IO.TAG_ERROR);
            return;
        }
        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - Send Notification to " + client.getClient_name());
        stage.setMinWidth(320);
        stage.setMinHeight(300);
        stage.setAlwaysOnTop(true);

        VBox vbox = new VBox(1);

        final TextField txt_client = new TextField();
        txt_client.setMinWidth(200);
        txt_client.setMaxWidth(Double.MAX_VALUE);
        txt_client.setEditable(false);
        txt_client.setText(client.getClient_name());
        HBox hbx_client = CustomTableViewControls.getLabelledNode("To: ", 200, txt_client);

        final TextField txt_subject = new TextField();
        txt_subject.setMinWidth(200);
        txt_subject.setMaxWidth(Double.MAX_VALUE);
        HBox hbx_subject = CustomTableViewControls.getLabelledNode("Notification Subject", 200, txt_subject);

        final TextArea txt_message = new TextArea();
        txt_message.setMinWidth(200);
        txt_message.setMaxWidth(Double.MAX_VALUE);
        HBox hbx_message = CustomTableViewControls.getLabelledNode("Message", 200, txt_message);

        HBox submit;
        submit = CustomTableViewControls.getSpacedButton("Submit", event ->
        {
            String date_regex="\\d+(\\-|\\/|\\\\)\\d+(\\-|\\/|\\\\)\\d+";

            if(!Validators.isValidNode(txt_subject, txt_subject.getText(), 1, ".+"))
                return;
            if(!Validators.isValidNode(txt_message, txt_message.getText(), 1, ".+"))
                return;

            if(SessionManager.getInstance().getActive()==null)
            {
                IO.logAndAlert("Error: Invalid Session", "Active session is invalid.\nPlease log in.", IO.TAG_ERROR);
                return;
            }
            if(SessionManager.getInstance().getActive().isExpired())
            {
                IO.logAndAlert("Error: Session Expired", "Active session has expired.\nPlease log in.", IO.TAG_ERROR);
                return;
            }

            Notification notification = new Notification(txt_subject.getText(),txt_message.getText(), client.get_id());
            notification.setCreator(SessionManager.getInstance().getActiveUser().getUsr());
            try
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));

                HttpURLConnection connection = RemoteComms.putJSON("/notifications", notification.asJSONString(), headers);
                if(connection!=null)
                {
                    if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                    {
                        IO.logAndAlert("Success", "Successfully sent notification to "+client.getClient_name()+"!", IO.TAG_INFO);
                        if(callback!=null)
                            callback.call(null);
                    } else
                    {
                        IO.logAndAlert( "ERROR_" + connection.getResponseCode(),  IO.readStream(connection.getErrorStream()), IO.TAG_ERROR);
                    }
                    connection.disconnect();
                }
            } catch (IOException e)
            {
                IO.log(TAG, IO.TAG_ERROR, e.getMessage());
            }
        });

        //Add form controls vertically on the stage
        vbox.getChildren().add(hbx_client);
        vbox.getChildren().add(hbx_subject);
        vbox.getChildren().add(hbx_message);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File("src/fadulousbms/styles/home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.onHidingProperty().addListener((observable, oldValue, newValue) ->
                loadDataFromServer());

        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        stage.setResizable(true);
    }

    class NotificationServerObject extends ServerObject
    {
        private Embedded _embedded;

        Embedded get_embedded()
        {
            return _embedded;
        }

        void set_embedded(Embedded _embedded)
        {
            this._embedded = _embedded;
        }

        class Embedded
        {
            private Notification[] notifications;

            public Notification[] getNotifications()
            {
                return notifications;
            }

            public void setNotifications(Notification[] notifications)
            {
                this.notifications = notifications;
            }
        }
    }
}