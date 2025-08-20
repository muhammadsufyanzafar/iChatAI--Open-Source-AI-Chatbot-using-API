package com.zafar.ichatai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 1;
    private static final int TYPE_AI = 2;

    private final List<com.zafar.ichatai.Message> messages = new ArrayList<>();

    public void addMessage(com.zafar.ichatai.Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void replaceLastAiMessage(String newText) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (!messages.get(i).isUser()) {
                messages.set(i, new com.zafar.ichatai.Message(newText, false));
                notifyItemChanged(i);
                return;
            }
        }
        // If not found, just add
        addMessage(new com.zafar.ichatai.Message(newText, false));
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? TYPE_USER : TYPE_AI;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_USER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_ai, parent, false);
            return new AiHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        com.zafar.ichatai.Message m = messages.get(position);
        if (holder instanceof UserHolder) {
            ((UserHolder) holder).txt.setText(m.getText());
            makeCopyable(holder.itemView.getContext(), ((UserHolder) holder).txt);
        } else if (holder instanceof AiHolder) {
            ((AiHolder) holder).txt.setText(m.getText());
            makeCopyable(holder.itemView.getContext(), ((AiHolder) holder).txt);
        }
    }

    private void makeCopyable(Context ctx, TextView tv) {
        // Native selection
        tv.setTextIsSelectable(true);
        // Long press copy fallback
        tv.setOnLongClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Message", tv.getText()));
                Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    static class UserHolder extends RecyclerView.ViewHolder {
        TextView txt;
        UserHolder(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtMessage);
        }
    }

    static class AiHolder extends RecyclerView.ViewHolder {
        TextView txt;
        AiHolder(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtMessage);
        }
    }
}
