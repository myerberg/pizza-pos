//Tyler Myerberg (Developer)
//12.1.23
//PizzaPOS Pro v35
//Team: EigenSolutions
//Course: Software Engineering (COSC 301)
//Description: PizzaPOS Pro is a full-service pizza order processing system, 
//with GUI and persistent HyperSQL database functionality.

//importing necessary libraries for GUI and database operations
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//main class for the PizzaPOS Pro system
public class PizzaPOS {

    //GUI components declaration
    private JFrame frame;      //main application window
    private JPanel mainPanel;  //main content panel inside the JFrame
    private JTextField pinField, nameField;  //text fields for user input
    private JLabel statusLabel; //label to display status messages
    private JCheckBox pepperoniCheckBox;
    private JCheckBox sausageCheckBox;
    private JCheckBox baconCheckBox;
    private JCheckBox onionCheckBox;
    private JCheckBox peppersCheckBox;
    private JCheckBox mushroomsCheckBox;
    private JCheckBox pineappleCheckBox;
    private JCheckBox xtraCheeseCheckBox;
    private Coke2L coke;
    
    //object for database connection
    private Connection conn;
    
    //declare the linked list to track orders
    private PizzaTotalOrderLinkedList pizzaOrderList;

    private JList<String> pizzaList;
    
    private DefaultListModel<String> pizzaListModel;

    private final String FIRST_USER_NAME = "FIRST_USER_NAME";

    //constructor initializes GUI components and database
    public PizzaPOS() {
    	
        //setting up the main application window
        frame = new JFrame("PizzaPOS Pro");
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null); //center the window on the screen
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        //initialize database
        initDatabase();
        
        //check and setup the first user if not exists
        setupFirstUser();

        frame.add(mainPanel);
        frame.setVisible(true); //make sure the JFrame is set visible after all initialization
    
    }
    
    //method to check if the users table is empty
    private boolean isUserTableEmpty() {
        
        try {
        	
            //sql statement to count the number of rows in users table
            String sql = "SELECT COUNT(*) FROM users";
            
            //prepare the sql statement
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            //execute the query and store the result
            ResultSet rs = stmt.executeQuery();
            
            //check if the result set has at least one row
            if (rs.next()) {
            	
                //retrieve the count from the result set
                int count = rs.getInt(1);
                
                //return true if count is 0, indicating table is empty
                return count == 0;
                
            }
            
        } catch (Exception e) {
        	
            //print the stack trace in case of an exception
            e.printStackTrace();
            
        }
        
        //return true by default in case of an error
        return true; 

    }
    
    //method to update the name of a user based on their PIN
    private void updateUserName(String pin, String newName) {
    	
        try {
        	
            //start a transaction
            conn.setAutoCommit(false);

            //SQL statement to update the user's name in the 'users' table based on their PIN
            String sql = "UPDATE users SET name = ? WHERE pin = ?";

            //prepare the SQL statement
            PreparedStatement stmt = conn.prepareStatement(sql);

            //set the new name in the first placeholder
            stmt.setString(1, newName);

            //set the pin in the second placeholder
            stmt.setString(2, pin);

            //execute the update operation
            stmt.executeUpdate();

            //commit the transaction
            conn.commit();

        } catch (Exception e) {
        	
            try {
            	
                //roll back the transaction in case of an exception
                conn.rollback();
                
            } catch (SQLException rollbackEx) {
            	
                rollbackEx.printStackTrace();
                
            }
            
            e.printStackTrace();
            
        } finally {
        	
            try {
            	
                //ensure auto-commit is re-enabled
                conn.setAutoCommit(true);
                
            } catch (SQLException autoCommitEx) {
            	
                autoCommitEx.printStackTrace();
                
            }
            
        }
        
    }
    
    //method to create the appropriate table in the database when it does not exist
    private void createTableIfNotExists(String tableName, String createTableSql) {
        
    	try {
    		
            //try to fetch from the table
            String checkTableExistsSql = "SELECT 1 FROM " + tableName + " LIMIT 1";
            PreparedStatement checkStmt = conn.prepareStatement(checkTableExistsSql);
            checkStmt.executeQuery(); //if this succeeds, the table exists
        
    	} catch (SQLException e) {
    		
            //if the table does not exist, this query will fail and we catch the exception to create the table
            if (e.getMessage().contains("object not found")) {
            	
                try {
                	
                    PreparedStatement createStmt = conn.prepareStatement(createTableSql);
                    createStmt.execute();
                    
                } catch (SQLException ex) {
                	
                    ex.printStackTrace();
                    
                }
                
            } else {
            	
                //if it is a different SQL error, print the stack trace
                e.printStackTrace();
                
            }
            
        }
    	
    }
    
    //method to check if any users exist in the database
    private boolean userExists() throws SQLException {
    	
        //SQL query to count all users in the 'users' table
        String checkSql = "SELECT COUNT(*) FROM users";
        
        //ensures Statement and ResultSet are closed after use
        try (Statement stmt = conn.createStatement();
        		
            ResultSet rs = stmt.executeQuery(checkSql)) {
        	
            //if there is a result, return true if the count is greater than zero
            if (rs.next()) {
            	
                return rs.getInt(1) > 0;
                
            }
            
            //if no result, return false indicating no users exist
            return false;
            
        }
        
    }

    //method to insert or update prices in the specified table for different sizes
    private void insertOrUpdatePrices(String tableName, String[] sizes, double[] prices) throws SQLException {
        
    	//SQL statement for inserting new size and price records
        String insertSql = "INSERT INTO " + tableName + " (size, price) VALUES (?, ?)";
        
        //SQL statement for updating price for a given size
        String updateSql = "UPDATE " + tableName + " SET price = ? WHERE size = ?";

        //loop through sizes array to insert or update each size and its price
        for (int i = 0; i < sizes.length; i++) {
        	
            //try inserting the size and price
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            	
                stmt.setString(1, sizes[i]);
                stmt.setDouble(2, prices[i]);
                stmt.executeUpdate();
                
            } catch (SQLException e) {
            	
                //if there is a unique constraint violation, it means the size already exists
                if (e.getErrorCode() == -104) {
                	
                    //update the existing size with the new price
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    	
                        updateStmt.setDouble(1, prices[i]);
                        updateStmt.setString(2, sizes[i]);
                        updateStmt.executeUpdate();
                        
                    }
                    
                } else {
                	
                    //if the error is not due to a unique constraint violation, throw the exception
                    throw e;
                    
                }
                
            }
            
        }
        
    }

    //method to insert or update topping prices
    private void insertOrUpdateToppingPrices(String size, String topping, double price) throws SQLException {
        
    	//SQL query to check if a specific size and topping combination already exists
        String selectSql = "SELECT COUNT(*) FROM toppingPrices WHERE size = ? AND topping = ?";
        
        //prepare the statement for execution
        PreparedStatement selectStmt = conn.prepareStatement(selectSql);
        selectStmt.setString(1, size);
        selectStmt.setString(2, topping);
        
        //execute the query
        ResultSet rs = selectStmt.executeQuery();
        rs.next();
        
        //get the count of existing records
        int count = rs.getInt(1);
        if (count == 0) {
        	
            //if no record exists, insert a new topping price
            String insertSql = "INSERT INTO toppingPrices (size, topping, price) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, size);
            insertStmt.setString(2, topping);
            insertStmt.setDouble(3, price);
            insertStmt.executeUpdate();
            
        } else {
        	
            //if a record exists, update the price for the existing topping
            String updateSql = "UPDATE toppingPrices SET price = ? WHERE size = ? AND topping = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setDouble(1, price);
            updateStmt.setString(2, size);
            updateStmt.setString(3, topping);
            updateStmt.executeUpdate();
            
        }
        
    }

    //method to insert or update Coke prices
    private void insertOrUpdateCokePrices(String tableName, String size, double price) throws SQLException {
        
    	//insert new Coke price record
        String insertSql = "INSERT INTO " + tableName + " (size, price) VALUES (?, ?)";
        
        //if there is a unique constraint violation, update the existing price
        String updateSql = "UPDATE " + tableName + " SET price = ? WHERE size = ?";
        
        //try inserting the Coke price
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
        	
            stmt.setString(1, size);
            stmt.setDouble(2, price);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
        	
            //if a record exists for the size, update the price instead
            if (e.getErrorCode() == -104) {
            	
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                	
                    updateStmt.setDouble(1, price);
                    updateStmt.setString(2, size);
                    updateStmt.executeUpdate();
                    
                }
                
            } else {
            	
                //throw exception if it is not due to unique constraint violation
                throw e;
                
            }
            
        }
        
    }

    //method to insert or update sales tax values
    private void insertOrUpdateSalesTax(String tableName, int id, double value) throws SQLException {
        
    	//insert new sales tax record
        String insertSql = "INSERT INTO " + tableName + " (id, value) VALUES (?, ?)";
        
        //if a record already exists, update the value
        String updateSql = "UPDATE " + tableName + " SET value = ? WHERE id = ?";
        
        //try inserting the sales tax
        try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
        	
            stmt.setInt(1, id);
            stmt.setDouble(2, value);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
        	
            //if the ID already exists, update the sales tax value
            if (e.getErrorCode() == -104) {
            	
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                	
                    updateStmt.setDouble(1, value);
                    updateStmt.setInt(2, id);
                    updateStmt.executeUpdate();
                    
                }
                
            } else {
            	
                //throw exception if it is not due to unique constraint violation
                throw e;
                
            }
            
        }
        
    }

    //populates database with default data
    private void insertDefaultData() throws SQLException {
    	
        //insert default pizza prices
        String[] sizes = {"Small", "Medium", "Large"};
        double[] pizzaPrices = {8, 10, 12};
        insertOrUpdatePrices("pizzaPrices", sizes, pizzaPrices);

        //insert default topping prices: $1 for Small, $2 for Medium, $3 for Large
        String[] toppings = {"Pepperoni", "Sausage", "Bacon", "Onion", "Green Peppers", "Mushrooms", "Pineapple", "Xtra Cheese"};
        double[] toppingPrices = {1.0, 2.0, 3.0};  //prices for each size

        for (int i = 0; i < sizes.length; i++) {
        	
            String size = sizes[i];
            double price = toppingPrices[i];
            for (String topping : toppings) {
            	
                insertOrUpdateToppingPrices(size, topping, price);
            
            }
            
        }

        //insert default Coke price
        insertOrUpdateCokePrices("cokePrices", "2L", 2.0);

        //insert default sales tax
        insertOrUpdateSalesTax("salesTax", 1, 0.06);
        
    }

    //method to initialize the HyperSQL database
    private void initDatabase() {
    	
        try {
        	
            //load the HyperSQL JDBC driver
            Class.forName("org.hsqldb.jdbc.JDBCDriver");

            //database URL
            String url = "jdbc:hsqldb:file:databaseName;hsqldb.write_delay=false;shutdown=true";

            //establish a connection to the database
            conn = DriverManager.getConnection(url, "SA", "");
            try {
            	
                //set auto-commit to false to manage transactions manually
                conn.setAutoCommit(false);

                //create tables if not exists
                createTableIfNotExists("users", "CREATE TABLE users (pin VARCHAR(255) PRIMARY KEY, name VARCHAR(255))");
                createTableIfNotExists("pizzaPrices", "CREATE TABLE pizzaPrices (size VARCHAR(255) PRIMARY KEY, price DOUBLE)");
                createTableIfNotExists("toppingPrices", "CREATE TABLE toppingPrices (size VARCHAR(255), topping VARCHAR(255), price DOUBLE, PRIMARY KEY(size, topping))");
                createTableIfNotExists("cokePrices", "CREATE TABLE cokePrices (size VARCHAR(255) PRIMARY KEY, price DOUBLE)");
                createTableIfNotExists("salesTax", "CREATE TABLE salesTax (id INTEGER PRIMARY KEY, value DOUBLE)");

                //check if default data needs to be inserted
                if (isDatabaseEmpty()) {
                	
                    insertDefaultData();
                    
                }

                //check if a user exists and setup the first user if not
                if (!userExists()) {
                	
                    setupFirstUser();
                    
                }

                //commit the transaction
                conn.commit();
                
            } catch (SQLException e) {
            	
                //in case of an exception, attempt to rollback changes
                conn.rollback();
                throw e;
                
            }
            
        } catch (Exception e) {
        	
            //handle exceptions such as ClassNotFoundException and SQLException
            e.printStackTrace();
            
        }
        
    }
    
    //method to check if database is empty
    private boolean isDatabaseEmpty() throws SQLException {
    	
        //check if the 'pizzaPrices' table is empty
        try (Statement stmt = conn.createStatement();
        		
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pizzaPrices")) {
            if (rs.next()) {
            	
                return rs.getInt(1) == 0; //return true if count is 0, indicating no data
            
            }
            
        }
        
        return true; //if unable to perform the query, assume the database is empty
    
    }

    //method to check if a given PIN exists in the database
    private boolean pinExists(String pin) {
    	
        try {
        	
            //SQL statement to fetch the username associated with a given PIN
            String sql = "SELECT name FROM users WHERE pin=?";
            
            //prepare the SQL statement
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            //set the pin parameter in the prepared statement
            stmt.setString(1, pin);
            
            //execute the query and get the result set
            ResultSet rs = stmt.executeQuery();
            
            //return true if a record exists for the given PIN
            return rs.next();
            
        } catch (Exception e) {
        	
            //print the stack trace in case of an exception
            e.printStackTrace();
            
            //return false in case of an exception
            return false;
            
        }
        
    }

    //method to add a new user to the database
    private void addUser(String pin, String name) {
    	
        try {
        	
            //SQL statement to insert a new user record into the 'users' table
            String sql = "INSERT INTO users (pin, name) VALUES(?, ?)";
            
            //prepare the sql statement
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            //set the pin parameter in the prepared statement
            stmt.setString(1, pin);
            
            //set the name parameter in the prepared statement
            stmt.setString(2, name);
            
            //execute the insert operation
            stmt.executeUpdate();  
            
        } catch (Exception e) {
        	
            //print the stack trace in case of an exception
            e.printStackTrace();
            
        }
        
    }

    //method to setup the first user if they do not already exist
    private void setupFirstUser() {
    	
        if (isUserTableEmpty()) {
        	
            //clear previous components and set the layout to BoxLayout for vertical stacking
            mainPanel.removeAll();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            //GUI components for initial user setup
            JLabel instructionLabel = new JLabel("Enter a 4-digit PIN for the system’s first user and press Enter:");
            pinField = new JTextField(4);
            pinField.setHorizontalAlignment(JTextField.CENTER); //center text
            pinField.setMaximumSize(new Dimension(pinField.getPreferredSize().width, pinField.getPreferredSize().height)); //constrain width
            
            //initialize the status label to display error messages
            statusLabel = new JLabel("");
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            
            //wrap the pinField in a panel with FlowLayout to prevent stretching
            JPanel pinPanel = new JPanel();
            pinPanel.setLayout(new FlowLayout(FlowLayout.CENTER)); //center the pinField horizontally
            pinPanel.add(pinField);
            
            //adjust alignment of components
            instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            pinPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            //add vertical glue to center components vertically
            mainPanel.add(Box.createVerticalGlue());
            
            //add components to the mainPanel with space between them
            mainPanel.add(instructionLabel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 5))); //spacer
            mainPanel.add(pinPanel);
            mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); //larger spacer
            mainPanel.add(statusLabel); //add the status label to the mainPanel

            //add more vertical glue to maintain vertical centering
            mainPanel.add(Box.createVerticalGlue());
            
            //add an action listener to pinField to handle the Enter key press
            pinField.addActionListener(new ActionListener() {
            	
                public void actionPerformed(ActionEvent e) {
                	
                    String pin = pinField.getText();
                    if (pin.matches("\\d{4}")) { //regex to check if the input is exactly 4 digits
                        
                    	if (!pinExists(pin)) {
                    		
                            //add the initial user's PIN to the database
                            addUser(pin, FIRST_USER_NAME); //this adds a placeholder name, which will be replaced next
                            //proceed to the name setup
                            setupNameForFirstUser(pin);
                            
                        } else {
                        	
                            statusLabel.setText("PIN already exists! Try a different one.");
                        
                        }
                    	
                    } else {
                    	
                        statusLabel.setText("Please enter a 4-digit PIN.");
                    
                    }
                    
                    pinField.setText(""); //clear the PIN field
                
                }
                
            });

            frame.setLocationRelativeTo(null); //center the window on the screen

            //repaint and show the updated panel
            mainPanel.revalidate();
            mainPanel.repaint();
            frame.setVisible(true);
            
            //use SwingUtilities.invokeLater to request focus on pinField after the window is displayed and focused
            SwingUtilities.invokeLater(new Runnable() {
            	
                public void run() {
                	
                    pinField.requestFocusInWindow();
                    
                }
                
            });
            
        } else {
        	
            //if the first user is already setup, show the login page
            showLogin();
            
        }
        
    }

    //method to determine the pizza topping based on the checkbox selected
    private PizzaOrder.Topping getToppingFromCheckBox(Object source) {
        if (source == pepperoniCheckBox) return PizzaOrder.Topping.PEPPERONI;
        if (source == sausageCheckBox) return PizzaOrder.Topping.SAUSAGE;
        if (source == baconCheckBox) return PizzaOrder.Topping.BACON;
        if (source == onionCheckBox) return PizzaOrder.Topping.ONION;
        if (source == peppersCheckBox) return PizzaOrder.Topping.PEPPERS;
        if (source == mushroomsCheckBox) return PizzaOrder.Topping.MUSHROOMS;
        if (source == pineappleCheckBox) return PizzaOrder.Topping.PINEAPPLE;
        if (source == xtraCheeseCheckBox) return PizzaOrder.Topping.XTRA_CHEESE;
        return null;
    }

    //method to get the current user's name based on their PIN
    private String getCurrentUserName(String userPIN) {
    	
        String userName = "";
        
        try {
        	
            //query to select the user name based on the provided PIN
            String sql = "SELECT name FROM users WHERE pin = ?";          
            PreparedStatement stmt = conn.prepareStatement(sql);   
            stmt.setString(1, userPIN); //set the userPIN as the query parameter
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userName = rs.getString("name");
            }
            
        } catch (Exception e) {
        	
            e.printStackTrace();
            
        }
        
        return userName;
        
    }

    //method to show options popup
    private void showOptionsPopup(String pin) {
    	
        Object[] options = {"Settings Menu", "Program Exit"};
        
        //create a transparent/empty icon
        BufferedImage emptyIcon = new BufferedImage(57, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        ImageIcon transparentIcon = new ImageIcon(emptyIcon);
        
        //show option dialog with custom icon and options
        int choice = JOptionPane.showOptionDialog(frame, "", "Options", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, transparentIcon, options, options[0]);

        switch (choice) {
        
            case 0:
                showSettingsMenu(pin); //show settings menu
                break;
                
            case 1:
                System.exit(0);  //close the program
                break;
                
        }
        
    }

    //method to show settings menu
    private void showSettingsMenu(String pin) {
    	
        Object[] settingsOptions = {"Adjust User PINs", "Adjust Prices", "Adjust Sales Tax"};
        
        //create a transparent/empty icon
        BufferedImage emptyIcon = new BufferedImage(148, 1, BufferedImage.TYPE_INT_ARGB_PRE);
        ImageIcon transparentIcon = new ImageIcon(emptyIcon);
        
        //show option dialog with custom icon and settings options
        int choice = JOptionPane.showOptionDialog(frame, "", "Settings Menu", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, transparentIcon, settingsOptions, null); //set default option to null

        switch (choice) {
            case 0:
                adjustUserPINs(pin); //adjust user PINs
                break;
            case 1:
                adjustPrices(); //adjust prices
                break;
            case 2:
                adjustSalesTax(); //adjust sales tax
                break;
                
        }
        
    }
    
    //method to adjust user PINs
    private void adjustUserPINs(String importedPin) {
    	
        //create a dialog for adjusting user PINs
        JDialog dialog = new JDialog(frame, "Adjust User PINs", true);
        
        //create a JLabel with instructional text and set its alignment to center
        JLabel instructionLabel = new JLabel("Double click cell to edit:");
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);
        
        //add some space above and below the label
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); //10 pixels padding at top and bottom
        
        //create a custom table model that disables editing on double-click
        DefaultTableModel model = new DefaultTableModel(new Object[]{"User", "PIN"}, 0) {
            
        	@Override
            public boolean isCellEditable(int row, int column) {
                
        		//prevents any cell from being editable
                return false;
                
            }
        	
        };
        
        //populate the table model with user data from the database
        try {
        	
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM users");
            
            while (rs.next()) {
            	
                model.addRow(new Object[]{rs.getString("name"), rs.getString("pin")});
            
            }
            
        } catch (SQLException ex) {
        	
            ex.printStackTrace();
            
        }
        
        //set up the table and add it to a scroll pane
        JTable table = new JTable(model);
        
        //disable column reordering in the JTable
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scrollPane = new JScrollPane(table);

        //add a mouse listener to the table for enabling cell editing on mouse click
        table.addMouseListener(new MouseAdapter() {
        	
            @Override
            public void mouseClicked(MouseEvent e) {
            	
            	//check if the mouse event is a double click
                if (e.getClickCount() == 2) {

	                //get the row and column that was clicked in the table
	                int row = table.getSelectedRow();
	                int column = table.getSelectedColumn();
	
	                //block for handling username editing when the first column (username) is clicked
	                if (column == 0) {
	                	
	                    //retrieve current username from the table model
	                    String currentName = (String) model.getValueAt(row, 0);
	
	                    //show input dialog to get new username from user
	                    String newName = JOptionPane.showInputDialog(dialog, "Edit username:", currentName);
	
	                    //check if the new name is not null and different from the current name
	                    if (newName != null && !newName.equals(currentName)) {
	                    	
	                        //validate new username against regex pattern for correct format
	                        if (newName.matches("[A-Z][a-z]{0,24}")) {
	                            try {
	                            	
	                                //prepare SQL statement to update username in the database
	                                PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET name = ? WHERE name = ?");
	                                updateStmt.setString(1, newName);
	                                updateStmt.setString(2, currentName);
	                                updateStmt.executeUpdate();
	
	                                //update the table model with the new username
	                                model.setValueAt(newName, row, 0);
	
	                            } catch (SQLException ex) {
	                            	
	                                //handle SQL exceptions by printing stack trace
	                                ex.printStackTrace();
	                            
	                            }
	                            
	                        } else {
	                            
	                        	//show error message if new username does not meet the required format
	                            JOptionPane.showMessageDialog(dialog, "Invalid name! Must have no spaces, start with a capital letter and end with all lowercase, and be 25 or less letters.");
	                        
	                        }
	                    }
	                
	                }
                
	                //block for handling PIN editing when the second column (PIN) is clicked
	                else if (column == 1) {
	                	
	                    //retrieve current PIN from the table model
	                    String currentPin = (String) model.getValueAt(row, 1);
	
	                    //show input dialog to get new PIN from user
	                    String newPin = JOptionPane.showInputDialog(dialog, "Edit 4-digit PIN:", currentPin);
	
	                    //check if the new PIN is not null and different from the current PIN
	                    if (newPin != null && !newPin.equals(currentPin)) {
	                    	
	                        //validate new PIN against regex pattern for a 4-digit number
	                        if (newPin.matches("[0-9]{4}")) {
	                        	
	                            try {
	                            	
	                                //prepare SQL statement to update PIN in the database
	                                PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET pin = ? WHERE pin = ?");
	                                updateStmt.setString(1, newPin);
	                                updateStmt.setString(2, currentPin);
	                                updateStmt.executeUpdate();
	
	                                //update the table model with the new PIN
	                                model.setValueAt(newPin, row, 1);
	
	                            } catch (SQLException ex) {
	                            	
	                                //handle SQL exceptions by printing stack trace
	                                ex.printStackTrace();
	                                
	                            }
	                            
	                        } else {
	                        	
	                            //show error message if new PIN does not meet the required format
	                            JOptionPane.showMessageDialog(dialog, "Invalid PIN. It must be a 4-digit number.");
	                        
	                        }
	                    
	                    }
	                    
	                }
	                
                }
                
            }
            
        });

        //add a button to allow adding new users
        JButton addButton = new JButton("+ Add New User"); 
        addButton.addActionListener(e -> {
        	
            //prompt for new username input
            String username = JOptionPane.showInputDialog(dialog, "Enter username:");
            if (username == null || !username.matches("[A-Z][a-z]{0,24}")) {
            	
                //show error message if username is invalid
                JOptionPane.showMessageDialog(dialog, "Invalid name! Must have no spaces, start with a capital letter and end with all lowercase, and be 25 or less letters.");
                return;
                
            }

            //prompt for new user's PIN, validate it
            String pin = JOptionPane.showInputDialog(dialog, "Enter 4-digit PIN for " + username + ":");
            if (pin == null || !pin.matches("[0-9]{4}")) {
            	
                //show error message if PIN is invalid
                JOptionPane.showMessageDialog(dialog, "Invalid PIN. It must be a 4-digit number.");
                return;
                
            }

            //check if the username already exists in the database
            try {
            	
                PreparedStatement checkUserStmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
                checkUserStmt.setString(1, username);
                ResultSet rsUser = checkUserStmt.executeQuery();
                if (rsUser.next()) {
                	
                    //show error message if username already exists
                    JOptionPane.showMessageDialog(dialog, "A user with this name already exists. Please use a different name.");
                    return;
                    
                }

                //check if the entered PIN already exists in the database
                PreparedStatement checkPINStmt = conn.prepareStatement("SELECT * FROM users WHERE pin = ?");
                checkPINStmt.setString(1, pin);
                ResultSet rsPIN = checkPINStmt.executeQuery();
                if (rsPIN.next()) {
                	
                    //show error message if PIN already exists
                    JOptionPane.showMessageDialog(dialog, "This 4-digit PIN already exists. Please choose a different PIN.");
                    return;
                    
                }

                //insert new user into the database
                PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (name, pin) VALUES (?, ?)");
                insertStmt.setString(1, username);
                insertStmt.setString(2, pin);
                insertStmt.executeUpdate();

                //add new user to the table model
                model.addRow(new Object[]{username, pin});

            } catch (SQLException ex) {
            	
                //handle SQL exceptions and show error message
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error adding new user. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
            
            }
            
        });

        //add a button to enable deletion of users
        JButton deleteButton = new JButton("- Delete User");
        deleteButton.addActionListener(e -> {
            
            //identify the selected user for deletion
            int row = table.getSelectedRow();
            if (row == -1) {
                
                //show error message if no user is selected
                JOptionPane.showMessageDialog(dialog, "No user selected for deletion.");
                return;
                
            }

            //get the PIN of the selected user
            String selectedUserPin = (String) model.getValueAt(row, 1);
            
            //check if the selected PIN matches the imported PIN
            if(selectedUserPin.equals(importedPin)) {
                //show error message if the PIN matches the imported PIN
                JOptionPane.showMessageDialog(dialog, "Cannot delete user that is currently logged in.");
                return;
            }

            //delete the selected user from the database
            try {
            	
                //count the number of users in the database to ensure at least one remains
                Statement countStmt = conn.createStatement();
                ResultSet countRs = countStmt.executeQuery("SELECT COUNT(*) AS userCount FROM users");
                countRs.next();
                
                //prevent deletion if it is the last user
                if (countRs.getInt("userCount") <= 1) {
                	
                    JOptionPane.showMessageDialog(dialog, "Cannot delete the last user. Must have minimum of 1 user.");
                    return;
                    
                }

                //perform the deletion of the selected user
                String name = (String) model.getValueAt(row, 0);
                conn.setAutoCommit(false);
                PreparedStatement pstmt = conn.prepareStatement("DELETE FROM users WHERE name = ?");
                pstmt.setString(1, name);
                pstmt.executeUpdate();
                model.removeRow(row);
                conn.commit();
                
            } catch (SQLException ex) {
            	
                //handle exceptions during deletion and rollback if necessary
                try {
                	
                    conn.rollback();
                    
                } catch (SQLException rollbackEx) {
                	
                    rollbackEx.printStackTrace();
                    
                }
                
                ex.printStackTrace();
                
                JOptionPane.showMessageDialog(dialog, "Error deleting user. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
           
            } finally {
            	
                //ensure that autocommit is enabled again after operation
            	
                try {
                	
                    conn.setAutoCommit(true);
                    
                } catch (SQLException autoCommitEx) {
                	
                    autoCommitEx.printStackTrace();
                    
                }
                
            }
            
        });

        //add a button to save changes made to the user data
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> {
        	
            try {
            	
                //start a transaction
                conn.setAutoCommit(false);

                //iterate through each user in the table model to save changes
                for (int i = 0; i < model.getRowCount(); i++) {
                    String name = (String) model.getValueAt(i, 0);
                    String pin = (String) model.getValueAt(i, 1);

                    //validate the PIN for each user
                    if (!pin.matches("[0-9]{4}")) {
                    	
                        JOptionPane.showMessageDialog(dialog, "PIN for user " + name + " is not a valid 4-digit number.", "Invalid PIN", JOptionPane.ERROR_MESSAGE);
                        return;  //stop processing further changes
                    
                    }

                    //update user PINs in the database, ensuring no duplicate PINs
                    PreparedStatement checkPINStmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE pin = ? AND name != ?");
                    checkPINStmt.setString(1, pin);
                    checkPINStmt.setString(2, name);
                    ResultSet rs = checkPINStmt.executeQuery();
                    rs.next();
                    if (rs.getInt(1) > 0) {
                    	
                        JOptionPane.showMessageDialog(dialog, "PIN already exists for another user. Please use a different PIN for user " + name + ".", "Duplicate PIN", JOptionPane.ERROR_MESSAGE);
                        return;  //stop processing further changes
                        
                    }
                    
                    PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET pin = ? WHERE name = ?");
                    updateStmt.setString(1, pin);
                    updateStmt.setString(2, name);
                    updateStmt.executeUpdate();
                    
                }

                //commit the transaction
                conn.commit();

                //notify user of successful save operation
                JOptionPane.showMessageDialog(dialog, "Changes saved successfully!");

            } catch (SQLException ex) {
            	
                try {
                	
                    //roll back the transaction in case of error
                    conn.rollback();
                    
                } catch (SQLException rollbackEx) {
                	
                    rollbackEx.printStackTrace();
                    
                }
                
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error occurred while updating user PINs.", "Database Error", JOptionPane.ERROR_MESSAGE);
            
            } finally {
            	
                try {
                	
                    //ensure auto-commit is re-enabled
                    conn.setAutoCommit(true);
                    
                } catch (SQLException autoCommitEx) {
                	
                    autoCommitEx.printStackTrace();
                    
                }
                
            }
            
        });

        //configure dialog layout and display it
        dialog.setLayout(new BorderLayout());
        dialog.add(instructionLabel, BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        JPanel southPanel = new JPanel();
        southPanel.add(addButton);
        southPanel.add(deleteButton);
        southPanel.add(saveButton);
        dialog.add(southPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        
    }

    //method to save price changes to the database
    private boolean savePriceChanges(DefaultTableModel model, String tableName) throws SQLException {
        
    	boolean success = true;
        conn.setAutoCommit(false);  //start transaction management
        
        try {
        	
            for (int i = 0; i < model.getRowCount(); i++) {
            	
                String size = (String) model.getValueAt(i, 0);
                double price = Double.parseDouble(model.getValueAt(i, 1).toString());
                PreparedStatement pstmt = conn.prepareStatement("UPDATE " + tableName + " SET price = ? WHERE size = ?");
                pstmt.setDouble(1, price);
                pstmt.setString(2, size);
                pstmt.executeUpdate();
                
            }
            
            conn.commit();  //commit the changes
            
        } catch (SQLException e) {
        	
            success = false;
            conn.rollback();  //roll back in case of error
            e.printStackTrace();
            
        } finally {
        	
            conn.setAutoCommit(true);  //reset to default behavior
            
        }
        
        return success;
        
    }

    //method to add a price editing listener to a table
    private void addPriceEditListener(JTable table, DefaultTableModel model) {
    	
        //add a mouse listener to handle clicks on the table
        table.addMouseListener(new MouseAdapter() {
        	
            public void mouseClicked(MouseEvent e) {
            	
                //determine the row and column where the click occurred
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                //check if the click is on the price column
                if (row >= 0 && col == 1) {
                	
                    //retrieve the current size and price
                    String size = model.getValueAt(row, 0).toString();
                    String currentPrice = model.getValueAt(row, 1).toString();
                    
                    //prompt the user to input a new price
                    String newPrice = JOptionPane.showInputDialog(frame, "Edit price for " + size + ":", currentPrice);

                    //validate the new price format and update the model
                    if (newPrice != null && newPrice.matches("\\d+(\\.\\d{1,2})?")) {
                    	
                        model.setValueAt(String.format("%.2f", Double.parseDouble(newPrice)), row, col);
                    
                    } else if (newPrice != null) {
                    	
                        JOptionPane.showMessageDialog(frame, "Invalid price format. Please enter a number with up to two decimal places.");
                    
                    }
                    
                }
                
            }
            
        });
        
    }

    //method to adjust prices of items such as pizza, toppings, and Coke
    private void adjustPrices() {
    	
        //create a dialog window titled "Adjust Prices"
        JDialog dialog = new JDialog(frame, "Adjust Prices", true);
        
        //create a JLabel with instructional text and set its alignment to center
        JLabel instructionLabel = new JLabel("Single click price to edit:");
        instructionLabel.setHorizontalAlignment(JLabel.CENTER);
        
        //add some space above and below the label
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); //10 pixels padding at top and bottom
        
        //create a JLabel with update info text and set its alignment to center
        JLabel updateInfoLabel = new JLabel("Must start a new order for all changes to take effect.");
        updateInfoLabel.setHorizontalAlignment(JLabel.CENTER);
        
        //add some space below the label
        updateInfoLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0)); //10 pixels padding at bottom
        
        //create a tabbed pane to organize different item categories
        JTabbedPane tabbedPane = new JTabbedPane();
        
        //define a table model for pizza prices with "Size" and "Price" columns
        DefaultTableModel pizzaModel = new DefaultTableModel(new Object[]{"Size", "Price"}, 0) {
            
        	@Override
            public boolean isCellEditable(int row, int column) {
            	
                return false; //prevents any cell from being directly editable
            
            }
            
        };
        
        //populate the table model with pizza price data from the database
        try {
        	
        	//SQL query to order by size explicitly
        	String pizzaPriceQuery = "SELECT DISTINCT size, price FROM pizzaPrices ORDER BY CASE size WHEN 'Small' THEN 1 WHEN 'Medium' THEN 2 WHEN 'Large' THEN 3 END";
        	
            //create and execute SQL statement to select distinct sizes and prices of pizza
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(pizzaPriceQuery);
            while (rs.next()) {
            	
                //format retrieved price value to two decimal places for consistency
                String formattedPrice = String.format("%.2f", rs.getDouble("price"));
                
                //add size and formatted price as a row in the pizzaModel
                pizzaModel.addRow(new Object[]{rs.getString("size"), formattedPrice});
                
            }
            
        } catch (SQLException ex) {
        	
            //print stack trace if there is an SQL exception
            ex.printStackTrace();
            
        }
        
        //create a table with the pizzaModel and add it to a scroll pane
        JTable pizzaTable = new JTable(pizzaModel);
        pizzaTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane pizzaScrollPane = new JScrollPane(pizzaTable);
        
        //add the scroll pane containing the pizza table to the tabbed pane
        tabbedPane.addTab("Pizza", pizzaScrollPane);
        
        //similar setup for the topping prices with its own table model
        DefaultTableModel toppingModel = new DefaultTableModel(new Object[]{"Topping", "Price"}, 0) {
            
        	@Override
            
        	public boolean isCellEditable(int row, int column) {
        		
                return false; //prevents any cell from being directly editable
            
        	}
        	
        };
        
        //populate the toppingModel with topping price data from the database
        try {
        	
        	//SQL query to order by size explicitly
        	String toppingPriceQuery = "SELECT DISTINCT size, price FROM toppingPrices ORDER BY CASE size WHEN 'Small' THEN 1 WHEN 'Medium' THEN 2 WHEN 'Large' THEN 3 END";
        	
            //select distinct sizes and prices of toppings
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(toppingPriceQuery);
            while (rs.next()) {
            	
                String formattedPrice = String.format("%.2f", rs.getDouble("price"));
                toppingModel.addRow(new Object[]{rs.getString("size"), formattedPrice});
           
            }
            
        } catch (SQLException ex) {
        	
            ex.printStackTrace();
            
        }
        
        //create a table for topping prices and add it to the tabbed pane
        JTable toppingTable = new JTable(toppingModel);
        toppingTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane toppingScrollPane = new JScrollPane(toppingTable);
        tabbedPane.addTab("Toppings", toppingScrollPane);
        
        //similar setup for Coke prices
        DefaultTableModel cokeModel = new DefaultTableModel(new Object[]{"Size", "Price"}, 0) {
            
        	@Override
            public boolean isCellEditable(int row, int column) {
        		
                return false; //prevents any cell from being directly editable
                
            }
        	
        };
        
        //populate the cokeModel with Coke price data from the database
        try {
        	
            //select all records from cokePrices table
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM cokePrices");
            while (rs.next()) {
            	
                String formattedPrice = String.format("%.2f", rs.getDouble("price"));
                cokeModel.addRow(new Object[]{rs.getString("size"), formattedPrice});
            
            }
            
        } catch (SQLException ex) {
        	
            ex.printStackTrace();
            
        }
        
        //create a table for Coke prices and add it to the tabbed pane
        JTable cokeTable = new JTable(cokeModel);
        cokeTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane cokeScrollPane = new JScrollPane(cokeTable);
        tabbedPane.addTab("Coke", cokeScrollPane);
        
        //add mouse listeners to the tables to enable interactive price editing
        addPriceEditListener(pizzaTable, pizzaModel);
        addPriceEditListener(toppingTable, toppingModel);
        addPriceEditListener(cokeTable, cokeModel);

        //create a save button to save the changed prices
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> {
        	
            boolean success = true;
            
            //start a transaction
            try {
            	
                conn.setAutoCommit(false);

                //attempt to save the price changes for each item category
                success = savePriceChanges(pizzaModel, "pizzaPrices") &&
                          savePriceChanges(toppingModel, "toppingPrices") &&
                          savePriceChanges(cokeModel, "cokePrices");

                //commit the transaction
                conn.commit();

                //dialog box indicating whether the price update was successful
                if (success) {
                	
                    JOptionPane.showMessageDialog(dialog, "Prices updated successfully!");
                
                } else {
                	
                    JOptionPane.showMessageDialog(dialog, "Failed to update some prices. Check logs for details.", "Error", JOptionPane.ERROR_MESSAGE);
                
                }

            } catch (SQLException ex) {
            	
                try {
                	
                    //roll back the transaction in case of error
                    conn.rollback();
                    
                } catch (SQLException rollbackEx) {
                	
                    rollbackEx.printStackTrace();
                    
                }
                
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Error occurred while updating prices.", "Database Error", JOptionPane.ERROR_MESSAGE);
                success = false;
                
            } finally {
            	
                try {
                	
                    //ensure auto-commit is re-enabled
                    conn.setAutoCommit(true);
                    
                } catch (SQLException autoCommitEx) {
                	
                    autoCommitEx.printStackTrace();
                    
                }
                
            }
            
        });

        //add components to the dialog and configure its properties
        dialog.setLayout(new BorderLayout());
        dialog.add(instructionLabel, BorderLayout.NORTH);
        dialog.add(tabbedPane, BorderLayout.CENTER);
   
        //create a panel for holding both the label and the button
        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BorderLayout());

        //add the updateInfoLabel and saveButton to the southPanel
        southPanel.add(updateInfoLabel, BorderLayout.CENTER); //add the label to the center
        southPanel.add(saveButton, BorderLayout.SOUTH); //add the button to the south

        //add the southPanel to the dialog
        dialog.add(southPanel, BorderLayout.SOUTH);
        
        dialog.pack(); //adjust dialog size to fit its contents
        dialog.setLocationRelativeTo(frame); //center dialog relative to the main application window
        dialog.setVisible(true); //make the dialog visible
        
    }

    //method to adjust sales tax settings
    private void adjustSalesTax() {
    	
        //create a dialog for adjusting sales tax
        JDialog dialog = new JDialog(frame, "Adjust Sales Tax", true);
        
        //label for the sales tax field
        JLabel salesTaxLabel = new JLabel("Sales Tax (%)");
        
        //text field for entering sales tax value
        JTextField salesTaxField = new JTextField(10);
        
        //retrieve current sales tax value from the database
        try {
        	
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT value FROM salesTax");
            if (rs.next()) {
            	
                //convert the decimal value to a percentage for display
                double salesTaxValue = rs.getDouble("value") * 100;
                salesTaxField.setText(String.format("%.2f", salesTaxValue));
                
            }
            
        } catch (SQLException ex) {
        	
            //handle SQL exceptions by printing stack trace
            ex.printStackTrace();
            
        }
        
        //button to save the changes made to sales tax
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(e -> {
        	
            try {
            	
                double newSalesTaxPercentage = Double.parseDouble(salesTaxField.getText());
                double newSalesTax = newSalesTaxPercentage / 100;

                conn.setAutoCommit(false);  //disable auto-commit for transaction management
                PreparedStatement pstmt = conn.prepareStatement("UPDATE salesTax SET value = ?");
                pstmt.setDouble(1, newSalesTax);
                pstmt.executeUpdate();
                conn.commit();  //commit changes

                JOptionPane.showMessageDialog(dialog, "Sales tax updated successfully!");
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid number for sales tax.");
            } catch (SQLException ex) {
            	
                try {
                	
                    conn.rollback();  //rollback changes on exception
                    
                } catch (SQLException rollbackEx) {
                	
                    rollbackEx.printStackTrace();
                    
                }
                
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog, "Failed to update sales tax.");
                
            } finally {
            	
                try {
                	
                    conn.setAutoCommit(true);  //re-enable auto-commit
                    
                } catch (SQLException autoCommitEx) {
                	
                    autoCommitEx.printStackTrace();
                    
                }
                
            }
            
        });

        
        //create a panel and add the label, text field, and save button
        JPanel panel = new JPanel();
        panel.add(salesTaxLabel);
        panel.add(salesTaxField);
        panel.add(saveButton);
        
        //add the panel to the dialog, pack, and display it
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        
    }

	//a helper method to reset the selections of toppings and size
	private void resetSelections(JComboBox<String> sizeBox, JCheckBox... checkBoxes) {
		
	    sizeBox.setSelectedIndex(0); //reset to default size
	    
	    for (JCheckBox checkBox : checkBoxes) {
	    	
	        checkBox.setSelected(false); //uncheck all toppings
	        
	    }
	    
	}

	//method to update the GUI display with current pizza orders from the linked list
	private void printOrderListToGUI() {
		
	    //clear the existing elements in the pizza list model to refresh the list
	    pizzaListModel.clear();
	    
	    //start with the first node in the linked list of pizza orders
	    Node currentNode = pizzaOrderList.getFirst();
	    
	    //loop through each node in the linked list
	    while (currentNode != null) {
	    	
	        //retrieve the pizza order from the current node
	        PizzaOrder order = currentNode.getOrder();
	        
	        //format and add the pizza order's string representation and its total price to the list model
	        pizzaListModel.addElement(order.toString() + " - $" + String.format("%.2f", order.calculateTotal()));
	        
	        //move to the next node in the linked list
	        currentNode = currentNode.getNext();
	        
	    }
	    
	}

	//utility method to create a clone of a PizzaOrder object
	private PizzaOrder clonePizzaOrder(PizzaOrder original) {
		
	    //create a new PizzaOrder instance with the same size and database connection as the original
	    PizzaOrder cloned = new PizzaOrder(original.getSize(), original.conn);
	    
	    //add all toppings from the original pizza order to the cloned instance
	    for (PizzaOrder.Topping topping : original.getToppings()) {
	    	
	        cloned.addTopping(topping);
	        
	    }
	    
	    //return the cloned pizza order
	    return cloned;
	    
	}

	//method to calculate and return the total price of the entire order
	private double updateTotalOrderPrice() {
		
	    //calculate the total price of all pizza orders
	    double totalOrderPrice = pizzaOrderList.calculateTotalOrderPrice();
	    
	    //calculate the total price of all coke products in the order
	    double totalCokePrice = coke.getTotalPrice();
	    
	    //calculate and return the sum of both pizza and coke prices
	    double completeOrderPrice = totalOrderPrice + totalCokePrice;
	    return completeOrderPrice;
	    
	}

	//method to retrieve the sales tax value from the database
	private double getSalesTaxValueFromDB() {
		
	    //initialize sales tax value to default of 0.0
	    double salesTaxValue = 0.0;
	    
	    //SQL query to select the sales tax value from the database where id = 1
	    String sql = "SELECT value FROM salesTax WHERE id = 1";
	    
	    //try statement to automatically close the PreparedStatement
	    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
	    	
	        ResultSet rs = pstmt.executeQuery();
	        
	        //if a record is found, set the sales tax value from the database
	        if (rs.next()) {
	        	
	            salesTaxValue = rs.getDouble("value");
	            
	        }
	        
	    } catch (SQLException e) {
	    	
	        //print stack trace in case of SQL exception
	        e.printStackTrace();
	        
	    }
	    
	    //return the retrieved or default sales tax value
	    return salesTaxValue;
	    
	}
	
	//method to reset the UI elements to their default states for a new pizza order
	private void resetNewOrderScreen(String pin, PizzaPOS pos) {
		
	    //clear the linked list that holds the current pizza orders
	    pizzaOrderList.clear();

	    //reset the state of all topping checkboxes to unchecked (false)
	    pepperoniCheckBox.setSelected(false);
	    sausageCheckBox.setSelected(false);
	    baconCheckBox.setSelected(false);
	    onionCheckBox.setSelected(false);
	    peppersCheckBox.setSelected(false);
	    mushroomsCheckBox.setSelected(false);
	    pineappleCheckBox.setSelected(false);
	    xtraCheeseCheckBox.setSelected(false);

	    //create a new JComboBox for pizza sizes and set the default selection to the first item
	    JComboBox<String> sizeBox = new JComboBox<>(new String[]{"Small", "Medium", "Large"});
	    sizeBox.setSelectedIndex(0);

	    //initialize a spinner for selecting Coke quantity and set its default value to 0
	    JSpinner cokeQuantitySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
	    cokeQuantitySpinner.setValue(0);

	    //clear the JList model that displays the list of pizza orders in the UI
	    pizzaListModel.clear();

	    //initialize JLabels for displaying the total prices and set them to their default text
	    JLabel totalOrderPriceLabel = new JLabel("Subtotal: $0.00");
	    JLabel totalCokePriceLabel = new JLabel("Total Coke: $0.00");
	    JLabel totalPriceLabel = new JLabel("Current Pizza Total: $0.00");
	    JLabel salesTaxLabel = new JLabel("Sales Tax: $0.00");
	    JLabel grandTotalPriceLabel = new JLabel("Grand Total: $0.00");

	    //refresh the UI components to reflect these changes
	    showNewOrder(pin, pos);
	    
	}
	
	//method to convert the Size enum to a corresponding string
	private String sizeEnumToString(PizzaOrder.Size size) {
		
	    //switch statement to handle different cases of the Size enum
	    switch (size) {
	    
	        case SMALL: 
	            return "Small"; //returns the string "Small" when the enum is SMALL
	            
	        case MEDIUM: 
	            return "Medium"; //returns the string "Medium" when the enum is MEDIUM
	            
	        case LARGE: 
	            return "Large"; //returns the string "Large" when the enum is LARGE
	            
	        default: 
	            return "Unknown"; //returns "Unknown" for any other values, which serves as a fallback
	    
	    }
	    
	}

	//method to load pizza order details into the UI components
	private void loadPizzaDetailsIntoUI(PizzaOrder order, JComboBox<String> sizeBox, JCheckBox... checkBoxes) {
	    
	    //set the size in the sizeBox based on the pizza order's size
	    String sizeString = sizeEnumToString(order.getSize());
	    sizeBox.setSelectedItem(sizeString);

	    //retrieve the toppings from the pizza order
	    List<PizzaPOS.PizzaOrder.Topping> toppings = order.getToppings();

	    //set each topping checkbox according to whether it is included in the pizza order
	    pepperoniCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.PEPPERONI));
	    sausageCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.SAUSAGE));
	    baconCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.BACON));
	    onionCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.ONION));
	    peppersCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.PEPPERS));
	    mushroomsCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.MUSHROOMS));
	    pineappleCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.PINEAPPLE));
	    xtraCheeseCheckBox.setSelected(toppings.contains(PizzaOrder.Topping.XTRA_CHEESE));
	    
	}
	
	//method to update a PizzaOrder object based on the UI selections
	private void updatePizzaOrderFromUI(
	        PizzaOrder order,
	        JComboBox<String> sizeBox,
	        JCheckBox pepperoniCheckBox,
	        JCheckBox sausageCheckBox,
	        JCheckBox baconCheckBox,
	        JCheckBox onionCheckBox,
	        JCheckBox peppersCheckBox,
	        JCheckBox mushroomsCheckBox,
	        JCheckBox pineappleCheckBox,
	        JCheckBox xtraCheeseCheckBox) {
		
		//update the size of the pizza order based on the selected item in the sizeBox
	    String selectedSize = (String) sizeBox.getSelectedItem();
	    order.setSize(PizzaPOS.PizzaOrder.Size.fromString(selectedSize));
	    
	    //create a list to store selected toppings
	    List<PizzaOrder.Topping> toppings = new ArrayList<>();
	    
	    //add each selected topping to the list based on the state of its corresponding checkbox
	    if (pepperoniCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.PEPPERONI);
	    if (sausageCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.SAUSAGE);
	    if (baconCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.BACON);
	    if (onionCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.ONION);
	    if (peppersCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.PEPPERS);
	    if (mushroomsCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.MUSHROOMS);
	    if (pineappleCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.PINEAPPLE);
	    if (xtraCheeseCheckBox.isSelected()) toppings.add(PizzaOrder.Topping.XTRA_CHEESE);
	    
	    //set the pizza order's toppings to the updated list
	    order.setToppings(toppings);
	    
	}

	//method to process a new order
	private void showNewOrder(String pin, PizzaPOS pos) {
		
	    //check if pizzaOrderList is uninitialized and initialize it
	    if (pizzaOrderList == null) { 
	    	
	        pizzaOrderList = new PizzaTotalOrderLinkedList();
	        
	    }

	    //remove all existing components from mainPanel to refresh UI
	    mainPanel.removeAll();

	    //create a new Coke2L object to represent a Coke product
	    coke = new Coke2L(); 

	    //instantiate a new list model for pizza items
	    pizzaListModel = new DefaultListModel<>(); 

	    //create a JList to display pizza items using the model
	    pizzaList = new JList<>(pizzaListModel); 

	    //embed pizzaList in a scroll pane
	    JScrollPane listScrollPane = new JScrollPane(pizzaList); 

	    //set preferred size for scroll pane
	    listScrollPane.setPreferredSize(new Dimension(420, listScrollPane.getPreferredSize().height)); 

	    //label to display total order price
	    JLabel totalOrderPriceLabel = new JLabel("Subtotal: $0.00"); 

	    //array holding current pizza order
	    final PizzaOrder[] currentOrder = {new PizzaOrder(this.conn)}; 

	    //dropdown for selecting pizza size
	    JComboBox<String> sizeBox = new JComboBox<>(new String[]{"Small", "Medium", "Large"}); 

	    //create checkboxes for various pizza toppings
	    pepperoniCheckBox = new JCheckBox("Pepperoni");
	    sausageCheckBox = new JCheckBox("Sausage");
	    baconCheckBox = new JCheckBox("Bacon");
	    onionCheckBox = new JCheckBox("Onion");
	    peppersCheckBox = new JCheckBox("Green Peppers");
	    mushroomsCheckBox = new JCheckBox("Mushrooms");
	    pineappleCheckBox = new JCheckBox("Pineapple");
	    xtraCheeseCheckBox = new JCheckBox("Xtra Cheese");
	    
	    //label showing total price for Coke
	    JLabel totalCokePriceLabel = new JLabel("Total Coke: $" + String.format("%.2f", coke.getTotalPrice())); 

	    //label for displaying total price of current pizza
	    JLabel totalPriceLabel = new JLabel("Current Pizza Total: $" + String.format("%.2f", currentOrder[0].calculateTotal())); 

	    //label for displaying total sales tax
	    JLabel salesTaxLabel = new JLabel("Sales Tax: $0.00"); 

	    //label for displaying grand total price
	    JLabel grandTotalPriceLabel = new JLabel("Grand Total: $0.00");

	    //retrieve current user's name based on pin
	    String userName = getCurrentUserName(pin); 

	    //create a label to display the user's name
	    JLabel userNameLabel = new JLabel(userName); 

	    //align the userNameLabel to the right
	    userNameLabel.setHorizontalAlignment(SwingConstants.RIGHT); 

	    //create a logout button for user to exit
	    JButton logoutButton = new JButton("Logout"); 

	    //attach action listener to logout button
	    logoutButton.addActionListener(new ActionListener() { 
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	        	//create a confirmation dialog for voiding the order
	            JDialog confirmDialog = new JDialog(frame, "Void Order", true);
	            confirmDialog.setLayout(new BorderLayout());
	            
	            //create a label for the dialog with centered text and top padding
	            JLabel label = new JLabel("Logout voids order. Continue?", JLabel.CENTER);
	            label.setBorder(BorderFactory.createEmptyBorder(10, 4, 0, 4));
	            confirmDialog.add(label, BorderLayout.CENTER);

	            //create a panel to hold Yes and No buttons
	            JPanel buttonPanel = new JPanel();
	            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	            JButton yesButton = new JButton("Yes");
	            JButton noButton = new JButton("No");
	            
	            buttonPanel.add(yesButton); //add Yes button to the panel
	            buttonPanel.add(noButton); //add No button to the panel

	            confirmDialog.add(buttonPanel, BorderLayout.SOUTH); //add the button panel to the bottom of the dialog

	            //add functionality to Yes button for voiding the order
	            yesButton.addActionListener(yesEvent -> {
	            	
	                //clear the current pizza order and reset all related UI components
	                pizzaOrderList.clear();
	                currentOrder[0] = new PizzaOrder(conn);
	                resetNewOrderScreen(pin, pos);	                
	                pizzaListModel.clear();
	                totalPriceLabel.setText("Total: $0.00"); //reset the total price label
	                totalOrderPriceLabel.setText("Subtotal: $0.00"); //reset the total order price label
	                totalCokePriceLabel.setText("Total Coke: $0.00"); //reset the total Coke price label
	                salesTaxLabel.setText("Sales Tax: $0.00"); //reset the sales tax label
	                grandTotalPriceLabel.setText("Grand Total: $0.00"); //reset the grand total label
	                printOrderListToGUI(); //refresh the GUI with updated order list
	                confirmDialog.dispose(); //close the confirmation dialog
	                
	            });
	            
	            //add functionality to close the dialog without voiding the order when No is clicked
	            noButton.addActionListener(noEvent -> confirmDialog.dispose());

	            //prepare and show the dialog
	            confirmDialog.pack(); //set the size of the dialog to fit its contents
	            confirmDialog.setLocationRelativeTo(frame); //center the dialog relative to the frame
	            confirmDialog.setVisible(true); //make the dialog visible
	        	
	            //on button click, invoke method to display login screen
	            showLogin();
	            
	        }
	        
	    });

	    //create an options button for additional settings
	    JButton optionsButton = new JButton("Options"); 

	    //add action listener to options button
	    optionsButton.addActionListener(new ActionListener() { 
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            //on click, display options popup
	            showOptionsPopup(pin);
	            
	        }
	        
	    });

	    //button to add selected pizza to order
	    JButton addToOrderButton = new JButton("Add Current Pizza to Order");  

	    //format for displaying date and time
	    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy, HH:mm:ss"); 

	    //get current date and time
	    Date date = new Date(); 

	    //label to display current date and time
	    JLabel dateTimeLabel = new JLabel(formatter.format(date)); 
	    
	    //create the "Void Order" button
	    JButton voidOrderButton = new JButton("Void Order");
	    voidOrderButton.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            //create a confirmation dialog for voiding the order
	            JDialog confirmDialog = new JDialog(frame, "Void Order", true);
	            confirmDialog.setLayout(new BorderLayout());
	            
	            //create a label for the dialog with centered text and top padding
	            JLabel label = new JLabel("Void order?", JLabel.CENTER);
	            label.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0)); //add 10 pixels space at the top for aesthetics
	            confirmDialog.add(label, BorderLayout.CENTER);

	            //create a panel to hold Yes and No buttons
	            JPanel buttonPanel = new JPanel();
	            buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	            JButton yesButton = new JButton("Yes");
	            JButton noButton = new JButton("No");
	            
	            buttonPanel.add(yesButton); //add Yes button to the panel
	            buttonPanel.add(noButton); //add No button to the panel

	            confirmDialog.add(buttonPanel, BorderLayout.SOUTH); //add the button panel to the bottom of the dialog

	            //add functionality to Yes button for voiding the order
	            yesButton.addActionListener(yesEvent -> {
	            	
	                //clear the current pizza order and reset all related UI components
	                pizzaOrderList.clear();
	                currentOrder[0] = new PizzaOrder(conn);
	                resetNewOrderScreen(pin, pos);	                
	                pizzaListModel.clear();
	                totalPriceLabel.setText("Total: $0.00"); //reset the total price label
	                totalOrderPriceLabel.setText("Subtotal: $0.00"); //reset the total order price label
	                totalCokePriceLabel.setText("Total Coke: $0.00"); //reset the total Coke price label
	                salesTaxLabel.setText("Sales Tax: $0.00"); //reset the sales tax label
	                grandTotalPriceLabel.setText("Grand Total: $0.00"); //reset the grand total label
	                printOrderListToGUI(); //refresh the GUI with updated order list
	                confirmDialog.dispose(); //close the confirmation dialog
	                
	            });
	            
	            //add functionality to close the dialog without voiding the order when No is clicked
	            noButton.addActionListener(noEvent -> confirmDialog.dispose());

	            //prepare and show the dialog
	            confirmDialog.pack(); //set the size of the dialog to fit its contents
	            confirmDialog.setLocationRelativeTo(frame); //center the dialog relative to the frame
	            confirmDialog.setVisible(true); //make the dialog visible
	            
	        }
	        
	    });

	    //create the "Submit Order" button
	    JButton finalizeOrderButton = new JButton("Submit Order");

	    finalizeOrderButton.setEnabled(false); //disable the button by default as there is no order initially

	    //create a spinner for selecting the quantity of Coke 2L
	    JSpinner cokeQuantitySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)); //set initial, minimum, maximum, and step values

	    //add a change listener to update the order details when the quantity of Coke changes
	    cokeQuantitySpinner.addChangeListener(e -> {
	    	
	        int quantity = (Integer) cokeQuantitySpinner.getValue(); //get the current quantity from the spinner
	        coke.setQuantity(quantity); //update the quantity of Coke in the order
	        double totalCokePrice = coke.getTotalPrice(); //calculate the total price for Coke
	        totalCokePriceLabel.setText("Total Coke: $" + String.format("%.2f", totalCokePrice)); //update the Coke price label

	        //recalculate and update the total order price and related labels
	        double totalOrderPrice = updateTotalOrderPrice(); //calculate the total order price
	        double salesTaxRate = getSalesTaxValueFromDB(); //fetch the sales tax rate from the database
	        double totalSalesTax = totalOrderPrice * salesTaxRate; //calculate the total sales tax
	        totalOrderPriceLabel.setText("Subtotal: $" + String.format("%.2f", totalOrderPrice)); //update the total order label
	        salesTaxLabel.setText("Sales Tax: $" + String.format("%.2f", totalSalesTax)); //update the sales tax label
	        grandTotalPriceLabel.setText("Grand Total: $" + String.format("%.2f", totalOrderPrice + totalSalesTax)); //update the grand total label

	        //enable or disable the finalize order button based on the total order price
	        if(totalOrderPrice > 0) {
	        	
	            finalizeOrderButton.setEnabled(true); //enable if the order has a price
	        
	        }
	        
	        if(totalOrderPrice == 0) {
	        	
	            finalizeOrderButton.setEnabled(false); //disable if the order is empty
	        
	        }
	        
	    });

	    //create a common listener for all components that affect the pizza's price
	    ItemListener updatePriceListener = e -> {
	    	
	        if (e.getSource() == sizeBox) {
	        	
	            String selectedSize = (String) sizeBox.getSelectedItem(); //get the selected pizza size
	            switch (selectedSize) { //update the pizza size in the current order based on selection
	            
	                case "Small":
	                    currentOrder[0].setSize(PizzaOrder.Size.SMALL);
	                    break;
	                case "Medium":
	                    currentOrder[0].setSize(PizzaOrder.Size.MEDIUM);
	                    break;
	                case "Large":
	                    currentOrder[0].setSize(PizzaOrder.Size.LARGE);
	                    break;
	                    
	            }
	            
	        } else {
	        	
	            //identify the topping selected or deselected and update the current order accordingly
	            PizzaOrder.Topping selectedTopping = getToppingFromCheckBox(e.getSource());
	            if (e.getStateChange() == ItemEvent.SELECTED) {
	            	
	                if (selectedTopping != null) currentOrder[0].addTopping(selectedTopping); //add the topping if selected
	            
	            } else {
	            	
	                if (selectedTopping != null) currentOrder[0].removeTopping(selectedTopping); //remove the topping if deselected
	            
	            }
	            
	        }
	        
	        //update the total price label for the current pizza
	        totalPriceLabel.setText("Total: $" + String.format("%.2f", currentOrder[0].calculateTotal()));
	    
	    };

        
	    //define a flag to indicate if the application is in edit mode and the index of the pizza being edited
	    boolean[] isEditMode = {false};
	    int[] editingIndex = {-1};

	    //define the "Save Changes" button but keep it hidden initially
	    JButton saveChangesButton = new JButton("Save Changes");
	    saveChangesButton.setVisible(false); //button is initially not visible

	    //define the "Edit Selected Pizza" button and its action
	    JButton editSelectedPizzaButton = new JButton("Edit Selected Pizza");
	    editSelectedPizzaButton.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            //get the index of the selected pizza from the pizza list
	            int selectedIndex = pizzaList.getSelectedIndex();
	            if (selectedIndex != -1) {
	            	
	                //if a pizza is selected, enable edit mode and store the index of the pizza
	                isEditMode[0] = true;
	                editingIndex[0] = selectedIndex;
	                
	                //load the selected pizza order for editing
	                currentOrder[0] = pizzaOrderList.get(selectedIndex);

	                //update the UI components to show the details of the pizza being edited
	                loadPizzaDetailsIntoUI(currentOrder[0], sizeBox, pepperoniCheckBox, sausageCheckBox, baconCheckBox, onionCheckBox, peppersCheckBox, mushroomsCheckBox, pineappleCheckBox, xtraCheeseCheckBox); //load details into UI components

	                //make the "Save Changes" button visible and hide the "Add to Order" button
	                saveChangesButton.setVisible(true);
	                addToOrderButton.setVisible(false);
	                
	            }
	            
	        }
	        
	    });

	    //action for the "Save Changes" button
	    saveChangesButton.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            if (isEditMode[0] && editingIndex[0] != -1) {
	            	
	                //update the pizza order from the UI if in edit mode and a pizza is selected
	                updatePizzaOrderFromUI(currentOrder[0], sizeBox, pepperoniCheckBox, sausageCheckBox, baconCheckBox, onionCheckBox, peppersCheckBox, mushroomsCheckBox, pineappleCheckBox, xtraCheeseCheckBox);

	                //update the UI to reflect the changes
	                printOrderListToGUI();
	                totalPriceLabel.setText("Total: $" + String.format("%.2f", currentOrder[0].calculateTotal()));

	                //calculate and update subtotal, sales tax, and grand total labels
	                double totalOrderPrice = updateTotalOrderPrice();
	                double salesTaxRate = getSalesTaxValueFromDB();
	                double totalSalesTax = updateTotalOrderPrice() * salesTaxRate;
	                totalOrderPriceLabel.setText("Subtotal: $" + String.format("%.2f", totalOrderPrice)); //update the total order label
	                salesTaxLabel.setText("Sales Tax: $" + String.format("%.2f", totalSalesTax));
	                grandTotalPriceLabel.setText("Grand Total: $" + String.format("%.2f", updateTotalOrderPrice() + totalSalesTax));

	                //prepare for a new pizza order and reset UI components
	                currentOrder[0] = new PizzaOrder(conn);
	                resetSelections(sizeBox, pepperoniCheckBox, sausageCheckBox, baconCheckBox, onionCheckBox, peppersCheckBox, mushroomsCheckBox, pineappleCheckBox, xtraCheeseCheckBox); //reset UI selections

	                //exit edit mode and reset indices
	                isEditMode[0] = false;
	                editingIndex[0] = -1;
	                
	                //hide "Save Changes" and show "Add to Order" buttons
	                saveChangesButton.setVisible(false);
	                addToOrderButton.setVisible(true);
	                
	            }
	            
	        }
	        
	    });

	    //attach the common price update listener to sizeBox and all toppings checkboxes
	    sizeBox.addItemListener(updatePriceListener);
	    pepperoniCheckBox.addItemListener(updatePriceListener);
	    sausageCheckBox.addItemListener(updatePriceListener);
	    baconCheckBox.addItemListener(updatePriceListener);
	    onionCheckBox.addItemListener(updatePriceListener);
	    peppersCheckBox.addItemListener(updatePriceListener);
	    mushroomsCheckBox.addItemListener(updatePriceListener);
	    pineappleCheckBox.addItemListener(updatePriceListener);
	    xtraCheeseCheckBox.addItemListener(updatePriceListener);

	    //add an action listener to the "Add to Order" button
	    addToOrderButton.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            //clone the current pizza order before adding it to the list to avoid reference issues
	            PizzaOrder orderToAdd = clonePizzaOrder(currentOrder[0]);
	            pizzaOrderList.add(orderToAdd); //add the cloned pizza order to the pizzaOrderList

	            //reset UI components to default values for a new pizza order
	            resetSelections(sizeBox, pepperoniCheckBox, sausageCheckBox, baconCheckBox, onionCheckBox, peppersCheckBox, mushroomsCheckBox, pineappleCheckBox, xtraCheeseCheckBox);

	            //create a new instance of PizzaOrder for the next order
	            currentOrder[0] = new PizzaOrder(conn);

	            //reset the total price label to $0.00 as we start a new order
	            totalPriceLabel.setText("Total: $" + String.format("%.2f", currentOrder[0].calculateTotal()));

	            //calculate and update the total order price, sales tax, and grand total
	            double totalOrderPrice = updateTotalOrderPrice();
	            double salesTaxRate = getSalesTaxValueFromDB();
	            double totalSalesTax = totalOrderPrice * salesTaxRate;
	            totalOrderPriceLabel.setText("Subtotal: $" + String.format("%.2f", totalOrderPrice)); //update the total order label
	            salesTaxLabel.setText("Sales Tax: $" + String.format("%.2f", totalSalesTax));
	            grandTotalPriceLabel.setText("Grand Total: $" + String.format("%.2f", totalOrderPrice + totalSalesTax));

	            //enable the finalize order button if there is an order to finalize
	            if(totalOrderPrice > 0) {
	            	
	                finalizeOrderButton.setEnabled(true);
	                
	            }

	            //update the UI to display the current order list
	            printOrderListToGUI();
	            
	        }
	        
	    });

	    //create and set up the "Delete Selected Pizza" button
	    JButton deleteButton = new JButton("Delete Selected Pizza");
	    deleteButton.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            //get the index of the selected pizza in the list
	            int selectedIndex = pizzaList.getSelectedIndex();
	            if (selectedIndex != -1) {
	            	
	                //delete the selected pizza from the pizza order list and the list model
	                pizzaOrderList.delete(selectedIndex);
	                pizzaListModel.remove(selectedIndex);
	            
	            }

	            //recalculate and update the total order price, sales tax, and grand total after deletion
	            double totalOrderPrice = updateTotalOrderPrice();
	            double salesTaxRate = getSalesTaxValueFromDB();
	            double totalSalesTax = totalOrderPrice * salesTaxRate;
	            totalOrderPriceLabel.setText("Subtotal: $" + String.format("%.2f", totalOrderPrice));
	            salesTaxLabel.setText("Sales Tax: $" + String.format("%.2f", totalSalesTax));
	            grandTotalPriceLabel.setText("Grand Total: $" + String.format("%.2f", totalOrderPrice + totalSalesTax));

	            //disable the finalize order button if there is no order to finalize
	            if(totalOrderPrice == 0) {
	            	
	                finalizeOrderButton.setEnabled(false);
	                
	            }
	            
	        }
	        
	    });

	    //add an action listener to the finalize order button
	    finalizeOrderButton.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            //create and set up a new frame to display the receipt
	            JFrame receiptFrame = new JFrame("PizzaPOS Pro");
	            receiptFrame.setSize(700, 800);
	            receiptFrame.setLayout(new BorderLayout()); //set the layout of the frame to BorderLayout

	            //create a top panel to hold the options button
	            JPanel topPanel = new JPanel();
	            JButton optionsButton = new JButton("Options");
	            topPanel.add(optionsButton);

	            //set action listener for the options button to show the options popup
	            optionsButton.addActionListener(new ActionListener() {
	            	
	                @Override
	                public void actionPerformed(ActionEvent e) {
	                	
	                	receiptFrame.dispose(); //close the receipt frame
	                	//clear the current pizza order and reset all related UI components
		                pizzaOrderList.clear();
		                currentOrder[0] = new PizzaOrder(conn);
		                resetNewOrderScreen(pin, pos);	                
		                pizzaListModel.clear();
		                totalPriceLabel.setText("Total: $0.00"); //reset the total price label
		                totalOrderPriceLabel.setText("Subtotal: $0.00"); //reset the total order price label
		                totalCokePriceLabel.setText("Total Coke: $0.00"); //reset the total Coke price label
		                salesTaxLabel.setText("Sales Tax: $0.00"); //reset the sales tax label
		                grandTotalPriceLabel.setText("Grand Total: $0.00"); //reset the grand total label
		                printOrderListToGUI(); //refresh the GUI with updated order list
	                    showOptionsPopup(pin);
	                    
	                }
	                
	            });

	            //create a panel for the receipt with a vertical BoxLayout and padding
	            JPanel receiptPanel = new JPanel();
	            receiptPanel.setLayout(new BoxLayout(receiptPanel, BoxLayout.Y_AXIS));
	            receiptPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //set padding around the panel

	            //create and style the header label for the receipt
	            JLabel headerLabel = new JLabel("Order Receipt");
	            styleLabel(headerLabel, 20, Font.BOLD);

	            //create and style the date and time label
	            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	            JLabel dateTimeLabel = new JLabel("Date/Time: " + formatter.format(new Date()));
	            styleLabel(dateTimeLabel, 16, Font.PLAIN);

	            //create and style the label showing the user who served the order
	            JLabel userNameLabel = new JLabel("Served by: " + getCurrentUserName(pin));
	            styleLabel(userNameLabel, 16, Font.PLAIN);

	            //add the header, date/time, and username labels to the receipt panel
	            receiptPanel.add(headerLabel);
	            receiptPanel.add(Box.createRigidArea(new Dimension(0, 10)));
	            receiptPanel.add(dateTimeLabel);
	            receiptPanel.add(userNameLabel);
	            receiptPanel.add(Box.createRigidArea(new Dimension(0, 20)));

	            //loop through each pizza order and add their details to the receipt
	            for (int i = 0; i < pizzaOrderList.size(); i++) {
	            	
	                PizzaOrder order = pizzaOrderList.get(i);
	                String itemText = order.toString(); //get the pizza order description
	                String priceText = String.format("$%.2f", order.calculateTotal()); //format the price of the pizza order
	                JPanel pizzaLinePanel = createLinePanel(itemText, priceText); //create a line panel with the pizza details
	                receiptPanel.add(pizzaLinePanel); //add the line panel to the receipt panel
	            
	            }

	            //create and add the Coke details to the receipt
	            String cokeText = "2L Coke Quantity: " + coke.getQuantity(); //description for Coke
	            String cokePriceText = String.format("$%.2f", coke.getTotalPrice()); //format the price of Coke
	            JPanel cokeLinePanel = createLinePanel(cokeText, cokePriceText); //create a line panel with the Coke details
	            receiptPanel.add(cokeLinePanel);
	            receiptPanel.add(Box.createRigidArea(new Dimension(0, 20)));

	            //create, style, and add labels for subtotal, tax, and grand total
	            JLabel subtotalLabel = new JLabel("Subtotal: $" + String.format("%.2f", updateTotalOrderPrice()));
	            JLabel taxLabel = new JLabel("Sales Tax: $" + String.format("%.2f", updateTotalOrderPrice() * getSalesTaxValueFromDB()));
	            JLabel grandTotalLabel = new JLabel("Grand Total: $" + String.format("%.2f", updateTotalOrderPrice() + (updateTotalOrderPrice() * getSalesTaxValueFromDB())));
	            styleLabel(subtotalLabel, 16, Font.PLAIN);
	            styleLabel(taxLabel, 16, Font.PLAIN);
	            styleLabel(grandTotalLabel, 16, Font.PLAIN);
	            receiptPanel.add(Box.createRigidArea(new Dimension(0, 20)));
	            receiptPanel.add(subtotalLabel);
	            receiptPanel.add(taxLabel);
	            receiptPanel.add(grandTotalLabel);

	            //add spacing and set alignment for the components in the receipt panel
	            receiptPanel.add(Box.createRigidArea(new Dimension(0, 20)));
	            receiptPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

	            //create, style, and set actions for logout and return to order buttons
	            JButton logoutButton = new JButton("Logout");
	            JButton returnToOrderButton = new JButton("Return to New Order");
	            logoutButton.addActionListener(e1 -> {
	            	receiptFrame.dispose(); //close the receipt frame
	            	//clear the current pizza order and reset all related UI components
	                pizzaOrderList.clear();
	                currentOrder[0] = new PizzaOrder(conn);
	                resetNewOrderScreen(pin, pos);	                
	                pizzaListModel.clear();
	                totalPriceLabel.setText("Total: $0.00"); //reset the total price label
	                totalOrderPriceLabel.setText("Subtotal: $0.00"); //reset the total order price label
	                totalCokePriceLabel.setText("Total Coke: $0.00"); //reset the total Coke price label
	                salesTaxLabel.setText("Sales Tax: $0.00"); //reset the sales tax label
	                grandTotalPriceLabel.setText("Grand Total: $0.00"); //reset the grand total label
	                printOrderListToGUI(); //refresh the GUI with updated order list
	                showLogin(); //call the login screen
	            });
	            returnToOrderButton.addActionListener(e1 -> {
	            	
	                receiptFrame.dispose(); //close the receipt frame
	                resetNewOrderScreen(pin, pos); //reset the new order screen
	                
	            });
	            
	            styleButton(logoutButton);
	            styleButton(returnToOrderButton);
	            receiptPanel.add(logoutButton);
	            receiptPanel.add(returnToOrderButton);

	            //wrap the receiptPanel in a JScrollPane for scrolling
	            JScrollPane scrollPane = new JScrollPane(receiptPanel);
	            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

	            //add the topPanel and JScrollPane to the receiptFrame
	            receiptFrame.add(topPanel, BorderLayout.NORTH);
	            receiptFrame.add(scrollPane, BorderLayout.CENTER);

	            //center the receipt window on the screen and make it visible
	            receiptFrame.setLocationRelativeTo(null);
	            receiptFrame.setVisible(true);

            }

	        //helper method for styling JLabels with specified font size and style
	        private void styleLabel(JLabel label, int fontSize, int fontStyle) {
	        	
	            label.setAlignmentX(Component.CENTER_ALIGNMENT); //center align the label
	            label.setFont(new Font("Arial", fontStyle, fontSize)); //set the font of the label
	       
	        }

	        //helper method for styling JButtons with a standard font and size
	        private void styleButton(JButton button) {
	        	
	            button.setAlignmentX(Component.CENTER_ALIGNMENT); //center align the button
	            button.setFont(new Font("Arial", Font.PLAIN, 16)); //set the font of the button
	        
	        }
            
	        //helper method to create a JPanel representing a line with an item and its price
	        private JPanel createLinePanel(String itemText, String priceText) {
	        	
	            JPanel linePanel = new JPanel(); //create a new JPanel
	            linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.LINE_AXIS)); //set layout to line axis for horizontal layout

	            JLabel itemLabel = new JLabel(itemText); //create a label for the item text
	            styleLabel(itemLabel, 16, Font.PLAIN); //style the item label
	            itemLabel.setHorizontalAlignment(SwingConstants.LEFT); //align the item label to the left

	            JLabel priceLabel = new JLabel(priceText); //create a label for the price text
	            styleLabel(priceLabel, 16, Font.PLAIN); //style the price label
	            priceLabel.setHorizontalAlignment(SwingConstants.RIGHT); //align the price label to the right

	            linePanel.add(itemLabel); //add the item label to the line panel
	            linePanel.add(Box.createHorizontalGlue()); //add a horizontal glue for spacing
	            linePanel.add(priceLabel); //add the price label to the line panel

	            //set maximum sizes for the labels to ensure they don't grow beyond their preferred size
	            itemLabel.setMaximumSize(itemLabel.getPreferredSize());
	            priceLabel.setMaximumSize(priceLabel.getPreferredSize());

	            return linePanel; //return the completed line panel
	            
	        }
	        
        });

	    //create a top panel to hold date/time and user information
	    JPanel topPanel = new JPanel(new BorderLayout()); //use BorderLayout for flexible layout
	    
	    //create a panel for displaying user information and logout button
	    JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); //use FlowLayout for centered alignment
	    userInfoPanel.add(new JLabel("New Order - Logged In As:")); //add a label to show the logged-in user
	    userInfoPanel.add(userNameLabel); //add the userNameLabel to display the current user's name

	    //create a new panel for the buttons, aligning them to the right
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    buttonPanel.add(voidOrderButton); //add the Void Order button to the panel
	    buttonPanel.add(optionsButton); //add the Options button to the panel
	    buttonPanel.add(logoutButton); //add the Logout button to the panel

	    //add the userInfoPanel to the center of the topPanel
	    topPanel.add(userInfoPanel, BorderLayout.CENTER);

	    //add the dateTimeLabel to the left side of the topPanel
	    topPanel.add(dateTimeLabel, BorderLayout.WEST);

	    //add the buttonPanel to the right side of the topPanel
	    topPanel.add(buttonPanel, BorderLayout.EAST);

	    //add the userInfoPanel to the top (north) of the topPanel for user information display
	    topPanel.add(userInfoPanel, BorderLayout.NORTH);

	    //add the buttonPanel to the right (east) side of the topPanel for easy access to buttons
	    topPanel.add(buttonPanel, BorderLayout.EAST);

	    //create a left panel for pizza size and toppings, arranging components vertically
	    JPanel leftPanel = new JPanel();
	    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

	    //create a new panel for pizza size selection, aligning components horizontally to the left
	    JPanel sizeSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    sizeSelectionPanel.add(new JLabel("Select Current Pizza Size:")); //add a label for size selection
	    sizeSelectionPanel.add(sizeBox); //add the sizeBox for size selection

	    //add the sizeSelectionPanel to the top of the leftPanel
	    leftPanel.add(sizeSelectionPanel, 0); //0 index ensures it is added at the top
	    leftPanel.add(new JLabel("Select Toppings:")); //add a label for toppings selection

	    //add checkboxes for each topping to the leftPanel
	    leftPanel.add(pepperoniCheckBox);
	    leftPanel.add(sausageCheckBox);
	    leftPanel.add(baconCheckBox);
	    leftPanel.add(onionCheckBox);
	    leftPanel.add(peppersCheckBox);
	    leftPanel.add(mushroomsCheckBox);
	    leftPanel.add(pineappleCheckBox);
	    leftPanel.add(xtraCheeseCheckBox);
	    leftPanel.add(totalPriceLabel); //add a label to display the total price of the current pizza

	    leftPanel.add(addToOrderButton); //add the Add to Order button to the leftPanel

	    //create a new panel for the Coke details, aligning components horizontally to the left
	    JPanel cokeDetailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
	    cokeDetailsPanel.add(new JLabel("2L Coke Quantity:")); //add a label for Coke quantity
	    cokeDetailsPanel.add(cokeQuantitySpinner); //add the spinner to select Coke quantity
	    cokeDetailsPanel.add(totalCokePriceLabel); //add a label to display the total price of Coke

	    //add some vertical space above the cokeDetailsPanel to push it down
	    leftPanel.add(Box.createVerticalGlue());

	    //add the cokeDetailsPanel to the bottom of the leftPanel
	    leftPanel.add(cokeDetailsPanel);

	    //create a right panel for the list of current pizzas and action buttons
	    JPanel rightPanel = new JPanel();
	    rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
	    rightPanel.add(listScrollPane); //add the scroll pane containing the list of pizzas

	    //add edit, save, and delete buttons to the rightPanel
	    rightPanel.add(editSelectedPizzaButton); //button for editing a selected pizza
	    rightPanel.add(saveChangesButton); //button for saving changes to a pizza
	    rightPanel.add(deleteButton); //button for deleting a selected pizza

	    //create a bottom panel for Coke details, order totals, and finalization
	    JPanel bottomPanel = new JPanel();
	    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)); //use BoxLayout for vertical stacking

	    //add labels for order totals to the bottomPanel
	    bottomPanel.add(totalOrderPriceLabel); //label for total order price
	    bottomPanel.add(salesTaxLabel); //label for sales tax
	    bottomPanel.add(grandTotalPriceLabel); //label for grand total

	    //add the finalize order button to the bottomPanel
	    bottomPanel.add(finalizeOrderButton);

	    //ensure all components in the bottomPanel are centered
	    for (Component comp : bottomPanel.getComponents()) {
	        ((JComponent) comp).setAlignmentX(Component.CENTER_ALIGNMENT);
	    }

	    //set the layout of the mainPanel and add subpanels to it
	    mainPanel.setLayout(new BorderLayout());
	    mainPanel.add(topPanel, BorderLayout.NORTH); //topPanel contains user info and buttons
	    mainPanel.add(leftPanel, BorderLayout.WEST); //leftPanel contains pizza size, toppings, and Coke details
	    mainPanel.add(rightPanel, BorderLayout.EAST); //rightPanel contains the list of pizzas and action buttons
	    mainPanel.add(bottomPanel, BorderLayout.SOUTH); //bottomPanel contains order totals and the finalize button

        ///initially populate the order list display and update the frame
	    printOrderListToGUI();
	    frame.repaint(); //refresh the frame to display the latest updates
	    frame.setVisible(true); //make the frame visible
	    
    }
    
	//method to set up the name of the first user using the provided PIN
	private void setupNameForFirstUser(String pin) {
		
	    mainPanel.removeAll(); //clear existing GUI components from the main panel
	    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); //set the layout of the main panel for vertical stacking of components

	    //create a label to instruct the user to enter the first user's name
	    JLabel nameLabel = new JLabel("Enter the name for the first user (one word, starting with a capital letter) and press Enter:");
	    nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT); //center align the nameLabel

	    //create a text field for the user to enter the name
	    nameField = new JTextField(15);
	    nameField.setHorizontalAlignment(JTextField.CENTER); //center text
	    nameField.setMaximumSize(new Dimension(110, 20)); //set the maximum size of the nameField
	    nameField.setAlignmentX(Component.CENTER_ALIGNMENT); //center align the nameField

	    //initialize the status label to display error messages, if it is not already initialized
	    statusLabel = new JLabel(""); //reset the status label
	    statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT); //center align the statusLabel

	    //use SwingUtilities to request focus for the nameField
	    SwingUtilities.invokeLater(new Runnable() {
	    	
	        public void run() {
	        	
	            nameField.requestFocusInWindow(); //request focus for the nameField when the window becomes active

	        }
	        
	    });

	    //add an action listener to handle Enter key press on the nameField
	    nameField.addActionListener(new ActionListener() {
	    	
	        public void actionPerformed(ActionEvent e) {
	        	
	            String name = nameField.getText().trim(); //retrieve and trim the entered name
	            
	            //regex to check if the name is one word, starts with a capital letter, and the rest are lowercase letters
	            if (name.matches("[A-Z][a-z]{0,24}")) {
	            	
	                updateUserName(pin, name); //update the initial user's name in the database using the provided PIN
	                showLogin(); //show the login page
	                
	            } else {
	            	
	                //if the name does not match the criteria, reset the name field and show an error message
	                nameField.setText(""); //clear the name field
	                statusLabel.setText("Invalid name! Must have no spaces, start with a capital letter and end with all lowercase, and be 25 or less letters.");
	                mainPanel.revalidate(); //revalidate the main panel to update its layout
	                mainPanel.repaint(); //repaint the main panel to reflect changes
	                
	            }
	            
	        }
	        
	    });

	    //add vertical glue to center components vertically in the main panel
	    mainPanel.add(Box.createVerticalGlue());

	    //add the GUI components to the main panel with space between them
	    mainPanel.add(nameLabel);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 5))); //add a small spacer after the nameLabel
	    mainPanel.add(nameField);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); //add a larger spacer after the nameField
	    mainPanel.add(statusLabel);

	    //add more vertical glue to maintain the vertical centering of components in the main panel
	    mainPanel.add(Box.createVerticalGlue());

	    frame.setLocationRelativeTo(null); //center the window on the screen

	    //repaint and show the updated panel
	    mainPanel.revalidate(); //revalidate the main panel to update its layout
	    mainPanel.repaint(); //repaint the main panel to reflect changes
	    frame.setVisible(true); //make the frame visible
	    
	}

	//method to display the login page
	private void showLogin() {
		
	    mainPanel.removeAll(); //clear existing GUI components from the main panel
	    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS)); //set the layout of the main panel for vertical stacking of components

	    //create a label for the login instructions
	    JLabel loginLabel = new JLabel("Enter PIN to login:");
	    pinField = new JTextField(4); //create a text field for PIN entry with a length of 4 characters
	    pinField.setHorizontalAlignment(JTextField.CENTER); //center align the text inside the pinField
	    //set the maximum size of the pinField to its preferred size to constrain its width
	    pinField.setMaximumSize(new Dimension(pinField.getPreferredSize().width, pinField.getPreferredSize().height));

	    //initialize the status label to display error messages
	    statusLabel = new JLabel("");
	    final PizzaPOS pos = this; //reference to the current PizzaPOS instance

	    //adjust the alignment of the login components to be centered
	    loginLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
	    pinField.setAlignmentX(Component.CENTER_ALIGNMENT);
	    statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

	    //add a DocumentListener to the pinField to handle changes in the text field
	    pinField.getDocument().addDocumentListener(new DocumentListener() {
	    	
	        public void changedUpdate(DocumentEvent e) {
	        	
	            autoLogin(); //call autoLogin on text change
	            
	        }
	        
	        public void removeUpdate(DocumentEvent e) {
	        	
	            //optional to handle text removal updates, intentionally left blank
	        	
	        }
	        
	        public void insertUpdate(DocumentEvent e) {
	        	
	            autoLogin(); //call autoLogin on text insertion
	       
	        }

	        //method to automatically login when the PIN is fully entered
	        private void autoLogin() {
	        	
	            if (pinField.getText().length() == 4) { //check if the length of the PIN is 4
	            	
	                if (pinExists(pinField.getText())) { //verify if the entered PIN exists
	                	
	                    //show the home page if the PIN is valid
	                    showHomePage(pinField.getText(), pos);
	                    
	                } else {
	                	
	                    //display an error message if the PIN is invalid
	                    statusLabel.setText("Invalid PIN! Try again.");
	                    
	                    //use SwingUtilities to ensure the PIN field is cleared
	                    SwingUtilities.invokeLater(() -> {
	                    	
	                        pinField.setText(""); //clear the pinField
	                        
	                    });
	                    
	                }
	                
	            }
	            
	        }
	        
	    });

	    //add vertical glue to the main panel before the components to center them vertically
	    mainPanel.add(Box.createVerticalGlue());

	    //add the GUI components to the main panel with spacers for layout
	    mainPanel.add(loginLabel);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 5))); //small spacer after loginLabel
	    mainPanel.add(pinField);
	    mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); //larger spacer after pinField
	    mainPanel.add(statusLabel);

	    //add more vertical glue after the components to maintain vertical centering
	    mainPanel.add(Box.createVerticalGlue());

	    //set the location of the frame to be centered on the screen
	    frame.setLocationRelativeTo(null);
	    frame.repaint(); //repaint the GUI components to reflect changes
	    frame.setVisible(true); //make the frame visible

	    //use SwingUtilities to request focus for the pinField when the window becomes active
	    SwingUtilities.invokeLater(new Runnable() {
	    	
	        public void run() {
	        	
	            pinField.requestFocusInWindow();
	            
	        }
	        
	    });
	    
	}

    //method to show the home page after successful login
    private void showHomePage(String pin, PizzaPOS pos) {

    	//directly navigate to New Order screen upon successful login
    	showNewOrder(pin, pos);  

    }

    //PizzaOrder object
    public class PizzaOrder {
        
        //enum representing pizza sizes
        public enum Size {
        	
            SMALL,
            MEDIUM,
            LARGE;
            
            //method to convert a string to a Size enum
            public static Size fromString(String sizeStr) {
            	
                //converts the input string to uppercase and matches it with the corresponding enum value
                switch (sizeStr.toUpperCase()) {
                
                    case "SMALL": return SMALL; //returns SMALL enum for "SMALL" input
                    case "MEDIUM": return MEDIUM; //returns MEDIUM enum for "MEDIUM" input
                    case "LARGE": return LARGE; //returns LARGE enum for "LARGE" input
                    default: throw new IllegalArgumentException("Unknown Size: " + sizeStr); //throws an exception for invalid inputs

                }
                
            }
            
        }
        
        //method to toggle a topping on the pizza
        public void toggleTopping(Topping topping) {
        	
            if (toppings.contains(topping)) {
            	
                toppings.remove(topping); //removes the topping if it already exists
                
            } else {
            	
                toppings.add(topping); //adds the topping if it does not exist
                
            }
            
        }

        //method to set multiple toppings at once
        public void setToppings(List<PizzaPOS.PizzaOrder.Topping> toppings) {
        	
            this.toppings.clear(); //clears existing toppings
            this.toppings.addAll(toppings); //adds the new set of toppings
            
        }

        //enum representing pizza toppings
        public enum Topping {
        	
            PEPPERONI,
            SAUSAGE,
            BACON,
            ONION,
            PEPPERS,
            MUSHROOMS,
            PINEAPPLE,
            XTRA_CHEESE;
        	
        }
        
        //member variables for the PizzaOrder class
        private final Connection conn; //database connection object
        private Size size; //size of the pizza
        private final List<Topping> toppings; //list of toppings on the pizza

        //constructor to create a PizzaOrder with a default size of SMALL
        public PizzaOrder(Connection conn) {
        	
            this.conn = conn; //initializes the database connection
            this.size = Size.SMALL; //sets the default pizza size to SMALL
            this.toppings = new ArrayList<>(); //initializes the toppings list
        
        }

        //constructor to create a PizzaOrder with a specified size
        public PizzaOrder(Size size, Connection conn) {
        	
            this.conn = conn; //initializes the database connection
            this.size = size; //sets the specified pizza size
            this.toppings = new ArrayList<>(); //initializes the toppings list
            
        }

        //method to add a single topping to the pizza
        public void addTopping(Topping topping) {
        	
            toppings.add(topping); //adds the specified topping to the toppings list
        
        }

        //method to remove a single topping from the pizza
        public void removeTopping(Topping topping) {
        	
            toppings.remove(topping); //removes the specified topping from the toppings list
        
        }
        
        //method to calculate the total price of the pizza
        public double calculateTotal() {
        	
            double total = getPizzaPrice(); //calculates the base price of the pizza
            total += getToppingsPrice(); //adds the price of the toppings to the total
            return total; //returns the total price of the pizza
        
        }
        
        //method to get the price of the pizza based on its size
        private double getPizzaPrice() {
        	
            try {
            	
                //adjust the capitalization of the first letter of the size for database matching
                String sizeInDb = size.name().charAt(0) + size.name().substring(1).toLowerCase(); //converts enum size to match database format
                String sql = "SELECT price FROM pizzaPrices WHERE size=?"; //SQL query to get the price for a given size
                PreparedStatement stmt = conn.prepareStatement(sql); //prepare the SQL statement
                stmt.setString(1, sizeInDb); //set the size parameter in the SQL query
                ResultSet rs = stmt.executeQuery(); //execute the query
                
                if (rs.next()) {
                	
                    return rs.getDouble("price"); //return the price from the result set if available
                
                }
                
            } catch (Exception e) {
            	
                e.printStackTrace(); //print the stack trace in case of an exception
                
            }
            
            return 0; //return a default value of 0 if no price is found or in case of an exception
        
        }

        //method to calculate the total price of all toppings on the pizza
        private double getToppingsPrice() {
        	
            double totalToppingPrice = 0; //initialize the total price of toppings to 0

            //iterate through each topping in the toppings list
            for (Topping topping : toppings) {
            	
                try {
                	
                    //SQL query to get the price for each topping
                    String sql = "SELECT price FROM toppingPrices WHERE size = ? AND topping = ?";
                    PreparedStatement stmt = conn.prepareStatement(sql); //prepare the SQL statement

                    //format the size string to match the database entries
                    String sizeInDb = capitalize2(size.name()); //capitalize the first letter

                    //convert the topping enum to the corresponding string format for the database
                    String toppingInDb;
                    switch (topping) {
                    
                        case PEPPERS:
                            toppingInDb = "Green Peppers";
                            break;
                            
                        case XTRA_CHEESE:
                            toppingInDb = "Xtra Cheese";
                            break;
                            
                        default:
                            toppingInDb = capitalize2(topping.name());
                            break;
                            
                    }

                    stmt.setString(1, sizeInDb); //set the size parameter in the SQL query
                    stmt.setString(2, toppingInDb); //set the topping parameter in the SQL query
                    ResultSet rs = stmt.executeQuery(); //execute the query

                    if (rs.next()) {
                    	
                        totalToppingPrice += rs.getDouble("price"); //add the price of the current topping to the total
                    
                    }

                } catch (SQLException e) {
                	
                    e.printStackTrace(); //print the stack trace in case of an SQLException
                
                }
                
            }

            return totalToppingPrice; //return the total price of all toppings
            
        }

        //helper method to capitalize the first letter of a string
        private String capitalize2(String input) {
        	
            if (input == null || input.isEmpty()) {
            	
                return input;
                
            }
            
            return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
        
        }

        //class method to get the size of the pizza
        public Size getSize() {
        	
            return size; //returns the current size of the pizza
            
        }

        //class method to set the size of the pizza
        public void setSize(Size size) {
        	
            this.size = size; //sets the pizza's size to the specified value
            
        }

        //class method to get a list of toppings on the pizza
        public List<Topping> getToppings() {
        	
            return new ArrayList<>(toppings); //returns a new list containing all the toppings
        
        }

        //overridden toString method to provide a string representation of the pizza order
        @Override
        public String toString() {
        	
            String sizeString; //variable to store the string representation of the size
            
            //convert the enum size to a string based on the pizza size
            switch (size) {
            
                case SMALL:
                    sizeString = "SM"; //abbreviates SMALL as SM
                    break;
                    
                case MEDIUM:
                    sizeString = "MED"; //abbreviates MEDIUM as MED
                    break;
                    
                case LARGE:
                	
                    sizeString = "LG"; //abbreviates LARGE as LG
                    break;
                    
                default:
                    sizeString = capitalize(size.toString()); //capitalizes other sizes if present
                    
            }

            //check if the pizza has toppings
            if (toppings.isEmpty()) {
            	
                return "(" + sizeString + ") No Toppings"; //returns a string indicating no toppings if the toppings list is empty
            
            }

            //create a string of toppings by abbreviating each topping's name
            String toppingsString = toppings.stream()
                    .map(topping -> {
                    	
                        //abbreviates each topping's name
                        switch (topping) {
                            case PEPPERONI:
                                return "Pepp";
                            case SAUSAGE:
                                return "Saus";
                            case BACON:
                                return "Bacon";
                            case ONION:
                                return "Onion";
                            case PEPPERS:
                                return "Gr Pep";
                            case MUSHROOMS:
                                return "Mush";
                            case PINEAPPLE:
                                return "Pine";
                            case XTRA_CHEESE:
                                return "X Chz";
                            default:
                            	
                                //capitalizes toppings with multiple words, replacing underscores with spaces
                                return capitalize(topping.toString().replace("_", " "));
                                
                        }
                        
                    })
                    .collect(Collectors.joining(", ")); //joins the toppings with commas
            
            return "(" + sizeString + ") " + toppingsString; //returns the final string representation of the pizza order
        
        }

        //helper method to capitalize the first letter of each word in a string
        private String capitalize(String word) {
        	
            if (word == null || word.isEmpty()) {
            	
                return word; //returns the original word if it is null or empty
                
            }
            
            return Arrays.stream(word.split(" "))
                    .map(str -> str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase()) //capitalizes the first letter of each word
                    .collect(Collectors.joining(" ")); //joins the words back into a single string
            
        }

    }
    
    //class representing a 2-liter bottle of Coke
    public class Coke2L {
    	
        private double price; //variable to store the price of one bottle
        private int quantity; //variable to store the quantity of bottles

        //constructor for Coke2L
        public Coke2L() {
        	
            //initialize the quantity as zero and the price as 0.0 initially
            this.quantity = 0;
            this.price = 0.0;
            fetchPriceFromDatabase(); //fetch the current price of Coke from the database
            
        }

        //method to fetch and update the price of Coke from the database
        private void fetchPriceFromDatabase() {
        	
            try {
            	
                String sql = "SELECT price FROM cokePrices LIMIT 1"; //SQL query to get the price of Coke
                PreparedStatement stmt = conn.prepareStatement(sql); //prepare the SQL statement
                ResultSet rs = stmt.executeQuery(); //execute the query
                if (rs.next()) {
                	
                    this.price = rs.getDouble("price"); //set the price if found in the database
                
                }
                
            } catch (Exception e) {
            	
                e.printStackTrace(); //print the stack trace if an exception occurs
                this.price = 0; //default the price to 0 in case of an exception
            
            }
        }

        //method to set the quantity of Coke bottles
        public void setQuantity(int quantity) {
        	
            this.quantity = quantity; //set the quantity of Coke bottles
            
        }

        //method to get the quantity of Coke bottles
        public int getQuantity() {
        	
            return this.quantity; //return the current quantity
            
        }

        //method to calculate the total price based on the quantity and price per bottle
        public double getTotalPrice() {
        	
            return this.price * this.quantity; //return the total price (price per bottle * quantity)
            
        }
        
        //method to refresh the price of Coke from the database
        public void refreshPrice() {
        	
            fetchPriceFromDatabase(); //fetch the latest price from the database
        
        }
        
    }

    //main method to start the application
    public static void main(String[] args) {
    	
        new PizzaPOS(); //create a new instance of PizzaPOS to start the application
    
    }

}

//class representing a node in a linked list, specifically for PizzaOrder objects
class Node {
	
	PizzaPOS.PizzaOrder data; //data field to store a PizzaOrder object
	Node next; //reference to the next node in the linked list

	//constructor to create a new node with a given PizzaOrder
	public Node(PizzaPOS.PizzaOrder data) {
		
		this.data = data; //set the data of the node to the given PizzaOrder
     	this.next = null; //initialize the next node reference to null
	
	}
	
	//method to get the PizzaOrder data of the current node
	public PizzaPOS.PizzaOrder getOrder() {
		
		return data; //return the PizzaOrder data stored in this node
	
	}

	//method to get the next node in the linked list
	public Node getNext() {
		
		return next; //return the reference to the next node
	
	}
	
}

//class representing a linked list to store a list of pizza orders
class PizzaTotalOrderLinkedList {
	
	static Node head; //static variable to store the head of the list

	//method to get the first node of the list
	public Node getFirst() {
		
		return head; //return the head node of the list
	
	}

	//method to clear all nodes from the list
	public void clear() {
		
		head = null; //set the head of the list to null, effectively clearing the list
	
	}
	
	//method to add a new node with a pizza order to the end of the list
	public void add(PizzaPOS.PizzaOrder data) {
		
		Node newNode = new Node(data); //create a new node with the given pizza order
		
		if (head == null) {
			
			head = newNode; //if the list is empty, set the new node as the head
		
		} else {
			
			Node current = head; //start from the head of the list
			
			while (current.next != null) {
		
				current = current.next; //traverse to the end of the list
		
			}
		
			current.next = newNode; //add the new node at the end of the list
		
		}
		
	}

	//method to delete a node at a specific index in the linked list
	public boolean delete(int index) {
		
	    //check if the index is valid
	    if (index < 0 || (index == 0 && head == null)) {
	    	
	        return false; //return false if index is out of bounds or list is empty
	    
	    }
	    
	    //if the index is 0, delete the head node
	    if (index == 0) {
	    
	    	head = head.next; //set the head to the next node, effectively deleting the current head
	        return true;
	        
	    }

	    //traverse the list to find the node before the one to be deleted
	    Node current = head;  
	    for (int i = 0; current != null && i < index - 1; i++) {
	    	
	        current = current.next; //move to the next node
	        
	    }

	    //check if the current node or the next node is null, indicating an out of range index
	    if (current == null || current.next == null) {
	    	
	        return false; //return false if index is out of the range of the list
	        
	    }

	    //delete the node at the specified index
	    current.next = current.next.next; //bypass the node to be deleted
	    return true; //return true to indicate successful deletion
	    
	}

	//method to get the data of a node at a specific index in the linked list
	public PizzaPOS.PizzaOrder get(int index) {
		
	    //check if the index is valid
	    if (index < 0) {
	    	
	        throw new IndexOutOfBoundsException("Index " + index + " is out of bounds!"); //throw an exception if index is negative

	    }
	    
	    //traverse the list to find the node at the specified index
	    Node current = head;
	    int count = 0;
	    while (current != null) {
	    	
	        if (count == index) {
	        	
	            return current.data; //return the data found at the specified index
	        
	        }
	        
	        count++; //increment the count
	        current = current.next; //move to the next node
	        
	    }
	    
	    //throw an exception if index is out of the bounds of the list
	    throw new IndexOutOfBoundsException("Index " + index + " is out of bounds!");
	    
	}

	//helper method to get the size of the linked list
	public int size() {
		
	    int size = 0; //initialize size counter
	    
	    //traverse the list to count the number of nodes
	    Node current = head;
	    while (current != null) {
	    	
	        size++; //increment the size for each node
	        current = current.next; //move to the next node
	        
	    }
	    
	    return size; //return the total number of nodes in the list
	    
	}

	//method to calculate the total price of all pizza orders in the linked list
	public double calculateTotalOrderPrice() {
		
	    double totalOrderPrice = 0.0; //initialize the total price
	    Node currentNode = head; //start at the head of the linked list
	    while (currentNode != null) {
	    	
	        totalOrderPrice += currentNode.getOrder().calculateTotal(); //add the total price of the current pizza order
	        currentNode = currentNode.getNext(); //move to the next node in the list
	    
	    }
	    
	    return totalOrderPrice; //return the total price of all orders
	    
	}

	//helper method to print all pizza orders for debugging purposes
	public void printOrders() {
		
	    Node current = head; //start at the head of the linked list
	    int index = 0; //initialize an index counter for printing
	    while (current != null) {
	    	
	        System.out.println("Order " + index + ": " + current.data); //print the data of the current node
	        current = current.next; //move to the next node
	        index++; //increment the index
	    
	    }
	    
	}
    
}