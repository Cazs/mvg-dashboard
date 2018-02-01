/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import mvg.MVG;
import mvg.auxilary.*;
import mvg.managers.EnquiryManager;
import mvg.managers.ScreenManager;
import mvg.managers.SessionManager;
import mvg.managers.SlideshowManager;
import mvg.model.Enquiry;
import mvg.model.Screens;
import java.io.File;
import java.io.IOException;
import javafx.fxml.FXML;
import java.net.URL;
import java.time.ZoneId;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;

/**
 * views Controller class
 *
 * @author ghost
 */

public class HomescreenController extends ScreenController implements Initializable, MapComponentInitializedListener
{
    @FXML
    private Button btnCreateAccount, btnTransport, btnAccommodation, btnExperience;
    @FXML
    private ImageView btnPrev, btnNext;
    @FXML
    private ImageView imgSlide;
    @FXML
    private HBox hboxSliderNav;
    @FXML
    private VBox enquiryForm;
    @FXML
    private TextField txtEnquiry, txtTime, txtAddress, txtDestination, txtTripType, txtComments;
    @FXML
    private DatePicker dateScheduled;
    @FXML
    private BorderPane popup_window;

    //@FXML
    //private GoogleMapView mapView;

    private GoogleMap map;

    @Override
    public void refreshView()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading homescreen view..");

        try
        {
            if(SlideshowManager.getInstance().getImagePaths()!=null)
            {
                //System.out.println(Files.createDirectories(new File("./images/slider/").toPath(), null));
                //Setup first slide
                /*Image image = SwingFXUtils.toFXImage(ImageIO.read(
                        new File(MVG.class.getResource("/images/slider/" + SlideshowManager.getInstance()
                                .getImagePaths()[SlideshowManager.getInstance().getCurrentIndex()]).getFile())), null);*/
                Image image = SwingFXUtils.toFXImage(ImageIO.read(
                        new File("images/slider/" + SlideshowManager.getInstance()
                                .getImagePaths()[SlideshowManager.getInstance().getCurrentIndex()])), null);
                imgSlide.setImage(image);
                imgSlide.setSmooth(true);

                //Set ImageView sizes
                if(ScreenManager.getInstance()!=null)
                {
                    imgSlide.setFitHeight(ScreenManager.getInstance().getStage().getHeight());
                    imgSlide.setFitWidth(ScreenManager.getInstance().getStage().getWidth());

                    //imgSlide2.setFitHeight(ScreenManager.getInstance().getStage().getHeight());
                    //imgSlide2.setFitWidth(ScreenManager.getInstance().getStage().getWidth());

                    hboxSliderNav.getChildren().setAll(new Node[]{});

                    hboxSliderNav.getStylesheets().add(MVG.class.getResource("styles/home.css").toExternalForm());
                    //Setup slider nav
                    for(int i=0;i<SlideshowManager.getInstance().getImagePaths().length;i++)
                    {
                        /*Pane pane = new Pane();
                        pane.setMaxWidth(20);
                        pane.setMaxHeight(20);
                        pane.getStylesheets().add(MVG.class.getResource("styles/home.css").toExternalForm());
                        pane.getStyleClass().add("slide-nav-item");*/
                        Circle circle = new Circle();
                        circle.setRadius(10);
                        circle.setStrokeWidth(2);
                        circle.setOnMouseEntered(event -> circle.setStroke(Color.CYAN));
                        circle.setOnMouseExited(event -> circle.setStroke(Color.TRANSPARENT));
                        final int new_index = i;
                        circle.setOnMouseClicked(event ->
                        {
                            SlideshowManager.getInstance().setCurrentIndex(new_index);
                            refreshView();
                        });

                        if(i==SlideshowManager.getInstance().getCurrentIndex())
                            //pane.setStyle("-fx-background-color: red");
                            circle.setFill(Color.color(1.0,.35f,0).brighter());
                        else circle.setFill(Color.DARKGREY);//pane.setStyle("-fx-background-color: #343434;");
                        hboxSliderNav.getChildren().add(circle);
                    }

                    //set slider image fixed y pos
                    imgSlide.setTranslateY(-80);

                    //set slider nav buttons pos
                    //HBox.setMargin(btnPrev.getParent(), new Insets(0,0,300,0));
                    //HBox.setMargin(btnNext.getParent(), new Insets(0,0,300,0));

                    //animate x-axis transition
                    final DoubleProperty x_transition =  imgSlide.translateXProperty();
                    Timeline transition = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(x_transition, -imgSlide.getFitWidth())),
                            new KeyFrame(Duration.millis(500),new KeyValue(x_transition, ScreenManager.getInstance().getStage().getWidth()*.005)));
                    transition.play();

                    //animate opacity
                    final DoubleProperty opacity =  imgSlide.opacityProperty();//ScreenManager.getInstance().opacityProperty();
                    Timeline fade = new Timeline(new KeyFrame(Duration.ONE, new KeyValue(opacity, 0.0)),
                            new KeyFrame(Duration.millis(1000),new KeyValue(opacity, 1.0)));
                    fade.play();
                } else
                {
                    IO.log(getClass().getName(), IO.TAG_ERROR, "MVG ScreenManager is null.");
                }
            } else IO.log(getClass().getName(), IO.TAG_ERROR, "slider image paths are null.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showEnquiries()
    {
        popup_window.setVisible(true);
    }

    @Override
    public void refreshModel()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "reloading homescreen data model..");
        SlideshowManager.getInstance().initialize();
    }
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
        //mapView.addMapInializedListener(this);
        if(ScreenManager.getInstance()!=null)
        {
            SlideshowManager.getInstance().initialize();
            //imgSlide.setFitWidth(Double.parseDouble(String.valueOf(ScreenManager.getInstance().getStage().getWidth())));
            //imgSlide.setFitHeight(Double.parseDouble(String.valueOf(ScreenManager.getInstance().getStage().getHeight())));

            ScreenManager.getInstance().getStage().widthProperty().addListener((observable, oldValue, newValue) ->
            {
                imgSlide.setFitWidth(Double.parseDouble(String.valueOf(newValue)));
                //imgSlide2.setFitWidth(Double.parseDouble(String.valueOf(newValue)));
            });
            ScreenManager.getInstance().getStage().heightProperty().addListener((observable, oldValue, newValue) ->
            {
                imgSlide.setFitHeight(Double.parseDouble(String.valueOf(newValue)));
                //imgSlide2.setFitHeight(Double.parseDouble(String.valueOf(newValue)));
            });

            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    while (true)
                    {
                        try
                        {
                            Platform.runLater(() -> nextSlide());
                            Thread.sleep(MVG.DELAY);
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                        }
                    }
                }
            }).start();
        }
    }

    private void showEnquiryWindow(String enquiry, double pos)
    {
        if(!txtEnquiry.getText().toLowerCase().equals(enquiry.toLowerCase()))//if text is not {enquiry}
        {
            txtEnquiry.setText(enquiry);
            //show if hidden
            if(!enquiryForm.isVisible())
                enquiryForm.setVisible(true);
        } else //text is {enquiry}
        {
            //show if hidden, hide if shown
            enquiryForm.setVisible(!enquiryForm.isVisible());
        }
        enquiryForm.setTranslateY(pos);
        //enquiryForm.getParent().setTranslateY(pos);
        enquiryForm.setLayoutY(pos);
        //enquiryForm.getParent().setTranslateY(pos);
    }

    @FXML
    public void transportClick()
    {
        showEnquiryWindow("Transport", btnTransport.getBoundsInParent().getMinY());
    }

    @FXML
    public void accommodationClick()
    {
        showEnquiryWindow("Accommodation", btnAccommodation.getBoundsInParent().getMinY());
    }

    @FXML
    public void experienceClick()
    {
        showEnquiryWindow("Experience", btnExperience.getBoundsInParent().getMinY());
    }

    @FXML
    public void submitEnquiry()
    {
        if(!Validators.isValidNode(txtEnquiry, "Invalid Enquiry", 5, "^.*(?=.{5,}).*"))//"please enter a valid enquiry"
            return;
        if(!Validators.isValidNode(dateScheduled, (dateScheduled.getValue()==null?"":String.valueOf(dateScheduled.getValue())), "^.*(?=.{1,}).*"))
            return;
        if(!Validators.isValidNode(txtTime, "Invalid Pickup Location", 5, "^.*(?=.{1,}).*"))//"please enter a valid pickup address"
            return;
        if(!Validators.isValidNode(txtAddress, "Invalid Pickup Location", 1, "^.*(?=.{1,}).*"))//"please enter a valid pickup address"
            return;
        if(!Validators.isValidNode(txtDestination, "Invalid Destination", 1, "^.*(?=.{1,}).*"))//"please enter a valid destination"
            return;
        if(!Validators.isValidNode(txtTripType, "Invalid Trip Type", 1, "^.*(?=.{1,}).*"))//"please enter a valid trip type"
            return;

        Enquiry enquiry = new Enquiry();
        enquiry.setEnquiry(txtEnquiry.getText());
        enquiry.setComments(txtComments.getText());
        enquiry.setPickup_location(txtAddress.getText());
        enquiry.setDestination(txtDestination.getText());
        enquiry.setDate_scheduled(dateScheduled.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond());
        enquiry.setTrip_type(txtTripType.getText());
        enquiry.setCreator(SessionManager.getInstance().getActive().getUsername());

        try
        {
            EnquiryManager.getInstance().createEnquiry(enquiry, new_enquiry_id ->
            {
                IO.logAndAlert("Success", "Created Enquiry ["+new_enquiry_id+"].", IO.TAG_INFO);
                return null;
            });
        } catch (IOException e)
        {
            IO.logAndAlert("I/O Error", e.getMessage(), IO.TAG_ERROR);
        }
    }

    @FXML
    public void showSettings()
    {
        try
        {
            if(ScreenManager.getInstance().loadScreen(Screens.SETTINGS.getScreen(),getClass().getResource("../views/"+Screens.SETTINGS.getScreen())))
                ScreenManager.getInstance().setScreen(Screens.SETTINGS.getScreen());
            else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load settings screen.");
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void nextSlide()
    {
        if(SlideshowManager.getInstance().getImagePaths()!=null)
        {
            if(SlideshowManager.getInstance().getCurrentIndex()+1 < SlideshowManager.getInstance().getImagePaths().length)
                SlideshowManager.getInstance().setCurrentIndex(SlideshowManager.getInstance().getCurrentIndex()+1);
            else SlideshowManager.getInstance().setCurrentIndex(0);

            refreshView();
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "No slider images found.");
    }

    @FXML
    public void previousSlide()
    {
        if(SlideshowManager.getInstance().getImagePaths()!=null)
        {
            if(SlideshowManager.getInstance().getCurrentIndex()-1 >= 0)
                SlideshowManager.getInstance().setCurrentIndex(SlideshowManager.getInstance().getCurrentIndex()-1);
            else SlideshowManager.getInstance().setCurrentIndex(SlideshowManager.getInstance().getImagePaths().length-1);

            refreshView();
        } else IO.log(getClass().getName(), IO.TAG_ERROR, "No slider images found.");
    }

    @Override
    public void mapInitialized()
    {
        IO.log(getClass().getName(), IO.TAG_INFO, "map initialized");
        LatLong joeSmithLocation = new LatLong(47.6197, -122.3231);
        LatLong joshAndersonLocation = new LatLong(47.6297, -122.3431);
        LatLong bobUnderwoodLocation = new LatLong(47.6397, -122.3031);
        LatLong tomChoiceLocation = new LatLong(47.6497, -122.3325);
        LatLong fredWilkieLocation = new LatLong(47.6597, -122.3357);


        //Set the initial properties of the map.
        MapOptions mapOptions = new MapOptions();

        mapOptions.center(new LatLong(47.6097, -122.3331))
                .mapType(MapTypeIdEnum.ROADMAP)
                .overviewMapControl(false)
                .panControl(false)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(false)
                .zoom(12);

        //map = mapView.createMap(mapOptions);

        //Add markers to the map
        MarkerOptions markerOptions1 = new MarkerOptions();
        markerOptions1.position(joeSmithLocation);

        MarkerOptions markerOptions2 = new MarkerOptions();
        markerOptions2.position(joshAndersonLocation);

        MarkerOptions markerOptions3 = new MarkerOptions();
        markerOptions3.position(bobUnderwoodLocation);

        MarkerOptions markerOptions4 = new MarkerOptions();
        markerOptions4.position(tomChoiceLocation);

        MarkerOptions markerOptions5 = new MarkerOptions();
        markerOptions5.position(fredWilkieLocation);

        Marker joeSmithMarker = new Marker(markerOptions1);
        Marker joshAndersonMarker = new Marker(markerOptions2);
        Marker bobUnderwoodMarker = new Marker(markerOptions3);
        Marker tomChoiceMarker= new Marker(markerOptions4);
        Marker fredWilkieMarker = new Marker(markerOptions5);

        map.addMarker( joeSmithMarker );
        map.addMarker( joshAndersonMarker );
        map.addMarker( bobUnderwoodMarker );
        map.addMarker( tomChoiceMarker );
        map.addMarker( fredWilkieMarker );

        InfoWindowOptions infoWindowOptions = new InfoWindowOptions();
        infoWindowOptions.content("<h2>Fred Wilkie</h2>"
                + "Current Location: Safeway<br>"
                + "ETA: 45 minutes" );

        InfoWindow fredWilkeInfoWindow = new InfoWindow(infoWindowOptions);
        fredWilkeInfoWindow.open(map, fredWilkieMarker);
    }
}