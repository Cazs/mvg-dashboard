package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import mvg.auxilary.*;
import mvg.controllers.NavController;
import mvg.model.CustomTableViewControls;
import mvg.model.Resource;
import mvg.model.ResourceType;
import mvg.model.User;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ghost on 2017/01/11.
 */
public class UserManager extends MVGObjectManager
{
    private HashMap<String, User> users;
    private Gson gson;
    private static UserManager userManager = new UserManager();
    public static String[] access_levels = {"NONE", "NORMAL", "ADMIN", "SUPER"};
    public static String[] sexes = {"MALE", "FEMALE"};

    private UserManager()
    {
    }

    @Override
    public void initialize()
    {
        synchroniseDataset();
    }

    public static UserManager getInstance()
    {
        return userManager;
    }

    @Override
    public HashMap<String, User> getDataset(){return this.users;}

    @Override
    Callback getSynchronisationCallback()
    {
        return new Callback()
        {
            @Override
            public Object call(Object param)
            {
                //if first run, don't refresh model's data-set yet.
                try
                {
                    if(!NavController.first_run)
                    {
                        SessionManager smgr = SessionManager.getInstance();
                        if (smgr.getActive() != null)
                        {
                            if (!smgr.getActive().isExpired())
                            {
                                gson = new GsonBuilder().create();
                                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                                headers.add(new AbstractMap.SimpleEntry<>("Cookie", smgr.getActive().getSessionId()));

                                String user_json_object = RemoteComms.sendGetRequest("/users", headers);
                                UserServerObject userServerObject = gson
                                        .fromJson(user_json_object, UserServerObject.class);
                                //User userObject = gson.fromJson(users_json, User.class);
                                //System.out.println("Embedded: "+userObject.get_embedded());
                                if (userServerObject != null)
                                {
                                    if (userServerObject.get_embedded() != null)
                                    {
                                        User[] users_arr = userServerObject.get_embedded().get_users();
                                /*System.out.println("User count: " + userServerObject.getPage().getTotalElements());
                                System.out
                                        .println("User link: " + userServerObject.get_links().getSelf().getHref());*/

                                        users = new HashMap();
                                        for (User user : users_arr)
                                            users.put(user.getUsr(), user);
                                    }
                                    else IO.log(getClass()
                                            .getName(), IO.TAG_ERROR, "could not find any Users in database.");
                                }
                                else IO.log(getClass()
                                        .getName(), IO.TAG_ERROR, "UserServerObject (containing User objects & other metadata) is null");
                                IO.log(getClass().getName(), IO.TAG_INFO, "reloaded user collection.");
                            }
                            else IO.log("Session Expired", IO.TAG_ERROR, "Active session has expired.");
                        }
                        else IO.log("Session Expired", IO.TAG_ERROR, "Active session is invalid");
                    }
                } catch (IOException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                }
                return null;
            }
        };
    }

    public void newExternalUserWindow(String title, String organisation_id, Callback callback)
    {
        if(SessionManager.getInstance().getActive()==null)
        {
            IO.logAndAlert("Error: Invalid Session", "Active session is invalid.\nPlease sign in.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActiveUser()==null)
        {
            IO.logAndAlert("Error: Invalid Session", "Active session is invalid.\nPlease sign in.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive().isExpired())
        {
            IO.logAndAlert("Error: Session Expired", "Active session has expired.\nPlease sign in.", IO.TAG_ERROR);
            return;
        }

        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - " + title);
        stage.setMinWidth(320);
        stage.setMinHeight(350);
        stage.setHeight(350);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.centerOnScreen();

        final TextField txtFirstname,txtLastname,txtEmail,txtTelephone,txtCellphone;
        final TextArea txtOther;

        VBox vbox = new VBox(1);

        txtFirstname = new TextField();
        txtFirstname.setMinWidth(200);
        txtFirstname.setMaxWidth(Double.MAX_VALUE);
        HBox first_name = CustomTableViewControls.getLabelledNode("First Name", 200, txtFirstname);

        txtLastname = new TextField();
        txtLastname.setMinWidth(200);
        txtLastname.setMaxWidth(Double.MAX_VALUE);
        HBox last_name = CustomTableViewControls.getLabelledNode("Last Name", 200, txtLastname);

        txtEmail = new TextField();
        txtEmail.setMinWidth(200);
        txtEmail.setMaxWidth(Double.MAX_VALUE);
        HBox email = CustomTableViewControls.getLabelledNode("eMail Address:", 200, txtEmail);

        txtTelephone = new TextField();
        txtTelephone.setMinWidth(200);
        txtTelephone.setMaxWidth(Double.MAX_VALUE);
        HBox telephone = CustomTableViewControls.getLabelledNode("Telephone #: ", 200, txtTelephone);

        txtCellphone = new TextField();
        txtCellphone.setMinWidth(200);
        txtCellphone.setMaxWidth(Double.MAX_VALUE);
        HBox cellphone = CustomTableViewControls.getLabelledNode("Cellphone #: ", 200, txtCellphone);

        txtOther = new TextArea();
        txtOther.setMinWidth(200);
        txtOther.setMaxWidth(Double.MAX_VALUE);
        HBox other = CustomTableViewControls.getLabelledNode("Other: ", 200, txtOther);

        HBox submit;
        submit = CustomTableViewControls.getSpacedButton("Submit", event ->
        {
            if(!validateFormField(txtFirstname, "Invalid Firstname", "please enter a valid first name", "^.*(?=.{1,}).*"))
                return;
            if(!validateFormField(txtLastname, "Invalid Lastname", "please enter a valid last name", "^.*(?=.{1,}).*"))
                return;

            if(!validateFormField(txtEmail, "Invalid Email", "please enter a valid email address", "^.*(?=.{5,})(?=(.*@.*\\.)).*"))
                return;
            if(!validateFormField(txtTelephone, "Invalid Telephone Number", "please enter a valid telephone number", "^.*(?=.{10,}).*"))
                return;
            if(!validateFormField(txtCellphone, "Invalid Cellphone Number", "please enter a valid cellphone number", "^.*(?=.{10,}).*"))
                return;

            //all valid, send data to server
            int access_lvl=0;

            ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
            params.add(new AbstractMap.SimpleEntry<>("usr", txtEmail.getText()));
            params.add(new AbstractMap.SimpleEntry<>("pwd", txtTelephone.getText()));
            params.add(new AbstractMap.SimpleEntry<>("access_level", String.valueOf(access_lvl)));
            params.add(new AbstractMap.SimpleEntry<>("firstname", txtFirstname.getText()));
            params.add(new AbstractMap.SimpleEntry<>("lastname", txtLastname.getText()));
            params.add(new AbstractMap.SimpleEntry<>("gender", "female"));
            params.add(new AbstractMap.SimpleEntry<>("email", txtEmail.getText()));
            params.add(new AbstractMap.SimpleEntry<>("tel", txtTelephone.getText()));
            params.add(new AbstractMap.SimpleEntry<>("cell", txtCellphone.getText()));
            params.add(new AbstractMap.SimpleEntry<>("organisation_id", organisation_id));
            params.add(new AbstractMap.SimpleEntry<>("creator", SessionManager.getInstance().getActiveUser().getUsr()));

            if(txtOther.getText()!=null)
                params.add(new AbstractMap.SimpleEntry<>("other", txtOther.getText()));

            try
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                HttpURLConnection connection = RemoteComms.putJSONData("/users", params, headers);
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    IO.logAndAlert("Account Creation Success", "Successfully created new contact!", IO.TAG_INFO);

                    //dismiss stage if successful
                    Platform.runLater(() ->
                    {
                        if(stage!=null)
                            if(stage.isShowing())
                                stage.close();
                    });

                    //execute callback w/ args
                    if(callback!=null)
                        callback.call(IO.readStream(connection.getInputStream()));
                } else
                {
                    IO.logAndAlert("Account Creation Failure", IO.readStream(connection.getErrorStream()), IO.TAG_ERROR);

                    //execute callback w/o args
                    if(callback!=null)
                        callback.call(null);
                }

                connection.disconnect();
            } catch (IOException e)
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }
        });
        //Add form controls vertically on the stage
        vbox.getChildren().add(first_name);
        vbox.getChildren().add(last_name);
        vbox.getChildren().add(email);
        vbox.getChildren().add(telephone);
        vbox.getChildren().add(cellphone);
        vbox.getChildren().add(other);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File("src/mvg/styles/home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.onHidingProperty().addListener((observable, oldValue, newValue) ->
                forceSynchronise());

        stage.setScene(scene);
        stage.show();
    }

    public void newUserWindow(Callback callback)
    {
        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - Create New User [User]");
        stage.setMinWidth(320);
        stage.setMinHeight(350);
        stage.setHeight(500);
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);
        stage.centerOnScreen();

        final ComboBox cbxSex,cbxAccessLevel;
        final TextField txtUsername,txtPassword,txtFirstname,txtLastname,txtEmail,txtTelephone,txtCellphone;
        final TextArea txtOther;

        VBox vbox = new VBox(1);

        txtUsername = new TextField();
        txtUsername.setMinWidth(200);
        txtUsername.setMaxWidth(Double.MAX_VALUE);
        HBox username = CustomTableViewControls.getLabelledNode("Username", 200, txtUsername);

        txtPassword = new TextField();
        txtPassword.setMinWidth(200);
        txtPassword.setMaxWidth(Double.MAX_VALUE);
        HBox password = CustomTableViewControls.getLabelledNode("Password", 200, txtPassword);

        txtFirstname = new TextField();
        txtFirstname.setMinWidth(200);
        txtFirstname.setMaxWidth(Double.MAX_VALUE);
        HBox first_name = CustomTableViewControls.getLabelledNode("First Name", 200, txtFirstname);

        txtLastname = new TextField();
        txtLastname.setMinWidth(200);
        txtLastname.setMaxWidth(Double.MAX_VALUE);
        HBox last_name = CustomTableViewControls.getLabelledNode("Last Name", 200, txtLastname);

        txtEmail = new TextField();
        txtEmail.setMinWidth(200);
        txtEmail.setMaxWidth(Double.MAX_VALUE);
        HBox email = CustomTableViewControls.getLabelledNode("eMail Address:", 200, txtEmail);

        txtTelephone = new TextField();
        txtTelephone.setMinWidth(200);
        txtTelephone.setMaxWidth(Double.MAX_VALUE);
        HBox telephone = CustomTableViewControls.getLabelledNode("Telephone #: ", 200, txtTelephone);

        txtCellphone = new TextField();
        txtCellphone.setMinWidth(200);
        txtCellphone.setMaxWidth(Double.MAX_VALUE);
        HBox cellphone = CustomTableViewControls.getLabelledNode("Cellphone #: ", 200, txtCellphone);

        txtOther = new TextArea();
        txtOther.setMinWidth(200);
        txtOther.setMaxWidth(Double.MAX_VALUE);
        HBox other = CustomTableViewControls.getLabelledNode("Other: ", 200, txtOther);

        cbxSex = new ComboBox(FXCollections.observableArrayList(sexes));
        cbxSex.setMinWidth(200);
        cbxSex.setMaxWidth(Double.MAX_VALUE);
        HBox sex = CustomTableViewControls.getLabelledNode("Sex: ", 200, cbxSex);

        cbxAccessLevel = new ComboBox(FXCollections.observableArrayList(access_levels));
        cbxAccessLevel.setMinWidth(200);
        cbxAccessLevel.setMaxWidth(Double.MAX_VALUE);
        HBox access_level = CustomTableViewControls.getLabelledNode("Acccess Level: ", 200, cbxAccessLevel);

        HBox submit;
        submit = CustomTableViewControls.getSpacedButton("Create User", event ->
        {
            int sex_index = cbxSex.getSelectionModel().selectedIndexProperty().get();

            if(!validateFormField(txtUsername, "Invalid Username", "please enter a valid username", "^.*(?=.{5,}).*"))
                return;
            if(!validateFormField(txtPassword, "Invalid Password", "please enter a valid password", "^.*(?=.{8,}).*"))//(?=[a-zA-Z])(?=.*[0-9])(?=.*[@#!$%^&*-+=])
                return;
            if(!validateFormField(txtFirstname, "Invalid Firstname", "please enter a valid first name", "^.*(?=.{1,}).*"))
                return;
            if(!validateFormField(txtLastname, "Invalid Lastname", "please enter a valid last name", "^.*(?=.{1,}).*"))
                return;

            if(sex_index<0)
            {
                cbxSex.getStyleClass().remove("form-control-default");
                cbxSex.getStyleClass().add("control-input-error");
            }else{
                cbxSex.getStyleClass().remove("control-input-error");
                cbxSex.getStyleClass().add("form-control-default");
            }
            if(!validateFormField(txtEmail, "Invalid Email", "please enter a valid email address", "^.*(?=.{5,})(?=(.*@.*\\.)).*"))
                return;
            if(!validateFormField(txtTelephone, "Invalid Telephone Number", "please enter a valid telephone number", "^.*(?=.{10,}).*"))
                return;
            if(!validateFormField(txtCellphone, "Invalid Cellphone Number", "please enter a valid cellphone number", "^.*(?=.{10,}).*"))
                return;

            //all valid, send data to server
            int access_level_index = cbxAccessLevel.getSelectionModel().getSelectedIndex();
            if(access_level_index>=0)
            {
                int access_lvl=0;
                if(access_levels[access_level_index].toLowerCase().equals("super"))
                    access_lvl=3;
                if(access_levels[access_level_index].toLowerCase().equals("admin"))
                    access_lvl=2;
                if(access_levels[access_level_index].toLowerCase().equals("normal"))
                    access_lvl=1;
                if(access_levels[access_level_index].toLowerCase().equals("none"))
                    access_lvl=0;

                ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
                params.add(new AbstractMap.SimpleEntry<>("usr", txtUsername.getText()));
                params.add(new AbstractMap.SimpleEntry<>("pwd", txtPassword.getText()));
                params.add(new AbstractMap.SimpleEntry<>("access_level", String.valueOf(access_lvl)));
                params.add(new AbstractMap.SimpleEntry<>("firstname", txtFirstname.getText()));
                params.add(new AbstractMap.SimpleEntry<>("lastname", txtLastname.getText()));
                params.add(new AbstractMap.SimpleEntry<>("gender", cbxSex.getItems().get(sex_index).toString()));
                params.add(new AbstractMap.SimpleEntry<>("email", txtEmail.getText()));
                params.add(new AbstractMap.SimpleEntry<>("tel", txtTelephone.getText()));
                params.add(new AbstractMap.SimpleEntry<>("cell", txtCellphone.getText()));
                if(txtOther.getText()!=null)
                    params.add(new AbstractMap.SimpleEntry<>("other", txtOther.getText()));

                try
                {
                    HttpURLConnection connection = RemoteComms.postData("/user/add", params, null);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    {
                        IO.logAndAlert("Account Creation Success", "Successfully created a new user!", IO.TAG_INFO);

                        //dismiss stage if successful
                        Platform.runLater(() ->
                        {
                            if(stage!=null)
                                if(stage.isShowing())
                                    stage.close();
                        });

                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(IO.readStream(connection.getInputStream()));
                    } else
                    {
                        IO.logAndAlert("Account Creation Failure", IO
                                .readStream(connection.getErrorStream()), IO.TAG_ERROR);
                        //execute callback w/o args
                        if(callback!=null)
                            callback.call(null);
                    }
                    //close connection
                    if(connection!=null)
                        connection.disconnect();
                } catch (IOException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                }
            }
        });
        //Add form controls vertically on the stage
        vbox.getChildren().add(username);
        vbox.getChildren().add(password);
        vbox.getChildren().add(first_name);
        vbox.getChildren().add(last_name);
        vbox.getChildren().add(sex);
        vbox.getChildren().add(email);
        vbox.getChildren().add(telephone);
        vbox.getChildren().add(cellphone);
        vbox.getChildren().add(access_level);
        vbox.getChildren().add(other);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File("src/mvg/styles/home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.onHidingProperty().addListener((observable, oldValue, newValue) ->
                forceSynchronise());

        stage.setScene(scene);
        stage.show();
    }

    private boolean validateFormField(TextField txt, String errTitle, String errMsg, String regex)
    {
        if(!Validators.isValidNode(txt, txt.getText(), regex))
        {
            //IO.logAndAlert(errTitle, errMsg, IO.TAG_ERROR);
            IO.log(getClass().getName(), IO.TAG_ERROR, errMsg);
            return false;
        }
        return true;
    }

    class UserServerObject extends ServerObject
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
            private User[] users;

            public User[] get_users()
            {
                return users;
            }

            public void set_users(User[] users)
            {
                this.users = users;
            }
        }
    }
}
