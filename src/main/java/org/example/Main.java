package org.example;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Main {
    // Database credentials
    private static final String URL = "jdbc:mysql://localhost:3306/myDB";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static void main(String[] args) {
        /* Assume creation of the Orders and OrderDetails database as:
         *            CREATE TABLE Orders (
         *                    id INT AUTO_INCREMENT PRIMARY KEY,
         *                    customer_name VARCHAR(100) NOT NULL,
         *                    order_date DATETIME NOT NULL
         *            );
         *
         *            CREATE TABLE OrderDetails (
         *                    id INT AUTO_INCREMENT PRIMARY KEY,
         *                    order_id INT NOT NULL,
         *                    product_name VARCHAR(100) NOT NULL,
         *                    quantity INT NOT NULL,
         *                    unit_price DECIMAL(10, 2) NOT NULL,
         *                    CONSTRAINT fk_orderdetails_order
         *                       FOREIGN KEY (order_id) REFERENCES Orders(id)
         *                       ON DELETE CASCADE
         *            );
         * */


        try (
                // Create Connection with the database
                Connection con = DriverManager.getConnection(URL, USER, PASSWORD);
        ) {
            // Set order_id
            int order_id = 1001;


            // Create a transaction to preserve Atomicity through insertions
            con.setAutoCommit(false);


            // Insert new rows in the Orders table
            String insertOrderDetails = "INSERT INTO OrderDetails (id, order_id, product_name, quantity, unit_price)\n" +
                    "VALUES (?, ?, ?, ?, ?);";
            String insertOrders = "INSERT INTO Orders (id, customer_name, order_date) VALUES (?, 'Youssef Benali', '2026-11-23 14:30:00');";

            try (
                    // Format the query accordingly
                    PreparedStatement psOrders = con.prepareStatement(insertOrders);
                    PreparedStatement psOrderDetails = con.prepareStatement(insertOrderDetails);
            ) {
                // Set the variables and execute the insertion into Orders
                psOrders.setInt(1, order_id);
                psOrders.executeUpdate();

                // Execute the batch insertion into OrderDetails
                List<List<Object>> inserts = new ArrayList<>();
                inserts.add(List.of(1, order_id, "Mechanical Keyboard", 2, 45.00));
                inserts.add(List.of(2, order_id, "Wireless Mouse", 1, 55.00));

                for (List<Object> insert : inserts) {
                    psOrderDetails.setInt(1, (Integer) insert.get(0));
                    psOrderDetails.setInt(2, (Integer) insert.get(1));
                    psOrderDetails.setString(3, (String) insert.get(2));
                    psOrderDetails.setInt(4, (Integer) insert.get(3));
                    psOrderDetails.setDouble(5, (Double) insert.get(4));

                    psOrderDetails.addBatch();
                }

                int[] counts = psOrderDetails.executeBatch();

                con.commit();

            } catch (SQLException e) {
                // Rollback in case the transaction didn't come through
                con.rollback();
            }

            // Verifying Insertions
            printTables(con);

            // Delete the rows we've just added
            String deleteOrderDetail = "DELETE FROM OrderDetails WHERE order_id = ?";
            String deleteOrder = "DELETE FROM Orders WHERE id = ?";

            try (
                    // Create PreparedStatement-s to use the delete orders
                    PreparedStatement psOrders = con.prepareStatement(deleteOrder);
                    PreparedStatement psOrderDetails = con.prepareStatement(deleteOrderDetail);
            ) {
                // Set the data in the sql statements
                psOrders.setInt(1, order_id);
                psOrderDetails.setInt(1, order_id);

                // Execute (we are deleting the children even though ON CASCADE is on - a good practice it seems)
                psOrderDetails.executeUpdate();
                psOrders.executeUpdate();

                con.commit();

            } catch (SQLException e) {
                // Rollback in case the transaction didn't come through
                con.rollback();
            }

            // Verifying Deletions
            printTables(con);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static void printTables(Connection con) throws SQLException {
        try (
                // Creating a statement
                Statement stOrders = con.createStatement();
                Statement stOrderDetails = con.createStatement();

                // Getting the content of Orders & OrderDetails
                ResultSet rsOrders = stOrders.executeQuery("SELECT * FROM Orders;");
                ResultSet rsOrderDetails = stOrderDetails.executeQuery("SELECT * FROM OrderDetails;");
        ) {
            // Get content of Orders
            System.out.println("|\tid\t| customer_name | order_date |");
            while (rsOrders.next()) {
                int id = rsOrders.getInt(1);
                String customer_name = rsOrders.getString(2);
                LocalDateTime order_date = rsOrders.getObject(3, LocalDateTime.class);

                System.out.println("|" + id + "|" + customer_name + "|" + order_date + "|");
            }

            // Get content of OrderDetails
            System.out.println("|\tid\t| order_id | product_name | quantity | unit_price |");
            while (rsOrderDetails.next()) {
                int id = rsOrderDetails.getInt(1);
                int o_id = rsOrderDetails.getInt(2);
                String product_name = rsOrderDetails.getString(3);
                int qty = rsOrderDetails.getInt(4);
                BigDecimal price = rsOrderDetails.getObject(5, BigDecimal.class);

                System.out.println("|" + id + "|" + o_id + "|" + product_name + "|" + qty + "|" + price + "|");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}