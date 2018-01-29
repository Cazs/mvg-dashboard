package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.util.Callback;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.*;

import mvg.auxilary.*;
import mvg.model.Inquiry;

/**
 * Created by ghost on 2018/01/13.
 */
public class InquiryManager extends MVGObjectManager
{
    private HashMap<String, Inquiry> enquiries;
    private Gson gson;
    private static InquiryManager enquiry_manager = new InquiryManager();
    private Inquiry selected_inquiry;
    private long timestamp;
    public static final String ROOT_PATH = "cache/enquiries/";
    public String filename = "";
    public static final double VAT = 14.0;

    private InquiryManager()
    {
    }

    public static InquiryManager getInstance()
    {
        return enquiry_manager;
    }

    @Override
    public void initialize()
    {
        loadDataFromServer();
    }

    public HashMap<String, Inquiry> getEnquiries()
    {
        return enquiries;
    }

    public void setSelectedEnquiry(Inquiry inquiry)
    {
        if(inquiry !=null)
        {
            this.selected_inquiry = inquiry;
            IO.log(getClass().getName(), IO.TAG_INFO, "set selected inquiry to: " + selected_inquiry.get_id());
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "inquiry to be set as selected is null.");
    }

    public void setSelectedEnquiry(String enquiry_id)
    {
        if(enquiries==null)
        {
            IO.logAndAlert(getClass().getName(), IO.TAG_ERROR, "No enquiries were found on the database.");
            return;
        }
        if(enquiries.get(enquiry_id)!=null)
        {
            setSelectedEnquiry(enquiries.get(enquiry_id));
        }
    }

    public Inquiry getSelectedEnquiry()
    {
        /*if(selected_inquiry>-1)
            return enquiries[selected_inquiry];
        else return null;*/
        return selected_inquiry;
    }

    public void nullifySelected()
    {
        this.selected_inquiry =null;
    }

    public void loadDataFromServer()
    {
        try
        {
            if(enquiries==null)
                reloadDataFromServer();
            else IO.log(getClass().getName(), IO.TAG_INFO, "enquiries object has already been set.");
        }catch (MalformedURLException ex)
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
        //enquiries = null;
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
                    IO.log(InquiryManager.getInstance().getClass().getName(), IO.TAG_INFO, "Server Timestamp: "+enquiries_timestamp.getCount());
                } else {
                    IO.logAndAlert(this.getClass().getName(), "could not get valid timestamp", IO.TAG_ERROR);
                    return;
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
                            Inquiry[] enquiries_arr = enquiryServerObject.get_embedded().getEnquiries();
                            enquiries = new HashMap<>();
                            for (Inquiry inquiry : enquiries_arr)
                                enquiries.put(inquiry.get_id(), inquiry);
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not find any Enquiries in the database.");
                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "EnquiryServerObject (containing Inquiry objects & other metadata) is null");

                    IO.log(getClass().getName(), IO.TAG_INFO, "reloaded collection of enquiries.");
                    this.serialize(ROOT_PATH + filename, enquiries);
                } else {
                    IO.log(this.getClass().getName(), IO.TAG_INFO, "binary object ["+ROOT_PATH+filename+"] on local disk is already up-to-date.");
                    enquiries = (HashMap<String, Inquiry>) this.deserialize(ROOT_PATH+filename);
                }
            } else IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
        } else IO.logAndAlert("Invalid Session", "No valid active sessions were found.", IO.TAG_ERROR);
    }

    public void generatePDF() throws IOException
    {
        if(selected_inquiry !=null)
            PDF.createEnquiryPDF(selected_inquiry);
        else IO.logAndAlert("Error", "Please choose a valid enquiry.", IO.TAG_ERROR);
    }

    public void createEnquiry(Inquiry inquiry, Callback callback) throws IOException
    {
        ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
        headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
        if(SessionManager.getInstance().getActive()!=null)
            headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
        else
        {
            IO.logAndAlert("Session Expired", "No active sessions.", IO.TAG_ERROR);
            return;
        }

        //create new inquiry on database
        HttpURLConnection connection = RemoteComms.putJSON("/enquiries", inquiry.asJSONString(), headers);
        if(connection!=null)
        {
            if(connection.getResponseCode()==HttpURLConnection.HTTP_OK)
            {
                String response = IO.readStream(connection.getInputStream());

                if(response==null)
                {
                    IO.logAndAlert("Inquiry Creation Error", "Invalid server response.", IO.TAG_ERROR);
                    return;
                }
                if(response.isEmpty())
                {
                    IO.logAndAlert("Inquiry Creation Error", "Invalid server response.", IO.TAG_ERROR);
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
        } else IO.logAndAlert("New Inquiry Creation Failure", "Could not connect to server.", IO.TAG_ERROR);
    }

    public void updateEnquiry(Inquiry inquiry)
    {
        if (inquiry == null)
        {
            IO.logAndAlert("Invalid Inquiry", "Invalid Inquiry object.", IO.TAG_ERROR);
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

        //Inquiry selected = getSelectedEnquiry();
        if(inquiry !=null)
        {
            //prepare inquiry parameters
            try
            {
                ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));
                headers.add(new AbstractMap.SimpleEntry<>("Cookie", SessionManager.getInstance().getActive().getSessionId()));
                //update Inquiry on database
                HttpURLConnection connection = RemoteComms.postJSON("/enquiries/"+ inquiry.get_id(), inquiry.asJSONString(), headers);
                if (connection != null)
                {
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    {
                        String response = IO.readStream(connection.getInputStream());
                        IO.log(getClass().getName(), IO.TAG_INFO, "updated Inquiry[" + inquiry.get_id() + "].");

                        if (response == null)
                        {
                            IO.logAndAlert("Inquiry Update Error", "Invalid server response.", IO.TAG_ERROR);
                            return;
                        }
                        if (response.isEmpty())
                        {
                            IO.logAndAlert("Inquiry Update Error", "Invalid server response: " + response, IO.TAG_ERROR);
                            return;
                        }

                        IO.logAndAlert("Inquiry Manager","successfully updated Inquiry[" + inquiry.get_id() + "].", IO.TAG_INFO);
                        loadDataFromServer();
                    } else
                    {
                        //Get error message
                        String msg = IO.readStream(connection.getErrorStream());
                        IO.logAndAlert("Error " + String.valueOf(connection.getResponseCode()), msg, IO.TAG_ERROR);
                    }
                    //Close connection
                    if (connection != null)
                        connection.disconnect();
                } else IO.logAndAlert("Inquiry Update Failure", "Could not connect to server.", IO.TAG_ERROR);
            } catch (IOException e)
            {
                IO.logAndAlert(getClass().getName(), e.getMessage(), IO.TAG_ERROR);
            }
        }else IO.logAndAlert("Update Inquiry","Selected Inquiry is invalid.", IO.TAG_ERROR);
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
            private Inquiry[] enquiries;

            public Inquiry[] getEnquiries()
            {
                return enquiries;
            }

            public void setEnquiries(Inquiry[] enquiries)
            {
                this.enquiries = enquiries;
            }
        }
    }
}
