/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

import mvg.auxilary.IO;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import mvg.managers.ClientManager;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author ghost
 */
public class User extends MVGObject implements Serializable
{
    private String usr;
    private String pwd;//hashed
    private String firstname;
    private String lastname;
    private String organisation_id;
    private String gender;
    private String email;
    private String tel;
    private String cell;
    private int access_level;
    private boolean active;
    public static final String TAG = "User";
    public static int ACCESS_LEVEL_NONE = 0;
    public static int ACCESS_LEVEL_NORMAL = 1;
    public static int ACCESS_LEVEL_ADMIN = 2;
    public static int ACCESS_LEVEL_SUPER = 3;

    public String getUsr()
    {
        return usr;
    }

    public void setUsr(String usr)
    {
        this.usr = usr;
    }

    public void setPwd(String pwd)
    {
        this.pwd = pwd;
    }

    public int getAccessLevel()
    {
        return access_level;
    }

    public void setAccessLevel(int access_level)
    {
        this.access_level = access_level;
    }

    public String isActive()
    {
        return String.valueOf(active);
    }

    public boolean isActiveVal()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public String getName()
    {
        return firstname + " " + lastname;
    }

    public String getFirstname()
    {
        return firstname;
    }

    public void setFirstname(String firstname)
    {
        this.firstname = firstname;
    }

    public String getLastname()
    {
        return lastname;
    }

    public void setLastname(String lastname)
    {
        this.lastname = lastname;
    }

    public String getEmail()
    {
        return email;
    }

    public String getOrganisation_id()
    {
        return organisation_id;
    }

    public void setOrganisation_id(String organisation_id)
    {
        this.organisation_id = organisation_id;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getTel()
    {
        return tel;
    }

    public void setTel(String tel)
    {
        this.tel = tel;
    }

    public String getCell()
    {
        return cell;
    }

    public void setCell(String cell)
    {
        this.cell = cell;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public Client getOrganisation()
    {
        if(getOrganisation_id()!=null)
        {
            IO.log(getClass().getName(), IO.TAG_WARN, "user "+getName()+" does not belong to any organisation.");
            return null;
        }
        HashMap<String, Client> clients = ClientManager.getInstance().getClients();
        if(clients!=null)
        {
            return clients.get(getOrganisation_id());
        }else IO.log(getClass().getName(), IO.TAG_ERROR, "no clients were found in database.");
        return null;
    }

    //Properties

    public StringProperty usrProperty(){return new SimpleStringProperty(usr);}
    public StringProperty access_levelProperty(){return new SimpleStringProperty(String.valueOf(access_level));}
    public StringProperty activeProperty(){return new SimpleStringProperty(String.valueOf(active));}
    public StringProperty nameProperty(){return new SimpleStringProperty(getName());}
    public StringProperty firstnameProperty(){return new SimpleStringProperty(firstname);}
    public StringProperty lastnameProperty(){return new SimpleStringProperty(lastname);}
    public StringProperty emailProperty(){return new SimpleStringProperty(email);}
    public StringProperty organisationProperty()
    {
        if(getOrganisation()!=null)
            return new SimpleStringProperty(getOrganisation().getClient_name());
        else return new SimpleStringProperty("N/A");
    }
    public StringProperty telProperty(){return new SimpleStringProperty(usr);}
    public StringProperty cellProperty(){return new SimpleStringProperty(usr);}
    public StringProperty genderProperty(){return new SimpleStringProperty(usr);}

    @Override
    public void parse(String var, Object val)
    {
        super.parse(var, val);
        try
        {
            switch (var.toLowerCase())
            {
                case "firstname":
                    setFirstname((String)val);
                    break;
                case "lastname":
                    setLastname((String)val);
                    break;
                case "usr":
                    setUsr((String)val);
                    break;
                case "organisation_id":
                    setOrganisation_id((String)val);
                    break;
                case "gender":
                    setGender((String)val);
                    break;
                case "email":
                    setEmail((String)val);
                    break;
                case "access_level":
                    setAccessLevel(Integer.parseInt((String)val));
                    break;
                case "tel":
                    setTel((String)val);
                    break;
                case "cell":
                    setCell((String)val);
                    break;
                case "active":
                    setActive(Boolean.parseBoolean((String)val));
                    break;
                case "other":
                    setOther((String)val);
                    break;
                default:
                    IO.log(TAG, IO.TAG_WARN, String.format("unknown "+getClass().getName()+" attribute '%s'", var));
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
            case "firstname":
                return firstname;
            case "lastname":
                return lastname;
            case "usr":
                return usr;
            case "organisation_id":
                return organisation_id;
            case "access_level":
                return access_level;
            case "gender":
                return gender;
            case "email":
                return email;
            case "tel":
                return tel;
            case "cell":
                return cell;
            case "active":
                return active;
        }
        return super.get(var);
    }

    public String getInitials(){return new String(firstname.substring(0,1) + lastname.substring(0,1));}

    @Override
    public String asJSONString()
    {
        //return String.format("[id = %s, firstname = %s, lastname = %s]", get_id(), getFirstname(), getLastname());
        String super_json = super.asJSONString();
        String json_obj = super_json.substring(0,super_json.length()-1)//ignore last brace
                +",\"firstname\":\""+firstname+"\""
                +",\"lastname\":\""+lastname+"\""
                +",\"usr\":\""+usr+"\""
                +",\"pwd\":\""+pwd+"\""
                +(organisation_id!=null?",\"organisation_id\":\""+organisation_id+"\"":"")
                +",\"access_level\":\""+access_level+"\""
                +",\"gender\":\""+gender+"\""
                +",\"email\":\""+email+"\""
                +",\"tel\":\""+tel+"\""
                +",\"cell\":\""+cell+"\""
                +",\"active\":\""+active+"\""
                +"}";
        return json_obj;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public String apiEndpoint()
    {
        return "/users";
    }
}