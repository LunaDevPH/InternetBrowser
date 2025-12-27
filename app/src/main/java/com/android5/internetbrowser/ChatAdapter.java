package com.android5.internetbrowser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    public static final String PAYLOAD_TEXT = "payload_text";

    public static class Message {
        public String text;
        public boolean isUser;
        public Bitmap image;

        public Message(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }

        public Message(String text, boolean isUser, Bitmap image) {
            this.text = text;
            this.isUser = isUser;
            this.image = image;
        }
    }

    private final List<Message> messages;
    private Markwon markwon;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (markwon == null) {
            markwon = Markwon.builder(parent.getContext())
                    .usePlugin(TablePlugin.create(parent.getContext()))
                    .build();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        onBindViewHolder(holder, position, List.of());
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position, @NonNull List<Object> payloads) {
        Message message = messages.get(position);

        if (!payloads.isEmpty()) {
            for (Object payload : payloads) {
                if (PAYLOAD_TEXT.equals(payload)) {
                    markwon.setMarkdown(holder.messageText, message.text);
                    updateCopyButtonVisibility(holder, message);
                    return;
                }
            }
        }

        markwon.setMarkdown(holder.messageText, message.text);

        if (message.image != null) {
            holder.messageImage.setVisibility(View.VISIBLE);
            holder.messageImage.setImageBitmap(message.image);
        } else {
            holder.messageImage.setVisibility(View.GONE);
        }

        RelativeLayout.LayoutParams cardParams = (RelativeLayout.LayoutParams) holder.messageCard.getLayoutParams();
        RelativeLayout.LayoutParams iconParams = (RelativeLayout.LayoutParams) holder.messageIcon.getLayoutParams();

        holder.messageIcon.setColorFilter(Color.WHITE);

        if (message.isUser) {
            iconParams.removeRule(RelativeLayout.ALIGN_PARENT_START);
            iconParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            
            cardParams.removeRule(RelativeLayout.END_OF);
            cardParams.addRule(RelativeLayout.START_OF, R.id.message_icon);
            
            holder.messageIcon.setImageResource(R.drawable.ic_user);
            
            holder.messageCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_user_bg));
            holder.messageText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_user_text));
            holder.btnCopy.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_user_text));
        } else {
            iconParams.removeRule(RelativeLayout.ALIGN_PARENT_END);
            iconParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            
            cardParams.removeRule(RelativeLayout.START_OF);
            cardParams.addRule(RelativeLayout.END_OF, R.id.message_icon);
            
            holder.messageIcon.setImageResource(R.drawable.ic_ai2);

            holder.messageCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_ai_bg));
            holder.messageText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_ai_text));
            holder.btnCopy.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.chat_ai_text));
        }
        
        holder.messageCard.setLayoutParams(cardParams);
        holder.messageIcon.setLayoutParams(iconParams);

        updateCopyButtonVisibility(holder, message);
        holder.btnCopy.setOnClickListener(v -> {
            String code = extractCode(message.text);
            if (!code.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Code", code);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(v.getContext(), "Code copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCopyButtonVisibility(ChatViewHolder holder, Message message) {
        // Only show copy button if the AI message contains a code block
        if (!message.isUser && !extractCode(message.text).isEmpty()) {
            holder.btnCopy.setVisibility(View.VISIBLE);
        } else {
            holder.btnCopy.setVisibility(View.GONE);
        }
    }

    private String extractCode(String markdown) {
        if (markdown == null) return "";
        
        // Regex to find content between triple backticks
        Pattern pattern = Pattern.compile("(?s)```(?:\\w*\\n)?(.*?)\\n?```");
        Matcher matcher = pattern.matcher(markdown);
        
        StringBuilder codeBlocks = new StringBuilder();
        while (matcher.find()) {
            if (codeBlocks.length() > 0) codeBlocks.append("\n\n");
            codeBlocks.append(matcher.group(1).trim());
        }
        
        return codeBlocks.toString();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        ImageView messageImage;
        ImageView messageIcon;
        ImageButton btnCopy;
        MaterialCardView messageCard;

        ChatViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            messageImage = itemView.findViewById(R.id.message_image);
            messageIcon = itemView.findViewById(R.id.message_icon);
            btnCopy = itemView.findViewById(R.id.btn_copy);
            messageCard = itemView.findViewById(R.id.message_card);
        }
    }
}