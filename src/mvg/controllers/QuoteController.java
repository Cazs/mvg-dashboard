/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mvg.controllers;

import mvg.auxilary.*;
import mvg.managers.*;
import mvg.model.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.*;

/**
 * views Controller class
 *
 * @author ghost
 */
public abstract class QuoteController extends ScreenController implements Initializable
{
    protected boolean itemsModified;

    @FXML
    protected TableView<QuoteItem> tblQuoteItems;
    //Quote items table columns
    @FXML
    protected TableColumn colMarkup,colQuantity, colItemNumber, colEquipmentName, colDescription, colUnit, colValue, colRate, colTotal, colAction;
    @FXML
    protected ComboBox<Client> cbxClients;
    @FXML
    protected ComboBox<User> cbxContactPerson;
    @FXML
    protected TextField txtCell,txtTel,txtTotal,txtQuoteId,txtFax,txtEmail,txtDateGenerated,txtStatus,txtRevision,txtExtra;
    @FXML
    protected ToggleButton toggleVatExempt;
    @FXML
    protected ComboBox<String> cbxAccount;
    @FXML
    protected Label lblVat;
    @FXML
    protected Button btnApprove;
    @FXML
    protected TextArea txtRequest;
    protected HashMap<String, TableColumn> colsMap = new HashMap<>();
    private ObservableList<TableColumn<QuoteItem, ?>> default_cols;

    @Override
    public void refreshView()
    {
        if(UserManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getName(), "no users were found in the database.", IO.TAG_WARN);
            //return;
        }
        if( ClientManager.getInstance().getDataset()==null)
        {
            IO.logAndAlert(getClass().getName(), "no clients were found in the database.", IO.TAG_WARN);
            //return;
        }

        User[] users=null;
        if(UserManager.getInstance().getDataset()!=null)
        {
            users = new User[UserManager.getInstance().getDataset().values().toArray().length];
            UserManager.getInstance().getDataset().values().toArray(users);
        }

        //setup Quote default accounts
        cbxAccount.setItems(FXCollections.observableArrayList(new String[]{"Cash"}));
        cbxClients.valueProperty().addListener((observable, oldValue, newValue) ->
        {
            if(newValue!=null)
            {
                IO.log(getClass().getName(), IO.TAG_INFO, "selected client id: " + newValue.get_id());
                cbxAccount.setItems(FXCollections.observableArrayList(new String[]{"Cash", newValue.getAccount_name()}));
                txtFax.setText(newValue.getFax());
                itemsModified=true;
            }
        });

        refreshTotal();
        toggleVatExempt.selectedProperty().addListener((observable, oldValue, newValue) ->
        {
            if(newValue)
                toggleVatExempt.setText("VAT exempt");
            else toggleVatExempt.setText(QuoteManager.VAT+ "%");
            refreshTotal();
        });

        tblQuoteItems.getItems().clear();

        //Setup Quote Items table
        colMarkup.setCellValueFactory(new PropertyValueFactory<>("markup"));
        colMarkup.setCellFactory(param -> new TableCell()
        {
            final TextField txt = new TextField("0.0");

            @Override
            protected void updateItem(Object item, boolean empty)
            {
                super.updateItem(item, empty);
                if (getIndex() >= 0 && getIndex() < tblQuoteItems.getItems().size())
                {
                    QuoteItem quoteItem = tblQuoteItems.getItems().get(getIndex());
                    //update QuoteItem object on TextField commit
                    txt.setOnKeyPressed(event ->
                    {
                        if(event.getCode()== KeyCode.ENTER)
                        {
                            QuoteItem quote_item = (QuoteItem) getTableView().getItems().get(getIndex());
                            try
                            {
                                quote_item.setMarkup(Double.valueOf(txt.getText()));
                                txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(QuoteManager.getInstance().computeQuoteTotal(getTableView().getItems())));
                                tblQuoteItems.refresh();
                                //RemoteComms.updateBusinessObjectOnServer(quote_item, "/api/quote/resource", "markup");
                                //txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(QuoteManager.computeQuoteTotal(tblQuoteItems.getItems())));
                                //tblQuoteItems.refresh();
                            }catch (NumberFormatException e)
                            {
                                IO.logAndAlert("Error","Please enter a valid markup percentage.", IO.TAG_ERROR);
                                return;
                            }
                            IO.log(getClass().getName(), IO.TAG_INFO,"Successfully updated [markup percentage] property for quote item #" + quote_item.getItem_number());
                        }
                    });

                    if (!empty)
                    {
                        txt.setText(quoteItem.getMarkup());
                        setGraphic(txt);
                    } else setGraphic(null);
                    getTableView().refresh();
                }
            }
        });

        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colQuantity.setCellFactory(param -> new TableCell()
        {
            final TextField txt = new TextField("0.0");

            @Override
            protected void updateItem(Object item, boolean empty)
            {
                super.updateItem(item, empty);
                if (getIndex() >= 0 && getIndex() < tblQuoteItems.getItems().size())
                {
                    QuoteItem quoteItem = tblQuoteItems.getItems().get(getIndex());
                    //update QuoteItem object on TextField commit
                    txt.setOnKeyPressed(event ->
                    {
                        if(event.getCode()== KeyCode.ENTER)
                        {
                            QuoteItem quote_item = (QuoteItem) getTableView().getItems().get(getIndex());
                            try
                            {
                                quote_item.setQuantity(Integer.valueOf(txt.getText()));
                                //txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(QuoteManager.getInstance().computeQuoteTotal(getTableView().getItems())));
                                refreshTotal();
                                tblQuoteItems.refresh();
                            }catch (NumberFormatException e)
                            {
                                IO.logAndAlert("Error","Please enter a valid quantity.", IO.TAG_ERROR);
                                return;
                            }
                            IO.log(getClass().getName(), IO.TAG_INFO,"Successfully updated [quantity] property for quote item #" + quote_item.getItem_number());
                        }
                    });

                    if (!empty)
                    {
                        txt.setText(quoteItem.getQuantity());
                        setGraphic(txt);
                    } else setGraphic(null);
                    getTableView().refresh();
                }
            }
        });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));

        cbxClients.setCellFactory(new Callback<ListView<Client>, ListCell<Client>>()
        {
            @Override public ListCell<Client> call(ListView<Client> p)
            {
                return new ListCell<Client>()
                {
                    @Override
                    protected void updateItem(Client item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (item == null || empty)
                        {
                            setGraphic(null);
                        } else{
                            setText(item.getClient_name());
                        }
                    }
                };
            }
        });
        cbxClients.setButtonCell(null);
        cbxClients.setItems(FXCollections.observableArrayList(ClientManager.getInstance().getDataset().values()));

        cbxContactPerson.setCellFactory(new Callback<ListView<User>, ListCell<User>>()
        {
            @Override public ListCell<User> call(ListView<User> p)
            {
                return new ListCell<User>()
                {
                    @Override
                    protected void updateItem(User item, boolean empty)
                    {
                        super.updateItem(item, empty);

                        if (item == null || empty)
                        {
                            setGraphic(null);
                        } else{
                            setText(item.getFirstname() + " " + item.getLastname());
                        }
                    }
                };
            }
        });
        cbxContactPerson.setButtonCell(null);
        if(users!=null)
            cbxContactPerson.setItems(FXCollections.observableArrayList(users));
        cbxContactPerson.setOnAction(event ->
        {
            User user = cbxContactPerson.getValue();
            if(user!=null)
            {
                txtCell.setText(user.getCell());
                txtTel.setText(user.getTel());
                txtEmail.setText(user.getEmail());
                itemsModified=true;
            }//else IO.logAndAlert("Invalid User", "Selected contact person is invalid", IO.TAG_ERROR);
        });

        if(default_cols==null)
            default_cols=tblQuoteItems.getColumns();

        //set status
        String status;
        if((Quote)QuoteManager.getInstance().getSelected()!=null)
        {
            switch (((Quote)((Quote)QuoteManager.getInstance().getSelected())).getStatus())
            {
                case Quote.STATUS_PENDING:
                    status = "PENDING";
                    break;
                case Quote.STATUS_APPROVED:
                    status = "APPROVED";
                    break;
                case Quote.STATUS_ARCHIVED:
                    status = "ARCHIVED";
                    break;
                default:
                    status = "UNKNOWN";
                    IO.logAndAlert("Error", "Unknown Quote status: " + ((Quote)((Quote)QuoteManager.getInstance().getSelected()))
                            .getStatus(), IO.TAG_ERROR);
                    break;
            }
            if(txtStatus!=null)
                txtStatus.setText(status);
        }
    }

    protected void refreshTotal()
    {
        double vat = QuoteManager.VAT;
        if(toggleVatExempt.isSelected())//if is VAT exempt
            vat =0.0;//is VAT exempt
        lblVat.setText("VAT ["+new DecimalFormat("##.##").format(vat)+"%]");
        if(tblQuoteItems.getItems()!=null)
        {
            double total = QuoteManager.computeQuoteTotal(tblQuoteItems.getItems());
            txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " +
                    new DecimalFormat("##.##").format((total + (total*(vat)))));
        }
        if(tblQuoteItems.getItems()!=null)
        {
            double total = QuoteManager.computeQuoteTotal(tblQuoteItems.getItems());
            txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " +
                    new DecimalFormat("##.##").format((total + (total*(vat/100)))));
        }
    }

    @Override
    public void refreshModel()
    {
        UserManager.getInstance().initialize();
        ClientManager.getInstance().initialize();
        ResourceManager.getInstance().initialize();
        QuoteManager.getInstance().initialize();
    }

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
        colAction.setCellFactory(new ButtonTableCellFactory<>());
        colAction.setCellValueFactory(new PropertyValueFactory<>(""));

        Callback<TableColumn<QuoteItem, String>, TableCell<QuoteItem, String>> cellFactory
                =
                new Callback<TableColumn<QuoteItem, String>, TableCell<QuoteItem, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<QuoteItem, String> param)
                    {
                        final TableCell<QuoteItem, String> cell = new TableCell<QuoteItem, String>()
                        {
                            final Button btnAdd = new Button("Add materials");
                            final Button btnRemove = new Button("Remove item");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);
                                btnAdd.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnAdd.getStyleClass().add("btnAdd");
                                btnAdd.setMinWidth(100);
                                btnAdd.setMinHeight(35);
                                HBox.setHgrow(btnAdd, Priority.ALWAYS);

                                btnRemove.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnRemove.getStyleClass().add("btnBack");
                                btnRemove.setMinWidth(100);
                                btnRemove.setMinHeight(35);
                                HBox.setHgrow(btnRemove, Priority.ALWAYS);

                                if (empty)
                                {
                                    setGraphic(null);
                                    setText(null);
                                } else
                                {
                                    HBox hBox = new HBox(btnAdd, btnRemove);

                                    btnAdd.setOnAction(event ->
                                    {
                                        QuoteItem quoteItem = getTableView().getItems().get(getIndex());
                                        addQuoteItemAdditionalMaterial(quoteItem);
                                    });

                                    btnRemove.setOnAction(event ->
                                    {
                                        //TODO: deal with server side
                                        QuoteItem quoteItem = getTableView().getItems().get(getIndex());
                                        //remove QuoteItem's additional costs TableColumns
                                        if (quoteItem.getAdditional_costs() != null)
                                        {
                                            for (String str_cost : quoteItem.getAdditional_costs().split(";"))
                                            {
                                                String[] arr = str_cost.split("=");
                                                if (arr != null)
                                                {
                                                    if (arr.length > 1)
                                                    {
                                                        //if column exists in map, remove it
                                                        if(colsMap.get(arr[0].toLowerCase())!=null)
                                                        {
                                                            tblQuoteItems.getColumns().remove(colsMap.get(arr[0].toLowerCase()));
                                                            IO.log(getClass().getName(), IO.TAG_INFO, "removed QuoteItem["+quoteItem.get_id()+"] additional cost column: "+arr[0]);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        getTableView().getItems().remove(quoteItem);
                                        getTableView().refresh();
                                        IO.log(getClass().getName(), IO.TAG_INFO, "removed QuoteItem["+quoteItem.get_id()+"]");
                                        txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(((Quote)(Quote)QuoteManager.getInstance().getSelected()).getTotal()));
                                    });

                                    hBox.setFillHeight(true);
                                    HBox.setHgrow(hBox, Priority.ALWAYS);
                                    hBox.setSpacing(5);
                                    setGraphic(hBox);
                                    setText(null);
                                }
                            }
                        };
                    return cell;
                    }
                };

        colAction.setCellFactory(cellFactory);

        Callback<TableColumn<User, String>, TableCell<User, String>> actionColCellFactory
                =
                new Callback<TableColumn<User, String>, TableCell<User, String>>()
                {
                    @Override
                    public TableCell call(final TableColumn<User, String> param)
                    {
                        final TableCell<User, String> cell = new TableCell<User, String>()
                        {
                            final Button btnRemove = new Button("Remove");

                            @Override
                            public void updateItem(String item, boolean empty)
                            {
                                super.updateItem(item, empty);

                                btnRemove.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
                                btnRemove.getStyleClass().add("btnBack");
                                btnRemove.setMinWidth(100);
                                btnRemove.setMinHeight(35);
                                HBox.setHgrow(btnRemove, Priority.ALWAYS);

                                if (empty)
                                {
                                    setGraphic(null);
                                    setText(null);
                                } else
                                {
                                    btnRemove.setOnAction(event ->
                                    {
                                        User user = getTableView().getItems().get(getIndex());
                                        getTableView().getItems().remove(user);
                                        getTableView().refresh();
                                        //TODO: remove from server
                                        System.out.println("Successfully removed sale representative: " + user.getName());
                                    });
                                    setGraphic(btnRemove);
                                    setText(null);
                                }
                            }
                        };
                        return cell;
                    }
                };
    }

    public Callback getAdditionalCostCallback(TableColumn col)
    {
        return new Callback<TableColumn<QuoteItem, String>, TableCell<QuoteItem, String>>()
        {
            @Override
            public TableCell<QuoteItem, String> call(TableColumn<QuoteItem, String> param)
            {
                return new TableCell<QuoteItem, String>()
                {
                    final TextField txtCost = new TextField("0.0");
                    final TextField txtMarkup = new TextField("0.0");

                    @Override
                    protected void updateItem(String item, boolean empty)
                    {
                        super.updateItem(item, empty);
                        if (getIndex() >= 0 && getIndex() < tblQuoteItems.getItems().size())
                        {
                            QuoteItem quoteItem = tblQuoteItems.getItems().get(getIndex());
                            //update QuoteItem object on TextField commit
                            Callback callback = new Callback()
                            {
                                @Override
                                public Object call(Object param)
                                {
                                    if (quoteItem != null)
                                    {
                                        String new_cost = col.getText().toLowerCase() + "=" + txtCost.getText() + "*" + txtMarkup.getText();
                                        String old_add_costs = "";
                                        if (quoteItem.getAdditional_costs() != null)
                                            old_add_costs = quoteItem.getAdditional_costs().toLowerCase();
                                        String new_add_costs = "";

                                        int old_var_index = old_add_costs.indexOf(col.getText().toLowerCase());
                                        if (old_var_index < 0)
                                        {
                                            //additional cost pair not found
                                            if (old_add_costs.isEmpty())
                                                //** key-value pair is the first and only pair - add it w/o semi-colon
                                                new_add_costs += new_cost;
                                            else
                                            /** Additional cost pair DNE but other additional costs exist
                                             * - append pair then add the rest of the pairs
                                             */
                                                new_add_costs += new_cost + ";" + old_add_costs;//.substring(old_add_costs.indexOf(';')-1)
                                        } else if (old_var_index == 0)
                                        {
                                            /** Additional cost key-value pair exists and is first pair. **/
                                            new_add_costs += new_cost;
                                            if (old_add_costs.indexOf(';') > 0)//if there are other pairs append them
                                                //copy from first occurrence of semi-colon - cutting the old record out.
                                                new_add_costs += ";" + old_add_costs.substring(old_add_costs.indexOf(';') + 1);
                                        }
                                        else
                                        {
                                            /** Additional cost key-value pair is not first - append to additional costs.**/
                                            //copy additional costs before current cost
                                            new_add_costs = old_add_costs.substring(0, old_var_index - 1);
                                            //append current cost
                                            new_add_costs += ";" + new_cost;
                                            //append additional costs after current cost
                                            int i = old_add_costs.substring(old_var_index).indexOf(';');
                                            new_add_costs += ";" + old_add_costs.substring(i + 1);
                                        }
                                        IO.log(getClass().getName(), IO.TAG_INFO, "committed additional costs for quote item [#" + quoteItem.getItem_number() + "]:: " + new_add_costs);
                                        quoteItem.setAdditional_costs(new_add_costs);
                                    }
                                    return null;
                                }
                            };

                            txtCost.setOnKeyPressed(event ->
                            {
                                if (event.getCode() == KeyCode.ENTER)
                                {
                                    callback.call(null);
                                    tblQuoteItems.refresh();
                                    txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(QuoteManager.getInstance().computeQuoteTotal(tblQuoteItems.getItems())));
                                }
                            });
                            txtMarkup.setOnKeyPressed(event ->
                            {
                                if (event.getCode() == KeyCode.ENTER)
                                {
                                    callback.call(null);
                                    tblQuoteItems.refresh();
                                    txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(QuoteManager.getInstance().computeQuoteTotal(tblQuoteItems.getItems())));
                                }
                            });

                            //render the cell
                            if (!empty)// && item!=null
                            {
                                if (quoteItem.getAdditional_costs() == null)
                                {
                                    txtCost.setText("0.0");
                                    txtMarkup.setText("0.0");
                                } else if (quoteItem.getAdditional_costs().length() <= 0)
                                {
                                    txtCost.setText("0.0");
                                    txtMarkup.setText("0.0");
                                } else if (quoteItem.getAdditional_costs().length() > 0)
                                {
                                    if(quoteItem.getAdditional_costs() != null)
                                    {
                                        for (String str_cost : quoteItem.getAdditional_costs().split(";"))
                                        {
                                            String[] arr = str_cost.split("=");
                                            if (arr != null)
                                            {
                                                if (arr.length > 1)
                                                {
                                                    if (arr[0].toLowerCase().equals(col.getText().toLowerCase()))
                                                    {
                                                        if(arr[1].contains("*"))
                                                        {
                                                            txtCost.setText(arr[1].split("\\*")[0]);
                                                            txtMarkup.setText(arr[1].split("\\*")[1]);
                                                        }else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid quote item additional cost ["+str_cost+"].");
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                txtCost.setPrefWidth(50);
                                txtMarkup.setPrefWidth(50);
                                VBox vBoxCost = new VBox(new Label("Cost: "), txtCost);
                                vBoxCost.setAlignment(Pos.CENTER);
                                VBox.setMargin(txtCost, new Insets(0,10,0,10));
                                HBox.setHgrow(txtCost, Priority.ALWAYS);
                                HBox.setHgrow(vBoxCost, Priority.ALWAYS);

                                VBox vBoxMarkup = new VBox(new Label("Markup: "), txtMarkup);
                                vBoxMarkup.setAlignment(Pos.CENTER);
                                VBox.setMargin(txtMarkup, new Insets(0,10,0,10));
                                HBox.setHgrow(txtMarkup, Priority.ALWAYS);
                                HBox.setHgrow(vBoxMarkup, Priority.ALWAYS);

                                HBox hBox = new HBox(vBoxCost, vBoxMarkup);
                                hBox.setAlignment(Pos.CENTER);
                                HBox.setHgrow(hBox, Priority.ALWAYS);
                                setGraphic(hBox);
                            } else setGraphic(null);
                            getTableView().refresh();
                        } //else IO.log("Quote Materials Table", IO.TAG_ERROR, "index out of bounds [" + getIndex() + "]");
                    }
                };
            }
        };
    }

    public void addQuoteItemAdditionalMaterial(QuoteItem quoteItem)
    {
        Stage stage = new Stage();
        stage.setAlwaysOnTop(true);
        stage.setResizable(false);

        TextField txtName = new TextField();
        txtName.setMinWidth(150);
        Label lblName = new Label("Material name: ");
        lblName.setMinWidth(150);

        TextField txtCost = new TextField();
        txtCost.setMinWidth(150);
        Label lblCost = new Label("Material value: ");
        lblCost.setMinWidth(150);

        TextField txtMarkup = new TextField();
        txtMarkup.setMinWidth(150);
        Label lblMarkup = new Label("Markup [%]: ");
        lblMarkup.setMinWidth(150);

        Button btnAdd = new Button("Add");
        btnAdd.getStyleClass().add("btnAdd");
        btnAdd.setMinWidth(140);
        btnAdd.setOnAction(event ->
        {
            //validate cost name
            if(txtName.getText()!=null)
            {
                if (txtName.getText().isEmpty())
                {
                    IO.logAndAlert("New Additional Cost Error", "Please enter a valid cost name.", IO.TAG_ERROR);
                    return;
                }
            }else{
                IO.logAndAlert("New Additional Cost Error", "Please enter a valid cost name.", IO.TAG_ERROR);
                return;
            }
            //validate cost value
            if(txtCost.getText()!=null)
            {
                if (txtCost.getText().isEmpty())
                {
                    IO.logAndAlert("New Additional Cost Error", "Please enter a valid cost value.", IO.TAG_ERROR);
                    return;
                }
            }else{
                IO.logAndAlert("New Additional Cost Error", "Please enter a valid cost value.", IO.TAG_ERROR);
                return;
            }

            String new_cost = txtName.getText()+"="+txtCost.getText()+"*"+txtMarkup.getText();
            if(quoteItem.getAdditional_costs()==null)
                quoteItem.setAdditional_costs(new_cost);
            else if(quoteItem.getAdditional_costs().isEmpty())
                quoteItem.setAdditional_costs(new_cost);
            else
            {
                //if additional cost exists already, update its value
                if(quoteItem.getAdditional_costs().toLowerCase().contains(txtName.getText().toLowerCase()))
                {
                    String old_add_costs = quoteItem.getAdditional_costs().toLowerCase();
                    String new_add_costs="";
                    int old_var_index = old_add_costs.indexOf(txtName.getText().toLowerCase());
                    if(old_var_index==0)
                    {
                        //key-value pair is first add it w/o semi-colon
                        new_add_costs += new_cost;
                    }else
                    {
                        new_add_costs = old_add_costs.substring(0, old_var_index);
                        new_add_costs += ";" + new_cost;
                    }
                    quoteItem.setAdditional_costs(new_add_costs);
                } else quoteItem.setAdditional_costs(quoteItem.getAdditional_costs()+";"+new_cost);
            }

            //RemoteComms.updateBusinessObjectOnServer(quoteItem, "/api/quote/resource", txtName.getText());

            TableColumn<QuoteItem, String> col = new TableColumn(txtName.getText());
            col.setPrefWidth(80);
            col.setCellFactory(getAdditionalCostCallback(col));

            colsMap.put(txtName.getText().toLowerCase(), col);
            /*boolean found=false;
            for(TableColumn c: tblQuoteItems.getColumns())
                if(col.getText().toLowerCase().equals(c.getText().toLowerCase()))
                {
                    found=true;
                    break;
                }
            if(!found)
                tblQuoteItems.getColumns().add(col);
            tblQuoteItems.refresh();*/
            tblQuoteItems.getColumns().add(col);
            tblQuoteItems.refresh();

            txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " + String.valueOf(QuoteManager.getInstance().computeQuoteTotal(tblQuoteItems.getItems())));
        });

        HBox row1 = new HBox(lblName, txtName);
        HBox row2 = new HBox(lblCost, txtCost);
        HBox row3 = new HBox(lblMarkup, txtMarkup);
        row1.setSpacing(20);
        row2.setSpacing(20);
        row3.setSpacing(20);
        //HBox row3 = new HBox(new Label("Markup"), txtMarkup);

        stage.setTitle("Extra Costs For Quote Item #"+quoteItem.getItem_numberValue());
        stage.setScene(new Scene(new VBox(row1, row2, row3, btnAdd)));
        stage.show();
    }

    public void addAdditionalCostColToMap(String id, TableColumn col)
    {
        colsMap.put(id, col);
    }

    @FXML
    public void newQuoteItem()
    {
        if(ResourceManager.getInstance()!=null)
        {
            if(ResourceManager.getInstance().getDataset()!=null)
            {
                if(ResourceManager.getInstance().getDataset().size()>0)
                {
                    ComboBox<Resource> resourceComboBox = new ComboBox<>();
                    resourceComboBox.setMinWidth(240);
                    resourceComboBox.setItems(FXCollections.observableArrayList(ResourceManager.getInstance().getAll_resources().values()));
                    HBox.setHgrow(resourceComboBox, Priority.ALWAYS);

                    Button btnAdd = new Button("Add");
                    btnAdd.setMinWidth(80);
                    btnAdd.setMinHeight(40);
                    btnAdd.setDefaultButton(true);
                    btnAdd.getStyleClass().add("btnDefault");
                    btnAdd.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());

                    Button btnNewMaterial = new Button("New Material");
                    btnNewMaterial.setMinWidth(130);
                    btnNewMaterial.setMinHeight(40);
                    btnNewMaterial.setDefaultButton(true);
                    btnNewMaterial.getStyleClass().add("btnAdd");
                    btnNewMaterial.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());

                    Button btnCancel = new Button("Close");
                    btnCancel.setMinWidth(80);
                    btnCancel.setMinHeight(40);
                    btnCancel.getStyleClass().add("btnBack");
                    btnCancel.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());

                    HBox hBox = new HBox(new Label("Material: "), resourceComboBox);
                    HBox.setHgrow(hBox, Priority.ALWAYS);
                    HBox.setHgrow(resourceComboBox, Priority.ALWAYS);
                    hBox.setSpacing(20);
                    HBox.setMargin(hBox, new Insets(0, 0, 0, 20));

                    HBox hBoxButtons = new HBox(btnAdd, btnNewMaterial, btnCancel);
                    hBoxButtons.setHgrow(btnAdd, Priority.ALWAYS);
                    hBoxButtons.setHgrow(btnCancel, Priority.ALWAYS);
                    hBoxButtons.setSpacing(20);
                    HBox.setMargin(hBoxButtons, new Insets(0, 0, 0, 20));

                    VBox vBox = new VBox(hBox, hBoxButtons);
                    VBox.setVgrow(vBox, Priority.ALWAYS);
                    vBox.setSpacing(20);
                    HBox.setHgrow(vBox, Priority.ALWAYS);
                    vBox.setFillWidth(true);

                    Stage stage = new Stage();
                    stage.setMaxWidth(350);
                    stage.setTitle("Add Quote Materials");
                    stage.setScene(new Scene(vBox));
                    stage.setAlwaysOnTop(true);
                    stage.setResizable(false);
                    stage.show();

                    btnAdd.setOnAction(event ->
                    {
                        if(resourceComboBox.getValue()!=null)
                        {
                            QuoteItem quoteItem = new QuoteItem();

                            quoteItem.setItem_number(tblQuoteItems.getItems().size());
                            quoteItem.setQuantity(1);
                            quoteItem.setUnit_cost(resourceComboBox.getValue().getResource_value());
                            quoteItem.setMarkup(0);
                            quoteItem.setResource_id(resourceComboBox.getValue().get_id());
                            //quoteItem.setEquipment_description(resourceComboBox.getValue().getResource_description());
                            //quoteItem.setUnit(resourceComboBox.getValue().getUnit());
                            //quoteItem.setRate(resourceComboBox.getValue().getResource_value());
                            //quoteItem.setValue(resourceComboBox.getValue().getResource_value());
                            //quoteItem.setResource(resourceComboBox.getValue());
                            //quoteItem.setEquipment_name(resourceComboBox.getValue().getResource_name());

                            tblQuoteItems.getItems().add(quoteItem);
                            tblQuoteItems.refresh();

                            itemsModified = true;

                            //txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue() + " " +
                            //        String.valueOf(QuoteManager.computeQuoteTotal((Quote)QuoteManager.getInstance().getSelected())));
                            txtTotal.setText(Globals.CURRENCY_SYMBOL.getValue()+" "+String.valueOf(QuoteManager.computeQuoteTotal(tblQuoteItems.getItems())));

                        } else IO.logAndAlert("New Quote Resource", "Invalid resource selected.", IO.TAG_ERROR);
                    });

                    btnNewMaterial.setOnAction(event ->
                    {
                        //close material addition window
                        stage.close();
                        ResourceManager.getInstance().newResourceWindow(param ->
                        {
                            //refresh model & view when material has been created.
                            new Thread(() ->
                            {
                                refreshModel();
                                Platform.runLater(() ->
                                {
                                    refreshView();
                                    //show material addition window again after material has been created.
                                    newQuoteItem();
                                });
                            }).start();
                            return null;
                        });
                    });

                    btnCancel.setOnAction(event ->
                            stage.close());
                    return;
                }
            }
        }
        IO.logAndAlert("New Quote Resource", "No resources were found in the database, please add some resources first and try again.",IO.TAG_ERROR);
    }

    @FXML
    public void apply()
    {
        SessionManager smgr = SessionManager.getInstance();
        if(smgr.getActive()!=null)
        {
            if(!smgr.getActive().isExpired())
            {
                //Update Quote if already been added
                if(txtQuoteId.getText()!=null)
                {
                    if(!txtQuoteId.getText().isEmpty())
                    {
                        updateQuote();
                        return;
                    }
                }
                //else create new Quote
                createQuote();
            }else IO.showMessage("Session Expired", "Active session has expired.", IO.TAG_ERROR);
        }else IO.showMessage("Session Expired", "No active sessions.", IO.TAG_ERROR);
    }

    @FXML
    public void approveQuote()
    {
        Quote selected = (Quote)(Quote)QuoteManager.getInstance().getSelected();
        if(selected!=null)
        {
            if(selected.getStatus()!=Quote.STATUS_APPROVED)
            {
                selected.setStatus(Quote.STATUS_APPROVED);
                QuoteManager.getInstance().updateQuote(selected, tblQuoteItems.getItems());
                refreshModel();
                refreshView();
            } else IO.logAndAlert("Error", "Selected quote has already been approved.", IO.TAG_ERROR);
        } else IO.logAndAlert("Error", "Selected quote is invalid.", IO.TAG_ERROR);
    }

    public void createQuote()
    {
        cbxClients.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
        if(cbxClients.getValue()==null)
        {
            cbxClients.getStyleClass().remove("form-control-default");
            cbxClients.getStyleClass().add("control-input-error");
            return;
        }else{
            cbxClients.getStyleClass().remove("control-input-error");
            cbxClients.getStyleClass().add("form-control-default");
        }

        cbxContactPerson.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
        if(cbxContactPerson.getValue()==null)
        {
            cbxContactPerson.getStyleClass().remove("form-control-default");
            cbxContactPerson.getStyleClass().add("control-input-error");
            return;
        }else{
            cbxContactPerson.getStyleClass().remove("control-input-error");
            cbxContactPerson.getStyleClass().add("form-control-default");
        }

        if(!Validators.isValidNode(txtCell, txtCell.getText(), 1, ".+"))
        {
            txtCell.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }
        if(!Validators.isValidNode(txtTel, txtTel.getText(), 1, ".+"))
        {
            txtTel.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }
        if(!Validators.isValidNode(txtEmail, txtEmail.getText(), 1, ".+"))
        {
            txtEmail.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }

        cbxAccount.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
        if(cbxAccount.getValue()==null)
        {
            cbxAccount.getStyleClass().remove("form-control-default");
            cbxAccount.getStyleClass().add("control-input-error");
            return;
        }else{
            cbxAccount.getStyleClass().remove("control-input-error");
            cbxAccount.getStyleClass().add("form-control-default");
        }

        if(!Validators.isValidNode(txtRequest, txtRequest.getText(), 1, ".+"))
        {
            txtRequest.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }

        List<QuoteItem> quoteItems = tblQuoteItems.getItems();

        if(quoteItems==null)
        {
            IO.logAndAlert("Invalid Quote", "Quote items list is null.", IO.TAG_ERROR);
            return;
        }
        if(quoteItems.size()<=0)
        {
            IO.logAndAlert("Invalid Quote", "Quote has no materials", IO.TAG_ERROR);
            return;
        }
        //prepare quote attributes
        Quote quote = new Quote();
        quote.setClient_id(cbxClients.getValue().get_id());
        quote.setContact_person_id(cbxContactPerson.getValue().getUsr());
        quote.setRequest(txtRequest.getText());
        quote.setStatus(Quote.STATUS_PENDING);
        quote.setAccount_name(cbxAccount.getValue());
        quote.setCreator(SessionManager.getInstance().getActive().getUsername());
        quote.setRevision(1.0);

        if(toggleVatExempt.isSelected())
            quote.setVat(0);
        else quote.setVat(QuoteManager.VAT);

        if(txtExtra!=null)
            if(txtExtra.getText()!=null)
                quote.setOther(txtExtra.getText());

        try
        {
            QuoteManager.getInstance().createQuote(quote, tblQuoteItems.getItems(), new Callback()
            {
                @Override
                public Object call(Object quote_id)
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
                                    if(ScreenManager.getInstance().loadScreen(Screens.VIEW_QUOTE.getScreen(),mvg.MVG.class.getResource("views/"+Screens.VIEW_QUOTE.getScreen())))
                                    {
                                        //Platform.runLater(() ->
                                        ScreenManager.getInstance().setScreen(Screens.VIEW_QUOTE.getScreen());
                                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load view quote screen.");
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                    IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                }
                            }
                        }).start();
                        return null;
                    });
                    txtQuoteId.setText(quote_id.toString());
                    return null;
                }
            });
        } catch (IOException e)
        {
            IO.logAndAlert(getClass().getName(), e.getMessage(), IO.TAG_ERROR);
        }
    }

    @FXML
    public void updateQuote()
    {
        cbxClients.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
        if(cbxClients.getValue()==null)
        {
            cbxClients.getStyleClass().remove("form-control-default");
            cbxClients.getStyleClass().add("control-input-error");
            return;
        }else{
            cbxClients.getStyleClass().remove("control-input-error");
            cbxClients.getStyleClass().add("form-control-default");
        }

        cbxContactPerson.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
        if(cbxContactPerson.getValue()==null)
        {
            cbxContactPerson.getStyleClass().remove("form-control-default");
            cbxContactPerson.getStyleClass().add("control-input-error");
            return;
        } else {
            cbxContactPerson.getStyleClass().remove("control-input-error");
            cbxContactPerson.getStyleClass().add("form-control-default");
        }

        if(!Validators.isValidNode(txtCell, txtCell.getText(), 1, ".+"))
        {
            txtCell.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }
        if(!Validators.isValidNode(txtTel, txtTel.getText(), 1, ".+"))
        {
            txtTel.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }
        if(!Validators.isValidNode(txtEmail, txtEmail.getText(), 1, ".+"))
        {
            txtEmail.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }
        cbxAccount.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
        if(cbxAccount.getValue()==null)
        {
            cbxAccount.getStyleClass().remove("form-control-default");
            cbxAccount.getStyleClass().add("control-input-error");
            return;
        }else{
            cbxAccount.getStyleClass().remove("control-input-error");
            cbxAccount.getStyleClass().add("form-control-default");
        }

        if(!Validators.isValidNode(txtRequest, txtRequest.getText(), 1, ".+"))
        {
            txtRequest.getStylesheets().add(mvg.MVG.class.getResource("styles/home.css").toExternalForm());
            return;
        }

        Quote selected = (Quote)QuoteManager.getInstance().getSelected();
        if(selected!=null)
        {
            // create a new Quote with selected Quote as parent and +1 revision number
            Quote quote = new Quote();
            quote.setClient_id(cbxClients.getValue().get_id());
            quote.setContact_person_id(cbxContactPerson.getValue().getUsr());
            quote.setRequest(txtRequest.getText());
            quote.setAccount_name(cbxAccount.getValue());
            quote.setStatus(Quote.STATUS_PENDING);
            quote.setAccount_name(cbxAccount.getValue());
            quote.setCreator(SessionManager.getInstance().getActive().getUsername());
            quote.setRevision(1.0);//set default revision number
            quote.setEnquiry_id(selected.getEnquiry_id());
            //get selected Quote's siblings sorted by revision number
            Quote[] quote_revisions = selected.getSortedSiblings("revision");
            if(quote_revisions!=null)
                if(quote_revisions.length>0)
                    quote.setRevision(quote_revisions[quote_revisions.length-1].getRevision()+1);//get revision # of last Quote +1
            //set parent of new Quote to be the root Quote of the selected Quote, if is root/base then use selected Quote
            if(selected.getRoot()!=null)
                quote.setParent_id(selected.getRoot().get_id());
            else quote.setParent_id(selected.get_id());

            if(toggleVatExempt.isSelected())
                quote.setVat(0);
            else quote.setVat(QuoteManager.VAT);

            if(txtExtra!=null)
                if(txtExtra.getText()!=null)
                    quote.setOther(txtExtra.getText());

            try
            {
                //Create new Quote with new _id & +1 revision number, with parent Quote pointing to current selected Quote
                QuoteManager.getInstance().createQuote(quote, tblQuoteItems.getItems(), new Callback()
                {
                    @Override
                    public Object call(Object quote_id)
                    {
                        //on successful Quote creation refresh model & UI
                        ScreenManager.getInstance().showLoadingScreen(param ->
                        {
                            //refresh Quote Model's data-set
                            QuoteManager.getInstance().forceSynchronise();

                            //update selected Quote
                            if(quote_id!=null)
                            {
                                if (QuoteManager.getInstance().getDataset() != null)
                                {
                                    Quote new_selected = QuoteManager.getInstance().getDataset().get(quote_id);
                                    if(new_selected!=null)
                                    {
                                        IO.log(getClass().getName(), IO.TAG_INFO,
                                                "setting selected quote to: " + quote_id + " revision: "
                                                        +new_selected.getRevision());
                                        QuoteManager.getInstance().setSelected(new_selected);
                                    } else IO.log(getClass().getName(), IO.TAG_ERROR, "newly created Quote is invalid.");
                                } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not update Quote, no Quotes were found in the database.");
                            } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not update Quote, quote_id is not set.");

                            //refresh GUI
                            new Thread(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        if(ScreenManager.getInstance().loadScreen(Screens.VIEW_QUOTE.getScreen(),mvg.MVG.class.getResource("views/"+Screens.VIEW_QUOTE.getScreen())))
                                        {
                                            //Platform.runLater(() ->
                                            ScreenManager.getInstance().setScreen(Screens.VIEW_QUOTE.getScreen());
                                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load view quote screen.");
                                    } catch (IOException e)
                                    {
                                        e.printStackTrace();
                                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                    }
                                }
                            }).start();
                            return null;
                        });
                        txtQuoteId.setText(quote_id.toString());
                        return null;
                    }
                });
            } catch (IOException e)
            {
                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
            }
        } else IO.logAndAlert("Error", "Selected quote is invalid.", IO.TAG_ERROR);
    }

    @FXML
    public void createTrip()
    {
        SessionManager smgr = SessionManager.getInstance();
        if(smgr.getActive()!=null)
        {
            if(!smgr.getActive().isExpired())
            {
                Quote selected = (Quote)QuoteManager.getInstance().getSelected();
                if(selected!=null)
                {
                    if(selected.getStatus()==Quote.STATUS_APPROVED)
                    {
                        Trip trip = new Trip();
                        trip.setQuote_id(selected.get_id());
                        trip.setCreator(smgr.getActiveUser().getUsr());
                        trip.setClient_id(selected.getEnquiry().getCreatorUser().getOrganisation_id());
                        String new_trip_id = TripManager.getInstance().createNewTrip(trip, null);
                        if (new_trip_id != null)
                        {
                            IO.logAndAlert("Success", "Successfully created a new trip.", IO.TAG_INFO);

                            //refresh Trips data-set
                            TripManager.getInstance().forceSynchronise();

                            if (TripManager.getInstance().getDataset() != null)
                            {
                                TripManager.getInstance()
                                        .setSelected(TripManager.getInstance().getDataset().get(new_trip_id));

                                ScreenManager.getInstance().showLoadingScreen(param ->
                                {
                                    new Thread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            try
                                            {
                                                //TODO: load trips screen
                                                if (ScreenManager.getInstance()
                                                        .loadScreen(Screens.DASHBOARD.getScreen(), mvg.MVG.class.getResource("views/" + Screens.DASHBOARD
                                                                        .getScreen())))
                                                {
                                                    Platform.runLater(() -> ScreenManager.getInstance()
                                                            .setScreen(Screens.DASHBOARD.getScreen()));
                                                }
                                                else IO.log(getClass()
                                                        .getName(), IO.TAG_ERROR, "could not load dashboard screen.");
                                            } catch (IOException e)
                                            {
                                                IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                                            }
                                        }
                                    }).start();
                                    return null;
                                });
                            } else IO.logAndAlert("Error", "Could not find any trips in the database.", IO.TAG_INFO);
                        } else IO.logAndAlert("Error", "Could not successfully create a new trip.", IO.TAG_ERROR);
                    } else IO.logAndAlert("Error", "Quote has not been approved yet.", IO.TAG_ERROR);
                }else IO.logAndAlert("Error: Cannot Create Trip", "Cannot create trip because the selected quote is invalid.", IO.TAG_ERROR);
            }else IO.showMessage("Error: Session Expired", "Active session has expired.", IO.TAG_ERROR);
        }else IO.showMessage("Error: Session Expired", "No active sessions.", IO.TAG_ERROR);
    }

    @FXML
    public void newClient()
    {
        ClientManager.getInstance().newClientWindow("Create a new Client for this Quote", param ->
        {
            new Thread(() ->
            {
                refreshModel();
                Platform.runLater(() -> refreshView());
            }).start();
            return null;
        });
    }

    @FXML
    public void newUser()
    {
        String org_id="";
        if(cbxClients.getSelectionModel().getSelectedItem()!=null)
        {
            org_id = cbxClients.getSelectionModel().getSelectedItem().get_id();
            UserManager.getInstance().newExternalUserWindow("Create a new Contact Person for this Quote", org_id , param ->
            {
                new Thread(() ->
                {
                    refreshModel();
                    Platform.runLater(() -> refreshView());
                }).start();
                return null;
            });
        } else IO.logAndAlert("Error", "Please choose a valid client [to be used as new user's organisation].", IO.TAG_WARN);
    }

    @FXML
    public void createPDF()
    {
        try
        {
            String path = PDF.createQuotePdf((Quote)QuoteManager.getInstance().getSelected());
            if(path!=null)
            {
                PDFViewer pdfViewer = PDFViewer.getInstance();
                pdfViewer.setVisible(true);
                pdfViewer.doOpen(path);
            } else IO.log(getClass().getName(), IO.TAG_ERROR, "invalid quote PDF path returned.");
        } catch (IOException ex)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, ex.getMessage());
        }
    }

    @FXML
    public void requestApproval()
    {
        //send email requesting approval of Quote
        try
        {
            if((Quote)QuoteManager.getInstance().getSelected()!=null)
                QuoteManager.getInstance().requestQuoteApproval((Quote)QuoteManager.getInstance().getSelected(), null);
        } catch (IOException e)
        {
            IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
        }
    }

    @FXML
    public void back()
    {
        final ScreenManager screenManager = ScreenManager.getInstance();
        ScreenManager.getInstance().showLoadingScreen(param ->
        {
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if(screenManager.loadScreen(Screens.HOME.getScreen(),mvg.MVG.class.getResource("views/"+Screens.HOME.getScreen())))
                        {
                            //Platform.runLater(() ->
                            screenManager.setScreen(Screens.HOME.getScreen());
                        } else IO.log(getClass().getName(), IO.TAG_ERROR, "could not load home screen.");
                    } catch (IOException e)
                    {
                        IO.log(getClass().getName(), IO.TAG_ERROR, e.getMessage());
                    }
                }
            }).start();
            return null;
        });
    }
}
