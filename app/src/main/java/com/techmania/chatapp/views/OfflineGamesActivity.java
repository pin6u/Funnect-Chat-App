package com.techmania.chatapp.views;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.techmania.chatapp.R;

import java.util.Random;

public class OfflineGamesActivity extends AppCompatActivity {

    private int ticTacToeWins = 0, numberGuessWins = 0, rpsWins = 0, coinWins = 0, mathWins = 0, scrambleWins = 0, evenOddWins = 0;
    private Random random;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_games);

        random = new Random();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        findViewById(R.id.btnTicTacToe).setOnClickListener(v -> startTicTacToe());
        findViewById(R.id.btnNumberGuess).setOnClickListener(v -> startNumberGuess());
        findViewById(R.id.btnRockPaper).setOnClickListener(v -> startRockPaperScissors());
        findViewById(R.id.btnCoinToss).setOnClickListener(v -> startCoinToss());
        findViewById(R.id.btnMathQuiz).setOnClickListener(v -> startMathQuiz());
        findViewById(R.id.btnWordScramble).setOnClickListener(v -> startWordScramble());
        findViewById(R.id.btnEvenOdd).setOnClickListener(v -> startEvenOdd());
    }



    private void showScore(String game, int score) {
        Toast.makeText(this, game + " Wins: " + score, Toast.LENGTH_SHORT).show();
    }

    private void startTicTacToe() {

        ticTacToeWins++; // Simulated win for now
        showScore("Tic Tac Toe", ticTacToeWins);
    }

    private void startNumberGuess() {
        int numberToGuess = random.nextInt(100) + 1;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guess the number (1-100)");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Guess", (dialog, which) -> {
            try {
                int guess = Integer.parseInt(input.getText().toString());
                if (guess == numberToGuess) {

                    numberGuessWins++;
                    showScore("Number Guess", numberGuessWins);
                } else {
                    Toast.makeText(this, guess < numberToGuess ? "Too Low!" : "Too High!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startRockPaperScissors() {
        String[] choices = {"Rock", "Paper", "Scissors"};
        int computerChoice = random.nextInt(3);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Rock, Paper, or Scissors");

        builder.setItems(choices, (dialog, which) -> {
            int user = which;
            int comp = computerChoice;
            if (user == comp) {
                Toast.makeText(this, "Draw!", Toast.LENGTH_SHORT).show();
            } else if ((user == 0 && comp == 2) || (user == 1 && comp == 0) || (user == 2 && comp == 1)) {

                rpsWins++;
                showScore("RPS", rpsWins);
            } else {
                Toast.makeText(this, "You Lose!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void startCoinToss() {
        String[] options = {"Heads", "Tails"};
        int coin = random.nextInt(2);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Heads or Tails");

        builder.setItems(options, (dialog, which) -> {
            if (which == coin) {

                coinWins++;
                showScore("Coin Toss", coinWins);
            } else {
                Toast.makeText(this, "Wrong Guess!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }

    private void startMathQuiz() {
        int a = random.nextInt(20) + 1;
        int b = random.nextInt(20) + 1;
        int result = a + b;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What is " + a + " + " + b + "?");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            try {
                int ans = Integer.parseInt(input.getText().toString());
                if (ans == result) {

                    mathWins++;
                    showScore("Math Quiz", mathWins);
                } else {
                    Toast.makeText(this, "Incorrect!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void startWordScramble() {
        String[] words = {"apple", "banana", "orange", "grapes", "melon"};
        String original = words[random.nextInt(words.length)];
        String scrambled = scrambleWord(original);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Unscramble: " + scrambled);

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            if (input.getText().toString().equalsIgnoreCase(original)) {

                scrambleWins++;
                showScore("Word Scramble", scrambleWins);
            } else {
                Toast.makeText(this, "Wrong Answer!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String scrambleWord(String word) {
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int j = random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    private void startEvenOdd() {
        int number = random.nextInt(100) + 1;
        String correct = number % 2 == 0 ? "Even" : "Odd";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Is " + number + " Even or Odd?");

        builder.setItems(new String[]{"Even", "Odd"}, (dialog, which) -> {
            String guess = which == 0 ? "Even" : "Odd";
            if (guess.equals(correct)) {
             
                evenOddWins++;
                showScore("Even/Odd", evenOddWins);
            } else {
                Toast.makeText(this, "Wrong!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.show();
    }
}
