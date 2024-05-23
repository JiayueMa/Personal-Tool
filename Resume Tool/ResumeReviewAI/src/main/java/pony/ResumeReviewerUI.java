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
    private String apiKey;

    private JTextArea resumeTextArea;
    private JTextArea jobDescTextArea;

    public ResumeReviewerUI() {
        setTitle("Resume Reviewer");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        apiKey = JOptionPane.showInputDialog(this, "Enter your API Key:", "API Key Input", JOptionPane.PLAIN_MESSAGE);

        resumeTextArea = createTextArea("Drag Resume Here");
        jobDescTextArea = createTextArea("Drag Job Description Here");

        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String resume = resumeTextArea.getText();
                String jobDescription = jobDescTextArea.getText();
                if (!resume.isEmpty() && !jobDescription.isEmpty()) {
                    String result = analyzeResume(resume, jobDescription);
                    displayResult(result);
                } else {
                    JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Please provide both resume and job description.");
                }
            }
        });

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JScrollPane(resumeTextArea));
        panel.add(new JScrollPane(jobDescTextArea));

        add(panel, BorderLayout.CENTER);
        add(submitButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    private JTextArea createTextArea(String placeholder) {
        JTextArea textArea = new JTextArea(placeholder);
        textArea.setDropTarget(new DropTarget() {
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
                            JOptionPane.showMessageDialog(ResumeReviewerUI.this, "Unsupported file type.");
                        }
                        if (!content.isEmpty()) {
                            textArea.setText(content);
                            JOptionPane.showMessageDialog(ResumeReviewerUI.this, "File " + file.getName() + " successfully loaded!");
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return textArea;
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

    private String analyzeResume(String resume, String jobDescription) {
        try {
            String prompt = String.format("Compare the following resume with the job description and determine if the candidate is qualified for the position.\n\nResume:\n%s\n\nJob Description:\n%s", resume, jobDescription);

            URL url = new URL(API_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setDoOutput(true);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-3.5-turbo");

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

    private void displayResult(String result) {
        JTextArea textArea = new JTextArea(result);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));

        JOptionPane.showMessageDialog(ResumeReviewerUI.this, scrollPane, "Analysis Result", JOptionPane.INFORMATION_MESSAGE);
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
