package pony;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResumeReviewerUI extends JFrame {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private JTextArea jobDescTextArea;
    private JPasswordField apiKeyField;
    private JTextField qualifiedFolderField;
    private JComboBox<String> apiVersionComboBox;
    private DefaultListModel<String> resumeListModel;
    private List<File> resumeFiles;

    public ResumeReviewerUI() {
        setTitle("Resume Reviewer");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel apiKeyLabel = new JLabel("API Key:");
        apiKeyLabel.setFont(new Font("Arial", Font.BOLD, 14));
        apiKeyLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(apiKeyLabel, gbc);

        apiKeyField = new JPasswordField(20);
        gbc.gridx = 1;
        inputPanel.add(apiKeyField, gbc);

        JLabel qualifiedFolderLabel = new JLabel("Qualified Folder Path:");
        qualifiedFolderLabel.setFont(new Font("Arial", Font.BOLD, 14));
        qualifiedFolderLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(qualifiedFolderLabel, gbc);

        qualifiedFolderField = new JTextField(20);
        gbc.gridx = 1;
        inputPanel.add(qualifiedFolderField, gbc);

        JLabel apiVersionLabel = new JLabel("API Version:");
        apiVersionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        apiVersionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(apiVersionLabel, gbc);

        apiVersionComboBox = new JComboBox<>(new String[]{"gpt-3.5-turbo", "gpt-4"});
        gbc.gridx = 1;
        inputPanel.add(apiVersionComboBox, gbc);

        JLabel resumeLabel = new JLabel("Drag Resumes Here:");
        resumeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        resumeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        inputPanel.add(resumeLabel, gbc);

        resumeListModel = new DefaultListModel<>();
        resumeFiles = new ArrayList<>();
        JList<String> resumeList = new JList<>(resumeListModel);
        JScrollPane resumeScrollPane = new JScrollPane(resumeList);
        resumeScrollPane.setPreferredSize(new Dimension(350, 150));
        resumeScrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        resumeScrollPane.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        resumeListModel.addElement(file.getName());
                        resumeFiles.add(file);
                    }
                    JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Files successfully loaded!");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        inputPanel.add(resumeScrollPane, gbc);

        JLabel jobDescLabel = new JLabel("Drag Job Description Here:");
        jobDescLabel.setFont(new Font("Arial", Font.BOLD, 14));
        jobDescLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.weighty = 0;
        inputPanel.add(jobDescLabel, gbc);

        jobDescTextArea = new JTextArea();
        jobDescTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        jobDescTextArea.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File file : droppedFiles) {
                        String content = "";
                        if (file.getName().endsWith(".docx")) {
                            content = readDocxFile(file);
                        } else if (file.getName().endsWith(".pdf")) {
                            content = readPdfFile(file);
                        } else if (file.getName().endsWith(".rtf")) {
                            content = readRtfFile(file);
                        } else if (file.getName().endsWith(".txt")) {
                            content = readTxtFile(file);
                        } else {
                            JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Unsupported file type: " + file.getName());
                        }
                        jobDescTextArea.setText(content);
                        JOptionPane.showMessageDialog(ResumeReviewerUI.this, "File " + file.getName() + " successfully loaded!");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        JScrollPane jobDescScrollPane = new JScrollPane(jobDescTextArea);
        jobDescScrollPane.setPreferredSize(new Dimension(350, 150));
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        inputPanel.add(jobDescScrollPane, gbc);

        JPanel buttonPanel = new JPanel();
        JButton submitButton = new JButton("Submit");
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String apiKey = new String(apiKeyField.getPassword());
                String qualifiedFolderPath = qualifiedFolderField.getText();
                String jobDescription = jobDescTextArea.getText();
                String apiVersion = (String) apiVersionComboBox.getSelectedItem();

                if (apiKey.isEmpty() || qualifiedFolderPath.isEmpty() || jobDescription.isEmpty() || resumeFiles.isEmpty()) {
                    JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Please fill in all fields and add resumes.");
                } else {
                    for (File file : resumeFiles) {
                        processResumeFile(file, apiKey, qualifiedFolderPath, jobDescription, apiVersion);
                    }
                    JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Resume review process completed.");
                }
            }
        });

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Arial", Font.BOLD, 14));
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resumeListModel.clear();
                resumeFiles.clear();
                jobDescTextArea.setText("");
                JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Cleared all resumes and job description.");
            }
        });

        buttonPanel.add(submitButton);
        buttonPanel.add(clearButton);

        gbc.gridy = 7;
        gbc.gridwidth = 2;
        inputPanel.add(buttonPanel, gbc);

        add(inputPanel, BorderLayout.CENTER);

        // 添加您的信息和联系方式
        JLabel infoLabel = new JLabel("v1.0 by Jiayue Ma | oldhorse1984@gmail.com");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(infoLabel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JTextArea createTextArea(String placeholder) {
        JTextArea textArea = new JTextArea(placeholder);
        return textArea;
    }

    private void processResumeFile(File file, String apiKey, String qualifiedFolderPath, String jobDescription, String apiVersion) {
        try {
            String resumeContent = "";
            if (file.getName().endsWith(".docx")) {
                resumeContent = readDocxFile(file);
            } else if (file.getName().endsWith(".pdf")) {
                resumeContent = readPdfFile(file);
            } else if (file.getName().endsWith(".rtf")) {
                resumeContent = readRtfFile(file);
            } else if (file.getName().endsWith(".txt")) {
                resumeContent = readTxtFile(file);
            } else {
                JOptionPane.showMessageDialog(this, "Unsupported file type: " + file.getName());
                return;
            }

            String result = analyzeResume(apiKey, resumeContent, jobDescription, apiVersion);
            System.out.println("Processing resume: " + resumeContent);
            System.out.println("API response: " + result);

            // Display the result in a dialog with limited width and reason for qualification
            String[] resultParts = result.split("\n", 2);
            String qualification = resultParts[0];
            String reason = resultParts.length > 1 ? resultParts[1] : "No reason provided";

            // Highlighting "qualified" or "not qualified" with bold font
            String highlightedQualification = qualification.replaceAll("(?i)(qualified|not qualified)", "<b>$1</b>");

            JTextPane resultTextPane = new JTextPane();
            resultTextPane.setContentType("text/html");
            resultTextPane.setText("<html><body style='font-family: Arial; font-size: 12px;'>" + highlightedQualification + "<br><br>Reason:<br>" + reason + "</body></html>");
            resultTextPane.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(resultTextPane);
            scrollPane.setPreferredSize(new Dimension(400, 300));

            UIManager.put("OptionPane.okButtonText", "OK");
            JOptionPane.showMessageDialog(this, scrollPane, "API response", JOptionPane.INFORMATION_MESSAGE);

            // Ensure case-insensitive matching for both qualified and disqualified terms
            if (qualification.toLowerCase().contains("qualified") && !qualification.toLowerCase().contains("not qualified")) {
                File qualifiedFolder = new File(qualifiedFolderPath);
                if (!qualifiedFolder.exists()) {
                    qualifiedFolder.mkdirs();
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(qualifiedFolder, "qualified_resume_" + System.currentTimeMillis() + ".txt")))) {
                    writer.write(resumeContent);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readDocxFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            for (XWPFParagraph para : document.getParagraphs()) {
                content.append(para.getText()).append("\n");
            }
        }
        return content.toString();
    }

    private String readPdfFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            content.append(pdfStripper.getText(document));
        }
        return content.toString();
    }

    private String readRtfFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis)) {
            WordExtractor extractor = new WordExtractor(document);
            content.append(extractor.getText());
        }
        return content.toString();
    }

    private String readTxtFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private String analyzeResume(String apiKey, String resume, String jobDescription, String apiVersion) {
        try {
            String prompt = String.format("Compare the following resume with the job description and determine if the candidate is qualified for the position. Provide the reason for qualification or disqualification.\n\nResume:\n%s\n\nJob Description:\n%s", resume, jobDescription);

            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", apiVersion);

            JSONArray messagesArray = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messagesArray.put(message);

            jsonBody.put("messages", messagesArray);
            jsonBody.put("max_tokens", 150);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONArray choices = jsonResponse.getJSONArray("choices");
                JSONObject messageResponse = choices.getJSONObject(0).getJSONObject("message");
                return messageResponse.getString("content");
            } else {
                return "Failed to get a response from the API.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "An error occurred while analyzing the resume.";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ResumeReviewerUI();
            }
        });
    }
}
