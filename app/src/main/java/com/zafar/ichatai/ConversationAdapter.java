package com.zafar.ichatai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zafar.ichatai.data.local.entity.Chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.VH> implements Filterable {

    public interface Listener {
        void onChatClicked(Chat chat);
        void onRename(Chat chat);
        void onDelete(Chat chat);
    }

    private final List<Chat> all = new ArrayList<>();
    private final List<Chat> shown = new ArrayList<>();
    private final Listener listener;

    public ConversationAdapter(Listener listener) { this.listener = listener; }

    public void setItems(List<Chat> chats) {
        all.clear(); if (chats != null) all.addAll(chats);
        shown.clear(); shown.addAll(all);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Chat c = shown.get(position);
        h.title.setText(c.title);
        h.itemView.setOnClickListener(v -> listener.onChatClicked(c));
        h.more.setOnClickListener(v -> {
            PopupMenu pm = new PopupMenu(v.getContext(), v);
            pm.getMenu().add(0, 1, 0, "Rename");
            pm.getMenu().add(0, 2, 1, "Delete");
            pm.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) listener.onRename(c);
                else if (item.getItemId() == 2) listener.onDelete(c);
                return true;
            });
            pm.show();
        });
    }

    @Override public int getItemCount() { return shown.size(); }

    @Override public Filter getFilter() {
        return new Filter() {
            @Override protected FilterResults performFiltering(CharSequence constraint) {
                String q = constraint != null ? constraint.toString().toLowerCase(Locale.getDefault()) : "";
                List<Chat> out = new ArrayList<>();
                if (q.isEmpty()) out.addAll(all);
                else for (Chat c : all) if (c.title != null && c.title.toLowerCase(Locale.getDefault()).contains(q)) out.add(c);
                FilterResults fr = new FilterResults();
                fr.values = out; fr.count = out.size();
                return fr;
            }
            @Override protected void publishResults(CharSequence constraint, FilterResults results) {
                shown.clear();
                if (results.values != null) shown.addAll((List<Chat>) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageButton more;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            more = itemView.findViewById(R.id.btnMore);
        }
    }
}
