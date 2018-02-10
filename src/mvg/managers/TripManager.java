package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import mvg.auxilary.*;
import mvg.model.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ghost on 2017/01/11.
 */
public class TripManager extends MVGObjectManager
{
    private HashMap<String, Trip> trips;
    private Gson gson;
    private static TripManager trip_manager = new TripManager();
    public static final String TAG = "TripManager";
    public static final String ROOT_PATH = "cache/trips/";
    public String filename = "";
    private long timestamp;

    private TripManager()
    {
    }

    @Override
    public void initialize()
    {
        synchroniseDataset();
    }

    public static TripManager getInstance()
    {
        return trip_manager;
    }

    @Override
    public HashMap<String, Trip> getDataset()
    {
        return this.trips;
    }

    @Override
    Callback getSynchronisationCallback()
    {
        return new Callback()
        {
            @Override
            public Object call(Object param)
            {
                try
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
                            String timestamp_json = RemoteComms.sendGetRequest("/timestamp/trips_timestamp", headers);
                            Counters cntr_timestamp = gson.fromJson(timestamp_json, Counters.class);
                            if (cntr_timestamp != null)
                            {
                                timestamp = cntr_timestamp.getCount();
                                filename = "trips_" + timestamp + ".dat";
                                IO.log(this.getClass().getName(), IO.TAG_INFO, "Server Timestamp: " + timestamp);
                            }
                            else
                            {
                                IO.log(this.getClass().getName(), IO.TAG_ERROR, "could not get valid timestamp");
                                return null;
                            }

                            if (!isSerialized(ROOT_PATH + filename))
                            {
                                //Load Trip objects from server
                                String trips_json = RemoteComms.sendGetRequest("/trips", headers);
                                TripServerObject tripServerObject = gson.fromJson(trips_json, TripServerObject.class);
                                if (tripServerObject != null)
                                {
                                    if (tripServerObject.get_embedded() != null)
                                    {
                                        Trip[] trips_arr = tripServerObject.get_embedded().getTrips();

                                        trips = new HashMap<>();
                                        for (Trip trip : trips_arr)
                                        {
                                            trips.put(trip.get_id(), trip);
                                        }
                                    }
                                    else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Trips in the database.");
                                }
                                if (trips != null)
                                {
                                    for (Trip trip : trips.values())
                                    {
                                        //Load TripDriver objects using Trip_id
                                        String trip_users_json = RemoteComms.sendGetRequest("/trips/drivers/" + trip.get_id(), headers);
                                        if(trip_users_json!=null)
                                        {
                                            TripUserServerObject tripUserServerObject = gson.fromJson(trip_users_json, TripUserServerObject.class);
                                            if (tripUserServerObject != null)
                                            {
                                                if(tripUserServerObject.get_embedded()!=null)
                                                {
                                                    TripDriver[] trip_users_arr = tripUserServerObject.get_embedded().getTripusers();
                                                    if (trip_users_arr != null)
                                                    {
                                                        // make User[] of same size as TripDriver[]
                                                        User[] users_arr = new User[trip_users_arr.length];
                                                        // Load actual User objects from TripDriver[] objects
                                                        int i = 0;
                                                        for (TripDriver tripDriver : trip_users_arr)
                                                            if (UserManager.getInstance().getDataset() != null)
                                                                users_arr[i++] = UserManager.getInstance().getDataset().get(tripDriver
                                                                        .getUsr());
                                                            else IO.log(getClass()
                                                                    .getName(), IO.TAG_ERROR, "no Users found in database.");
                                                        // Set User objects on to Trip object.
                                                        trip.setAssigned_users(users_arr);
                                                    } else IO.log(getClass()
                                                            .getName(), IO.TAG_ERROR, "could not load assigned Drivers for trip #" + trip
                                                            .get_id());

                                                } else IO.log(getClass()
                                                        .getName(), IO.TAG_ERROR, "could not load assigned Drivers for trip #"
                                                        + trip.get_id()+". Could not find any TripDriver documents in collection.");
                                            } else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid TripDriverServerObject for Trip#"+trip.get_id());
                                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "Trip#"+trip.get_id() + " has no assigned Users.");
                                    }
                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Trips in the database.");
                                IO.log(getClass().getName(), IO.TAG_INFO, "reloaded collection of trips.");
                                serialize(ROOT_PATH + filename, trips);
                            } else
                            {
                                IO.log(this.getClass()
                                        .getName(), IO.TAG_INFO, "binary object [" + ROOT_PATH + filename + "] on local disk is already up-to-date.");
                                trips = (HashMap<String, Trip>) deserialize(ROOT_PATH + filename);
                            }
                        } else IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
                    } else IO.logAndAlert("Session Expired", "No valid active sessions found.", IO.TAG_ERROR);
                } catch (ClassNotFoundException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                } catch (IOException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                }
                return null;
            }
        };
    }

    /**
     * Method to create new Trip object on the database server.
     * @param trip Trip object to be created.
     * @param callback Callback to be executed on if request was successful.
     * @return server response.
     */
    public String createNewTrip(Trip trip, Callback callback)
    {
        try
        {
            ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
            headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
            headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));

            //create new trip on database
            HttpURLConnection connection = RemoteComms.putJSON("/trips", trip.asJSONString(), headers);
            if(connection!=null)
            {
                if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                {
                    String response = IO.readStream(connection.getInputStream());
                    //server will return message object in format "<trip_id>"
                    String new_trip_id = response.replaceAll("\"","");//strip inverted commas around trip_id
                    new_trip_id = new_trip_id.replaceAll("\n","");//strip new line chars
                    new_trip_id = new_trip_id.replaceAll(" ","");//strip whitespace chars
                    IO.log(getClass().getName(), IO.TAG_INFO, "successfully created a new trip: " + new_trip_id);

                    TripManager.getInstance().forceSynchronise();

                    if(connection!=null)
                        connection.disconnect();

                    //execute Callback w/ args
                    if(callback!=null)
                        callback.call(new_trip_id);

                    return new_trip_id;
                } else
                {
                    //Get error message
                    String msg = IO.readStream(connection.getErrorStream());
                    IO.logAndAlert("Error " +String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);

                    //execute Callback w/o args
                    if(callback!=null)
                        callback.call(null);

                    if(connection!=null)
                        connection.disconnect();
                    return null;
                }
            } else IO.logAndAlert("Error: Trip Creation Failure", "Could not connect to server.", IO.TAG_ERROR);
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
        return null;
    }

    /**
     * Method to send request to server to sign a Trip.
     * @param trip Trip object to be signed.
     * @param callback Callback to be executed on successful request.
     */
    public static void approveTrip(Trip trip, Callback callback)
    {
        if(trip==null)
        {
            IO.logAndAlert("Error: Invalid Trip", "Selected Trip object is invalid.", IO.TAG_ERROR);
            return;
        }
        //TODO: stricter validation before approval
        /*if(trip.getDate_started()<=0)
        {
            IO.logAndAlert("Error: Trip Start Date Invalid", "Selected Trip has not been started yet.", IO.TAG_ERROR);
            return;
        }*/

        trip.setStatus(MVGObject.STATUS_APPROVED);
        updateTrip(trip, callback);
    }

    public static void updateTrip(Trip trip, Callback callback)
    {
        if(trip==null)
        {
            IO.logAndAlert("Error: Invalid Trip", "Selected Trip object is invalid.", IO.TAG_ERROR);
            return;
        }
        //TODO: stricter validation before approval
        /*if(trip.getDate_started()<=0)
        {
            IO.logAndAlert("Error: Trip Start Date Invalid", "Selected Trip has not been started yet.", IO.TAG_ERROR);
            return;
        }*/
        ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<AbstractMap.SimpleEntry<String, String>>();
        headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));

        Session active = SessionManager.getInstance().getActive();
        try
        {
            if (active != null)
            {
                if(SessionManager.getInstance().getActiveUser().getAccessLevel()>=AccessLevels.SUPERUSER.getLevel())
                {
                    if (!active.isExpired())
                    {
                        headers.add(new AbstractMap.SimpleEntry<>("Cookie", active.getSessionId()));
                        HttpURLConnection conn = RemoteComms.postJSON(trip.apiEndpoint(), trip.asJSONString(), headers);
                        if (conn != null)
                        {
                            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
                            {
                                IO.logAndAlert("Success", "Successfully updated Trip[" + trip.get_id() + "]", IO.TAG_INFO);
                                //execute callback w/ params
                                if (callback != null)
                                    callback.call(true);
                            } else
                            {
                                IO.logAndAlert("Error", "Could not update Trip[" + trip.get_id() + "]: "
                                        + IO.readStream(conn.getErrorStream()), IO.TAG_ERROR);

                                //execute callback w/o params
                                if (callback != null)
                                    callback.call(null);
                            }
                            conn.disconnect();
                        } else IO.logAndAlert("Error", "Connection to server has been lost..", IO.TAG_ERROR);
                    } else IO.logAndAlert("Error: Session Expired", "Active session has expired.", IO.TAG_ERROR);
                } else IO.logAndAlert("Error: Unauthorised", "Active session is not authorised to perform this action.", IO.TAG_ERROR);
            } else IO.logAndAlert("Error: Session Expired", "No active sessions.", IO.TAG_ERROR);
        } catch (IOException e)
        {
            IO.log(TripManager.class.getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    public static void assignTripDrivers(Trip trip, User[] drivers, Callback callback)
    {
        if(trip==null)
        {
            IO.logAndAlert("Error: Invalid Trip", "Selected Trip object is invalid.", IO.TAG_ERROR);
            return;
        }
        if(drivers==null)
        {
            IO.logAndAlert("Error: Invalid Drivers List", "No drivers were chosen to be assigned to trip #"+ trip.get_id(), IO.TAG_ERROR);
            return;
        }

        ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<AbstractMap.SimpleEntry<String, String>>();
        headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));

        Session active = SessionManager.getInstance().getActive();
        trip.setStatus(MVGObject.STATUS_APPROVED);
        try
        {
            if (active != null)
            {
                if(SessionManager.getInstance().getActiveUser().getAccessLevel()>=AccessLevels.SUPERUSER.getLevel())
                {
                    if (!active.isExpired())
                    {
                        headers.add(new AbstractMap.SimpleEntry<>("Cookie", active.getSessionId()));

                        //create TripDriver objects on server
                        boolean all_successful=true;
                        for(User driver: drivers)
                        {
                            TripDriver trip_driver = new TripDriver();
                            trip_driver.setTrip_id(trip.get_id());
                            trip_driver.setUsr(driver.getUsr());
                            trip_driver.setCreator(active.getUsername());

                            HttpURLConnection conn = RemoteComms
                                    .putJSON(trip_driver.apiEndpoint(), trip_driver.asJSONString(), headers);
                            if (conn != null)
                            {
                                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
                                {
                                    IO.logAndAlert("Error", "Could not assign drivers to Trip[" + trip.get_id() + "]: "
                                            + IO.readStream(conn.getErrorStream()), IO.TAG_ERROR);
                                    all_successful=false;
                                }
                                conn.disconnect();
                            } else IO.logAndAlert("Error", "Connection to server has been lost..", IO.TAG_ERROR);
                        }
                        if(all_successful)
                        {
                            IO.logAndAlert("Success", "Successfully assigned [" + drivers.length + "] drivers to Trip[" + trip
                                    .get_id() + "]", IO.TAG_INFO);

                            //update Trips date_assigned property
                            trip.setDate_assigned(System.currentTimeMillis());
                            updateTrip(trip, callback);
                            /*//execute callback
                            if (callback != null)
                                callback.call(true);*/
                        }//error message would've been shown in the loop already
                        else if (callback != null)
                                callback.call(null);
                    } else IO.logAndAlert("Error: Session Expired", "Active session has expired.", IO.TAG_ERROR);
                } else IO.logAndAlert("Error: Unauthorised", "Active session is not authorised to perform this action.", IO.TAG_ERROR);
            } else IO.logAndAlert("Error: Session Expired", "No active sessions.", IO.TAG_ERROR);
        } catch (IOException e)
        {
            IO.log(TripManager.class.getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    public void requestTripApproval(Trip trip, Callback callback) throws IOException
    {
        if(trip==null)
        {
            IO.logAndAlert("Error", "Invalid Trip.", IO.TAG_ERROR);
            return;
        }
        if(trip.getQuote()==null)
        {
            IO.logAndAlert("Error", "Invalid Trip->Quote.", IO.TAG_ERROR);
            return;
        }
        if(trip.getQuote().getClient()==null)
        {
            IO.logAndAlert("Error", "Invalid Trip->Quote->Client.", IO.TAG_ERROR);
            return;
        }
        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert("Error", "Could not find any users in the system.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive()==null)
        {
            IO.logAndAlert("Error: Invalid Session", "Could not find any valid sessions.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActive().isExpired())
        {
            IO.logAndAlert("Error: Session Expired", "The active session has expired.", IO.TAG_ERROR);
            return;
        }
        if(SessionManager.getInstance().getActiveUser()==null)
        {
            IO.logAndAlert("Error: Invalid User Session", "Could not find any active user sessions.", IO.TAG_ERROR);
            return;
        }
        /*String path = PDF.createTripCardPdf(trip);
        String base64_trip = null;
        if(path!=null)
        {
            File f = new File(path);
            if (f != null)
            {
                if (f.exists())
                {
                    FileInputStream in = new FileInputStream(f);
                    byte[] buffer =new byte[(int) f.length()];
                    in.read(buffer, 0, buffer.length);
                    in.close();
                    base64_trip = Base64.getEncoder().encodeToString(buffer);
                } else
                {
                    IO.logAndAlert(TripManager.class.getName(), "File [" + path + "] not found.", IO.TAG_ERROR);
                }
            } else
            {
                IO.log(TripManager.class.getName(), "File [" + path + "] object is null.", IO.TAG_ERROR);
            }
        } else IO.log(TripManager.class.getName(), "Could not get valid path for created Trip pdf.", IO.TAG_ERROR);*/
        final String finalBase64_trip = "base64_trip";

        Stage stage = new Stage();
        stage.setTitle(Globals.APP_NAME.getValue() + " - Request Trip ["+trip.get_id()+"] Approval");
        stage.setMinWidth(320);
        stage.setHeight(350);
        stage.setAlwaysOnTop(true);

        VBox vbox = new VBox(1);

        final TextField txt_subject = new TextField();
        txt_subject.setMinWidth(200);
        txt_subject.setMaxWidth(Double.MAX_VALUE);
        txt_subject.setPromptText("Type in an eMail subject");
        txt_subject.setText("TRIP ["+trip.get_id()+"] APPROVAL REQUEST");
        HBox subject = CustomTableViewControls.getLabelledNode("Subject: ", 200, txt_subject);

        final TextArea txt_message = new TextArea();
        txt_message.setMinWidth(200);
        txt_message.setMaxWidth(Double.MAX_VALUE);
        HBox message = CustomTableViewControls.getLabelledNode("Message: ", 200, txt_message);

        //set default message
        User sender = SessionManager.getInstance().getActiveUser();
        String title = sender.getGender().toLowerCase().equals("male") ? "Mr." : "Miss.";;
        String def_msg = "Good day,\n\nCould you please assist me" +
                " by approving this trip to be issued to "  + trip.getQuote().getClient().getClient_name() + ".\nThank you.\n\nBest Regards,\n"
                + title + " " + sender.getFirstname().toCharArray()[0]+". "+sender.getLastname();
        txt_message.setText(def_msg);

        HBox submit;
        submit = CustomTableViewControls.getSpacedButton("Send", event ->
        {
            String date_regex="\\d+(\\-|\\/|\\\\)\\d+(\\-|\\/|\\\\)\\d+";

            //TODO: check this
            //if(!Validators.isValidNode(cbx_destination, cbx_destination.getValue()==null?"":cbx_destination.getValue().getEmail(), 1, ".+"))
            //    return;
            if(!Validators.isValidNode(txt_subject, txt_subject.getText(), 1, ".+"))
                return;
            if(!Validators.isValidNode(txt_message, txt_message.getText(), 1, ".+"))
                return;

            String msg = txt_message.getText();

            //convert all new line chars to HTML break-lines
            msg = msg.replaceAll("\\n", "<br/>");

            //ArrayList<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<>();
            //params.add(new AbstractMap.SimpleEntry<>("message", msg));

            try
            {
                //send email
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));//multipart/form-data
                headers.add(new AbstractMap.SimpleEntry<>("trip_id", trip.get_id()));
                //headers.add(new AbstractMap.SimpleEntry<>("to_email", cbx_destination.getValue().getEmail()));
                headers.add(new AbstractMap.SimpleEntry<>("message", msg));
                headers.add(new AbstractMap.SimpleEntry<>("subject", txt_subject.getText()));

                if(SessionManager.getInstance().getActive()!=null)
                {
                    headers.add(new AbstractMap.SimpleEntry<>("session_id", SessionManager.getInstance().getActive().getSessionId()));
                    headers.add(new AbstractMap.SimpleEntry<>("from_name", SessionManager.getInstance().getActiveUser().getName()));
                } else
                {
                    IO.logAndAlert( "No active sessions.", "Session expired", IO.TAG_ERROR);
                    return;
                }

                FileMetadata fileMetadata = new FileMetadata("trip_"+trip.get_id()+".pdf","application/pdf");
                fileMetadata.setFile(finalBase64_trip);
                HttpURLConnection connection = RemoteComms.postJSON("/trips/approval_request", fileMetadata.asJSONString(), headers);
                if(connection!=null)
                {
                    if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
                    {
                        //TODO: CC self
                        IO.logAndAlert("Success", "Successfully requested Trip approval!", IO.TAG_INFO);

                        //dismiss stage if successful
                        Platform.runLater(() ->
                        {
                            if(stage!=null)
                                if(stage.isShowing())
                                    stage.close();
                        });

                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(true);
                    } else
                    {
                        IO.logAndAlert( "ERROR " + connection.getResponseCode(),  IO.readStream(connection.getErrorStream()), IO.TAG_ERROR);
                        //execute callbacks w/o args
                        if(callback!=null)
                            callback.call(null);
                    }
                    connection.disconnect();
                }
            } catch (IOException e)
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }
        });

        //Add form controls vertically on the stage
        vbox.getChildren().add(subject);
        vbox.getChildren().add(message);
        vbox.getChildren().add(submit);

        //Setup scene and display stage
        Scene scene = new Scene(vbox);
        File fCss = new File(IO.STYLES_ROOT_PATH+"home.css");
        scene.getStylesheets().clear();
        scene.getStylesheets().add("file:///"+ fCss.getAbsolutePath().replace("\\", "/"));

        stage.setScene(scene);
        stage.show();
        stage.centerOnScreen();
        stage.setResizable(true);
    }

    /**
     * Method to show Trip representatives
     * @param trip Trip object whose representatives are to be shown.
     */
    public static void showTripReps(Trip trip)
    {
        /*SessionManager smgr = SessionManager.getInstance();
        if(smgr.getActive()!=null)
        {
            if(!smgr.getActive().isExpired())
            {
                Stage stage = new Stage();
                stage.setTitle(Globals.APP_NAME.getValue() + " - trip representatives");
                stage.setMinWidth(320);
                stage.setMinHeight(340);
                //stage.setAlwaysOnTop(true);

                tblTripRepresentatives = new TableView();
                tblTripRepresentatives.setEditable(true);

                TableColumn<BusinessObject, String> resource_id = new TableColumn<>("User ID");
                resource_id.setMinWidth(100);
                resource_id.setCellValueFactory(new PropertyValueFactory<>("short_id"));

                TableColumn<BusinessObject, String> firstname = new TableColumn("First name");
                CustomTableViewControls.makeEditableTableColumn(firstname, TextFieldTableCell.forTableColumn(), 80, "firstname", "/api/user");

                TableColumn<BusinessObject, String> lastname = new TableColumn("Last name");
                CustomTableViewControls.makeEditableTableColumn(lastname, TextFieldTableCell.forTableColumn(), 80, "lastname", "/api/user");

                //TableColumn<BusinessObject, String> access_level = new TableColumn("Access level");
                //CustomTableViewControls.makeEditableTableColumn(access_level, TextFieldTableCellOld.forTableColumn(), 80, "access_level", "/api/user");

                TableColumn<BusinessObject, String> gender = new TableColumn("Gender");
                CustomTableViewControls.makeComboBoxTableColumn(gender, genders, "gender", "gender", "/api/user", 80);

                TableColumn<BusinessObject, String> email_address = new TableColumn("eMail address");
                CustomTableViewControls.makeEditableTableColumn(email_address, TextFieldTableCell.forTableColumn(), 80, "email", "/api/user");

                TableColumn<BusinessObject, Long> date_joined = new TableColumn("Date joined");
                CustomTableViewControls.makeDatePickerTableColumn(date_joined, "date_joined", "/api/user");

                TableColumn<BusinessObject, String> tel = new TableColumn("Tel. number");
                CustomTableViewControls.makeEditableTableColumn(tel, TextFieldTableCell.forTableColumn(), 80, "tel", "/api/user");

                TableColumn<BusinessObject, String> cell = new TableColumn("Cell number");
                CustomTableViewControls.makeEditableTableColumn(cell, TextFieldTableCell.forTableColumn(), 80, "cell", "/api/user");

                TableColumn<BusinessObject, String> domain = new TableColumn("Domain");
                CustomTableViewControls.makeComboBoxTableColumn(domain, domains, "active", "domain", "/api/user", 80);
                //CustomTableViewControls.makeEditableTableColumn(other, TextFieldTableCellOld.forTableColumn(), 80, "other", "/api/quote/resource");

                TableColumn<BusinessObject, String> other = new TableColumn("Other");
                CustomTableViewControls.makeEditableTableColumn(other, TextFieldTableCell.forTableColumn(), 80, "other", "/api/user");

                MenuBar menu_bar = new MenuBar();
                Menu file = new Menu("File");
                Menu edit = new Menu("Edit");

                MenuItem new_resource = new MenuItem("New representative");
                new_resource.setOnAction(event -> handleNewTripRep(stage));
                MenuItem save = new MenuItem("Save");
                MenuItem print = new MenuItem("Print");


                ObservableList<User> lst_trip_reps = FXCollections.observableArrayList();

                //Quote selected_quote = (Quote) tblQuotes.selectionModelProperty().get();
                //make fancy "New representative" label - not really necessary though
                if(trips!=null)
                {
                    if(trip.getAssigned_drivers()!=null)
                    {
                       lst_trip_reps.addAll(trip.getAssigned_drivers());
                       IO.log(TAG, IO.TAG_INFO, String.format("trip '%s'  has %s representatives.", trip.get_id(), trip.getAssigned_drivers().length));
                       IO.log(TAG, IO.TAG_INFO, String.format("added trip '%s' representatives.", trip.get_id()));
                        /*for (BusinessObject businessObject : organisations)
                        {
                            if(businessObject.get_id()!=null)
                            {
                                if (businessObject.get_id().equals(quotes[selected_index].get("issuer_org_id")))
                                {
                                    if (label_properties.split("\\|").length > 1)
                                    {
                                        String name = (String) businessObject.get(label_properties.split("\\|")[0]);
                                        if (name == null)
                                            name = (String) businessObject.get(label_properties.split("\\|")[1]);
                                        if (name == null)
                                            IO.log(TAG, IO.TAG_ERROR, "neither of the label_properties were found in object!");
                                        else
                                        {
                                            new_resource.setText("New representative for quote issued by " + name);
                                            IO.log(TAG, IO.TAG_INFO, String.format("set quote [representative] context to [quote issued by] '%s'", name));
                                        }
                                    } else IO.log(TAG, IO.TAG_ERROR, "label_properties split array index out of bounds!");
                                }
                            }else IO.log(TAG, IO.TAG_WARN, "business object id is null.");
                        }*
                    }else IO.log(TAG, IO.TAG_ERROR, String.format("assigned_users of selected trip '%s' is null.", trip.get_id()));
                }else IO.log(TAG, IO.TAG_ERROR, "trips array is null!");

                tblTripRepresentatives.setItems(lst_trip_reps);
                tblTripRepresentatives.getColumns().addAll(firstname, lastname, gender,
                        email_address, date_joined, tel, cell, domain, other);


                file.getItems().addAll(new_resource, save, print);

                menu_bar.getMenus().addAll(file, edit);

                BorderPane border_pane = new BorderPane();
                border_pane.setTop(menu_bar);
                border_pane.setCenter(tblTripRepresentatives);

                stage.setOnCloseRequest(event ->
                {
                    IO.log(TAG, IO.TAG_INFO,"reloading local data.");
                    loadDataFromServer();
                    stage.close();
                });

                Scene scene = new Scene(border_pane);
                stage.setScene(scene);
                stage.show();
                stage.centerOnScreen();
                stage.setResizable(true);
            }else IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
        }else IO.logAndAlert("Session Expired", "No active sessions.", IO.TAG_ERROR);*/
    }

    class TripServerObject extends ServerObject
    {
        private TripServerObject.Embedded _embedded;

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
            private Trip[] trips;

            public Trip[] getTrips()
            {
                return trips;
            }

            public void setTrips(Trip[] trips)
            {
                this.trips = trips;
            }
        }
    }

    class TripUserServerObject extends ServerObject
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
            private TripDriver[] tripusers;

            public TripDriver[] getTripusers()
            {
                return tripusers;
            }

            public void setTripusers(TripDriver[] tripusers)
            {
                this.tripusers = tripusers;
            }
        }
    }
}