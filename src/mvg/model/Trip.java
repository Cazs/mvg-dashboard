/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.managers.QuoteManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.ArrayList;

/**
 *
 * @author ghost
 */
public class Trip extends MVGObject
{
    private long date_assigned;
    private String quote_id;
    private int status;
    private User[] assigned_users;

    //Getters and setters

    public long getDate_assigned() 
    {
        return date_assigned;
    }

    public void setDate_assigned(long date_assigned) 
    {
        this.date_assigned = date_assigned;
    }

    public long getDate_scheduled()
    {
        if(getQuote()!=null)
            if(getQuote().getEnquiry()!=null)
                return getQuote().getEnquiry().getDate_scheduled();
        return 0;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getQuote_id()
    {
        return quote_id;
    }

    public void setQuote_id(String quote_id)
    {
        this.quote_id = quote_id;
    }

    public Quote getQuote()
    {
        QuoteManager.getInstance().initialize();
        if(QuoteManager.getInstance().getQuotes()!=null)
        {
            //return latest revision
            Quote[] revisions = QuoteManager.getInstance().getQuotes().get(quote_id).getSortedSiblings("revision");
            return revisions[revisions.length-1];
        }
        else IO.logAndAlert(getClass().getName(), IO.TAG_ERROR, "No quotes were found on the database.");
        return null;
    }

    /**
     * @return Array of Users assigned to a Trip object.
     */
    public User[] getAssigned_users()
    {
        return assigned_users;
    }

    /**
     * @param users Array of Users to be assigned to a Trip object.
     */
    public void setAssigned_users(User[] users)
    {
        this.assigned_users=users;
    }

    /**
     * @param reps ArrayList of Users to be assigned to a Trip object.
     */
    public void setAssigned_users(ArrayList<User> reps)
    {
        this.assigned_users = new User[reps.size()];
        for(int i=0;i<reps.size();i++)
        {
            this.assigned_users[i] = reps.get(i);
        }
    }

    //Properties

    public StringProperty quote_idProperty()
    {
        return new SimpleStringProperty(quote_id);
    }

    public StringProperty trip_descriptionProperty()
    {
        Quote quote = getQuote();
        if(quote!=null)
            return new SimpleStringProperty(quote.getRequest());
        else return new SimpleStringProperty("N/A");
    }

    public StringProperty client_nameProperty()
    {
        Quote quote = getQuote();
        if(quote!=null)
            if(quote.getClient()!=null)
                return new SimpleStringProperty(quote.getClient().getClient_name());
            else return new SimpleStringProperty("N/A");
        else return new SimpleStringProperty("N/A");
    }

    public StringProperty addressProperty()
    {
        Quote quote = getQuote();
        if(quote!=null)
            if(quote.getEnquiry()!=null)
                return new SimpleStringProperty(quote.getEnquiry().getPickup_location());
            else return new SimpleStringProperty("N/A");
        else return new SimpleStringProperty("N/A");
    }

    public StringProperty destinationProperty()
    {
        Quote quote = getQuote();
        if(quote!=null)
            if(quote.getEnquiry()!=null)
                return new SimpleStringProperty(quote.getEnquiry().getDestination());
            else return new SimpleStringProperty("N/A");
        else return new SimpleStringProperty("N/A");
    }

    public StringProperty contact_personProperty()
    {
        Quote quote = getQuote();
        if(quote!=null)
            if(quote.getContact_person()!=null)
                return new SimpleStringProperty(quote.getContact_person().getName());
            else return new SimpleStringProperty("N/A");
        else return new SimpleStringProperty("N/A");
    }

    public SimpleStringProperty totalProperty(){return new SimpleStringProperty(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(getTotal()));}

    /**
     * @return The total cost of this Trip (derived from Quote object)
     */
    public double getTotal()
    {
        Quote quote = getQuote();
        //Compute trip total
        if(quote!=null)
            return quote.getTotal();
        else return 0;
    }

    /**
     * Method to parse Model attribute.
     * @param var Model attribute to be parsed.
     * @param val Model attribute value to be set.
     */
    @Override
    public void parse(String var, Object val)
    {
        super.parse(var, val);
        try
        {
            switch (var.toLowerCase())
            {
                case "quote_id":
                    quote_id = (String)val;
                    break;
                case "status":
                    status = Integer.parseInt(String.valueOf(val));
                    break;
                case "date_assigned":
                    date_assigned = Long.parseLong(String.valueOf(val));
                    break;
                case "assigned_users":
                    if(val!=null)
                        assigned_users = (User[]) val;
                    else IO.log(getClass().getName(), IO.TAG_WARN, "value to be casted to User[] is null.");
                    break;
                default:
                    IO.log(getClass().getName(), IO.TAG_ERROR, "unknown "+getClass().getName()+" attribute '" + var + "'.");
                    break;
            }
        } catch (NumberFormatException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    /**
     * @param var Model attribute whose value is to be returned.
     * @return Model attribute value.
     */
    @Override
    public Object get(String var)
    {
        switch (var.toLowerCase())
        {
            case "quote_id":
                return getQuote_id();
            case "status":
                return getStatus();
            case "date_assigned":
                return getDate_assigned();
            case "assigned_users":
                return getAssigned_users();
        }
        return super.get(var);
    }

    /**
     * @return JSON representation of Trip object.
     */
    @Override
    public String asJSONString()
    {
        String super_json = super.asJSONString();
        String json_obj = super_json.substring(0, super_json.length()-1)//toString().length()-1 to ignore the last brace.
                +",\"quote_id\":\""+quote_id+"\""
                +",\"status\":\""+status+"\"";
        if(date_assigned>0)
            json_obj+=",\"date_assigned\":\""+date_assigned+"\"";
        json_obj+="}";

        IO.log(getClass().getName(),IO.TAG_INFO, json_obj);
        return json_obj;
    }

    /**
     * @return Trip model's endpoint URL.
     */
    @Override
    public String apiEndpoint()
    {
        return "/trips";
    }
}