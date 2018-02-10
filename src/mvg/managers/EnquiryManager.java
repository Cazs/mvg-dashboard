package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.util.Callback;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.*;

import mvg.auxilary.*;
import mvg.model.Enquiry;

/**
 * Created by ghost on 2018/01/13.
 */
public class EnquiryManager extends MVGObjectManager
{
    private HashMap<String, Enquiry> enquiries;
    private Gson gson;
    private static EnquiryManager enquiry_manager = new EnquiryManager();
    private long timestamp;
    public static final String ROOT_PATH = "cache/enquiries/";
    public String filename = "";
    public static final double VAT = 14.0;

    private EnquiryManager()
    {
    }

    @Override
    public void initialize()
    {
        synchroniseDataset();
    }

    public static EnquiryManager getInstance()
    {
        return enquiry_manager;
    }

    @Override
    public HashMap<String, Enquiry> getDataset()
    {
        return enquiries;
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

                            gson  = new GsonBuilder().create();
                            ArrayList<AbstractMap.SimpleEntry<String,String>> headers = new ArrayList<>();
                            headers.add(new AbstractMap.SimpleEntry<>("Cookie", smgr.getActive().getSessionId()));
                            //Get Timestamp
                            String enquiries_timestamp_json = RemoteComms.sendGetRequest("/timestamp/enquiries_timestamp", headers);
                            Counter enquiries_timestamp = gson.fromJson(enquiries_timestamp_json, Counter.class);
                            if(enquiries_timestamp!=null)
                            {
                                timestamp = enquiries_timestamp.getCount();
                                filename = "enquiries_"+timestamp+".dat";
                                IO.log(EnquiryManager.getInstance().getClass().getName(), IO.TAG_INFO, "Server Timestamp: "+enquiries_timestamp.getCount());
                            } else {
                                IO.logAndAlert(this.getClass().getName(), "could not get valid timestamp", IO.TAG_ERROR);
                                return null;
                            }

                            if(!isSerialized(ROOT_PATH+filename))
                            {
                                //Load Enquiries
                                String enquiries_json = RemoteComms.sendGetRequest("/enquiries", headers);
                                EnquiryServerObject enquiryServerObject = gson.fromJson(enquiries_json, EnquiryServerObject.class);
                                if(enquiryServerObject!=null)
                                {
                                    if(enquiryServerObject.get_embedded()!=null)
                                    {
                                        Enquiry[] enquiries_arr = enquiryServerObject.get_embedded().getEnquiries();
                                        enquiries = new HashMap<>();
                                        for (Enquiry enquiry : enquiries_arr)
                                            enquiries.put(enquiry.get_id(), enquiry);
                                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Enquiries in the database.");
                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "EnquiryServerObject (containing Enquiry objects & other metadata) is null");

                                IO.log(getClass().getName(), IO.TAG_INFO, "reloaded collection of enquiries.");
                                serialize(ROOT_PATH + filename, enquiries);
                            } else {
                                IO.log(this.getClass().getName(), IO.TAG_INFO, "binary object ["+ROOT_PATH+filename+"] on local disk is already up-to-date.");
                                enquiries = (HashMap<String, Enquiry>) deserialize(ROOT_PATH+filename);
                            }
                        } else IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
                    } else IO.logAndAlert("Invalid Session", "No valid active sessions were found.", IO.TAG_ERROR);
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

    public void createEnquiry(Enquiry enquiry, Callback callback) throws IOException
    {
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

        ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
        headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
        headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));

        //create new enquiry on database
        HttpURLConnection connection = RemoteComms.putJSON("/enquiries", enquiry.asJSONString(), headers);
        if(connection!=null)
        {
            if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
            {
                String response = IO.readStream(connection.getInputStream());

                if(response==null)
                {
                    IO.logAndAlert("Enquiry Creation Error", "Invalid server response.", IO.TAG_ERROR);
                    return;
                }
                if(response.isEmpty())
                {
                    IO.logAndAlert("Enquiry Creation Error", "Invalid server response.", IO.TAG_ERROR);
                    return;
                }

                //server will return message object in format "<enquiry_id>"
                String new_enquiry_id = response.replaceAll("\"","");//strip inverted commas around enquiry_id
                new_enquiry_id = new_enquiry_id.replaceAll("\n","");//strip new line chars
                new_enquiry_id = new_enquiry_id.replaceAll(" ","");//strip whitespace chars

                if(callback!=null)
                    callback.call(new_enquiry_id);
                //Close connection
                if(connection!=null)
                    connection.disconnect();
            } else
            {
                //Get error message
                String msg = IO.readStream(connection.getErrorStream());
                IO.logAndAlert("Error " +String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
            }
            if(connection!=null)
                connection.disconnect();
        } else IO.logAndAlert("New Enquiry Creation Failure", "Could not connect to server.", IO.TAG_ERROR);
    }

    public void updateEnquiry(Enquiry enquiry, Callback callback)
    {
        if (enquiry == null)
        {
            IO.logAndAlert("Invalid Enquiry", "Invalid Enquiry object.", IO.TAG_ERROR);
            return;
        }
        if (SessionManager.getInstance().getActive() == null)
        {
            IO.logAndAlert("Session Expired", "No active sessions.", IO.TAG_ERROR);
            return;
        }
        if (SessionManager.getInstance().getActive().isExpired())
        {
            IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
            return;
        }

        //Enquiry selected = getSelectedEnquiry();
        if(enquiry !=null)
        {
            //prepare enquiry parameters
            try
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                //update Enquiry on database
                HttpURLConnection connection = RemoteComms.postJSON("/enquiries/"+ enquiry.get_id(), enquiry.asJSONString(), headers);
                if (connection != null)
                {
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    {
                        String response = IO.readStream(connection.getInputStream());
                        IO.log(getClass().getName(), IO.TAG_INFO, "updated Enquiry[" + enquiry.get_id() + "].");

                        if (response == null)
                        {
                            IO.logAndAlert("Enquiry Update Error", "Invalid server response.", IO.TAG_ERROR);
                            return;
                        }
                        if (response.isEmpty())
                        {
                            IO.logAndAlert("Enquiry Update Error", "Invalid server response: " + response, IO.TAG_ERROR);
                            return;
                        }

                        IO.logAndAlert("Enquiry Manager","successfully updated Enquiry[" + enquiry.get_id() + "].", IO.TAG_INFO);
                        forceSynchronise();

                        //execute callback w/ args
                        if(callback!=null)
                            callback.call(true);
                    } else
                    {
                        //Get error message
                        String msg = IO.readStream(connection.getErrorStream());
                        IO.logAndAlert("Error " + String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
                        //execute callback w/o args
                        if(callback!=null)
                            callback.call(null);
                    }
                    //Close connection
                    if (connection != null)
                        connection.disconnect();
                } else IO.logAndAlert("Enquiry Update Failure", "Could not connect to server.", IO.TAG_ERROR);
            } catch (IOException e)
            {
                IO.logAndAlert(getClass().getName(), e.getMessage(), IO.TAG_ERROR);
            }
        }else IO.logAndAlert("Update Enquiry","Selected Enquiry is invalid.", IO.TAG_ERROR);
    }

    class EnquiryServerObject extends ServerObject
    {
        private EnquiryServerObject.Embedded _embedded;

        EnquiryServerObject.Embedded get_embedded()
        {
            return _embedded;
        }

        void set_embedded(EnquiryServerObject.Embedded _embedded)
        {
            this._embedded = _embedded;
        }

        class Embedded
        {
            private Enquiry[] enquiries;

            public Enquiry[] getEnquiries()
            {
                return enquiries;
            }

            public void setEnquiries(Enquiry[] enquiries)
            {
                this.enquiries = enquiries;
            }
        }
    }
}
