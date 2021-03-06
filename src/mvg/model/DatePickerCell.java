package mvg.model;

import mvg.auxilary.IO;
import mvg.auxilary.RemoteComms;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Created by ghost on 2017/01/07.
 */
public class DatePickerCell extends TableCell<MVGObject, Long>
{
    private final SimpleDateFormat formatter;
    private final DatePicker datePicker;
    private String property, api_method;

    public DatePickerCell(String property, boolean editable)
    {
        this.property = property;
        this.api_method = "";

        formatter = new SimpleDateFormat("yyyy-MM-dd");
        datePicker = new DatePicker();
        //datePicker.setEditable(editable);
        datePicker.setDisable(!editable);

        datePicker.setOnAction(event ->
        {
            if(editable)
            {
                if (!isEmpty())
                {
                    commitEdit(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond());
                    updateItem(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), isEmpty());
                }
            }
        });
    }

    public DatePickerCell(String property, String api_method)
    {
        this.property = property;
        this.api_method = api_method;

        formatter = new SimpleDateFormat("yyyy-MM-dd");
        datePicker = new DatePicker();

        datePicker.setOnAction(event ->
        {
            if(!isEmpty())
            {
                commitEdit(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond());
                updateItem(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), isEmpty());
            }
        });
        /*datePicker.addEventFilter( EventType.ROOT, event ->
        {
            //System.out.println("DatepickerCell event!");
            /*if (event.getCode() == KeyEvent.VK_ENTER || event.getKeyCode() == KeyEvent.VK_TAB)
            {
                datePicker.setValue(datePicker.getConverter().fromString(datePicker.getEditor().getText()));
                commitEdit(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toEpochSecond());
            }
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
            {
                cancelEdit();
            }
        });*/


        /*datePicker.setDayCellFactory(picker ->
        {
            DateCell cell = new DateCell();
            cell.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
            {
                datePicker.setValue(cell.getItem());
                if (event.getClickCount() == 2)
                {
                    datePicker.hide();
                    //commitEdit(MonthDay.from(cell.getItem()));
                }
                event.consume();
            });
            /*cell.addEventFilter(KeyEvent.KEY_PRESSED, event ->
            {
                System.out.println(datePicker.getValue());
                /*if (event.get == KeyCode.ENTER) {
                    commitEdit(MonthDay.from(datePicker.getValue()));
                }*
            });*
            return cell ;
        });*/

        /*contentDisplayProperty().bind(Bindings.when(editingProperty())
                .then(ContentDisplay.CENTER)
                .otherwise(ContentDisplay.TEXT_ONLY));*/
    }

    @Override
    public void commitEdit(Long newValue)
    {
        super.commitEdit(newValue);
        MVGObject obj = (MVGObject) getTableRow().getItem();
        if(obj!=null)
        {
            obj.parse(property, newValue);
            RemoteComms.updateObjectOnServer(obj, api_method, property);
        }else IO.log(getClass().getName(), IO.TAG_WARN, "TableRow BusinessObject is null.");
    }

    @Override
    protected void updateItem(Long date, boolean empty)
    {
        super.updateItem(date, empty);
        if (empty || date==null)
        {
            setText(null);
            setGraphic(null);
        } else
        {
            datePicker.setValue(LocalDate.parse(formatter.format(new Date(date*1000))));
            //setText(formatter.format(new Date(date*1000)));
            setGraphic(datePicker);
        }
    }

    @Override
    public void startEdit()
    {
        super.startEdit();
        if (!isEmpty())
        {
            //datePicker.setValue(getItem().atYear(LocalDate.now().getYear()));
        }
    }
}
