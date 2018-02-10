package mvg.auxilary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import mvg.controllers.ScreenController;
import mvg.managers.ScreenManager;
import mvg.model.MVGObject;
import mvg.model.Message;
import javafx.application.Platform;
import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Created by ghost on 2017/01/28.
 */
public class IO<T extends MVGObject>
{

    public static final String TAG_INFO = "info";
    public static final String TAG_WARN = "warning";
    public static final String TAG_ERROR = "error";
    public static final String YES = "Yes";
    public static final String NO = "No";
    public static final String OK = "OK";
    public static final String CANCEL = "Cancel";
    public static final String STYLES_ROOT_PATH= "styles/";//FadulousBMS.class.getResource("styles/").getPath();
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
                    current_screen.refreshStatusBar(src.substring(src.lastIndexOf(".") + 1) + "> " + tag + ":: " + msg.replaceAll("\n",""));
                else current_screen.refreshStatusBar(src + "> " + tag + ":: " + msg.replaceAll("\n",""));
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

    public static String generateRandomString(int len)
    {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        String str="";
        for(int i=0;i<len;i++)
            str+=chars.charAt((int)(Math.floor(Math.random()*chars.length())));
        return str;
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
        NotificationPane notificationPane = new NotificationPane();
        Platform.runLater(() ->
        {
            switch (type.toLowerCase())
            {
                case TAG_INFO:
                    //notificationPane.getStyleClass().add(NotificationPane.STYLE_CLASS_DARK);
                    //notificationPane.setText(msg);
                    //notificationPane.setGraphic(new Button("a button"));
                    //notificationPane.show();
                    Notifications.create()
                            .title(title)
                            .text(msg)
                            .hideAfter(Duration.seconds(15))
                            .position(Pos.BOTTOM_LEFT)
                            .owner(ScreenManager.getInstance())
                            .showInformation();
                    break;
                case TAG_WARN:
                    //notificationPane.getStyleClass().add(NotificationPane.STYLE_CLASS_DARK);
                    //notificationPane.setShowFromTop(true);
                    //notificationPane.setText(msg);
                    //notificationPane.setGraphic(new Button("a button"));
                    //notificationPane.show();
                    Notifications.create()
                            .title(title)
                            .text(msg)
                            .hideAfter(Duration.seconds(10))
                            .owner(ScreenManager.getInstance())
                            .showWarning();
                    break;
                case TAG_ERROR:
                    //notificationPane.getStyleClass().add(NotificationPane.STYLE_CLASS_DARK);
                    //notificationPane.setText(msg);
                    //notificationPane.setGraphic(new Button("a button"));
                    //notificationPane.show();
                    Notifications.create()
                            .title(title)
                            .text(msg)
                            .hideAfter(Duration.INDEFINITE)
                            .position(Pos.CENTER)
                            .owner(ScreenManager.getInstance())
                            .showError();
                    break;
                default:
                    IO.log(IO.class.getName(), IO.TAG_ERROR, "unknown message type '" + type + "'");
                    //Notifications.create().title(title).text(msg).showWarning();
                    break;
            }
        });
    }

    public static String showConfirm(String title, String message, String... options)
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.UTILITY);
        alert.setTitle("Choose an option");
        alert.setHeaderText(title);
        alert.setContentText(message);

        //To make enter key press the actual focused button, not the first one. Just like pressing "space".
        alert.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event ->
        {
            if (event.getCode().equals(KeyCode.ENTER))
            {
                event.consume();
                try
                {
                    Robot r = new Robot();
                    r.keyPress(java.awt.event.KeyEvent.VK_SPACE);
                    r.keyRelease(java.awt.event.KeyEvent.VK_SPACE);
                } catch (Exception e)
                {
                    IO.log(IO.class.getName(), IO.TAG_ERROR, e.getMessage());
                }
            }
        });

        if (options == null || options.length == 0)
        {
            options = new String[]{OK, CANCEL};
        }

        ArrayList<ButtonType> buttons = new ArrayList<>();
        for (String option : options)
        {
            buttons.add(new ButtonType(option));
        }

        alert.getButtonTypes().setAll(buttons);

        Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent())
        {
            return CANCEL;
        } else
        {
            return result.get().getText();
        }
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
