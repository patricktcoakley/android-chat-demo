package com.chatdemopatrick.chatdemo;

// Android libraries
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;


// FireBase libraries
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Java libraries
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// App entry point
public class MainActivity extends AppCompatActivity {

    // Declare static final variables
    private static final String TAG = "MainActivity";
    private static final int RC_IMG_PICKER = 2;

    public static final int RC_SIGN_IN = 1;
    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    // Android UI components
    private ListView chatMessageListView;
    private MessageAdapter chatMessageAdapter;
    private ProgressBar chatProgressBar;
    private ImageButton chatImagePickerButton;
    private EditText chatMessageEditText;
    private Button chatSendButton;
    private String chatUserName;

    // FireBase components
    private FirebaseDatabase chatFirebaseDatabase;
    private DatabaseReference chatMsgDatabaseReference;
    private ChildEventListener chatChildEventListener;
    private FirebaseAuth chatFirebaseAuth;
    private FirebaseAuth.AuthStateListener chatAuthStateListener;
    private FirebaseStorage chatFirebaseStorage;
    private StorageReference chatImgStorageReference;

    @Override
    // Startup
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Default username
        chatUserName = ANONYMOUS;

        // Reference database
        chatFirebaseDatabase = FirebaseDatabase.getInstance();
        chatFirebaseAuth = FirebaseAuth.getInstance();

        // Reference storage
        chatFirebaseStorage = FirebaseStorage.getInstance();

        // Reference the type of object being stored
        chatMsgDatabaseReference = chatFirebaseDatabase.getReference().child("messages");
        chatImgStorageReference = chatFirebaseStorage.getReference().child("chat_img");

        // Reference to UI items
        chatProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatMessageListView = (ListView) findViewById(R.id.messageListView);
        chatImagePickerButton = (ImageButton) findViewById(R.id.imagePickerButton);
        chatMessageEditText = (EditText) findViewById(R.id.messageEditText);
        chatSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize and reference the message adapter
        List<Message> Messages = new ArrayList<>();
        chatMessageAdapter = new MessageAdapter(this, R.layout.item_message, Messages);
        chatMessageListView.setAdapter(chatMessageAdapter);

        // Initialize progress bar
        chatProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        chatImagePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_IMG_PICKER);
            }
        });

        // Enable Send button when there's text to send
        chatMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            // Changes the send button color based on text being entered
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    chatSendButton.setEnabled(true);
                } else {
                    chatSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Sets limits on text input length
        chatMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        chatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Message message = new Message(chatMessageEditText.getText().toString(), chatUserName, null);
                chatMsgDatabaseReference.push().setValue(message);

                // Clear input box
                chatMessageEditText.setText("");
            }
        });

        // Event listeners
        chatAuthStateListener = new FirebaseAuth.AuthStateListener() {
          @Override
          public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                // Signed in
                onSignedInInitialize(firebaseUser.getDisplayName());

            } else {
                // Signed out
                onSignedOutCleanup();
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        // Authorization providers; add or remove as needed
                        new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                        new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                );

                // Authorization process
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
          }

        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                // Sign out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chatChildEventListener != null) {
            chatFirebaseAuth.removeAuthStateListener(chatAuthStateListener);
        }
        detachDatabaseReadListener();
        chatMessageAdapter.clear();
    }

    @Override
    // Sign in process
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in.", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Cancelled sign in.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode  == RC_IMG_PICKER && resultCode == RESULT_OK){
                Uri selectedImageUri = data.getData();
                StorageReference imgRef =
                        chatImgStorageReference.child(selectedImageUri.getLastPathSegment());
                imgRef.putFile(selectedImageUri).addOnSuccessListener(this,
                        new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                   // Uploads the image if allowed
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Message message = new Message(null, chatUserName, downloadUrl.toString());
                        chatMsgDatabaseReference.push().setValue(message);
                    }
                });
            }
        }

    @Override
    // Enables the authorization state listener on app resume
    public void onResume() {
        super.onResume();
        chatFirebaseAuth.addAuthStateListener(chatAuthStateListener);
    }

    // Initializes the database reader on sign in
    private void onSignedInInitialize(String username) {
        chatUserName = username;
        attachDatabaseReadListener();
    }

    // Resets authorization and data stores on sign out
    private void onSignedOutCleanup() {
        chatUserName = ANONYMOUS;
        chatMessageAdapter.clear();
        detachDatabaseReadListener();

    }

    // Enables the database reader listener
    private void attachDatabaseReadListener() {
        if (chatChildEventListener == null) {
            chatChildEventListener = new ChildEventListener() {

                @Override
                // Called when a new message is pushed to the database
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Message message = dataSnapshot.getValue(Message.class);
                    chatMessageAdapter.add(message);
                }

                @Override
                // Called when a message is changed
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                // Called when a message is removed
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                // Called when a message changes position
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                // Called when a message data cannot be pushed due to auth or other reasons
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            // Starts the listener for the messages database
            chatMsgDatabaseReference.addChildEventListener(chatChildEventListener);
        }
    }

    // Disables the database reader listener
    private void detachDatabaseReadListener() {
        if (chatChildEventListener != null) {
            chatMsgDatabaseReference.removeEventListener(chatChildEventListener);
            chatChildEventListener = null;
        }
    }
}