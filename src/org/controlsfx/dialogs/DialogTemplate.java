package org.controlsfx.dialogs;

import static org.controlsfx.dialogs.DialogResources.getMessage;
import static org.controlsfx.dialogs.DialogResources.getString;
import static org.controlsfx.dialogs.Dialogs.DialogType.INFORMATION;
import static org.controlsfx.dialogs.Dialogs.DialogType.WARNING;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.controlsfx.dialogs.Dialogs.DialogOptions;
import org.controlsfx.dialogs.Dialogs.DialogResponse;
import org.controlsfx.dialogs.Dialogs.DialogType;

import com.sun.javafx.Utils;

/**
 * 
 * @param <T> The type for user input
 */
class DialogTemplate<T> {
    private static enum DialogStyle {
        SIMPLE,
        ERROR,
        INPUT;
    }
    
    // used for OS-specific button placement inside the dialog
    private static final String BUTTON_TYPE = "button_type";
    private static enum ButtonType {
        OK,
        NO,
        CANCEL, 
        ACTION;
    }
    
    // Defines max dialog width.
    final static int DIALOG_WIDTH = 516;
    
    // According to the UI spec, the width of the main message text in the upper
    // panel should be 426 pixels.
    private static int MAIN_TEXT_WIDTH = 400;
    
    // specifies the minimum allowable width for all buttons in the dialog
    private static int MINIMUM_BUTTON_WIDTH = 75;
    
    private FXDialog dialog;
    private BorderPane contentPane;
    
    private DialogType dialogType = INFORMATION;
    private final DialogOptions options;
    private DialogResponse userResponse = DialogResponse.CLOSED;
    
    private DialogStyle style;
    
    // for user input dialogs (textfield / choicebox / combobox)
    private T initialInputValue;
    private List<T> inputChoices;
    private T userInputResponse;
    
    
    // masthead
    private String mastheadString;
    private BorderPane mastheadPanel;
    private UITextArea mastheadTextArea;
    
    // center
    private Pane centerPanel;
    private String contentString = null;
    private Pane resizableArea = null;
    
    // masthead or center, depending on whether a mastheadString is specified
    private ImageView dialogBigIcon;
    
    // Buttons
    private static final String okBtnStr = "common.ok.btn";
    private static final String yesBtnStr = "common.yes.btn";
    private static final String noBtnStr = "common.no.btn";
    private static final String cancelBtnStr = "common.cancel.btn";
    private static final String detailBtnStr = "common.detail.button";
    
    // This is used in the exception dialog only.
    private Throwable throwable = null;    
    
//    // Visual indication of security level alert - either high or medium.
//    // Located in the lower left corner at the bottom of the dialog.
//    private static final String SECURITY_ALERT_HIGH = "security.alert.high.image";
//    private static final String SECURITY_ALERT_LOW  = "security.alert.low.image";
//    private ImageView securityIcon;
//
//    // These are for security dialog only.
//    private String[] alertStrs;
//    private String[] infoStrs;
    
    
    
    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/    

    DialogTemplate(Stage owner, String title, String masthead, DialogOptions options) {
        this.dialog = new FXDialog(title, owner, true);
        
        this.contentPane = new BorderPane();
        this.dialog.setContentPane(contentPane);

        this.mastheadString = masthead;
        this.options = options;
    }
    
    
    
    
    /***************************************************************************
     *                                                                         *
     * Dialog construction API                                                 *
     *                                                                         *
     **************************************************************************/
    
    void setSimpleContent(final String contentString, final DialogType dialogType) {
        this.style = DialogStyle.SIMPLE;
        this.contentString = contentString;
        
        this.dialogType = dialogType == null ? WARNING : dialogType;

        if (isMastheadVisible()) {
            contentPane.setTop(createMasthead());
        }
        
        Node centerPanel = createCenterPanel();
        contentPane.setCenter(centerPanel);

        dialog.setResizable(false);
    }
    
    void setErrorContent(String contentString, Throwable throwable) {
        this.style = DialogStyle.ERROR;
        this.contentString = contentString;
        this.throwable = throwable;

        this.dialogType = DialogType.ERROR;

        if (isMastheadVisible()) {
            contentPane.setTop(createMasthead());
        }
        
        contentPane.setCenter(createCenterPanel());
        
        dialog.setResizable(false);
    }
    
    void setInputContent(String message, T initialValue, List<T> choices) {
        this.style = DialogStyle.INPUT;
        this.contentString = message;
        this.initialInputValue = initialValue;
        this.inputChoices = choices;
        
        if (isMastheadVisible()) {
            contentPane.setTop(createMasthead());
        }
        
        contentPane.setCenter(createCenterPanel());

        dialog.setResizable(false);
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * 'Public' API                                                            *
     *                                                                         *
     **************************************************************************/
    
    public FXDialog getDialog() {
        return dialog;
    }
    
    public void show() {
        dialog.showAndWait();
    }
    
    public void hide() {
        dialog.hide();
    }
    
    /**
     * gets the response from the user.
     * @return the response
     */
    public DialogResponse getResponse() {
        return userResponse;
    }
    
    public T getInputResponse() {
        return userInputResponse;
    }
    
    
    
    /***************************************************************************
     *                                                                         *
     * Implementation                                                          *
     *                                                                         *
     **************************************************************************/
    
    /**
     * TODO delete me - this is just for testing!!
     */
    private static boolean isMac = false;
    private static boolean isWindows = false;
    
    static void setMacOS(boolean b) {
        isMac = b;
        isWindows = !b;
    }
    
    static void setWindows(boolean b) {
        isMac = !b;
        isWindows = b;
    }
    
    private boolean isWindows() {
        return isWindows || (! isMac && Utils.isWindows());
    }
    
    private boolean isMac() {
        return isMac || (! isWindows && Utils.isMac());
    }
    
    private boolean isUnix() {
        return Utils.isUnix();
    }
    
    private boolean isMastheadVisible() {
        return mastheadString != null && ! mastheadString.isEmpty();
    }

    /*
     * top part of the dialog contains short informative message, and either
     * an icon, or the text is displayed over a watermark image
     */
    private Pane createMasthead() {
        mastheadPanel = new BorderPane();
        mastheadPanel.getStyleClass().add("top-panel");

        // Create panel with text area and icon or just a background image:
        // Create topPanel's components.  UITextArea determines
        // the size of the dialog by defining the number of columns
        // based on font size.
        mastheadTextArea = new UITextArea(MAIN_TEXT_WIDTH);
        mastheadTextArea.getStyleClass().add("masthead-label-1");

        VBox mastheadVBox = new VBox();
        mastheadVBox.setAlignment(Pos.CENTER_LEFT);
        mastheadTextArea.setText(mastheadString);
        mastheadTextArea.setAlignment(Pos.CENTER_LEFT);
        mastheadVBox.getChildren().add(mastheadTextArea);

        mastheadPanel.setLeft(mastheadVBox);
        BorderPane.setAlignment(mastheadVBox, Pos.CENTER_LEFT);
        dialogBigIcon = new ImageView(dialogType == null ? DialogResources.getImage("java48.image") : dialogType.getImage());
        mastheadPanel.setRight(dialogBigIcon);

        return mastheadPanel;
    }

    private Pane createCenterPanel() {
        centerPanel = new VBox();
        centerPanel.getStyleClass().add("center-panel");

        BorderPane contentPanel = new BorderPane();
        contentPanel.getStyleClass().add("center-content-panel");
        VBox.setVgrow(contentPanel, Priority.ALWAYS);

        Node content = createCenterContent();
        if (content != null) {
            contentPanel.setCenter(content);
            contentPanel.setPadding(new Insets(0, 0, 12, 0));
        }
        
        if (contentPanel.getChildren().size() > 0) {
            centerPanel.getChildren().add(contentPanel);
        }

        // OS-specific button positioning
        Node buttonPanel = createButtonPanel();
        centerPanel.getChildren().add(buttonPanel);
        
        // dialog image can go to the left if there is no masthead
        if (! isMastheadVisible()) {
            dialogBigIcon = new ImageView(dialogType == null ? DialogResources.getImage("java48.image") : dialogType.getImage());
            Pane pane = new Pane(dialogBigIcon);
            contentPanel.setLeft(pane);
        }

        return centerPanel;
    }
    
    private Node createCenterContent() {
    	switch( style ) {
    	case SIMPLE:
            if (contentString != null) {
                UITextArea ta = new UITextArea(contentString);
                ta.getStyleClass().add("center-content-area");
                ta.setAlignment(Pos.TOP_LEFT);
                return ta;
            }
            break;
    	case ERROR:
            if (contentString != null) {
                UITextArea ta = new UITextArea(contentString);
                ta.getStyleClass().add("center-content-area");
                ta.setAlignment(Pos.TOP_LEFT);
            	VBox contentPanel = new VBox(10);
            	contentPanel.getChildren().add(ta);
            	
            	resizableArea = null;
            	if (throwable != null) {
            		resizableArea = new VBox(10);

                    Label label = new Label(getString("exception.dialog.label"));
                    VBox.setVgrow(label, Priority.NEVER);

                    resizableArea.getChildren().add(label);

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    throwable.printStackTrace(pw);
                    TextArea text = new TextArea(sw.toString());
                    text.setEditable(false);
                    text.setWrapText(true);
                    text.setPrefWidth(60 * 8);
                    text.setMaxHeight(Double.MAX_VALUE);
                    resizableArea.getChildren().add(text);
		            VBox.setVgrow(text, Priority.ALWAYS);

                    contentPanel.getChildren().add(resizableArea);
                    VBox.setVgrow(resizableArea, Priority.ALWAYS);
                    
                    resizableArea.managedProperty().bind(resizableArea.visibleProperty());
                    resizableArea.setVisible(false);
                }
            	
                return contentPanel;
            }
            break;
    	case INPUT:    
            Control inputControl = null;
            if (inputChoices == null || inputChoices.isEmpty()) {
                // no input constraints, so use a TextField
                final TextField textField = new TextField();
                textField.setOnAction(new EventHandler<ActionEvent>() {
                    @Override public void handle(ActionEvent t) {
                        userInputResponse = (T) textField.getText();
                        hide();
                    }
                });
                if (initialInputValue != null) {
                    textField.setText(initialInputValue.toString());
                }
                inputControl = textField;
            } else {
                // input method will be constrained to the given choices
                ChangeListener<T> changeListener = new ChangeListener<T>() {
                    @Override public void changed(ObservableValue<? extends T> ov, T t, T t1) {
                        userInputResponse = t1;
                    }
                };
                
                if (inputChoices.size() > 10) {
                    // use ComboBox
                    ComboBox<T> comboBox = new ComboBox<T>();
                    comboBox.getItems().addAll(inputChoices);
                    comboBox.getSelectionModel().select(initialInputValue);
                    comboBox.getSelectionModel().selectedItemProperty().addListener(changeListener);
                    inputControl = comboBox;
                } else {
                    // use ChoiceBox
                    ChoiceBox<T> choiceBox = new ChoiceBox<T>();
                    choiceBox.getItems().addAll(inputChoices);
                    choiceBox.getSelectionModel().select(initialInputValue);
                    choiceBox.getSelectionModel().selectedItemProperty().addListener(changeListener);
                    inputControl = choiceBox;
                }
            }
            
            HBox hbox = new HBox(10);
            hbox.setPrefWidth(MAIN_TEXT_WIDTH);
            
            if (contentString != null && ! contentString.isEmpty()) {
                Label label = new Label(contentString);
                hbox.getChildren().add(label);
                
            }
            
            if (inputControl != null) {
                hbox.getChildren().add(inputControl);
            }
            
            return hbox;
        }
        
        return null;
    }
    
    private Node createButtonPanel() {
        // Create buttons from okBtnStr and cancelBtnStr strings.
        final Map<ButtonType, ButtonBase> buttons = createButtons();
        
        HBox buttonsPanel = new HBox(6) {
            @Override protected void layoutChildren() {
                resizeButtons(buttons);
                super.layoutChildren();
            }
        };
        buttonsPanel.getStyleClass().add("button-bar");
        
        if (isWindows() || isUnix()) {
            // push all buttons to the right
            buttonsPanel.getChildren().add(createButtonSpacer());
            
            // then run in the following order:
            // ButtonType -> OK, NO, CANCEL, ACTION
            addButton(ButtonType.OK, buttons, buttonsPanel);
            addButton(ButtonType.NO, buttons, buttonsPanel);
            addButton(ButtonType.CANCEL, buttons, buttonsPanel);
            addButton(ButtonType.ACTION, buttons, buttonsPanel);
        } else if (isMac()) {
            // put ButtonType.ACTION button on left
            addButton(ButtonType.ACTION, buttons, buttonsPanel);
            
            // then put in spacer to push to right
            buttonsPanel.getChildren().add(createButtonSpacer());
            
            // then run in the following order:
            // ButtonType -> CANCEL, NO, OK
            addButton(ButtonType.CANCEL, buttons, buttonsPanel);
            addButton(ButtonType.NO, buttons, buttonsPanel);
            addButton(ButtonType.OK, buttons, buttonsPanel);
        }
        
        return buttonsPanel;
    }
    
    private void addButton(final ButtonType type, final Map<ButtonType, ButtonBase> buttons, final HBox buttonsPanel) {
        if (buttons.containsKey(type)) {
            ButtonBase button = buttons.get(type);
            buttonsPanel.getChildren().add(button);
        }
    }
    
    private Node createButtonSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private Map<ButtonType, ButtonBase> createButtons() {
        Map<ButtonType, ButtonBase> buttons = new HashMap<>();
        
        if (style == DialogStyle.INPUT) {
            buttons.put(ButtonType.OK, createButton(okBtnStr, DialogResponse.OK, true, false));
            buttons.put(ButtonType.CANCEL, createButton(cancelBtnStr, DialogResponse.CANCEL, false, true));
        } else if (DialogType.ERROR == dialogType && throwable != null) {
            // we've got an error dialog, which has 'OK' and 'Details..' buttons
            buttons.put(ButtonType.OK, createButton(okBtnStr, DialogResponse.OK, true, false));

//            Button detailsBtn = new Button((detailBtnStr == null) ? "" : getMessage(detailBtnStr));
            ToggleButton detailsBtn = new ToggleButton((detailBtnStr == null) ? "" : getMessage(detailBtnStr));
            buttons.put(ButtonType.ACTION, detailsBtn);
            detailsBtn.setOnAction(exceptionDetailsHandler);
        } else if (options == DialogOptions.OK) {
            buttons.put(ButtonType.OK, createButton(okBtnStr, DialogResponse.OK, true, false));
        } else if (options == DialogOptions.OK_CANCEL) {
            buttons.put(ButtonType.OK, createButton(okBtnStr, DialogResponse.OK, true, false));
            buttons.put(ButtonType.CANCEL, createButton(cancelBtnStr, DialogResponse.CANCEL, false, true));
        } else if (options == DialogOptions.YES_NO) {
            buttons.put(ButtonType.OK, createButton(yesBtnStr, DialogResponse.YES, true, false));
            buttons.put(ButtonType.NO, createButton(noBtnStr, DialogResponse.NO, false, true));
        } else if (options == DialogOptions.YES_NO_CANCEL) {
            buttons.put(ButtonType.OK, createButton(yesBtnStr, DialogResponse.YES, true, false));
            buttons.put(ButtonType.NO, createButton(noBtnStr, DialogResponse.NO, false, true));
            buttons.put(ButtonType.CANCEL, createButton(cancelBtnStr, DialogResponse.CANCEL, false, false));
        }
        
        return buttons;
    }
    
    private Button createButton(String extLabel, DialogResponse response, boolean isDefault, boolean isCancel) {
        Button btn = new Button((extLabel == null) ? "" : getMessage(extLabel));
        btn.setOnAction(createButtonHandler(response));
        btn.setDefaultButton(isDefault);
        btn.setCancelButton(isCancel);
        return btn;
    }
    
    /*
     * According to UI guidelines, all buttons should have the same length.
     * This function is to define the longest button in the array of buttons
     * and set all buttons in array to be the length of the longest button.
     */
    private void resizeButtons(Map<ButtonType, ButtonBase> buttonsMap) {
        Collection<ButtonBase> buttons = buttonsMap.values();
        
        // Find out the longest button...
        double widest = MINIMUM_BUTTON_WIDTH;
        for (ButtonBase btn : buttons) {
            if (btn == null) continue;
            widest = Math.max(widest, btn.prefWidth(-1));
        }
        
        // ...and set all buttons to be this width
        for (ButtonBase btn : buttons) {
            if (btn == null) continue;
            btn.setPrefWidth(btn.isVisible() ? widest : 0);
        }
    }
    
    private EventHandler<ActionEvent> createButtonHandler(final DialogResponse response) {
        return new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent ae) {
                userResponse = response;

                // hide the dialog.  We'll return from the dialog,
                // and who ever called it will retrieve user's answer
                // and will dispose of the dialog after that.
                hide();
            }
        };
    }
    
    private EventHandler<ActionEvent> exceptionDetailsHandler = new EventHandler<ActionEvent>() {
        @Override public void handle(ActionEvent ae) {
            if (throwable != null) {
                // old approach (show new window)
                //new ExceptionDialog(dialog, throwable).show();
                
                // new approach (dynamic expanding dialog)
            	boolean visible = !resizableArea.isVisible();
            	resizableArea.setVisible(visible);
            	dialog.setResizable(visible);
            	dialog.sizeToScene();
            }
        }
    };
}
