package mvg.model;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.auxilary.IO;
import mvg.managers.ClientManager;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author ghost
 */
public class Enquiry extends MVGObject implements Serializable
{
    private String enquiry;
    private String pickup_location;
    private String destination;
    private String trip_type;
    private String comments;
    private String client_id;
    private long date_scheduled;
    public static final String TAG = "Enquiry";

    public String getEnquiry()
    {
        return enquiry;
    }

    public void setEnquiry(String enquiry)
    {
        this.enquiry = enquiry;
    }

    public String getPickup_location()
    {
        return pickup_location;
    }

    public void setPickup_location(String pickup_location)
    {
        this.pickup_location = pickup_location;
    }

    public String getDestination()
    {
        return destination;
    }

    public void setDestination(String destination)
    {
        this.destination = destination;
    }

    public String getTrip_type()
    {
        return String.valueOf(trip_type);
    }

    public void setTrip_type(String trip_type)
    {
        this.trip_type = trip_type;
    }

    public String getComments()
    {
        return comments;
    }

    public void setComments(String comments)
    {
        this.comments = comments;
    }

    public long getDate_scheduled()
    {
        return date_scheduled;
    }

    public void setDate_scheduled(long date_scheduled)
    {
        this.date_scheduled = date_scheduled;
    }

    public Client getClient()
    {
        HashMap<String, Client> clients = ClientManager.getInstance().getDataset();
        if(clients!=null)
        {
            return clients.get(client_id);
        }else IO.log(getClass().getName(), IO.TAG_ERROR, "no clients were found in database.");
        return null;
    }

    public String getClient_id()
    {
        return client_id;
    }

    public void setClient_id(String client_id)
    {
        this.client_id = client_id;
    }

    // Properties
    public StringProperty client_nameProperty()
    {
        if(getClient()!=null)
            return new SimpleStringProperty(getClient().getClient_name());
        return new SimpleStringProperty("N/A");
    }
    public StringProperty pickup_locationProperty(){return new SimpleStringProperty(pickup_location);}
    public StringProperty destinationProperty(){return new SimpleStringProperty(String.valueOf(destination));}
    public StringProperty trip_typeProperty(){return new SimpleStringProperty(String.valueOf(trip_type));}
    public StringProperty commentsProperty(){return new SimpleStringProperty(comments);}
    public LongProperty date_scheduledProperty(){return new SimpleLongProperty(getDate_scheduled());}

    @Override
    public void parse(String var, Object val)
    {
        try
        {
            switch (var.toLowerCase())
            {
                case "enquiry":
                    setEnquiry((String)val);
                    break;
                case "date_scheduled":
                    setDate_scheduled(Long.parseLong(String.valueOf(val)));
                    break;
                case "pickup_location":
                    setPickup_location((String)val);
                    break;
                case "destination":
                    setDestination((String)val);
                    break;
                case "trip_type":
                    setTrip_type((String)val);
                    break;
                case "comments":
                    setComments((String)val);
                    break;
                case "client_id":
                    setClient_id((String)val);
                    break;
                default:
                    IO.log(TAG, IO.TAG_WARN, String.format("unknown "+getClass().getName()+" attribute '%s'", var));
                    break;
            }
        }catch (NumberFormatException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @Override
    public Object get(String var)
    {
        switch (var.toLowerCase())
        {
            case "enquiry":
                return enquiry;
            case "date_scheduled":
                return date_scheduled;
            case "pickup_location":
                return pickup_location;
            case "destination":
                return destination;
            case "trip_type":
                return trip_type;
            case "comments":
                return comments;
            case "client_id":
                return client_id;
            default:
                IO.log(TAG, IO.TAG_WARN, String.format("unknown "+getClass().getName()+" attribute '%s'", var));
                return null;
        }
    }

    @Override
    public String toString()
    {
        //return String.format("[id = %s, firstname = %s, lastname = %s]", get_id(), getFirstname(), getLastname());
        return "{"+(get_id()==null?"":"\"_id\":\""+get_id()+"\", ")+
                "\"enquiry\":\""+getEnquiry()+"\""+
                ",\"client_id\":\""+getClient_id()+"\""+
                ",\"destination\":\""+getDestination()+"\""+
                ",\"pickup_location\":\""+getPickup_location()+"\""+
                ",\"trip_type\":\""+getTrip_type()+"\""+
                ",\"date_scheduled\":\""+getDate_scheduled()+"\""+
                ",\"creator\":\""+getCreator()+"\""+
                ",\"date_logged\":\""+getDate_logged()+"\""+
                "\"other\":\""+getOther()+"\"}";
    }

    @Override
    public String apiEndpoint()
    {
        return "/enquiries";
    }
}

