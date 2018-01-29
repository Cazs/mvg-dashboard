package mvg;

import mvg.auxilary.IO;
import javafx.application.Preloader;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by ghost on 2017/06/11.
 */
public class MVGPreloader extends Preloader
{
    ProgressBar bar;
    Stage stage;
    int shadowSize = 50;
    TextField txtUsr, txtPwd;

    private Scene createPreloaderScene()
    {
        bar = new ProgressBar();

        ImageView logo = new ImageView();
        try
        {
            BufferedImage buff_image=null;
            if(MVG.class.getResource("gfx/logo.png")!=null)
                buff_image = ImageIO.read(MVG.class.getResource("gfx/logo.png"));
            else IO.logAndAlert("IO Error", "Logo file could not be found.", IO.TAG_ERROR);
            //SELECT * FROM users WHERE usr='' AND pwd=''
            if(buff_image!=null)
            {
                Image image = SwingFXUtils.toFXImage(buff_image, null);
                logo.setImage(image);
                logo.setFitHeight(50);
                logo.setFitWidth(150);
            }else IO.logAndAlert("Error", "Buffered image is null.", IO.TAG_ERROR);
        } catch (IOException e)
        {
            IO.logAndAlert("IO Error", e.getMessage(), IO.TAG_ERROR);
            e.printStackTrace();
        }

        Pane logo_cont = new Pane(logo);
        logo_cont.setId("logo_cont");

        Label lblMsg = new Label("Loading...");
        VBox vbxBottom = new VBox();
        vbxBottom.getChildren().addAll(bar, lblMsg);
        vbxBottom.setAlignment(Pos.CENTER);
        vbxBottom.setStyle("-fx-border-insets:0px 0px 0px 10px;");
        vbxBottom.setStyle("-fx-background-insets:0px 0px 0px 10px;");

        BorderPane main_pane = new BorderPane(createShadowPane());
        main_pane.setRight(logo_cont);
        main_pane.setBottom(vbxBottom);

        //scene.setStyle("-fx-background-color:rgba(255,255,255,0.5);" +
        //        "-fx-background-insets:50;");

        //Button btnLogin = new Button("Login");

        //login_container.getChildren().addAll(usr_cont, pwd_cont, btnLogin);

        //btnLogin.setOnAction(event -> login());

        /*BorderPane borderPane_login = new BorderPane();
        VBox login_container = new VBox();

        HBox usr_cont = new HBox(new Label("Username"));
        TextField txtUsr = new TextField();
        usr_cont.getChildren().add(txtUsr);

        HBox pwd_cont = new HBox(new Label("Password"));
        TextField txtPwd = new TextField();
        pwd_cont.getChildren().add(txtPwd);

        login_container.getChildren().addAll(usr_cont, pwd_cont);
        borderPane_login.setCenter(login_container);

        stackPane.getChildren().addAll(borderPane_login);*/

        return new Scene(main_pane, 300, 150);
    }

    // Create a shadow effect as a halo around the pane and not within
    // the pane's content area.
    private Pane createShadowPane()
    {
        Pane shadowPane = new Pane();

        Rectangle innerRect = new Rectangle();
        Rectangle outerRect = new Rectangle();
        shadowPane.layoutBoundsProperty().addListener(
                (observable, oldBounds, newBounds) -> {
                    innerRect.relocate(
                            newBounds.getMinX() + shadowSize,
                            newBounds.getMinY() + shadowSize
                    );
                    innerRect.setWidth(newBounds.getWidth() - shadowSize * 2);
                    innerRect.setHeight(newBounds.getHeight() - shadowSize * 2);

                    outerRect.setWidth(newBounds.getWidth());
                    outerRect.setHeight(newBounds.getHeight());

                    Shape clip = Shape.subtract(outerRect, innerRect);
                    shadowPane.setClip(clip);
                }
        );

        return shadowPane;
    }

    public void start(Stage stage) throws Exception
    {
        this.stage = stage;
        stage.setWidth(500);
        stage.setHeight(300);
        //stage.initStyle(StageStyle.TRANSPARENT);
        Scene scene = createPreloaderScene();
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(mvg.MVGPreloader.class.getResource("styles/splash.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void handleProgressNotification(ProgressNotification pn)
    {
        bar.setProgress(pn.getProgress());
    }

    @Override
    public void handleStateChangeNotification(StateChangeNotification evt)
    {
        if (evt.getType() == StateChangeNotification.Type.BEFORE_START)
        {
            /*try
            {
                Thread.sleep(5000);
            } catch (InterruptedException e)
            {
                System.err.println(e.getMessage());
            }*/
            stage.hide();
        }
    }
}
