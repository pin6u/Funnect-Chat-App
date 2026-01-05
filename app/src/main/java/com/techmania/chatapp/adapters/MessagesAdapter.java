package com.techmania.chatapp.adapters;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.techmania.chatapp.databinding.ReceivedMessagesItemBinding;
import com.techmania.chatapp.databinding.SendMessagesItemBinding;
import com.techmania.chatapp.models.MessagesModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT_MESSAGE = 1;
    private static final int VIEW_TYPE_RECEIVED_MESSAGE = 2;

    ArrayList<MessagesModel> messagesList;
    String currentUserId;
    OnMessageClickListener onMessageClickListener;

    public MessagesAdapter(ArrayList<MessagesModel> messagesList, String currentUserId, OnMessageClickListener onMessageClickListener) {
        this.messagesList = messagesList;
        this.currentUserId = currentUserId;
        this.onMessageClickListener = onMessageClickListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT_MESSAGE) {
            SendMessagesItemBinding binding = SendMessagesItemBinding.inflate(inflater, parent, false);
            return new SentMessagesViewHolder(binding);
        } else {
            ReceivedMessagesItemBinding binding = ReceivedMessagesItemBinding.inflate(inflater, parent, false);
            return new ReceivedMessagesViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessagesModel message = messagesList.get(position);
        boolean showDateHeader = false;
        String currentDateHeader = getDateHeader(message.getTimestamp());

        if (position == 0 || !getDateHeader(messagesList.get(position - 1).getTimestamp()).equals(currentDateHeader)) {
            showDateHeader = true;
        }

        if (holder.getItemViewType() == VIEW_TYPE_SENT_MESSAGE) {
            SentMessagesViewHolder viewHolder = (SentMessagesViewHolder) holder;
            bindMessage(viewHolder.binding.getRoot().getContext(), viewHolder.binding, message, showDateHeader, true);
        } else {
            ReceivedMessagesViewHolder viewHolder = (ReceivedMessagesViewHolder) holder;
            bindMessage(viewHolder.binding.getRoot().getContext(), viewHolder.binding, message, showDateHeader, false);
        }
    }

    private void bindMessage(Context context, Object bindingObj, MessagesModel message, boolean showDateHeader, boolean isSent) {
        if (isSent) {
            SendMessagesItemBinding binding = (SendMessagesItemBinding) bindingObj;
            setupCommonUI(binding.dateHeader, showDateHeader, message.getTimestamp());

            binding.textViewMessageSent.setVisibility(View.GONE);
            binding.imageViewMedia.setVisibility(View.GONE);
            binding.pdfContainer.setVisibility(View.GONE);
            binding.textViewDeletedMessage.setVisibility(View.GONE);

            if (Boolean.TRUE.equals(message.isDeleted())) {
                binding.textViewDeletedMessage.setVisibility(View.VISIBLE);
            } else if (message.getMediaUrl() != null) {
                handleMedia(context, binding.textViewMessageSent, binding.imageViewMedia, binding.pdfContainer,
                        binding.textViewPdfName, message);
            } else {
                binding.textViewMessageSent.setVisibility(View.VISIBLE);
                binding.textViewMessageSent.setText(message.getMessage());
            }

            binding.textViewTimeSent.setText(formatTimestamp(message.getTimestamp()));
            updateStatusIndicator(context, binding.textViewMessageStatus, message.getStatus());

            binding.getRoot().setOnLongClickListener(v -> {
                onMessageClickListener.onMessageClicked(message);
                return true;
            });

        } else {
            ReceivedMessagesItemBinding binding = (ReceivedMessagesItemBinding) bindingObj;
            setupCommonUI(binding.dateHeader, showDateHeader, message.getTimestamp());

            binding.textViewMessageReceived.setVisibility(View.GONE);
            binding.imageViewMedia.setVisibility(View.GONE);
            binding.pdfContainer.setVisibility(View.GONE);
            binding.textViewDeletedMessage.setVisibility(View.GONE);

            if (Boolean.TRUE.equals(message.isDeleted())) {
                binding.textViewDeletedMessage.setVisibility(View.VISIBLE);
            } else if (message.getMediaUrl() != null) {
                handleMedia(context, binding.textViewMessageReceived, binding.imageViewMedia, binding.pdfContainer,
                        binding.textViewPdfName, message);
            } else {
                binding.textViewMessageReceived.setVisibility(View.VISIBLE);
                binding.textViewMessageReceived.setText(message.getMessage());
            }

            binding.textViewTimeReceived.setText(formatTimestamp(message.getTimestamp()));

            binding.getRoot().setOnLongClickListener(v -> {
                onMessageClickListener.onMessageClicked(message);
                return true;
            });
        }
    }

    private void setupCommonUI(View dateHeaderView, boolean showDateHeader, long timestamp) {
        dateHeaderView.setVisibility(showDateHeader ? View.VISIBLE : View.GONE);
        if (showDateHeader && dateHeaderView instanceof android.widget.TextView) {
            ((android.widget.TextView) dateHeaderView).setText(getDateHeader(timestamp));
        }
    }

    private void handleMedia(Context context, View textView, View imageView, View pdfContainer,
                             android.widget.TextView pdfNameView, MessagesModel message) {

        String type = message.getType();
        String url = message.getMediaUrl();

        if ("pdf".equals(type)) {
            pdfContainer.setVisibility(View.VISIBLE);
            pdfNameView.setText("PDF File");

            pdfContainer.setOnClickListener(v -> downloadAndOpenFile(context, url, "application/pdf", "pdf"));
            pdfContainer.setOnLongClickListener(v -> {
                downloadFileOnly(context, url, "pdf");
                return true;
            });

        } else if ("image".equals(type)) {
            imageView.setVisibility(View.VISIBLE);
            Picasso.get().load(url).into((android.widget.ImageView) imageView);

            imageView.setOnClickListener(v -> downloadAndOpenFile(context, url, "image/*", "jpg"));
            imageView.setOnLongClickListener(v -> {
                downloadFileOnly(context, url, "jpg");
                return true;
            });

        } else {
            textView.setVisibility(View.VISIBLE);
            ((android.widget.TextView) textView).setText("Open file: " + type.toUpperCase());
            textView.setOnClickListener(v -> downloadAndOpenFile(context, url, "*/*", type));
            textView.setOnLongClickListener(v -> {
                downloadFileOnly(context, url, type);
                return true;
            });
        }
    }

    private void downloadAndOpenFile(Context context, String fileUrl, String mimeType, String fileType) {
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                InputStream input = url.openStream();
                File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(directory, "file_" + System.currentTimeMillis() + "." + fileType);

                FileOutputStream output = new FileOutputStream(file);
                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(uri, mimeType);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setClipData(ClipData.newRawUri("", uri));
                        context.startActivity(Intent.createChooser(intent, "Open with"));
                    } catch (Exception e) {
                        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void downloadFileOnly(Context context, String fileUrl, String fileType) {
        new Thread(() -> {
            try {
                URL url = new URL(fileUrl);
                URLConnection connection = url.openConnection();
                connection.connect();

                InputStream input = url.openStream();
                File directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(directory, "file_" + System.currentTimeMillis() + "." + fileType);

                FileOutputStream output = new FileOutputStream(file);
                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateStatusIndicator(Context context, android.widget.TextView statusView, String status) {
        String statusSymbol = "✓";
        int statusColorRes = android.R.color.darker_gray;

        switch (status) {
            case "delivered":
                statusSymbol = "✓✓";
                break;
            case "seen":
                statusSymbol = "✓✓";
                statusColorRes = android.R.color.holo_blue_dark;
                break;
        }

        statusView.setText(statusSymbol);
        statusView.setTextColor(ContextCompat.getColor(context, statusColorRes));
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messagesList.get(position).getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT_MESSAGE : VIEW_TYPE_RECEIVED_MESSAGE;
    }

    public static class SentMessagesViewHolder extends RecyclerView.ViewHolder {
        SendMessagesItemBinding binding;
        public SentMessagesViewHolder(@NonNull SendMessagesItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class ReceivedMessagesViewHolder extends RecyclerView.ViewHolder {
        ReceivedMessagesItemBinding binding;
        public ReceivedMessagesViewHolder(@NonNull ReceivedMessagesItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnMessageClickListener {
        void onMessageClicked(MessagesModel messagesModel);
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    private String getDateHeader(long timestamp) {
        Calendar messageCal = Calendar.getInstance();
        messageCal.setTimeInMillis(timestamp);
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DATE, -1);

        if (DateUtils.isToday(timestamp)) {
            return "Today";
        } else if (isSameDay(messageCal, yesterday)) {
            return "Yesterday";
        } else {
            return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(messageCal.getTime());
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
