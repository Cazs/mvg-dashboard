package mvg.auxilary;

/**
 * Created by ghost on 2017/01/18.
 */
public enum Globals
{
    COMPANY("Master Visionary Group"),
    APP_NAME("Dashboard"),
    DEBUG_WARNINGS("on"),
    DEBUG_INFO("on"),
    DEBUG_ERRORS("on"),
    CURRENCY_SYMBOL("R");

    private String value;

    Globals(String value){this.value=value;}

    public String getValue(){return this.value;}
}
