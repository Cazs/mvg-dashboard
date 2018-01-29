/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg;

import com.sun.javafx.application.LauncherImpl;
import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.managers.ScreenManager;
import mvg.model.Screens;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javax.swing.*;
import java.awt.*;

/**
 *
 * @author ghost
 */
public class MVG extends Application
{
    //private static ScreenManager screen_manager;
    public static final long DELAY = 4000;

    @Override
    public void start(Stage stage) throws Exception 
    {
        stage.setOnCloseRequest(event ->
        {
            //TODO: fix this!!! no swing!!
            int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit?");
            if(result==JOptionPane.OK_OPTION)
            {
                stage.close();
                System.exit(0);
            }
            else  event.consume();
        });
        //grid = new GridDisplay(2, 4);
        //screen_manager = new ScreenManager();
        ScreenManager screen_manager = ScreenManager.getInstance();
        screen_manager.setStage(stage);
        IO.getInstance().init(screen_manager);

        if(screen_manager.loadScreen(Screens.LOGIN.getScreen(),getClass().getResource("views/"+Screens.LOGIN.getScreen())))
        {
            screen_manager.setScreen(Screens.LOGIN.getScreen());
            HBox root = new HBox();
            HBox.setHgrow(screen_manager, Priority.ALWAYS);

            root.getChildren().addAll(screen_manager);

            Scene scene = new Scene(root);
            stage.setTitle(Globals.COMPANY.getValue()+" - "+Globals.APP_NAME.getValue());
            stage.setScene(scene);

            stage.setMinHeight(600);
            stage.setHeight(700);
            stage.setMinWidth(600);

            if(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getWidth()>=1200)
                stage.setWidth(900);
            stage.show();
        }else
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, "login screen was not successfully loaded.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        LauncherImpl.launchApplication(mvg.MVG.class, MVGPreloader.class, args);
    }

    /*public static ScreenManager getScreenManager()
    {
        return screen_manager;
    }*/
}
