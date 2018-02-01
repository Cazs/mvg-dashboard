package mvg.controllers;

import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import mvg.MVG;
import mvg.auxilary.IO;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.managers.UserManager;
import mvg.model.Screens;
import mvg.model.User;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import org.controlsfx.control.PopOver;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NavController extends ScreenController implements Initializable
{
    @FXML
    private Label lblScreen;
    @FXML
    private ImageView btnBack,btnNext,btnHome,img_logo, img_profile;

    @Override
    public void refreshView()
    {
        if (SessionManager.getInstance().getActive() != null)
        {
            if (!SessionManager.getInstance().getActive().isExpired())
            {
                //Render user name
                User e = SessionManager.getInstance().getActiveUser();
                if(e!=null)
                    this.getUserNameLabel().setText(e.getName());
                else IO.log(getClass().getName(), IO.TAG_ERROR, "No active sessions.");
            } else
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, "No active sessions were found!");
                return;
            }
        } else
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "No valid sessions were found!");
            return;
        }
        //Render current screen name
        lblScreen.setText(ScreenManager.getInstance().peekScreenControllers().getKey());

        img_profile.setOnMouseClicked(event ->
        {
            VBox container = new VBox();
            PopOver popOver = new PopOver(container);
            popOver.setTitle("Profile Menu");
            popOver.setAnimated(true);

            Button btnDash = new Button("Dashboard");
            btnDash.setOnAction(evt ->
            {
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
                                UserManager.getInstance().loadDataFromServer();

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
                });
            });

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
        ScreenManager.getInstance().setLblScreenName(lblScreen);
        refreshView();
    }
}
