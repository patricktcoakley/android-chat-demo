package com.chatdemopatrick.chatdemo;

// Android libraries
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

// Glide libraries for image viewer
import com.bumptech.glide.Glide;

// Java libraries
import java.util.List;


// Adapter for chat messages
public class MessageAdapter extends ArrayAdapter<Message> {
    public MessageAdapter(Context context, int resource, List<Message> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_message, parent, false);
        }

        // Views for UI
        ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView);
        TextView messageTextView = (TextView) convertView.findViewById(R.id.messageTextView);
        TextView authorTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        Message message = getItem(position);

        boolean isImage = message.getImageUrl() != null;

        // Checks for images and displays them if true
        if (isImage) {
            messageTextView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            Glide.with(imageView.getContext())
                    .load(message.getImageUrl())
                    .into(imageView);
        } else {
            messageTextView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            messageTextView.setText(message.getText());
        }

        // Sets authors name in view
        authorTextView.setText(message.getName());

        return convertView;
    }
}
