package mvg.auxilary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.geometry.Pos;
import javafx.util.Duration;
import mvg.controllers.ScreenController;
import mvg.managers.ScreenManager;
import mvg.model.MVGObject;
import mvg.model.Message;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.controlsfx.control.Notifications;

import java.io.*;

/**
 * Created by ghost on 2017/01/28.
 */
public class IO<T extends MVGObject>
{

    public static final String TAG_INFO = "info";
    public static final String TAG_WARN = "warning";
    public static final String TAG_ERROR = "error";
    private static final String TAG = "IO";
    private static IO io = new IO();
    private static ScreenManager screenManager;


    private IO()
    {
    }

    public static IO getInstance(){return io;}

    public void init(ScreenManager screenManager)
    {
        this.screenManager = screenManager;
    }

    /**
     * Every time you print out a log message, refresh the focused screen's status bar.
     * @param src source class calling the log method.
     * @param tag the log message type, i.e. error, warning or information.
     * @param msg the log message to be printed.
     */
    public static void log(String src, String tag, String msg)
    {
        if(screenManager!=null)
        {
            ScreenController current_screen = screenManager.getFocused();
            if(current_screen!=null)
            {
                //get filename with no extension from src param and apply it on status bar - then refresh it.
                if (src.contains("."))
                    current_screen.refreshStatusBar(src.substring(src.lastIndexOf(".") + 1) + "> " + tag + ":: " + msg);
                else current_screen.refreshStatusBar(src + "> " + tag + ":: " + msg);
            }else System.err.println(getInstance().getClass().getName() + "> error: focused screen is null.");
        }
        switch (tag.toLowerCase())
        {
            case TAG_INFO:
                if (Globals.DEBUG_INFO.getValue().toLowerCase().equals("on"))
                    System.out.println(String.format("%s> %s: %s", src, tag, msg));
                break;
            case TAG_WARN:
                if (Globals.DEBUG_WARNINGS.getValue().toLowerCase().equals("on"))
                    System.out.println(String.format("%s> %s: %s", src, tag, msg));
                break;
            case TAG_ERROR:
                if (Globals.DEBUG_ERRORS.getValue().toLowerCase().equals("on"))
                    System.err.println(String.format("%s> %s: %s", src, tag, msg));
                break;
            default://fallback for custom tags
                System.out.println(String.format("%s> %s: %s", src, tag, msg));
                break;
        }
    }

    public void quickSort(T arr[], int left, int right, String comparator)
    {
        int index = partition(arr, left, right, comparator);
        if (left < index - 1)
            quickSort(arr, left, index - 1, comparator);
        if (index < right)
            quickSort(arr, index, right, comparator);
    }

    public int partition(T arr[], int left, int right, String comparator) throws ClassCastException
    {
        int i = left, j = right;
        T tmp;
        double pivot = (Double) arr[(left + right) / 2].get(comparator);

        while (i <= j)
        {
            while ((Double) arr[i].get(comparator) < pivot)
                i++;
            while ((Double) arr[j].get(comparator) > pivot)
                j--;
            if (i <= j)
            {
                tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;
                i++;
                j--;
            }
        }
        return i;
    }

    public static void showMessage(String title, String msg, String type)
    {
        Platform.runLater(() ->
        {
            switch (type.toLowerCase())
            {
                case TAG_INFO:
                    /*NotificationPane notificationPane = new NotificationPane();
                    notificationPane.getStyleClass().add(NotificationPane.STYLE_CLASS_DARK);
                    notificationPane.setShowFromTop(true);
                    notificationPane.setText(msg);
                    //notificationPane.setGraphic(new Button("a button"));
                    notificationPane.show();*/
                    Notifications.create()
                            .title(title)
                            .text(msg)
                            .hideAfter(Duration.seconds(15))
                            .position(Pos.BOTTOM_LEFT)
                            .showInformation();
                    break;
                case TAG_WARN:
                    Notifications.create()
                            .title(title)
                            .text(msg)
                            .hideAfter(Duration.seconds(30))
                            .showWarning();
                    break;
                case TAG_ERROR:
                    Notifications.create()
                            .title(title)
                            .text(msg)
                            .hideAfter(Duration.INDEFINITE)
                            .position(Pos.CENTER)
                            .showError();
                    break;
                default:
                    IO.log(IO.class.getName(), IO.TAG_ERROR, "unknown message type '" + type + "'");
                    //Notifications.create().title(title).text(msg).showWarning();
                    break;
            }
        });
    }

    public static void logAndAlert(String title, String msg, String type)
    {
        log(title, type, msg);
        showMessage(title, msg, type);
    }

    public static String readStream(InputStream stream) throws IOException
    {
        //Get message from input stream
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        if(in!=null)
        {
            StringBuilder msg = new StringBuilder();
            String line;
            while ((line = in.readLine())!=null)
            {
                msg.append(line + "\n");
            }
            in.close();

            //try to read response as JSON object - default method of responses by server.
            Gson gson = new GsonBuilder().create();
            try
            {
                Message message = gson.fromJson(msg.toString(), Message.class);
                if (message != null)
                    if (message.getMessage() != null)
                        if (message.getMessage().length() > 0)
                            return message.getMessage();
            }catch (JsonSyntaxException e)
            {
                IO.log(TAG, IO.TAG_WARN, "message from server not in standard (JSON) Message format.");
            }
            return msg.toString();
        }else IO.logAndAlert(TAG, "could not read error stream from server response.", IO.TAG_ERROR);
        return null;
    }
}
