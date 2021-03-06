package mvg.model;

import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.managers.ResourceManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by ghost on 2017/01/21.
 */
public class QuoteItem extends MVGObject implements Serializable
{
    private int item_number;
    private int quantity;
    private double unit_cost;
    private double markup;
    private String additional_costs;
    private String quote_id;
    private String resource_id;
    public static final String TAG = "QuoteItem";

    private StringProperty item_numberProperty(){return new SimpleStringProperty(String.valueOf(item_number));}

    public String getItem_number()
    {
        return String.valueOf(item_number);
    }

    public int getItem_numberValue()
    {
        return item_number;
    }

    public void setItem_number(int item_number)
    {
        this.item_number = item_number;
    }

    private StringProperty quote_idProperty(){return new SimpleStringProperty(quote_id);}

    public String getQuote_id()
    {
        return quote_id;
    }

    public void setQuote_id(String quote_id)
    {
        this.quote_id = quote_id;
    }

    private StringProperty resource_idProperty(){return new SimpleStringProperty(resource_id);}

    public String getResource_id()
    {
        return resource_id;
    }

    public void setResource_id(String resource_id)
    {
        this.resource_id = resource_id;
    }

    private StringProperty equipment_nameProperty(){return new SimpleStringProperty(getEquipment_name());}

    public String getEquipment_name()
    {
        Resource resource = getResource();
        if(resource!=null)
            return resource.getResource_name();
        return "N/A";
    }

    private StringProperty equipment_descriptionProperty(){return new SimpleStringProperty(getEquipment_description());}

    public String getEquipment_description()
    {
        Resource resource = getResource();
        if(resource!=null)
            return resource.getResource_description();
        return "N/A";
    }

    private StringProperty additional_costsProperty(){return new SimpleStringProperty(additional_costs);}

    public String getAdditional_costs()
    {
        return additional_costs;
    }

    public void setAdditional_costs(String additional_costs)
    {
        this.additional_costs = additional_costs;
    }

    private StringProperty unitProperty(){return new SimpleStringProperty(getUnit());}

    public String getUnit()
    {
        Resource resource = getResource();
        if(resource!=null)
            return resource.getUnit();
        return "N/A";
    }

    private StringProperty quantityProperty(){return new SimpleStringProperty(String.valueOf(quantity));}

    public String getQuantity()
    {
        return String.valueOf(quantity);
    }

    public int getQuantityValue()
    {
        return quantity;
    }

    public void setQuantity(int quantity)
    {
        this.quantity = quantity;
    }

    private StringProperty unit_costProperty() {return new SimpleStringProperty(String.valueOf(unit_cost));}

    public String getUnit_cost()
    {
        return String.valueOf(getUnitCost());
    }

    public double getUnitCost()
    {
        return unit_cost;
    }

    public void setUnit_cost(double unit_cost)
    {
        this.unit_cost = unit_cost;
    }

    private StringProperty valueProperty(){return new SimpleStringProperty(String.valueOf(getCurrentValue()));}

    public String getCurrentValue()
    {
        return String.valueOf(getCurrentUnitCost());
    }

    public double getCurrentUnitCost()
    {
        Resource resource = getResource();
        if(resource!=null)
            return resource.getResource_value();
        return 0;
    }

    private StringProperty markupProperty(){return new SimpleStringProperty(String.valueOf(markup));}

    public String getMarkup(){return String.valueOf(this.markup);}

    public double getMarkupValue(){return this.markup;}

    public void setMarkup(double markup){this.markup=markup;}

    public Resource getResource()
    {
        HashMap<String, Resource> resources = ResourceManager.getInstance().getAll_resources();
        if(resources!=null)
            return resources.get(getResource_id());
        return null;
    }

    public StringProperty rateProperty()
    {
        return new SimpleStringProperty(Globals.CURRENCY_SYMBOL.getValue() + " " + getRate());
    }

    public double getRate()
    {
        //double marked_up = getUnitCost() + getUnitCost()*(markup/100);
        double total = 0;//getUnitCost();

        //check additional costs
        if (getAdditional_costs() != null)
        {
            if (!getAdditional_costs().isEmpty())
            {
                //compute additional costs for each Quote Item
                if(getAdditional_costs().contains(";"))//check cost delimiter
                {
                    String[] costs = getAdditional_costs().split(";");
                    for (String str_cost : costs)
                    {
                        if (str_cost.contains("="))
                        {
                            //retrieve cost and markup
                            String add_cost = str_cost.split("=")[1];//the cost value is [1] (the cost name is [0])

                            double cost,add_cost_markup=0;
                            if(add_cost.contains("*"))//if(in the form cost*markup)
                            {
                                cost = Double.parseDouble(add_cost.split("\\*")[0]);
                                add_cost_markup = Double.parseDouble(add_cost.split("\\*")[1]);
                            }else cost = Double.parseDouble(add_cost);

                            //add marked up additional cost to total
                            total += cost + cost*(add_cost_markup/100);
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid Quote Item additional cost.");
                    }
                } else if (getAdditional_costs().contains("="))//if only one additional cost
                {
                    double cost,add_cost_markup=0;
                    //get cost and markup
                    if(getAdditional_costs().split("=")[1].contains("*"))
                    {
                        cost = Double.parseDouble(getAdditional_costs().split("=")[1].split("\\*")[0]);
                        add_cost_markup = Double.parseDouble(getAdditional_costs().split("=")[1].split("\\*")[1]);
                    }else cost = Double.parseDouble(getAdditional_costs().split("=")[1]);
                    //add marked up additional cost to total
                    total += cost + cost*(add_cost_markup/100);
                }else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid Quote Item additional cost.");
            }
        }
        return total;
    }

    public StringProperty totalProperty()
    {
        return new SimpleStringProperty(Globals.CURRENCY_SYMBOL.getValue() + " " + getTotal());
    }

    public double getTotal()
    {
        return getRate()*getQuantityValue();
    }

    @Override
    public void parse(String var, Object val)
    {
        super.parse(var, val);
        try
        {
            switch (var.toLowerCase())
            {
                case "quote_id":
                    setQuote_id(String.valueOf(val));
                    break;
                case "resource_id":
                    setResource_id(String.valueOf(val));
                    break;
                case "item_number":
                    setItem_number(Integer.valueOf((String)val));
                    break;
                case "additional_costs":
                    setAdditional_costs((String)val);
                    break;
                case "quantity":
                    setQuantity(Integer.valueOf((String)val));
                    break;
                case "unit_cost":
                    setUnit_cost(Double.valueOf((String)val));
                    break;
                case "markup":
                    setMarkup(Double.parseDouble((String) val));
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
            case "quote_id":
                return getQuote_id();
            case "resource_id":
                return getResource_id();
            case "item_number":
                return getItem_number();
            case "equipment_name":
                return getEquipment_name();
            case "equipment_description":
                return getEquipment_description();
            case "additional_costs":
                return getAdditional_costs();
            case "unit":
                return getUnit();
            case "quantity":
                return getQuantityValue();
            case "unit_cost":
                return getUnitCost();
            case "value":
                return getCurrentValue();
            case "markup":
                return getMarkupValue();
        }
        return super.get(var);
    }

    @Override
    public String asJSONString()
    {
        String super_json = super.asJSONString();
        String json_obj = super_json.substring(0,super_json.length()-1);//ignore last brace
        json_obj+=",\"resource_id\":\""+resource_id+"\""
                +",\"quote_id\":\""+quote_id+"\""
                +",\"item_number\":\""+item_number+"\""
                +",\"quantity\":\""+quantity+"\""
                +",\"unit_cost\":\""+unit_cost+"\""
                +",\"markup\":\""+markup+"\"";
        if(additional_costs!=null)
                json_obj+=",\"additional_costs\":\""+additional_costs+"\"";
        json_obj+="}";

        return json_obj;
    }

    @Override
    public String apiEndpoint()
    {
        return "/quotes/resources";
    }
}