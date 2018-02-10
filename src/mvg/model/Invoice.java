/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.managers.TripManager;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;

/**
 *
 * @author ghost
 */
public class Invoice extends MVGObject implements Serializable
{
    private String trip_id;
    private String client_id;
    private double receivable;
    private int status;

    public String getAccount()
    {
        Trip trip =getTrip();
        if(trip==null)
            return "N/A";
        if(trip.getQuote()==null)
            return "N/A";
        return trip.getQuote().getAccount_name();
    }

    public String getClient_id()
    {
        return client_id;
    }

    public void setClient_id(String client_id)
    {
        this.client_id = client_id;
    }

    public double getReceivable()
    {
        return receivable;
    }

    public void setReceivable(double receivable)
    {
        this.receivable = receivable;
    }

    public String getTrip_id()
    {
        return trip_id;
    }

    public void setTrip_id(String trip_id)
    {
        this.trip_id = trip_id;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public double getTotal()
    {
        if(getTrip()!=null)
            return getTrip().getTotal();
        else IO.log(getClass().getName(), IO.TAG_ERROR, "Trip object is not set");
        return 0;
    }

    public Client getClient()
    {
        if(getTrip()!=null)
            if(getTrip().getQuote()!=null)
                return getTrip().getQuote().getClient();
            else IO.log(getClass().getName(), IO.TAG_ERROR, "Trip->Quote object is not set");
        else IO.log(getClass().getName(), IO.TAG_ERROR, "Trip object is not set");
        return null;
    }

    public Trip getTrip()
    {
        if(getTrip_id()==null)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "trip_id is not set.");
            return null;
        }
        if(TripManager.getInstance().getDataset()!=null)
        {
            return TripManager.getInstance().getDataset().get(trip_id);
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "No Trips were found in the database.");
        return null;
    }

    //Properties

    public StringProperty accountProperty(){return new SimpleStringProperty(getAccount());}

    public StringProperty receivableProperty(){return new SimpleStringProperty(String.valueOf(receivable));}

    public StringProperty trip_idProperty(){return new SimpleStringProperty(trip_id);}

    public StringProperty statusProperty(){return new SimpleStringProperty(String.valueOf(status));}

    public StringProperty totalProperty()
    {
        return new SimpleStringProperty(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(getTotal()));
    }

    public StringProperty clientProperty()
    {
        if(getClient()!=null)
            return new SimpleStringProperty(getClient().getClient_name());
        else IO.log(getClass().getName(), IO.TAG_ERROR, "Trip->Quote->Client object is not set");
        return null;
    }

    @Override
    public void parse(String var, Object val)
    {
        super.parse(var, val);
        try
        {
            switch (var.toLowerCase())
            {
                case "trip_id":
                    setTrip_id(String.valueOf(val));
                    break;
                case "receivable":
                    setReceivable(Double.valueOf(String.valueOf(val)));
                    break;
                case "status":
                    status = Integer.parseInt(String.valueOf(val));
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

    @Override
    public Object get(String var)
    {
        switch (var.toLowerCase())
        {
            case "trip_id":
                return getTrip_id();
            case "status":
                return getStatus();
            case "account":
                return getAccount();
            case "receivable":
                return getReceivable();
        }
        return super.get(var);
    }

    @Override
    public String asJSONString()
    {
        String super_json = super.asJSONString();
        String json_obj = super_json.substring(0, super_json.length()-1)//toString().length()-1 to ignore the last brace.
                +",\"trip_id\":\""+getTrip_id()+"\""
                +",\"client_id\":\""+getClient_id()+"\""
                +",\"receivable\":\""+getReceivable()+"\"";
        if(getStatus()>0)
            json_obj+=",\"status\":\""+getStatus()+"\"";
        json_obj+="}";

        IO.log(getClass().getName(),IO.TAG_INFO, json_obj);
        return json_obj;
    }

    @Override
    public String apiEndpoint()
    {
        return "/invoices";
    }
}
