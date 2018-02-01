package mvg.model;

import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.managers.ClientManager;
import mvg.managers.EnquiryManager;
import mvg.managers.UserManager;
import mvg.managers.QuoteManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.HashMap;

/**
 * Created by ghost on 2017/01/21.
 */
public class Quote extends MVGObject
{
    private String enquiry_id;
    private String client_id;
    private String contact_person_id;;
    private String request;
    private double vat;
    private String account_name;
    private double revision;
    private int status;
    private String parent_id;
    private QuoteItem[] resources;
    private int rev_cursor = -1;
    public static final String TAG = "Quote";

    public Enquiry getEnquiry()
    {
        HashMap<String, Enquiry> enquiries = EnquiryManager.getInstance().getEnquiries();
        if(enquiries!=null)
        {
            return enquiries.get(getEnquiry_id());
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "no enquiries were found in database.");
        return null;
    }

    public String getEnquiry_id()
    {
        return enquiry_id;
    }

    public void setEnquiry_id(String enquiry_id)
    {
        this.enquiry_id = enquiry_id;
    }

    public String getClient_id()
    {
        return client_id;
    }

    public void setClient_id(String client_id)
    {
        this.client_id = client_id;
    }

    public String getContact_person_id()
    {
        return contact_person_id;
    }

    public void setContact_person_id(String contact_person_id)
    {
        this.contact_person_id = contact_person_id;
    }

    public String getRequest()
    {
        return request;
    }

    public void setRequest(String request)
    {
        this.request = request;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public int getCursor()
    {
        return rev_cursor;
    }

    public void setCursor(int cursor)
    {
        this.rev_cursor = cursor;
    }

    public double getVat()
    {
        return vat;
    }

    public void setVat(double vat)
    {
        this.vat = vat;
    }

    public String getAccount_name()
    {
        return account_name;
    }

    public void setAccount_name(String account_name)
    {
        this.account_name = account_name;
    }

    public Quote getParent()
    {
        if(parent_id ==null)
            return null;
        else
        {
            QuoteManager.getInstance().loadDataFromServer();
            return QuoteManager.getInstance().getQuotes().get(parent_id);
        }
    }

    public String getParent_id(){return this.parent_id;}

    public void setParent_id(String parent_id)
    {
        this.parent_id = parent_id;
    }

    public double getRevision()
    {
        return revision;
    }

    public void setRevision(double revision)
    {
        this.revision = revision;
    }

    public double getTotal()
    {
        //Compute total including VAT
        double total=0;
        if(this.getResources()!=null)
        {
            for (QuoteItem item : this.getResources())
            {
                total += item.getTotal();
            }
        }
        return total * (getVat()/100) + total;
    }

    public QuoteItem[] getResources()
    {
        return resources;
    }

    public void setResources(QuoteItem[] resources)
    {
        this.resources=resources;
    }

    public Client getClient()
    {
        HashMap<String, Client> clients = ClientManager.getInstance().getClients();
        if(clients!=null)
        {
            return clients.get(client_id);
        }else IO.log(getClass().getName(), IO.TAG_ERROR, "no clients were found in database.");
        return null;
    }

    public User getContact_person()
    {
        UserManager.getInstance().loadDataFromServer();
        HashMap<String, User> employees = UserManager.getInstance().getUsers();
        if(employees!=null)
        {
            return employees.get(contact_person_id);
        }
        return null;
    }

    public Quote getRoot()
    {
        Quote quote = this;
        while(quote.getParent_id()!=null)
            quote=quote.getParent();
        return quote;
    }

    public HashMap<Double, Quote> getSiblingsMap()
    {
        HashMap<Double, Quote> siblings = new HashMap<>();
        siblings.put(this.getRevision(), this);//make self be first child of requested siblings
        if(getParent_id()!=null)
        {
            QuoteManager.getInstance().loadDataFromServer();
            siblings.put(getParent().getRevision(), getParent());//make parent_id be second child of requested siblings
            if (QuoteManager.getInstance().getQuotes() != null)
            {
                for (Quote quote : QuoteManager.getInstance().getQuotes().values())
                    if (getParent_id().equals(quote.getParent_id()))
                        siblings.put(quote.getRevision(), quote);
            }
            else IO.log(getClass().getName(), IO.TAG_WARN, "no quotes in database.");
        } else IO.log(getClass().getName(), IO.TAG_WARN, "quote ["+get_id()+"] has no parent_id.");
        return siblings;
    }

    public Quote[] getSortedSiblings(String comparator)
    {
        HashMap<Double, Quote> siblings = getSiblingsMap();
        Quote[] siblings_arr = new Quote[siblings.size()];
        siblings.values().toArray(siblings_arr);
        if(siblings_arr!=null)
            if(siblings_arr.length>0)
            {
                IO.getInstance().quickSort(siblings_arr, 0, siblings_arr.length - 1, comparator);
                return siblings_arr;
            }
        return null;
    }

    public HashMap<Double, Quote> getChildrenMap()
    {
        HashMap<Double, Quote> children = new HashMap<>();
        QuoteManager.getInstance().loadDataFromServer();
        if (QuoteManager.getInstance().getQuotes() != null)
        {
            for (Quote quote : QuoteManager.getInstance().getQuotes().values())
                if(quote.getParent_id()!=null)
                    if (quote.getParent_id().equals(get_id()))
                        children.put(quote.getRevision(), quote);
        } else IO.log(getClass().getName(), IO.TAG_WARN, "no quotes in database.");
        return children;
    }

    public Quote[] getChildren(String comparator)
    {
        HashMap<Double, Quote> children = getChildrenMap();
        Quote[] children_arr = new Quote[children.size()];
        children.values().toArray(children_arr);
        if(children_arr!=null)
            if(children_arr.length>0)
            {
                IO.getInstance().quickSort(children_arr, 0, children_arr.length - 1, comparator);
                return children_arr;
            }
        return null;
    }

    //Properties
    public StringProperty client_idProperty(){return new SimpleStringProperty(client_id);}
    public StringProperty client_nameProperty()
    {
        if(getClient()!=null)
            return new SimpleStringProperty(getClient().getClient_name());
        return new SimpleStringProperty("N/A");
    }
    public StringProperty contact_person_idProperty(){return new SimpleStringProperty(contact_person_id);}
    public StringProperty contact_personProperty()
    {
        if(getContact_person()!=null)
            return new SimpleStringProperty(getContact_person().getName());
        return new SimpleStringProperty("N/A");
    }
    public StringProperty requestProperty(){return new SimpleStringProperty(request);}
    public StringProperty addressProperty()
    {
        if(getEnquiry()!=null)
            return new SimpleStringProperty(getEnquiry().getPickup_location());
        else return new SimpleStringProperty("N/A");
    }
    public StringProperty statusProperty(){return new SimpleStringProperty(String.valueOf(status));}
    public StringProperty vatProperty()
    {
        return new SimpleStringProperty(String.valueOf(getVat()));
    }
    public StringProperty account_nameProperty(){return new SimpleStringProperty(getAccount_name());}
    public StringProperty parent_idProperty()
    {
        return new SimpleStringProperty(String.valueOf(getParent_id()));
    }
    public StringProperty revisionProperty(){return new SimpleStringProperty(String.valueOf(revision));}
    public SimpleStringProperty totalProperty(){return new SimpleStringProperty(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(getTotal()));}
    public SimpleStringProperty quoteProperty()
    {
        if(this!=null)
            if(this.getContact_person()!=null)
            {
                String quote_number = this.getContact_person().getFirstname() + "-"
                        + this.getContact_person().getInitials() + this.get_id().substring(0,8)
                        + " REV" + String.valueOf(this.getRevision()).substring(0,3);
                return new SimpleStringProperty(quote_number);
            }else return new SimpleStringProperty(this.getContact_person_id());
        else return new SimpleStringProperty("N/A");
    }

    @Override
    public void parse(String var, Object val)
    {
        super.parse(var, val);
        try
        {
            switch (var.toLowerCase())
            {
                case "enquiry_id":
                    enquiry_id = (String)val;
                case "client_id":
                    client_id = (String)val;
                    break;
                case "contact_person_id":
                    contact_person_id = (String)val;
                    break;
                case "request":
                    request = String.valueOf(val);
                    break;
                case "status":
                    status = Integer.parseInt(String.valueOf(val));
                    break;
                case "parent_id":
                    parent_id = String.valueOf(val);
                    break;
                case "revision":
                    revision = Integer.parseInt(String.valueOf(val));
                    break;
                case "vat":
                    vat = Double.parseDouble(String.valueOf(val));
                    break;
                case "account_name":
                    account_name = String.valueOf(val);
                    break;
                default:
                    IO.log(getClass().getName(), IO.TAG_ERROR, "Unknown "+getClass().getName()+" attribute '" + var + "'.");
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
            case "enquiry_id":
                return enquiry_id;
            case "client_id":
                return client_id;
            case "contact_person_id":
                return contact_person_id;
            case "request":
                return request;
            case "status":
                return status;
            case "parent_id":
                return parent_id;
            case "vat":
                return vat;
            case "account_name":
                return account_name;
            case "revision":
                return revision;
        }
        return super.get(var);
    }

    @Override
    public String asJSONString()
    {
        String super_json = super.asJSONString();
        String json_obj = super_json.substring(0,super_json.length()-1)//ignore last brace
                +",\"contact_person_id\":\""+contact_person_id+"\""
                +",\"request\":\""+request+"\""
                +",\"vat\":\""+vat+"\""
                +",\"account_name\":\""+account_name+"\""
                +",\"revision\":\""+revision+"\"";
                if(getClient_id()!=null)
                    json_obj+=",\"client_id\":\""+client_id+"\"";
                if(getEnquiry_id()!=null)
                    json_obj+=",\"enquiry_id\":\""+getEnquiry_id()+"\"";
                if(getParent_id()!=null)
                    json_obj+=",\"parent_id\":\""+ getParent_id() +"\"";
                if(getStatus()>0)
                    json_obj+=",\"status\":\""+getStatus()+"\"";
                json_obj+="}";

        return json_obj;
    }

    @Override
    public String apiEndpoint()
    {
        return "/quotes";
    }
}
