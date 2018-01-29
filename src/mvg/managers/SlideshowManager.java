package mvg.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import mvg.model.User;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import mvg.MVG;

public class SlideshowManager extends MVGObjectManager
{
    private String[] image_paths;
    private Gson gson;
    private int current_index;
    private static SlideshowManager slideshowManager = new SlideshowManager();

    private SlideshowManager()
    {
    }

    public void setCurrentIndex(int index)
    {
        this.current_index=index;
    }

    public int getCurrentIndex()
    {
        return this.current_index;
    }

    @Override
    public void initialize()
    {
        loadDataFromServer();
    }

    public static SlideshowManager getInstance()
    {
        return slideshowManager;
    }

    public void loadDataFromServer()
    {
        try
        {
            if(image_paths==null)
                reloadDataFromServer();
            else IO.log(getClass().getName(), IO.TAG_INFO, "image_paths object has already been set.");
        }catch (MalformedURLException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
            IO.showMessage("URL Error", ex.getMessage(), IO.TAG_ERROR);
        }catch (ClassNotFoundException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            IO.showMessage("ClassNotFoundException", e.getMessage(), IO.TAG_ERROR);
        }catch (IOException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
            IO.showMessage("I/O Error", ex.getMessage(), IO.TAG_ERROR);
        }
    }

    public void reloadDataFromServer() throws ClassNotFoundException, IOException
    {
        try
        {
            if(new File("images/slider/").mkdirs())
                IO.log(getClass().getName(), IO.TAG_ERROR, "successfully created [images/slider/] directory.");
            
            SessionManager smgr = SessionManager.getInstance();
            if(smgr.getActive()!=null)
            {
                if(!smgr.getActive().isExpired())
                {
                    gson  = new GsonBuilder().create();
                    ArrayList<AbstractMap.SimpleEntry<String,String>> headers = new ArrayList<>();
                    headers.add(new AbstractMap.SimpleEntry<>("Cookie", smgr.getActive().getSessionId()));

                    String slider_images_listing = RemoteComms.sendGetRequestByURL(RemoteComms.webserver_url+"/api/images/slider", headers);

                    image_paths = gson.fromJson(slider_images_listing, String[].class);
                    for(String img_filename: image_paths)
                    {
                        //if(!new File(new File(".").getCanonicalPath() + "/images/slider/" + img_filename).exists())
                        if(MVG.class.getResource("images/slider/" + img_filename)==null)
                        {
                            long start = System.currentTimeMillis();

                            byte[] img = RemoteComms.sendFileRequestByURL(RemoteComms.webserver_url+"/api/images/slider/" + img_filename, headers);
                           
                            FileOutputStream out = new FileOutputStream(new File("images/slider/"+img_filename));
                            out.write(img);
                            out.flush();
                            out.close();

                            IO.log(getClass()
                                    .getName(), IO.TAG_INFO, "downloaded [" + img_filename + ", " + img.length + " bytes] in [" + (System
                                    .currentTimeMillis() - start) + "] msec");
                        } else IO.log(getClass().getName(), IO.TAG_INFO, "file [/images/slider/"+img_filename+"] already exists.");
                    }
                    IO.log(getClass().getName(), IO.TAG_INFO, "reloaded slideshow collection.");
                }else{
                    IO.logAndAlert("Session Expired", "Active session has expired.", IO.TAG_ERROR);
                }
            }else{
                IO.logAndAlert("Session Expired", "No active sessions were found.", IO.TAG_ERROR);
            }
        }catch (MalformedURLException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
        }catch (IOException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
        }
    }

    public String[] getImagePaths()
    {
        return image_paths;
    }
}
