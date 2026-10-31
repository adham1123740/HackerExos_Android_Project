package com.example.myapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<MainActivity.Message> messages;

    public MessageAdapter(List<MainActivity.Message> messages) {
        this.messages = messages;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        MainActivity.Message m = messages.get(position);
        holder.text.setText(m.sender + ": " + m.text);
        holder.text.setTextColor(m.sender.equals("AI") ? 0xFFB8B8B8 : 0xFFFFFFFF);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ViewHolder(View v) { super(v); text = v.findViewById(R.id.textMessage); }
    }
}