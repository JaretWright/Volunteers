
package views;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import models.Volunteer;

/**
 * FXML Controller class
 *
 * @author jaret_000
 */
public class LogHoursViewController implements Initializable, ControllerClass {
    @FXML    private DatePicker datePicker;
    @FXML    private Spinner hoursWorkedSpinner;
    @FXML    private Label volunteerIDLabel;
    @FXML    private Label firstNameLabel;
    @FXML    private Label lastNameLabel;
    @FXML    private Label errMsgLabel;
    @FXML    private Button backButton;
    
    //objects used in the line chart
    @FXML    private LineChart<?, ?> lineChart;
    @FXML    private CategoryAxis monthAxis;
    @FXML    private NumberAxis hoursAxis;
             private XYChart.Series hoursLoggedSeries;

   
    private Volunteer volunteer;
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0,18,8);
        hoursWorkedSpinner.setValueFactory(valueFactory);
        
        //change the text on the button if it is not an administrative user
        if (!SceneChanger.getLoggedInUser().isAdmin())
            backButton.setText("Edit");
    }    
    
    
    /**
     * This method will log the user out of the application and return them to the
     * LoginView scene
     * @param volunteer 
     */
    public void logoutButtonPushed(ActionEvent event) throws IOException
    {
        SceneChanger.setLoggedInUser(null);
        SceneChanger sc = new SceneChanger();
        sc.changeScenes(event, "LoginView.fxml", "Login");
    }
    
    

    @Override
    public void preloadData(Volunteer volunteer) {
        this.volunteer = volunteer;
        volunteerIDLabel.setText(Integer.toString(volunteer.getVolunteerID()));
        firstNameLabel.setText(volunteer.getFirstName());
        lastNameLabel.setText(volunteer.getLastName());
        datePicker.setValue(LocalDate.now());
        errMsgLabel.setText("");
        
        updateLineChart();
    }
    
    /**
     * The goal of this method is to update the line chart with the latest
     * information stored in the database
     */
    public void updateLineChart()
    {
        //initialize the instance variables for the chart
        hoursLoggedSeries = new XYChart.Series<>();
        hoursLoggedSeries.setName(Integer.toString(LocalDate.now().getYear()));
        lineChart.getData().clear();
        
        try{
            populateSeriesFromDB();
        }
        catch (SQLException e)
        {
            System.err.println(e.getMessage());
        }
        lineChart.getData().addAll(hoursLoggedSeries);
    }
    
    /**
     * This method will populate the hoursLoggedSeries with the latest info
     * from the database
     */
    public void populateSeriesFromDB() throws SQLException
    {
        Connection conn=null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try{
            //1.  Connect to the database
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/volunteer?useSSL=false", "student", "student");
            
            //2. create a String with the sql statement
            String sql =    "SELECT MONTHNAME(dateWorked), SUM(hoursWorked) " +
                            "FROM hoursworked " +
                            "WHERE volunteerID=? AND YEAR(dateWorked)=? " +
                            "GROUP BY MONTH(dateWorked);";
            
            //3. create the statement
            statement = conn.prepareCall(sql);
            
            //4. bind the parameters
            statement.setInt(1, volunteer.getVolunteerID());
            statement.setInt(2, LocalDate.now().getYear());
            
            //5. execute the query
            resultSet = statement.executeQuery();
            
            //6. loop over the result set and build the series
            while (resultSet.next())
            {
                hoursLoggedSeries.getData().add(new XYChart.Data(resultSet.getString(1), resultSet.getInt(2)));
            }
        }
        catch (SQLException e)
        {
            System.err.println(e.getMessage());
        }
        finally
        {
            if (conn != null)
                conn.close();
            if (statement != null)
                statement.close();
            if (resultSet != null)
                resultSet.close();
        }
    }
        
    /**
     * This method will read/validate the inputs and store the information
     * in the hoursWorked table
     */
    public void saveButtonPushed(ActionEvent event)
    {
        try{
            volunteer.logHours(datePicker.getValue(), (int) hoursWorkedSpinner.getValue()); 
            errMsgLabel.setText("Hours logged");
            updateLineChart();
        }
        catch (SQLException e)
        {
            System.err.println(e.getMessage());
        }
        catch (IllegalArgumentException e)
        {
            errMsgLabel.setText(e.getMessage());
        }
    }
    
    
     /**
     * This will return the user to the table of all volunteers
     */
    public void cancelButttonPushed(ActionEvent event) throws IOException
    {
        //if this is an admin user, go back to the table of volunteers
        SceneChanger sc = new SceneChanger();
        
        if (SceneChanger.getLoggedInUser().isAdmin())
            sc.changeScenes(event, "VolunteerTableView.fxml", "All Volunteers");
        else
        {
            NewUserViewController controller = new NewUserViewController();
            sc.changeScenes(event, "NewUserView.fxml", "Edit", volunteer, controller);
        }
            
    }
}
