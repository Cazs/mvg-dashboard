package mvg.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.auxilary.IO;

import java.io.Serializable;

/**
 * Created by ghost on 2017/02/24.
 */
public class Notification extends MVGObject implements Serializable
{
    private String subject;
    private String message;
    private String client_id;
    private int status;
    public static final String TAG = "FileMetadata";

    public Notification(String subject, String message, String client_id)
    {
        setSubject(subject);
        setMessage(message);
        setClient_id(client_id);
        setStatus(0);
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getClient_id()
    {
        return client_id;
    }

    public void setClient_id(String client_id)
    {
        this.client_id = client_id;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    @Override
    public void parse(String var, Object val)
    {
        super.parse(var, val);
        switch (var.toLowerCase())
        {
            case "subject":
                subject = (String)val;
                break;
            case "message":
                message=(String)val;
                break;
            case "client_id":
                client_id=(String)val;
                break;
            case "status":
                status=Integer.parseInt((String)val);
                break;
            default:
                IO.log(TAG, IO.TAG_ERROR, "unknown "+TAG+" attribute '" + var + "'");
                break;
        }
    }

    @Override
    public Object get(String var)
    {
        Object val = super.get(var);
        if(val==null)
        {
            switch (var.toLowerCase())
            {
                case "subject":
                    return subject;
                case "message":
                    return message;
                case "client_id":
                    return client_id;
                case "status":
                    return status;
                default:
                    return null;
            }
        } else return val;
    }

    @Override
    public String asJSONString()
    {
        String super_json = super.asJSONString();
        String json_obj = super_json.substring(0,super_json.length()-1)//ignore last brace
                +",\"subject\":\""+getSubject()+"\""
                +",\"message\":\""+getMessage()+"\""
                +",\"client_id\":\""+getClient_id()+"\""
                +",\"status\":\""+getStatus()+"\"";
        json_obj+="}";

        return json_obj;
    }

    @Override
    public String apiEndpoint()
    {
        return "/notifications";
    }
}

