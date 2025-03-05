package com.zhoujh.lvtu.message.adapter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.flyjingfish.openimagelib.BaseInnerFragment;
import com.flyjingfish.openimagelib.OpenImage;
import com.flyjingfish.openimagelib.beans.OpenImageUrl;
import com.flyjingfish.openimagelib.listener.OnItemLongClickListener;
import com.flyjingfish.openimagelib.listener.SourceImageViewIdGet;
import com.flyjingfish.openimagelib.transformers.ScaleInTransformer;
import com.google.gson.Gson;
import com.hyphenate.chat.EMImageMessageBody;
import com.hyphenate.chat.EMMessage;
import com.zhoujh.lvtu.MainActivity;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.message.ChatActivity;
import com.zhoujh.lvtu.personal.UserInfoActivity;
import com.zhoujh.lvtu.utils.HuanXinUtils;
import com.zhoujh.lvtu.utils.Utils;
import com.zhoujh.lvtu.utils.modle.ImageEntity;
import com.zhoujh.lvtu.utils.modle.UserInfo;

import java.util.ArrayList;
import java.util.List;

import okhttp3.internal.Util;

public class MessageDisplayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<EMMessage> chatMessages = new ArrayList<>();
    private List<UserInfo> userInfoList;
    private final Context context;
    private final Gson gson = MainActivity.gson;
    // 定义不同的视图类型，比如自己发送的消息和对方发送的消息可以用不同布局展示
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public MessageDisplayAdapter(Context context, List<UserInfo> userInfoList) {
        this.context = context;
        this.userInfoList = userInfoList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            // 加载自己发送消息的布局文件
            View view = inflater.inflate(R.layout.item_chat_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            // 加载接收消息的布局文件，这里假设叫
            View view = inflater.inflate(R.layout.item_chat_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        RequestOptions requestOptions = new RequestOptions()
                .transform(new CircleCrop());
        EMMessage message = chatMessages.get(position);
        if (holder instanceof SentMessageViewHolder) {
            if (message.getType() == EMMessage.Type.TXT) {
                ((SentMessageViewHolder) holder).bindTxt(message);
            } else if (message.getType() == EMMessage.Type.IMAGE) {
                ((SentMessageViewHolder) holder).bindImg(message,context);
            }
            Glide.with(context)
                    .load("http://" + MainActivity.IP + MainActivity.user.getAvatarUrl())
                    .placeholder(R.drawable.headimg)
                    .apply(requestOptions)
                    .into(((SentMessageViewHolder) holder).avatar);
            ((SentMessageViewHolder) holder).avatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("fragment_to_load", "action_my");
                context.startActivity(intent);
            });
        } else if (holder instanceof ReceivedMessageViewHolder) {
            if (message.getType() == EMMessage.Type.TXT) {
                ((ReceivedMessageViewHolder) holder).bindTxt(message);
            } else if (message.getType() == EMMessage.Type.IMAGE) {
                ((ReceivedMessageViewHolder) holder).bindImg(message,context);
            }
            UserInfo u = new UserInfo();
            for (int i = 0; i < userInfoList.size(); i++) {
                if (HuanXinUtils.createHXId(userInfoList.get(i).getUserId()).equals(message.getFrom())) {
                    u = userInfoList.get(i);
                    break;
                }
            }
            Glide.with(context)
                    .load("http://" + MainActivity.IP + u.getAvatarUrl())
                    .placeholder(R.drawable.headimg)
                    .apply(requestOptions)
                    .into(((ReceivedMessageViewHolder) holder).avatar);
            UserInfo finalU = u;
            ((ReceivedMessageViewHolder) holder).avatar.setOnClickListener(v -> {
                Intent intent = new Intent(context, UserInfoActivity.class);
                intent.putExtra("userInfo", gson.toJson(finalU));
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        EMMessage message = chatMessages.get(position);
        if (message.getFrom().equals(ChatActivity.USER)) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    public void addMessage(EMMessage message) {
        chatMessages.add(message);
        notifyItemInserted(chatMessages.size() - 1);
    }

    public void addHistory(List<EMMessage> messages) {
        chatMessages.addAll(0, messages);
        notifyItemRangeInserted(0, messages.size());
    }

    // 自己发送消息的ViewHolder
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout bg;
        TextView textViewMessage;
        ImageView avatar;
        ImageView img;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.bg);
            textViewMessage = itemView.findViewById(R.id.text_view_sent_message);
            avatar = itemView.findViewById(R.id.avatar);
            img = itemView.findViewById(R.id.img);
        }

        public void bindTxt(EMMessage message) {
            bg.setBackgroundResource(R.drawable.bubble_me);
            img.setVisibility(View.GONE);
            textViewMessage.setVisibility(View.VISIBLE);
            textViewMessage.setText(Utils.absContent(message.getBody().toString()));
        }

        public void bindImg(EMMessage message, Context context) {
            EMImageMessageBody imgBody = (EMImageMessageBody) message.getBody();
            // 从服务器端获取图片文件。
            String imgRemoteUrl = imgBody.getRemoteUrl();
            // 从服务器端获取图片缩略图。
            String thumbnailUrl = imgBody.getThumbnailUrl();
//            ViewGroup.LayoutParams lp = img.getLayoutParams();
//            lp.height = Utils.dpToPx(100, itemView.getContext());
//            lp.width = Utils.dpToPx(100, itemView.getContext());
//            img.setLayoutParams(lp);
            Log.i("imgRemoteUrl", imgRemoteUrl);
            Log.i("thumbnailUrl", thumbnailUrl);
            String loadUrl = "";
            if (thumbnailUrl.equals("")){
                loadUrl = imgRemoteUrl;
            } else {
                loadUrl = thumbnailUrl;
            }
            bg.setBackground(null);
            img.setVisibility(View.VISIBLE);
            textViewMessage.setVisibility(View.GONE);
            // 加载为四个都是圆角的图片 可以设置圆角幅度
            RequestOptions options = new RequestOptions().bitmapTransform(new RoundedCorners(Utils.dip2px(context, 10)));
            Glide.with(itemView.getContext())
                    .load(loadUrl)
                    .apply(options)
                    .override(Utils.dpToPx(200, context), Utils.dpToPx(200, context))  // 设置最大宽高
                    .into(img);
            ImageEntity imgEntity = new ImageEntity(imgRemoteUrl, loadUrl, null, null, null, ImageEntity.TYPE_IMAGE);
            img.setOnClickListener(v -> {
                OpenImage.with(context)
                        .setClickImageView(img)
                        .setAutoScrollScanPosition(true)
                        .setImageUrl(imgEntity)
                        .setSrcImageViewScaleType(ImageView.ScaleType.CENTER_CROP, true)
                        .addPageTransformer(new ScaleInTransformer())
//                        .setOnItemLongClickListener(new OnItemLongClickListener() {
//                            @Override
//                            public void onItemLongClick(BaseInnerFragment fragment, OpenImageUrl openImageUrl, int position) {
//                                Toast.makeText(context, "长按图片", Toast.LENGTH_LONG).show();
//                            }
//                        })
                        .setShowDownload()
                        .show();
            });
        }
    }

    // 接收消息的ViewHolder
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout bg;
        TextView textViewMessage;
        ImageView avatar;
        ImageView img;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            bg = itemView.findViewById(R.id.bg);
            textViewMessage = itemView.findViewById(R.id.text_view_received_message);
            avatar = itemView.findViewById(R.id.avatar);
            img = itemView.findViewById(R.id.img);
        }

        public void bindTxt(EMMessage message) {
            bg.setBackgroundResource(R.drawable.bubble_other);
            textViewMessage.setVisibility(View.VISIBLE);
            textViewMessage.setText(Utils.absContent(message.getBody().toString()));
        }

        public void bindImg(EMMessage message, Context context) {
            EMImageMessageBody imgBody = (EMImageMessageBody) message.getBody();
            // 从服务器端获取图片文件。
            String imgRemoteUrl = imgBody.getRemoteUrl();
            // 从服务器端获取图片缩略图。
            String thumbnailUrl = imgBody.getThumbnailUrl();
//            ViewGroup.LayoutParams lp = img.getLayoutParams();
//            lp.height = Utils.dpToPx(100, itemView.getContext());
//            lp.width = Utils.dpToPx(100, itemView.getContext());
//            img.setLayoutParams(lp);
            String loadUrl = "";
            if (thumbnailUrl.equals("")){
                loadUrl = imgRemoteUrl;
            } else {
                loadUrl = thumbnailUrl;
            }
            bg.setBackground(null);
            img.setVisibility(View.VISIBLE);
            textViewMessage.setVisibility(View.GONE);
            // 加载为四个都是圆角的图片 可以设置圆角幅度
            RequestOptions options = new RequestOptions().bitmapTransform(new RoundedCorners(Utils.dip2px(context, 10)));
            Glide.with(itemView.getContext())
                    .load(loadUrl)
                    .apply(options)
                    .override(Utils.dpToPx(200, context), Utils.dpToPx(200, context))  // 设置最大宽高
                    .into(img);
            ImageEntity imgEntity = new ImageEntity(imgRemoteUrl, loadUrl, null, null, null, ImageEntity.TYPE_IMAGE);
            img.setOnClickListener(v -> {
                OpenImage.with(context)
                        .setClickImageView(img)
                        .setAutoScrollScanPosition(true)
                        .setImageUrl(imgEntity)
                        .setSrcImageViewScaleType(ImageView.ScaleType.CENTER_CROP, true)
                        .addPageTransformer(new ScaleInTransformer())
//                        .setOnItemLongClickListener(new OnItemLongClickListener() {
//                            @Override
//                            public void onItemLongClick(BaseInnerFragment fragment, OpenImageUrl openImageUrl, int position) {
//                                Toast.makeText(context, "长按图片", Toast.LENGTH_LONG).show();
//                            }
//                        })
                        .setShowDownload()
                        .show();
            });
        }
    }
}
