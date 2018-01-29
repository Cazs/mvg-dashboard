/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.model;

/**
 *
 * @author ghost
 */
public enum Screens 
{
    HOME("Homescreen.fxml"),
    LOGIN("Login.fxml"),
    DASHBOARD("Dashboard.fxml"),
    SETTINGS("Settings.fxml"),
    CREATE_ACCOUNT("CreateAccount.fxml"),
    RESET_PWD("ResetPassword.fxml"),
    NEW_CLIENT("NewClient.fxml"),
    NEW_RESOURCE("NewResource.fxml"),
    NEW_QUOTE("NewQuote.fxml"),
    QUOTES("Quotes.fxml"),
    VIEW_QUOTE("View_quote.fxml");

    private String screen;
    
    Screens(String screen){
        this.screen = screen;
    }
    
    public String getScreen()
    {
        return screen;
    }
}