package mvg.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.auxilary.IO;
import mvg.managers.UserManager;

import java.io.Serializable;
import java.util.HashMap;

public abstract class MVGObject implements Serializable
{
    private String _id;
    private long date_logged;
    private String creator;
    private String other;
    private boolean marked;
    private Link _links;
    public static final int STATUS_PENDING =0;
    public static final int STATUS_APPROVED =1;
    public static final int STATUS_ARCHIVED =2;

    public StringProperty idProperty(){return new SimpleStringProperty(_id);}

    /**
     * Function to get identifier of Quote object.
     * @return Quote identifier.
     */
    public String get_id()
    {
        return _id;
    }

    /**
     * Method to assign identifier to this object.
     * @param _id identifier to be assigned to this object.
     */
    public void set_id(String _id)
    {
        this._id = _id;
    }

    public long getDate_logged()
    {
        return this.date_logged;
    }

    public void setDate_logged(long date_logged)
    {
        this.date_logged = date_logged;
    }

    public StringProperty creatorProperty()
    {
        return new SimpleStringProperty(String.valueOf(getCreatorUser().getName()));
    }

    public String getCreator()
    {
        return this.creator;
    }

    public User getCreatorUser()
    {
        UserManager.getInstance().initialize();
        HashMap<String, User> users = UserManager.getInstance().getDataset();
        if(users!=null)
            return users.get(getCreator());
        return null;
    }

    public void setCreator(String creator)
    {
        this.creator = creator;
    }

    public String getOther()
    {
        return other;
    }

    public void setOther(String other)
    {
        this.other = other;
    }

    /**
     * Function to get a shortened identifier of this object.
     * @return The shortened identifier.
     */
    public StringProperty short_idProperty(){return new SimpleStringProperty(_id.substring(0, 8));}

    public String getShort_id()
    {
        return _id.substring(0, 8);
    }

    public boolean isMarked()
    {
        return marked;
    }

    public void setMarked(boolean marked){this.marked=marked;}

    public void set_links(Link links)
    {
        this._links=links;
        /*if(get_id()==null)
        {
            String id=null;
            //if no id has been set, try retrieve it from _links object
            if (_links != null)
                if (_links.getSelf() != null)
                    if (_links.getSelf().getHref() != null)
                    {
                        //returns data of format  http://localhost:8080/invoices/5a314faf6604db0001816e07
                        String[] arr = _links.getSelf().getHref().split("/");
                        if(arr!=null)
                            if(arr.length>0)
                                id=arr[arr.length-1];
                    }
            set_id(id);
        }*/
    }

    public void parse(String var, Object val)
    {
        switch (var.toLowerCase())
        {
            case "date_logged":
                date_logged = Long.parseLong(String.valueOf(val));
                break;
            case "creator":
                creator = String.valueOf(val);
                break;
            case "other":
                other = String.valueOf(val);
                break;
            case "marked":
                marked = Boolean.valueOf((String) val);
                break;
        }
    }

    public Object get(String var)
    {
        switch (var.toLowerCase())
        {
            case "_id":
                return get_id();
            case "date_logged":
                return getDate_logged();
            case "creator":
                return getCreator();
            case "marked":
                return isMarked();
            case "other":
                return getOther();
            default:
                IO.log(getClass().getName(), IO.TAG_ERROR, "unknown "+getClass().getName()+" attribute '" + var + "'.");
                return null;
        }
    }

    public abstract String apiEndpoint();

    public String asJSONString()
    {
        return "{"+(get_id()==null?"":"\"_id\":\""+get_id()+"\", ")
                +"\"creator\":\""+getCreator()+"\""
                +(getDate_logged()>0?",\"date_logged\":\""+getDate_logged()+"\"":"")
                +(getOther()!=null?",\"other\":\""+getOther()+"\"":"")
                +"}";
    }
}
