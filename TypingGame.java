package game;

import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.io.*;
import java.lang.InterruptedException;
import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;

// main class for typing game
/**
 * TypingGame
 */
public class TypingGame extends Application {

    Random rand = new Random();
    public int finalScore = 0;
  
    // IO streams
    DataOutputStream toServer = null;
    DataInputStream fromServer = null;

    public static void main(String[] args) {
        // main method to run program
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException{

        // open server stage
        Stage serverStage = new Stage();
        Server server = new Server(serverStage);
      
        BorderPane pane = new BorderPane(); // main pane for game
        GridPane scorePane = new GridPane();
        //add new button to start game
        Button startButton = new Button("Start!");

        // pane to display score
        scorePane.setPadding(new Insets(50,12.5,13.5,14.5));
        scorePane.setAlignment(Pos.CENTER);
        scorePane.setVgap(15.5);
        scorePane.setHgap(5.5);

        // labels to diplay the score
        Label welcome = new Label("Welcome to Typing Tutor!");
        welcome.setFont(new Font("Arial",30));
        welcome.setTextFill(Color.DARKSLATEBLUE);
        scorePane.setHalignment(welcome, HPos.CENTER);
        scorePane.add(welcome,0,0);
        //instructions
        Label description = new Label("Type the words before they reach the bottom of the screen!");
        scorePane.setHalignment(description, HPos.CENTER);
        scorePane.add(description ,0,1);
        //display of start button
        scorePane.add(startButton,0,2);
        scorePane.setHalignment(startButton, HPos.CENTER);
        pane.setTop(scorePane);

        //action on button start to begin game
        startButton.setOnAction(e-> {
            
            scorePane.getChildren().clear();//clears screen
            scorePane.setPadding(new Insets(10,12.5,13.5,14.5));
            scorePane.setAlignment(Pos.CENTER);
            
            TextField typing = new TextField(); // text field for text entry
            typing.setPromptText("Type Words Here");
            pane.setBottom(typing); // put in the main pane

            scorePane.add(new Label("Score:"), 0, 0);
            Label score = new Label("0");
            scorePane.add(score, 1, 0);
            scorePane.add(new Label("Lives:"), 2, 0);
            Label lives = new Label("3");
            scorePane.add(lives, 3, 0);

            pane.setTop(scorePane);// add scores to main pane

            Pane wordPane = new Pane(); // pane for words to appear
            pane.setCenter(wordPane);
            //game runs and words fall down the screen
            runGame(typing, wordPane, lives, score);

        });

        try {
            // getting localhost ip
            InetAddress ip = InetAddress.getByName("localhost");
            // establish the connection with server port
            Socket getServer = new Socket(ip, 8000);

            //Create an input and output stream
            fromServer = new DataInputStream(getServer.getInputStream()); //receive data from server
            toServer = new DataOutputStream(getServer.getOutputStream());//send data to server

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        Scene scene = new Scene(pane, 400,400);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Typing Game");
        //displays game
        primaryStage.show();

    }

    private void runGame(TextField typing, Pane wordPane, Label lives,
                         Label score){
        // main game functionality
        // create list of words to use in game
        ArrayList<String> words = GenerateStrings();
        if(words == null){ // check if file exists
            System.err.println("Could not open File");
            return;
        }

        // create the word to type
        Text word1 = new Text("");
        Text word2 = new Text("");
        Text word3 = new Text("");
        wordPane.getChildren().addAll(word1,word2,word3);
        resetWord(word1,words);
        resetWord(word2,words);
        resetWord(word3,words);

        // thread to run game
        new Thread(() -> {
            try {

                while(true){
                    int[] status={0};
                    Platform.runLater(() -> {
                        // move the words down
                        incrementWord(word1);
                        incrementWord(word2);
                        incrementWord(word3);
                        // check to see if at bottom of pane
                        checkPosition(word1,lives,words,status);
                        if(1!=status[0]) checkPosition(word2,lives,words,status);
                        if(1!=status[0]) checkPosition(word3,lives,words,status);
                    });
                    Thread.sleep(20);
                    // break game loop if run out of lives
                    if(1 == status[0]){
                        word1.setText("");
                        word2.setText("");
                        word3.setText("");
                        break;
                    }
                }

                try {
                    //send data to server
                    toServer.writeInt(finalScore);
                    toServer.flush();

                    //get data from server
                    String message = fromServer.readUTF();
                    System.out.println(message);

                }
                catch (IOException ex) {
                    System.err.println(ex);
                }
                Thread.currentThread().interrupt(); // exit thread
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();

        // add Typing functionality
        typing.setOnAction(e -> {
            try {
                // check if typed word equals one of the words in the game
                //  if yes increment score and reset the word
                String typed = typing.getText().trim();
                //if typed correctly, call incrementScore to tally score
                if(typed.equals(word1.getText().trim())){
                    incrementScore(word1,score,words);
                }
                else if(typed.equals(word2.getText().trim())){
                    incrementScore(word2,score,words);
                }
                else if(typed.equals(word3.getText().trim())){
                    incrementScore(word3,score,words);
                }
                typing.clear(); // clear the typing field

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }


    private void resetWord(Text word, ArrayList<String> words){
        // method to reset the word
        // randomly set word
        word.setText(words.get(rand.nextInt(words.size()-1))); 
        word.setY(0); // set y to top of pane
        // randomly set the x position
        word.setX(rand.nextInt(350)); 
    }

    private void incrementWord(Text word){
        word.setY(word.getY()+1); // move the word down in the pane
    }

    private void checkPosition(Text word, Label lives,ArrayList<String> words,
                               int[] status){
        // method to check if word has reached bottom of the pane
        if(400.0 == word.getY()){
            int l = Integer.parseInt(lives.getText().trim());
            if(0 >= l){
                status[0] = 1; // if no lives remaining increment status flag
                GameOver();
            } else{ // if lives remaining decrement the lives
                l -= 1;
                lives.setText(l+"");
                resetWord(word,words); // reset to new word
            }
        }
    }

    private void incrementScore(Text word, Label score, ArrayList<String> words){
        // method to increment the score
        int sc = Integer.parseInt(score.getText().trim());
        sc += 1;
        finalScore += 1;
        score.setText(sc+"");
        resetWord(word,words);
    }
    
    //opens File GenerateStrings.txt to generate list of words into ArrayList 
    private ArrayList<String> GenerateStrings() {
        try {
            ArrayList<String> words = new ArrayList<String>();
            //this will open the text file in the directory
            File file = new File("GenerateStrings.txt");

            //we will use Scanner to scan the file 
            Scanner input = new Scanner(file);
            //while the file has not ended
            while (input.hasNext()){
                //add each word into list
                words.add(input.next());
            }
            return words;
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    private String text = "";
    private void GameOver(){
        Stage endstage = new Stage();
        StackPane pane = new StackPane();
        Label label = new Label("TYPING TUTOR");
        pane.getChildren().add(label);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        if (label.getText().trim().length() == 0)
                            text = "GAME OVER";
                        else
                            text = "";
  
                        Platform.runLater(new Runnable() {
                            @Override 
                            public void run() {
                                label.setText(text);
                            }
                        });           
                        Thread.sleep(200);
                    }
                }
                catch (InterruptedException ex) {
                }
            }
        }).start();
        
        // Create a scene and place it in the stage
        Scene scene = new Scene(pane, 200, 50);
        endstage.setScene(scene); // Place the scene in the stage
        endstage.show(); // Display the stage 
    }
    
    

}
