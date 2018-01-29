/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.MVG;
import mvg.auxilary.Globals;
import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import mvg.auxilary.Session;
import mvg.exceptions.LoginException;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.managers.UserManager;
import mvg.model.Screens;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.ConnectException;

/**
 *
 * @author ghost
 */
public abstract class ScreenController
{
    @FXML
    private ImageView img_profile;
    @FXML
    private Label user_name;
    @FXML
    private Circle shpServerStatus;
    @FXML
    private Label lblOutput;
    @FXML
    private BorderPane loading_pane;

    public ScreenController()
    {
    }

    public abstract void refreshView();

    public abstract void refreshModel();

    public void refreshStatusBar(String msg)
    {
        try
        {
            boolean ping = RemoteComms.pingServer();
            Platform.runLater(() ->
            {
                shpServerStatus.setStroke(Color.TRANSPARENT);
                if(ping)
                    shpServerStatus.setFill(Color.LIME);
                else shpServerStatus.setFill(Color.RED);
                lblOutput.setText(msg);
            });
        } catch (IOException e)
        {
            if(Globals.DEBUG_ERRORS.getValue().equalsIgnoreCase("on"))
                System.out.println(getClass().getName() + ">" + IO.TAG_ERROR + ">" + "could not refresh status bar: "+e.getMessage());
            Platform.runLater(() ->
            {
                shpServerStatus.setFill(Color.RED);
                lblOutput.setText(msg);
            });
        }
    }

    @FXML
    public void showLogin()
    {
        try
        {
            if(ScreenManager.getInstance().loadScreen(Screens.LOGIN.getScreen(),getClass().getResource("../views/"+Screens.LOGIN.getScreen())))
                ScreenManager.getInstance().setScreen(Screens.LOGIN.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load login screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void showMain()
    {
        try
        {
            if(ScreenManager.getInstance().loadScreen(Screens.HOME.getScreen(),getClass().getResource("../views/"+Screens.HOME.getScreen())))
                ScreenManager.getInstance().setScreen(Screens.HOME.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load home screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void showEnquiries()
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
                        //load User data to memory
                        UserManager.getInstance().loadDataFromServer();

                        if (ScreenManager.getInstance().loadScreen(Screens.DASHBOARD.getScreen(), MVG.class.getResource("views/" + Screens.DASHBOARD.getScreen())))
                        {
                            ScreenManager.getInstance().setScreen(Screens.DASHBOARD.getScreen());
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load dashboard screen.");
                    } catch(ConnectException ex)
                    {
                        IO.logAndAlert("Error", ex.getMessage() + ", \nis the server up? are you connected to the network?", IO.TAG_ERROR);
                    } catch (IOException e)
                    {
                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        });
    }

    @FXML
    public void newClientHandler()
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
                        //load User data to memory
                        UserManager.getInstance().loadDataFromServer();

                        if (ScreenManager.getInstance().loadScreen(Screens.NEW_CLIENT.getScreen(), MVG.class.getResource("views/" + Screens.NEW_CLIENT.getScreen())))
                        {
                            ScreenManager.getInstance().setScreen(Screens.NEW_CLIENT.getScreen());
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load clients creation screen.");
                    } catch(ConnectException ex)
                    {
                        IO.logAndAlert("Error", ex.getMessage() + ", \nis the server up? are you connected to the network?", IO.TAG_ERROR);
                    } catch (IOException e)
                    {
                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        });
    }

    @FXML
    public void newResourceHandler()
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
                        //load User data to memory
                        UserManager.getInstance().loadDataFromServer();

                        if (ScreenManager.getInstance().loadScreen(Screens.NEW_RESOURCE.getScreen(), MVG.class.getResource("views/" + Screens.NEW_RESOURCE.getScreen())))
                        {
                            ScreenManager.getInstance().setScreen(Screens.NEW_RESOURCE.getScreen());
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load resource creation screen.");
                    } catch(ConnectException ex)
                    {
                        IO.logAndAlert("Error", ex.getMessage() + ", \nis the server up? are you connected to the network?", IO.TAG_ERROR);
                    } catch (IOException e)
                    {
                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        });
    }

    @FXML
    public void newQuoteHandler()
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
                        //load User data to memory
                        UserManager.getInstance().loadDataFromServer();

                        if (ScreenManager.getInstance().loadScreen(Screens.NEW_QUOTE.getScreen(), MVG.class.getResource("views/" + Screens.NEW_QUOTE.getScreen())))
                        {
                            ScreenManager.getInstance().setScreen(Screens.NEW_QUOTE.getScreen());
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load quote creation screen.");
                    } catch(ConnectException ex)
                    {
                        IO.logAndAlert("Error", ex.getMessage() + ", \nis the server up? are you connected to the network?", IO.TAG_ERROR);
                    } catch (IOException e)
                    {
                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }).start();
            return null;
        });
    }

    @FXML
    public void createAccount()
    {
        try
        {
            if(ScreenManager.getInstance().loadScreen(Screens.CREATE_ACCOUNT.getScreen(),getClass().getResource("../views/"+Screens.CREATE_ACCOUNT.getScreen())))
                ScreenManager.getInstance().setScreen(Screens.CREATE_ACCOUNT.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load account creation screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void comingSoon()
    {
        IO.logAndAlert("Coming Soon", "This feature is currently being implemented.", IO.TAG_INFO);
    }

    public ImageView getProfileImageView()
    {
        return this.img_profile;
    }

    public Label getUserNameLabel()
    {
        return this.user_name;
    }

    public BorderPane getLoadingPane()
    {
        return this.loading_pane;
    }

    @FXML
    public void previousScreen()
    {
        try
        {
            ScreenManager.getInstance().setPreviousScreen();
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }
}
