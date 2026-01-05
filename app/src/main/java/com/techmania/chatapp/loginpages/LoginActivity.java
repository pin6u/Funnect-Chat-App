package com.techmania.chatapp.loginpages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.techmania.chatapp.views.MainActivity;
import com.techmania.chatapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding loginBinding;
    FirebaseAuth auth=FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate ( savedInstanceState );
        loginBinding=ActivityLoginBinding.inflate ( getLayoutInflater () );
        setContentView ( loginBinding.getRoot () );
        FirebaseUser user=auth.getCurrentUser ();
        if(user!=null){
            Intent intent=new Intent ( LoginActivity.this, MainActivity.class );
            startActivity ( intent );
            finish ();

        }

        loginBinding.buttonLogin.setOnClickListener ( view -> {
            String email=loginBinding.editTextEmailLogin.getText ().toString ().trim ();
            String password=loginBinding.editTextPasswordLogin.getText ().toString ().trim ();
            if(email.isEmpty () || password.isEmpty ()){
                Toast.makeText (this,"Please enter your email and password",Toast.LENGTH_SHORT).show ();
            }else{
                auth.signInWithEmailAndPassword ( email,password ).addOnCompleteListener ( task -> {
                    if(task.isSuccessful ()){
                        Intent intent=new Intent ( LoginActivity.this,MainActivity.class );
                        startActivity ( intent );
                        finish ();
                    }else{
                        Toast.makeText ( this,task.getException ().getLocalizedMessage (),Toast.LENGTH_SHORT ).show();
                    }
                } );

            }

        });
        loginBinding.textViewSignup.setOnClickListener ( view -> {
            Intent intent=new Intent ( this,SignupActivity.class );
            startActivity ( intent );
        } );

    }
}