package mvg.model;

import javafx.scene.control.TableCell;
import mvg.auxilary.IO;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;

/**
 * Created by ghost on 2017/01/09.
 */

public class TextFieldTableCell extends TableCell<MVGObject, String>
{
    private TextField txt;
    private Label lbl;
    private String property, label_property;
    public static final String TAG = "TextFieldTableCell";
    //private MVGObject bo_selected;

    public TextFieldTableCell(String property, String label_property, Callback callback)
    {
        super();
        this.property = property;
        this.label_property = label_property;

        lbl = new Label();
        txt = new TextField();
        HBox.setHgrow(txt, Priority.ALWAYS);

        txt.setOnKeyPressed(event ->
        {
            if(event.getCode()== KeyCode.ENTER)
            {
                //IO.log(getClass().getName(),IO.TAG_INFO,"updated row:" + getTableRow().getItem());

                MVGObject obj = (MVGObject)getTableRow().getItem();
                obj.parse(property,txt.getText());

                if(callback!=null)
                    callback.call(obj);

                getTableRow().setItem(obj);
                //getTableView().refresh();
            }
            /*if(event.getCode() == KeyCode.ESCAPE)
            {
                setGraphic(lbl);
                getTableView().refresh();
            }*/
        });
        /*txt.focusedProperty().addListener((observable, oldValue, newValue) ->
        {
            if(oldValue && !newValue)//if was focused but not anymore then render label
            {
                setGraphic(lbl);
                getTableView().refresh();
            }
        });*/
    }

    @Override
    public void commitEdit(String selected_id)
    {
        super.commitEdit(selected_id);

        if(selected_id!=null)
        {
            if (getTableRow().getItem() instanceof MVGObject)
            {
                MVGObject bo = (MVGObject) getTableRow().getItem();
                bo.parse(property, selected_id);
            } else IO.log(TAG, IO.TAG_ERROR, String.format("unknown row object: " + getTableRow().getItem()));
        }else IO.log(TAG, IO.TAG_ERROR, "selected id is null.");
    }

    @Override
    protected void updateItem(String selected_id, boolean empty)
    {
        super.updateItem(selected_id, empty);

        if (getTableRow().getItem() instanceof MVGObject)
        {
            MVGObject bo = (MVGObject) getTableRow().getItem();
            Object val = bo.get(property);
            if(val!=null)
                txt.setText(val.toString());
            setGraphic(txt);
        } else IO.log(TAG, IO.TAG_ERROR, String.format("unknown row object: " + getTableRow().getItem()));
        //getTableView().refresh();
    }

    @Override
    public void startEdit()
    {
        super.startEdit();
        //setEditable(true);
        //setGraphic(txt);
        //txt.requestFocus();
    }
}
