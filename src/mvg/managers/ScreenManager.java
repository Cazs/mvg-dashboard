/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.managers;

import java.io.IOException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.Stack;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import mvg.auxilary.IO;
import mvg.auxilary.RadialMenuItemCustom;
import mvg.controllers.DashboardController;
import mvg.controllers.ScreenController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.util.Callback;
import jfxtras.labs.scene.control.radialmenu.RadialContainerMenuItem;
import jfxtras.labs.scene.control.radialmenu.RadialMenu;
import jfxtras.labs.scene.control.radialmenu.RadialMenuItem;
import org.controlsfx.control.MaskerPane;

/**
 *
 * @author ghost
 */
public class ScreenManager extends StackPane
{
    private RadialMenu radialMenu;
    private boolean show;
    public static final double MENU_SIZE = 40.0;
    private double lastOffsetValue;
    private double lastInitialAngleValue;
    private double gestureAngle = 0;
    public Double menuSize = 55.0;
    public Double containerSize = 30.0;
    public Double initialAngle = 0.0;//-90.0
    public Double innerRadius = 50.0;
    public Double radius = 150.0;
    public Double offset = 5.0;
    private Stack<AbstractMap.SimpleEntry<String, Node>> screens = new Stack<>();
    private Stack<AbstractMap.SimpleEntry<String, ScreenController>> controllers = new Stack<>();
    private ScreenController focused;
    private String focused_id;
    private String previous_id;
    private Node loading_screen;
    private ScreenController loading_screen_ctrl;
    private Node screen = null;
    private static ScreenManager screenManager = new ScreenManager();
    private Label lblScreenName;
    private Stage stage;
    private BorderPane translucent_borderpane;

    /*public ScreenManager(Stage stage)
    {
        super();

        if(stage!=null)
            this.stage=stage;
        else{
            IO.logAndAlert("Error", "Main stage is null.", IO.TAG_ERROR);
            System.exit(-1);
        }
        /*try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("../views/loading.fxml"));
            loading_screen = loader.load();
            loading_screen_ctrl = loader.getController();
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }/
    }*/

    public void setStage(Stage stage)
    {
        this.stage = stage;
    }

    public Stage getStage()
    {
        return stage;
    }

    private ScreenManager()
    {
        super();
        try
        {
            FXMLLoader loader = new FXMLLoader(mvg.MVG.class.getResource("views/loading.fxml"));
            loading_screen = loader.load();
            loading_screen_ctrl = loader.getController();
            //loading_screen_ctrl.setParent(this);
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    public static ScreenManager getInstance()
    {
        return screenManager;
    }

    /**
     * Method to load a single ScreenController into memory.
     * @param id ScreenController identifier
     * @param path Path to the FXML view for the ScreenController.
     * @return true if successfully added new screen, false otherwise.
     * @throws IOException
     */
    public boolean  loadScreen(String id, URL path) throws IOException
    {
        FXMLLoader loader = new FXMLLoader(path);
        Parent screen = loader.load();

        ScreenController screen_controller = loader.getController();
        //screen_controller.setParent(this);

        controllers.push(new AbstractMap.SimpleEntry<>(id, screen_controller));
        screens.push(new AbstractMap.SimpleEntry<>(id, screen));
        IO.log(getClass().getName(), IO.TAG_INFO, "loaded screen: "+id);
        return true;
    }

    public AbstractMap.SimpleEntry<String, ScreenController> peekScreenControllers()
    {
        if(controllers!=null)
            return controllers.peek();
        else return null;
    }

    public Node peekScreens()
    {
        if(screens!=null)
            return screens.peek().getValue();
        else return null;
    }

    public void setPreviousScreen() throws IOException
    {
        if(screens.size()>1)
        {
            screens.pop().getValue();
            controllers.pop().getValue();
            setScreen("previous");
        }
    }

    public void setLblScreenName(Label screenName)
    {
        this.lblScreenName=screenName;
    }

    public void initRadialMenu(RadialMenuItem[] menuItems)//, RadialContainerMenuItem[] containerMenuItems
    {
        if(menuItems==null)
        {
            IO.log(getClass().getName(), IO.TAG_WARN, "context menu for active screen is null.");
            return;
        }
        radialMenu = new RadialMenu(initialAngle, innerRadius, radius, offset, Color.DARKCYAN, Color.CYAN,
                Color.DARKGREY.darker().darker(), Color.DARKGREY.darker(),
                false, RadialMenu.CenterVisibility.ALWAYS, null);
        radialMenu.setTranslateX(400);
        radialMenu.setTranslateY(400);
        radialMenu.setVisible(false);
        //radialMenu.hideRadialMenu();
        //radialMenu.setRotate(-90);

        //Add items/containers to RadialMenu component
        for(RadialMenuItem menuItem: menuItems)
            radialMenu.addMenuItem(menuItem);

        getChildren().add(radialMenu);
    }

    public void showContextMenu()//final double x, final double y
    {
        /*if(focused!=null)
        {
            if(focused instanceof DashboardController)
                initRadialMenu(((DashboardController)focused).getContextMenu());
            else if(focused instanceof OtherController)
                initRadialMenu(((OtherController)focused).getContextMenu());
            else initRadialMenu(focused.getDefaultContextMenu());
        }*/

        /*if(radialMenu.isVisible())
        {
            lastInitialAngleValue = radialMenu.getInitialAngle();
            lastOffsetValue = radialMenu.getOffset();
            //radialMenu.setVisible(false);
        }*/
        radialMenu.setTranslateX(this.getMinWidth()/2);
        //radialMenu.setTranslateY(y);
        radialMenu.setVisible(true);

        translucent_borderpane = new BorderPane();
        translucent_borderpane.setStyle("-fx-background-color: rgba(0,0,0,.7);");
        this.getChildren().add(this.getChildren().size()-1, translucent_borderpane);

        final DoubleProperty y_transition =  radialMenu.translateYProperty();
        Timeline yTransition = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(y_transition, 0)),
                new KeyFrame(Duration.millis(150),new KeyValue(y_transition, this.getMinHeight()/2)));
        yTransition.play();

        /*final DoubleProperty x_transition =  radialMenu.translateXProperty();
        Timeline transition = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(x_transition, this.getWidth())),
                new KeyFrame(Duration.millis(150),new KeyValue(x_transition, this.getMinWidth()/2)));
        transition.play();*/

        final DoubleProperty opacity_transition_property =  radialMenu.opacityProperty();
        Timeline opacity_transition = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(opacity_transition_property, 0.0)),
                new KeyFrame(Duration.millis(250),new KeyValue(opacity_transition_property, 1.0)));
        opacity_transition.play();

        radialMenu.showRadialMenu();
    }

    public void hideContextMenu()
    {
        radialMenu.hideRadialMenu();
        final DoubleProperty opacity_transition =  radialMenu.opacityProperty();
        Timeline transition = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(opacity_transition, 1.0)),
                new KeyFrame(Duration.millis(250),new KeyValue(opacity_transition, 0.0)));
        transition.play();
        if(translucent_borderpane!=null)
            this.getChildren().remove(translucent_borderpane);
        radialMenu.setVisible(false);
    }

    private void showRadialMenu(final double x, final double y)
    {
        if (radialMenu.isVisible())
        {
            lastInitialAngleValue = radialMenu.getInitialAngle();
            lastOffsetValue = radialMenu.getOffset();
            radialMenu.setVisible(false);
        }
        radialMenu.setTranslateX(x);
        radialMenu.setTranslateY(y);
        radialMenu.setVisible(true);

        /*final FadeTransition fade = FadeTransitionBuilder.create()
                .node(radialMenu).duration(Duration.millis(400))
                .fromValue(0).toValue(1.0).build();

        final Animation offset = new Timeline(new KeyFrame(Duration.ZERO,
                new KeyValue(radialMenu.offsetProperty(), 0)),
                new KeyFrame(Duration.millis(300), new KeyValue(radialMenu
                        .offsetProperty(), lastOffsetValue)));

        final Animation angle = new Timeline(new KeyFrame(Duration.ZERO,
                new KeyValue(radialMenu.initialAngleProperty(),
                        lastInitialAngleValue + 20)), new KeyFrame(
                Duration.millis(300), new KeyValue(
                radialMenu.initialAngleProperty(),
                lastInitialAngleValue)));

        final ParallelTransition transition = ParallelTransitionBuilder
                .create().children(fade, offset, angle).build();

        transition.play();*/
    }

    /**
     * Method to add a ScreenController object to ScreenManager
     * @param id ScreenController identifier
     */
    public void setScreen(final String id)
    {
        if(screens.peek()!=null)
            screen = screens.peek().getValue();
        if(screen!=null)
        {
            ScreenController controller = null;
            //update UI of current view
            if(controllers.peek()!=null)
                controller = controllers.peek().getValue();

            if(controller!=null)
            {
                if(focused_id!=null)
                {
                    if (!focused_id.equals(previous_id))
                    {
                        previous_id = focused_id;
                        IO.log(getClass().getName(), IO.TAG_INFO, "set previous screen to: " + previous_id);
                    }
                }

                focused_id = id;
                focused = controller;
                //screen.setOpacity(1);
                focused.refreshModel();
                Platform.runLater(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if(getChildren().setAll(new Node[]{}))//remove all screens
                        {
                            focused.refreshStatusBar("Welcome back" + (SessionManager.getInstance()
                                    .getActiveUser() != null ? " " + SessionManager.getInstance()
                                    .getActiveUser() + "!" : "!"));
                            focused.refreshView();//refresh the screen every time it's loaded
                            IO.log(getClass().getName(), IO.TAG_INFO, "set screen: " + id);

                            if(lblScreenName!=null)
                                lblScreenName.setText(focused_id.split("\\.")[0]);
                            getChildren().add(screen);
                            //createRadialMenu();
                            //showRadialMenu(300,250);
                            //getChildren().add(radialMenu);
                        }else
                        {
                            IO.logAndAlert(getClass().getName(), "Could not remove StackPane children.", IO.TAG_ERROR);
                        }
                    }
                });
            }


            /*final DoubleProperty opacity =  opacityProperty();
            Timeline fade = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(opacity, 0.0)),
                    new KeyFrame(Duration.millis(20),new KeyValue(opacity, 1.0)));
            fade.play();*/
        }else{
            IO.logAndAlert(getClass().getName(), "ScreenController ["+id+"] not loaded to memory.", IO.TAG_ERROR);
        }
    }

    public void showLoadingScreen(Callback callback)
    {
        //if(getChildren().setAll(new Node[]{}))//remove all screens
        {
            //loading_screen_ctrl.refreshStatusBar("Loading data, please wait...");
            //loading_screen_ctrl.refreshView();
            MaskerPane maskerPane= new MaskerPane();
            maskerPane.setVisible(true);
            getChildren().add(maskerPane);
            //getChildren().add(loading_screen);
            /*if(focused!=null)
            {
                focused.getLoadingPane().setVisible(true);
                //System.out.println(focused.getLoadingPane()==null);
            }*/
            if(callback!=null)
                callback.call(null);
        }
    }

    public ScreenController getFocused()
    {
        return this.focused;
    }

    public String getFocused_id()
    {
        return this.focused_id;
    }

    public Node removeScreen(Node screen)
    {
        //screens
        for(Node n: getChildren())
        {
            if(n==screen)
            {
                if(getChildren().remove(n))
                    return n;
                else
                    return null;
            }
        }
        return null;
    }
}
