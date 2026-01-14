package com.android5.internetbrowser;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiChatActivity extends AppCompatActivity {

    private static final String TAG = "AiChatActivity";
    private static final String OPENROUTER_API_KEY = "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";
    
    // Persistent messages
    private static final List<ChatAdapter.Message> globalMessages = new ArrayList<>();
    private static final List<OpenRouterService.Message> conversationContext = new ArrayList<>();

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText messageEditText;
    private MaterialButton sendButton;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Object typingToken = new Object();
    
    private OpenRouterService openRouterService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_chat);

        initRetrofit();

        View mainView = findViewById(R.id.main_chat_container);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.chat_recycler_view);
        messageEditText = findViewById(R.id.message_edit_text);
        sendButton = findViewById(R.id.send_button);

        adapter = new ChatAdapter(globalMessages);
        
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
        
        recyclerView.setAdapter(adapter);
        if (!globalMessages.isEmpty()) {
            recyclerView.scrollToPosition(globalMessages.size() - 1);
        }

        sendButton.setOnClickListener(v -> sendMessage(messageEditText.getText().toString().trim()));

        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                sendMessage(messageEditText.getText().toString().trim());
                return true;
            }
            return false;
        });

        // Handle Analysis Intent
        handleAnalyzeIntent(getIntent());
    }

    private void handleAnalyzeIntent(Intent intent) {
        if (intent != null && intent.hasExtra("analyze_content")) {
            String url = intent.getStringExtra("page_url");
            String content = intent.getStringExtra("analyze_content");
            
            // Limit content size to avoid token issues (basic trimming)
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "...";
            }

            String prompt = "Please analyze and summarize the following web page content from " + url + ":\n\n" + content;
            
            // Show a friendly message to the user in the UI
            addMessage("Analyze" + url, true);
            
            // Trigger AI request with the hidden full prompt
            performAiRequest(prompt);
        }
    }

    private void initRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://openrouter.ai/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        openRouterService = retrofit.create(OpenRouterService.class);
    }

    private void sendMessage(String text) {
        if (text.isEmpty()) return;

        // Add to UI
        addMessage(text, true);
        messageEditText.setText("");
        
        performAiRequest(text);
    }

    private void performAiRequest(String text) {
        sendButton.setEnabled(false);

        // Add to API context
        conversationContext.add(new OpenRouterService.Message("user", text));

        // Add placeholder for AI
        final int aiMessageIndex = globalMessages.size();
        addMessage("", false);

        OpenRouterService.Request request = new OpenRouterService.Request(
                "openai/gpt-3.5-turbo",
                conversationContext
        );

        openRouterService.getCompletion("Bearer " + OPENROUTER_API_KEY, request)
                .enqueue(new Callback<OpenRouterService.Response>() {
            @Override
            public void onResponse(Call<OpenRouterService.Response> call, Response<OpenRouterService.Response> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().choices.isEmpty()) {
                    String aiText = response.body().choices.get(0).message.content;
                    conversationContext.add(new OpenRouterService.Message("assistant", aiText));
                    simulateTyping(aiText, aiMessageIndex);
                } else {
                    String errorMsg = "API Error: " + response.code();
                    if (response.errorBody() != null) {
                        try {
                            String errorJson = response.errorBody().string();
                            OpenRouterService.ErrorResponse errorObj = new Gson().fromJson(errorJson, OpenRouterService.ErrorResponse.class);
                            if (errorObj != null && errorObj.error != null) {
                                errorMsg = errorObj.error.message;
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    if (response.code() == 429) {
                        errorMsg = "Model is busy/rate-limited. Please wait 10-20 seconds and try again.";
                    }
                    handleError(errorMsg, aiMessageIndex);
                }
            }

            @Override
            public void onFailure(Call<OpenRouterService.Response> call, Throwable t) {
                handleError("Network Error: " + t.getMessage(), aiMessageIndex);
            }
        });
    }

    private void handleError(String message, int index) {
        runOnUiThread(() -> {
            sendButton.setEnabled(true);
            updateMessage(message, index);
            Toast.makeText(AiChatActivity.this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void simulateTyping(String fullText, int index) {
        typingToken = new Object();
        final Object currentToken = typingToken;
        final long delayPerChar = fullText.length() > 500 ? 5 : 15;

        for (int i = 0; i < fullText.length(); i++) {
            final String textToUpdate = fullText.substring(0, i + 1);
            final boolean isLast = (i == fullText.length() - 1);
            
            mainHandler.postDelayed(() -> {
                if (currentToken == typingToken) {
                    updateMessage(textToUpdate, index);
                    if (isLast) {
                        sendButton.setEnabled(true);
                    }
                }
            }, (long) i * delayPerChar);
        }
    }

    private void addMessage(String text, boolean isUser) {
        runOnUiThread(() -> {
            globalMessages.add(new ChatAdapter.Message(text, isUser));
            adapter.notifyItemInserted(globalMessages.size() - 1);
            recyclerView.scrollToPosition(globalMessages.size() - 1);
        });
    }

    private void updateMessage(String text, int index) {
        runOnUiThread(() -> {
            if (index < globalMessages.size()) {
                globalMessages.get(index).text = text;
                adapter.notifyItemChanged(index, ChatAdapter.PAYLOAD_TEXT);
                if (!recyclerView.canScrollVertically(1)) {
                    recyclerView.scrollToPosition(globalMessages.size() - 1);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
    }
}