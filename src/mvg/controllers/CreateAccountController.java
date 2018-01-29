/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.auxilary.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import mvg.model.CustomTableViewControls;
import mvg.model.User;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * views Controller class
 *
 * @author ghost
 */
public class CreateAccountController extends ScreenController implements Initializable
{
    @FXML
    private ComboBox<String> cbxSex, cbxAccessLevel;
    @FXML
    private TextField txtUsername, txtPassword, txtFirstname, txtLastname, txtEmail, txtOrganisationId, txtTelephone, txtCellphone;
    @FXML
    private TextArea txtOther;
    private String[] access_levels = {"NONE", "NORMAL", "ADMIN", "SUPER"};

    @Override
    public void refreshView()
    {
        cbxSex.setItems(FXCollections.observableArrayList(new String[]{"Male", "Female"}));
        cbxAccessLevel.setItems(FXCollections.observableArrayList(access_levels));
    }

    @Override
    public void refreshModel()
    {
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
    }

    @FXML
    public void createAccountSubmit()
    {
        int sex_index = cbxSex.getSelectionModel().selectedIndexProperty().get();

        if(!Validators.isValidNode(txtUsername, "Invalid Username", 5, "^.*(?=.{5,}).*"))//"please enter a valid username"
            return;
        if(!Validators.isValidNode(txtPassword, "Invalid Password", 8, "^.*(?=.{8,}).*"))//(?=[a-zA-Z])(?=.*[0-9])(?=.*[@#!$%^&*-+=])//"please enter a valid password"
            return;
        if(!Validators.isValidNode(txtFirstname, "Invalid Firstname", 1, "^.*(?=.{1,}).*"))//"please enter a valid first name"
            return;
        if(!Validators.isValidNode(txtLastname, "Invalid Lastname", 1, "^.*(?=.{1,}).*"))//"please enter a valid last name"
            return;

        if(sex_index<0)
        {
            cbxSex.getStyleClass().remove("form-control-default");
            cbxSex.getStyleClass().add("control-input-error");
        }else{
            cbxSex.getStyleClass().remove("control-input-error");
            cbxSex.getStyleClass().add("form-control-default");
        }
        if(!Validators.isValidNode(txtEmail, "Invalid Email", 5, "^.*(?=.{5,})(?=(.*@.*\\.)).*"))//"please enter a valid email address"
            return;
        if(!Validators.isValidNode(txtTelephone, "Invalid Telephone Number", 10, "^.*(?=.{10,}).*"))//"please enter a valid telephone number"
            return;
        if(!Validators.isValidNode(txtCellphone, "Invalid Cellphone Number", 10, "^.*(?=.{10,}).*"))//"please enter a valid cellphone number"
            return;

        //all valid, send data to server
        int access_level_index = cbxAccessLevel.getSelectionModel().getSelectedIndex();
        if(access_level_index>=0)
        {
            if(access_levels[access_level_index].toLowerCase().equals("super"))
            {
                User user = new User();
                user.setUsr(txtUsername.getText());
                user.setPwd(txtPassword.getText());
                user.setAccessLevel(3);
                user.setFirstname(txtFirstname.getText());
                user.setLastname(txtLastname.getText());
                user.setGender(cbxSex.getValue());
                user.setEmail(txtEmail.getText());
                user.setTel(txtTelephone.getText());
                user.setCell(txtCellphone.getText());
                user.setOrganisation_id(txtOrganisationId.getText());
                //user.setCreator("system");//TODO: check this
                if(txtOther.getText()!=null)
                    user.setOther(txtOther.getText());

                try
                {
                    ArrayList<AbstractMap.SimpleEntry<String, String>> headers = new ArrayList<>();
                    headers.add(new AbstractMap.SimpleEntry<>("Content-Type", "application/json"));

                    HttpURLConnection connection = RemoteComms.putJSON("/users", user.asJSONString(), headers);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    {
                        IO.logAndAlert("Account Creation Success", "Successfully created account.", IO.TAG_INFO);
                    } else
                        IO.logAndAlert("Account Creation Failure", IO.readStream(connection.getErrorStream()), IO.TAG_ERROR);

                    connection.disconnect();
                } catch (IOException e)
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                }
            }
        }
    }
}
