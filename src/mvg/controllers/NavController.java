package mvg.controllers;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Window;
import mvg.MVG;
import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.managers.UserManager;
import mvg.model.Screens;
import mvg.model.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import org.controlsfx.control.PopOver;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NavController extends ScreenController implements Initializable
{
    @FXML
    private Label lblScreen,company_name;
    @FXML
    private ImageView btnBack,btnNext,btnHome,img_logo, img_profile;
    public static int FONT_SIZE_MULTIPLIER = 50;
    public static boolean first_run=true;

    @Override
    public void refreshView()
    {
        if(!first_run)
        {
            if (SessionManager.getInstance().getActive() != null)
            {
                if (!SessionManager.getInstance().getActive().isExpired())
                {
                    //Render user name
                    User user= SessionManager.getInstance().getActiveUser();
                    if (user != null)
                        this.getUserNameLabel().setText(user.getName());
                    else IO.log(getClass().getName(), IO.TAG_ERROR, "No active sessions.");
                }
                else
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, "No active sessions were found!");
                    return;
                }
            }
            else
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, "No valid sessions were found!");
                return;
            }
            //Render current screen name
            lblScreen.setText(ScreenManager.getInstance().peekScreenControllers().getKey());

            //Render company name
            company_name.setText(Globals.COMPANY.getValue() + " " + Globals.APP_NAME.getValue());
        } else
        {
            IO.log(getClass().getName(), IO.TAG_INFO, "first run, ignoring session checks.");
            first_run = false;
        }

        if(ScreenManager.getInstance().getScene()!=null)
        {
            Window app_window = ScreenManager.getInstance().getScene().getWindow();
            //resize app title on screen resize
            app_window.widthProperty().addListener((observable, oldValue, newValue) ->
                    company_name.setFont(Font.font(newValue.intValue() / FONT_SIZE_MULTIPLIER)));

            //trigger resize handler to resize font every time the nav is reloaded, yes it is hacky but works ;)
            app_window.setWidth(
                    app_window.getWidth() + 1 >= GraphicsEnvironment.getLocalGraphicsEnvironment()
                            .getDefaultScreenDevice().getDisplayMode().getWidth() - 60 ?
                            app_window.getWidth() - 1 : app_window.getWidth() + 1);
        }

        img_profile.setOnMouseClicked(event ->
        {
            VBox container = new VBox();
            PopOver popOver = new PopOver(container);
            popOver.setTitle("Profile Menu");
            popOver.setAnimated(true);

            Button btnDash = new Button("Dashboard");
            btnDash.setOnAction(evt ->
                    ScreenManager.getInstance().showLoadingScreen(param ->
                    {
                        new Thread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    popOver.hide();
                                    //load User data to memory
                                    UserManager.getInstance().initialize();

                                    //TODO: set screen to notifications screen
                                    if (ScreenManager.getInstance()
                                            .loadScreen(Screens.DASHBOARD
                                                    .getScreen(), MVG.class
                                                    .getResource("views/" + Screens.DASHBOARD
                                                            .getScreen())))
                                    {
                                        ScreenManager.getInstance()
                                                .setScreen(Screens.DASHBOARD.getScreen());
                                    } else IO.log(getClass()
                                            .getName(), IO.TAG_ERROR, "could not load dashboard screen.");
                                } catch (IOException e)
                                {
                                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                }
                            }
                        }).start();
                        return null;
                    }));

            container.getChildren().add(btnDash);
            //popOver.getRoot().getChildren().add(container);
            popOver.show(img_profile);
        });
    }

    @Override
    public void refreshModel()
    {
    }

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        if(ScreenManager.getInstance()!=null)
            ScreenManager.getInstance().setLblScreenName(lblScreen);
        Platform.runLater(() -> refreshView());
    }
}
