/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.auxilary.IO;

/**
 *
 * @author ghost
 */
public class Inquiry extends MVGObject
{
    private String enquiry;
    private String pickup_location;
    private String destination;
    private String trip_type;
    private String comments;
    private long date_scheduled;
    public static final String TAG = "Inquiry";

    public String getEnquiry()
    {
        return enquiry;
    }

    public void setEnquiry(String enquiry) {
        this.enquiry = enquiry;
    }

    public String getPickup_location()
    {
        return pickup_location;
    }

    public void setPickup_location(String pickup_location) {
        this.pickup_location = pickup_location;
    }

    public String getDestination() {
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

    //Properties

    private StringProperty client_nameProperty()
    {
        if(getCreatorUser()!=null)
        {
            if (getCreatorUser().getOrganisation() != null)
                return new SimpleStringProperty(getCreatorUser().getOrganisation().getClient_name());
            else {
                IO.log(getClass().getName(), IO.TAG_ERROR, "could not get Inquiry creator's organisation.");
                return new SimpleStringProperty(getCreator());
            }
        } else
        {
          IO.log(getClass().getName(), IO.TAG_ERROR, "could not get Inquiry creator user object");
          return new SimpleStringProperty(getCreator());
        }
    }
    private StringProperty enquiryProperty(){return new SimpleStringProperty(enquiry);}
    private StringProperty pickup_locationProperty(){return new SimpleStringProperty(pickup_location);}
    private StringProperty destinationProperty(){return new SimpleStringProperty(String.valueOf(destination));}
    private StringProperty trip_typeProperty(){return new SimpleStringProperty(String.valueOf(trip_type));}
    private StringProperty commentsProperty(){return new SimpleStringProperty(comments);}
    private StringProperty date_scheduledProperty(){return new SimpleStringProperty(String.valueOf(getDate_scheduled()));}

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
                    setDate_scheduled(Long.parseLong((String)val));
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
            default:
                IO.log(TAG, IO.TAG_WARN, String.format("unknown "+getClass().getName()+" attribute '%s'", var));
                return null;
        }
    }

    @Override
    public String asJSONString()
    {
        //return String.format("[id = %s, firstname = %s, lastname = %s]", get_id(), getFirstname(), getLastname());
        String super_json = super.asJSONString();
        return super_json.substring(0,super_json.length()-1)//ignore last brace
                +",\"enquiry\":\""+getEnquiry()+"\""
                +",\"destination\":\""+getDestination()+"\""
                +",\"pickup_location\":\""+getPickup_location()+"\""
                +",\"trip_type\":\""+getTrip_type()+"\""
                +",\"date_scheduled\":\""+getDate_scheduled()+"\""
                +"}";
    }

    @Override
    public String toString()
    {
        return getEnquiry();
    }

    @Override
    public String apiEndpoint()
    {
        return "/enquiries";
    }
}
