package com.hoasao.dangnh.jfx.controller;

import com.hoasao.dangnh.jfx.Main;
import com.hoasao.dangnh.jfx.utils.MailServerPropertiesFactory;
import javafx.application.Platform;
import javafx.beans.value.*;
import javafx.concurrent.*;
import javafx.concurrent.Service;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.web.*;
import javafx.stage.*;
import org.apache.log4j.Logger;
import org.controlsfx.control.Notifications;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class MainViewController implements Initializable {
    private static final Logger logger = Logger.getLogger(MainViewController.class);
    @FXML
    private WebView templateViewer;

    @FXML
    private TextField tfHtmlPath;

    @FXML
    private Button btChooseHtmlPath;

    @FXML
    private TextField tfEmail;

    @FXML
    private PasswordField tfPw;

    @FXML
    private Button btLogin;

    @FXML
    private TextArea taSendTo;

    @FXML
    private TextArea taCc;

    @FXML
    private TextArea taBcc;

    @FXML
    private Button btSend;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label lbStatus;

    @FXML
    private Button btAbout;

    @FXML
    private TextField tfSubject;

    @FXML
    private TextField tfFrom;

    private static final Session mailSession = Session.getDefaultInstance(MailServerPropertiesFactory.getInstance());
    private Transport transport;
    private final LoginService loginService = new LoginService();
    private final SendMailService sendMailService = new SendMailService();
    private WebEngine webEngine;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        webEngine = templateViewer.getEngine();
        tfHtmlPath.textProperty().addListener(new TfHtmlPathChangeListener());
        loginService.setOnFailed(new LoginServiceFailedHandler());
        loginService.setOnSucceeded(new LoginServiceSuccessHandler());
        loginService.stateProperty().addListener(new LoginServiceStateListener());

        sendMailService.setOnFailed(new SendMailServiceFailedHandler());
        sendMailService.setOnSucceeded(new SendMailServiceSuccessHandler());
        sendMailService.stateProperty().addListener(new SendMailServiceStateListener());
    }

    @FXML
    void handleBtAbout(ActionEvent event) {
        try {
            Stage dialogStage = new Stage();
            Parent root = FXMLLoader.load(Main.class.getResource("view/AboutDialog.fxml"));
            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.setTitle("About this little tool");
            dialogStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UNIFIED);
            dialogStage.setResizable(false);
            dialogStage.show();
        } catch (IOException ex){
            logger.error("IO Exception when load About Dialog", ex);
        }
    }

    @FXML
    void handleBtChooseHtml(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        FileChooser fileChooser = new FileChooser();

        //Set extension filter
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("HTML files (*.html)", "*.html");
        fileChooser.getExtensionFilters().add(extFilter);

        //Show save file dialog
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            tfHtmlPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    void handleBtLogin(ActionEvent event) {
        if (loginService.getState() == State.READY) {
            loginService.start();
        } else loginService.restart();
    }

    @FXML
    void handleBtSend(ActionEvent event) {
        if (tfHtmlPath.getText().isEmpty()) {
            Notifications.create()
                    .text("Vui lòng chọn template HTML")
                    .showInformation();
        } else if (transport == null) {
            Notifications.create()
                    .text("Vui lòng đăng nhập")
                    .showInformation();
        } else if (tfSubject.getText().isEmpty()) {
            Notifications.create()
                    .text("Vui lòng nhập chủ đề")
                    .showInformation();
        } else if (taSendTo.getText().isEmpty() && taCc.getText().isEmpty() && taBcc.getText().isEmpty()) {
            Notifications.create()
                    .text("Vui điền người gửi")
                    .showInformation();
        } else {
            if (sendMailService.getState() == State.READY) {
                sendMailService.start();
            } else sendMailService.restart();
        }
    }

    private class LoginService extends Service<Transport> {
        @Override
        protected Task<Transport> createTask() {
            return new Task<Transport>() {
                @Override
                protected Transport call() throws Exception {
                    Transport transport = mailSession.getTransport("smtp");
                    transport.connect("smtp.gmail.com", tfEmail.getText(), tfPw.getText());
                    return transport;
                }
            };
        }
    }

    private class TfHtmlPathChangeListener implements ChangeListener<String> {
        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (newValue.endsWith(".html")) {
                try {
                    webEngine.load(new File(newValue).toURI().toURL().toString());
                } catch (MalformedURLException e) {
                    logger.error("Wrong html path", e);
                    Notifications.create()
                            .title("Error")
                            .text("Sai đường dẫn đến file html")
                            .showError();
                }
            }
        }
    }

    private class LoginServiceFailedHandler implements EventHandler<WorkerStateEvent> {
        @Override
        public void handle(WorkerStateEvent event) {
            Throwable exception = loginService.getException();
            if (exception instanceof AuthenticationFailedException) {
                logger.error("Login Failed. Wrong email/password", exception);
                Notifications.create()
                        .title("Login failed")
                        .text("Wrong email or password")
                        .showError();
            } else {
                logger.error("Login Failed. Error: ", exception);
                Notifications.create()
                        .title("Login failed")
                        .text("Unknown Error occurred")
                        .showError();
            }
        }
    }

    private class LoginServiceSuccessHandler implements EventHandler<WorkerStateEvent> {
        @Override
        public void handle(WorkerStateEvent event) {
            transport = (Transport) event.getSource().getValue();
            Notifications.create()
                    .text("Login Successfully")
                    .showInformation();
        }
    }

    private class LoginServiceStateListener implements ChangeListener<State> {
        @Override
        public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
            switch (newValue) {
                case RUNNING: {
                    progressBar.setOpacity(100);
                    progressBar.setProgress(-1.0);
                    lbStatus.setText("Đang đăng nhập...");
                    break;
                }
                case SUCCEEDED: {
                    progressBar.setOpacity(0);
                    progressBar.setProgress(0);
                    lbStatus.setText("");
                    break;
                }
                case FAILED: {
                    progressBar.setOpacity(0);
                    progressBar.setProgress(0);
                    lbStatus.setText("");
                    break;
                }
            }
        }
    }

    private class SendMailService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<Void>() {
                @Override
                protected Void call() throws IOException, MessagingException {
                    Platform.runLater(() -> lbStatus.setText("Reading HTML file..."));
                    File file = new File(tfHtmlPath.getText());
                    FileInputStream fis = new FileInputStream(file);
                    byte[] data = new byte[(int) file.length()];
                    fis.read(data);
                    fis.close();
                    Platform.runLater(() -> lbStatus.setText("Read operation completed"));
                    //create mail content string with utf 8 charset encoding
                    String mailContent = new String(data, StandardCharsets.UTF_8);

                    MimeMessage message = new MimeMessage(mailSession);

                    //create from address + name
                    InternetAddress fromAddress = new InternetAddress(tfEmail.getText(), tfFrom.getText());
                    message.setFrom(fromAddress);

                    message.setSubject(tfSubject.getText(), "utf-8");

                    //set content
                    message.setContent(mailContent, "text/html; charset=utf-8");

                    //add recipients
                    if (!taSendTo.getText().isEmpty()) {
                        String[] sendTo = taSendTo.getText().trim().split(";");
                        for (String send : sendTo) {
                            message.addRecipient(Message.RecipientType.TO, new InternetAddress(send));
                        }
                    }
                    if (!taCc.getText().isEmpty()) {
                        String[] ccs = taCc.getText().trim().split(";");
                        for (String cc : ccs) {
                            message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
                        }
                    }
                    if (!taBcc.getText().isEmpty()) {
                        String[] bbcs = taBcc.getText().trim().split(";");
                        for (String bbc : bbcs) {
                            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bbc));
                        }
                    }
                    Platform.runLater(() -> lbStatus.setText("Sending..."));

                    //send mail
                    transport.sendMessage(message, message.getAllRecipients());
                    return null;
                }
            };
        }
    }

    private class SendMailServiceFailedHandler implements EventHandler<WorkerStateEvent> {

        @Override
        public void handle(WorkerStateEvent event) {
            Throwable exception = sendMailService.getException();
            if (exception instanceof MessagingException) {
                logger.error("Error when send message: ", exception);
                Notifications.create()
                        .title("Send message error")
                        .text("Gửi mail thất bại")
                        .showError();
            } else if (exception instanceof IOException) {
                logger.error("IO Error with HTML File", exception);
                Notifications.create()
                        .text("Lỗi đọc file HTML")
                        .showError();
            }

        }
    }

    private class SendMailServiceSuccessHandler implements EventHandler<WorkerStateEvent> {

        @Override
        public void handle(WorkerStateEvent event) {
            Notifications.create()
                    .title("Success")
                    .text("Gửi mail thành công")
                    .showInformation();
        }
    }

    private class SendMailServiceStateListener implements ChangeListener<State> {

        @Override
        public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
            switch (newValue) {
                case RUNNING: {
                    progressBar.setOpacity(100);
                    progressBar.setProgress(-1.0);
                    break;
                }
                case SUCCEEDED: {
                    progressBar.setOpacity(0);
                    progressBar.setProgress(0);
                    lbStatus.setText("");
                    break;
                }
                case FAILED: {
                    progressBar.setOpacity(0);
                    progressBar.setProgress(0);
                    lbStatus.setText("");
                    break;
                }
            }
        }
    }
}
