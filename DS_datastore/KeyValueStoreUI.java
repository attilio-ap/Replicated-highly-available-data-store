package DS_datastore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * A graphical user interface (GUI) client for interacting with a key-value store server.
 * <p>
 * This class provides fields to enter server connection information, key-value inputs,
 * and buttons to perform READ, WRITE, and SHOW operations. It uses TCP socket connections
 * to send commands and receive responses from the server.
 * <p>
 * The UI is built using Java Swing and includes real-time display of server responses.
 */
public class KeyValueStoreUI extends JFrame {

    private JTextField serverField;
    private JTextField portField;
    private JTextField keyField;
    private JTextField valueField;
    private JTextArea outputArea;
    private JButton readButton;
    private JButton writeButton;
    private JButton showButton;

    /**
     * Constructs and initializes the Key-Value Store UI window.
     * <p>
     * It configures the look and feel (using Nimbus if available),
     * sets up the layout, and initializes action listeners.
     */
    public KeyValueStoreUI() {
        // Set Nimbus Look-and-Feel if available
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()){
                if ("Nimbus".equals(info.getName())){
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback to default look-and-feel if Nimbus is not available
        }

        setTitle("Key-Value Store Client");
        setSize(900, 600); // Bigger overall window size
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window on screen

        buildUI();
    }

    /**
     * Builds the user interface by composing Swing panels, fields, and buttons.
     * The layout includes connection fields, input fields, and a server response display.
     */
    private void buildUI() {
        // Main panel with padding
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Create left panel for connection settings, inputs, and buttons
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Connection Panel
        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.setBorder(new TitledBorder("Connection Settings"));
        connectionPanel.add(new JLabel("Server:"));
        serverField = new JTextField("localhost", 15);
        connectionPanel.add(serverField);
        connectionPanel.add(new JLabel("Port:"));
        portField = new JTextField("8080", 7);
        connectionPanel.add(portField);
        leftPanel.add(connectionPanel);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Input Panel using GridBagLayout for control
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new TitledBorder("Input Data"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Key:"), gbc);
        gbc.gridx = 1;
        keyField = new JTextField(15);
        inputPanel.add(keyField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Value:"), gbc);
        gbc.gridx = 1;
        valueField = new JTextField(15);
        inputPanel.add(valueField, gbc);
        leftPanel.add(inputPanel);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Button Panel
        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        readButton = new JButton("READ");
        readButton.setToolTipText("Click to read a value using the key");
        writeButton = new JButton("WRITE");
        writeButton.setToolTipText("Click to store a key-value pair");

        // Add  button
        showButton = new JButton("SHOW");
        showButton.setToolTipText("Click to print all key-value pairs stored in the server");

        buttonPanel.add(readButton);
        buttonPanel.add(writeButton);
        buttonPanel.add(showButton);
        leftPanel.add(buttonPanel);


        // Output (Server Response) Area on the right
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(new TitledBorder("Server Response"));
        // Set a larger default size for the output area
        outputScrollPane.setPreferredSize(new Dimension(450, 0));

        // Use a JSplitPane so that the user can resize the panels as needed.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, outputScrollPane);
        splitPane.setDividerLocation(400);
        splitPane.setOneTouchExpandable(true);

        mainPanel.add(splitPane, BorderLayout.CENTER);

        addActionListeners();
    }

    /**
     * Adds listeners to the READ, WRITE, and SHOW buttons to trigger network operations.
     */
    private void addActionListeners() {
        // READ operation
        readButton.addActionListener(e -> {
            String server = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String key = keyField.getText().trim();
            performRead(server, port, key);
        });

        // WRITE operation
        writeButton.addActionListener(e -> {
            String server = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            String key = keyField.getText().trim();
            String value = valueField.getText().trim();
            performWrite(server, port, key, value);
        });

        // SHOW operation: send "SHOW" to request complete state
        showButton.addActionListener(e -> {
            String server = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());
            performShow(server, port);
        });
    }

    /**
     * Sends a SHOW command to the server and appends the full key-value store to the output area.
     *
     * @param host the server host
     * @param port the server port
     */
    private void performShow(String host, int port) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send "SHOW" to server
            out.println("SHOW");

            // Reads responses and visualizes them in outputArea
            String line;
            outputArea.append("SHOW Response:\n");
            while ((line = in.readLine()) != null) {
                outputArea.append(line + "\n");
            }
        } catch (Exception ex) {
            outputArea.append("Error during SHOW: " + ex.getMessage() + "\n");
        }
    }


    /**
     * Sends a READ command to the server using the given key.
     * Displays the value in the output area.
     *
     * @param host the server host
     * @param port the server port
     * @param key  the key to read
     */
    private void performRead(String host, int port, String key) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send command (protocol: "READ key")
            out.println("READ " + key);
            String response = in.readLine();
            outputArea.append("READ Response: " + response + "\n");
        } catch (Exception ex) {
            outputArea.append("Error during READ: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Sends a WRITE command to the server with the given key and value.
     * Displays the server response in the output area.
     *
     * @param host  the server host
     * @param port  the server port
     * @param key   the key to write
     * @param value the value to associate with the key
     */
    private void performWrite(String host, int port, String key, String value) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send command (protocol: "WRITE key value")
            out.println("WRITE " + key + " " + value);
            String response = in.readLine();
            outputArea.append("WRITE Response: " + response + "\n");
        } catch (Exception ex) {
            outputArea.append("Error during WRITE: " + ex.getMessage() + "\n");
        }
    }

    /**
     * Entry point of the application.
     * Launches the Key-Value Store UI.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            KeyValueStoreUI ui = new KeyValueStoreUI();
            ui.setVisible(true);
        });
    }
}
