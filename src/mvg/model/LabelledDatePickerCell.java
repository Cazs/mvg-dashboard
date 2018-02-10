package mvg.model;

import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Created by ghost on 2018/01/17.
 */
public class LabelledDatePickerCell extends TableCell<MVGObject, Long>
{
    private final SimpleDateFormat formatter;
    private final DatePicker datePicker;
    private boolean editable=true;
    private final Label label = new Label("double click to set");
    private String property;

    public LabelledDatePickerCell(String property, boolean editable)
    {
        this.property = property;
        this.editable=editable;

        formatter = new SimpleDateFormat("yyyy-MM-dd");
        datePicker = new DatePicker();
        //datePicker.setEditable(editable);
        datePicker.setDisable(!editable);

        datePicker.valueProperty().addListener((observable, oldVal, newVal)->
        {
            //System.out.println("\noldVal: " + oldVal + ", newVal: " + newVal + ", isShowing? " + datePicker.isShowing() + ", isFocused?" + datePicker.isFocused() + "\n");
            updateItem(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), isEmpty());
            if(datePicker.isFocused() || datePicker.isShowing())
                commitEdit(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond());
        });


        datePicker.getEditor().focusedProperty().addListener((observable, oldValue, newValue) ->
        {
            if(!newValue)//if lost focus
            {
                if(datePicker.getValue()!=null)//if datepicker value is not null
                {
                    long date_epoch = datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();

                    if (date_epoch > 0)
                        setGraphic(datePicker);
                    else setGraphic(label);
                    getTableView().refresh();
                } else setGraphic(label);
            }
        });
    }

    @Override
    public void commitEdit(Long newValue)
    {
        super.commitEdit(newValue);
        MVGObject bo = (MVGObject) getTableRow().getItem();
        if(bo!=null)
        {
            bo.parse(property, newValue);
            try
            {
                RemoteComms.updateObjectOnServer(bo);
            } catch (IOException e)
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }
        }else IO.log(getClass().getName(), IO.TAG_WARN, "TableRow MVGObject is null.");
    }

    @Override
    protected void updateItem(Long date, boolean empty)
    {
        super.updateItem(date, empty);
        if(date==null || getTableRow().getItem()==null)//don't render anything if invalid date
        {
            setGraphic(null);
            return;
        }
        if(date>0 && getTableRow().getItem()!=null)
        {
            //render date
            //some models use epoch seconds and some use epoch millis, convert the date accordingly
            long real_date=0;
            if(date.toString().length()<=10)//if less than 10 chars, then
                real_date=date*1000;//is using epoch seconds
            else real_date = date;//is using epoch millis

            label.setText(String.valueOf(LocalDate.parse(formatter.format(new Date(real_date)))));//date is store in epoch seconds, convert to epoch millis
            //datePicker.setValue(LocalDate.parse(formatter.format(new Date(date))));
            //setText(formatter.format(new Date(date)));
            setGraphic(label);
        } else if(date==0 || empty)//render label prompting user to pick a date
            setGraphic(label);
        //getTableView().refresh();
    }

    @Override
    public void startEdit()
    {
        super.startEdit();
        if (editable)
        {
            setGraphic(datePicker);
            datePicker.requestFocus();
            //datePicker.setValue(getItem().atYear(LocalDate.now().getYear()));
        }
    }
}
