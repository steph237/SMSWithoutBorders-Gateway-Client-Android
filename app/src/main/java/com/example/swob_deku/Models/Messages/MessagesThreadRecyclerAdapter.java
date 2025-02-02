package com.example.swob_deku.Models.Messages;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.example.swob_deku.Commons.Contacts;
import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.R;
import com.example.swob_deku.RouterActivity;
import com.example.swob_deku.BroadcastSMSTextActivity;
import com.example.swob_deku.SMSSendActivity;


import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MessagesThreadRecyclerAdapter extends RecyclerView.Adapter<MessagesThreadRecyclerAdapter.ViewHolder> {

    private final AsyncListDiffer<SMS> mDiffer = new AsyncListDiffer(this, DIFF_CALLBACK);

    Context context;
    int renderLayout;
    Boolean isSearch = false;
    String searchString = "";
    RouterActivity routerActivity;

    WorkManager workManager;
    LiveData<List<WorkInfo>> workers;

    private String getSMSFromWorkInfo(WorkInfo workInfo) {
        String[] tags = Helpers.convertSetToStringArray(workInfo.getTags());
        String messageId = "";
        for(int i = 0; i< tags.length; ++i) {
            if (tags[i].contains("swob.work.id")) {
                tags = tags[i].split("\\.");
                messageId = tags[tags.length - 1];
                return messageId;
            }
        }
        return messageId;
    }

    private void workManagerFactories() {

        WorkQuery workQuery = WorkQuery.Builder
                .fromTags(Collections.singletonList(BroadcastSMSTextActivity.TAG_NAME))
                .addStates(Arrays.asList(
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.CANCELLED,
                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.RUNNING))
                .build();

        workers = workManager.getWorkInfosLiveData(workQuery);
        workers.observe(routerActivity, new Observer<List<WorkInfo>>() {
            @Override
            public void onChanged(List<WorkInfo> workInfos) {
                if(workInfos.isEmpty())
                    return;

                List<SMS> smsList = new ArrayList<>(mDiffer.getCurrentList());

                for(WorkInfo workInfo: workInfos) {
                    String messageId = getSMSFromWorkInfo(workInfo);
                    for(int i=0;i<mDiffer.getCurrentList().size();++i)
                        if(mDiffer.getCurrentList().get(i).id.equals(messageId)) {
                            SMS sms = smsList.get(i);
                            sms.routerStatus = workInfo.getState().name();
                            smsList.set(i, sms);
                            break;
                        }
                }
                mDiffer.submitList(smsList);
                notifyDataSetChanged();
            }
        });
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout) {
       this.context = context;
       this.renderLayout = renderLayout;
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout, Boolean isSearch, String searchString) {
        this.context = context;
        this.renderLayout = renderLayout;
        this.isSearch = isSearch;
        this.searchString = searchString;
    }

    public MessagesThreadRecyclerAdapter(Context context, int renderLayout, Boolean isSearch, String searchString,
                                         RouterActivity routerActivity) {
        this.context = context;
        this.renderLayout = renderLayout;
        this.isSearch = isSearch;
        this.searchString = searchString;
        this.routerActivity = routerActivity;

        workManager = WorkManager.getInstance(context);
    }

    @NonNull
    @Override
    public MessagesThreadRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(this.context);
        View view = inflater.inflate(this.renderLayout, parent, false);
        return new MessagesThreadRecyclerAdapter.ViewHolder(view);
    }

    public boolean checkPermissionToReadContacts() {
        int check = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS);

        return (check == PackageManager.PERMISSION_GRANTED);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SMS sms = mDiffer.getCurrentList().get(position);

        if(isSearch && searchString != null && !searchString.isEmpty()) {
            Spannable spannable = Spannable.Factory.getInstance().newSpannable(sms.getBody());
            for(int index = sms.getBody().indexOf(searchString); index >=0; index = sms.getBody().indexOf(searchString, index + 1)) {
                spannable.setSpan(new BackgroundColorSpan(context.getResources().getColor(R.color.highlight_yellow)),
                        index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.black)),
                        index, index + (searchString.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            holder.snippet.setText(spannable);
        }
        else
            holder.snippet.setText(sms.getBody());

        holder.state.setText(sms.getRouterStatus());

        String address = sms.getAddress();
        String contactPhotoUri = "";

        if(checkPermissionToReadContacts() && !address.isEmpty()) {
            contactPhotoUri = Contacts.retrieveContactPhoto(context, address);
            address = Contacts.retrieveContactName(context, address);
        }

        if(!contactPhotoUri.isEmpty() && !contactPhotoUri.equals("null"))
            holder.contactPhoto.setImageURI(Uri.parse(contactPhotoUri));

        holder.address.setText(address);

        String date = sms.getDate();
        if (DateUtils.isToday(Long.parseLong(date))) {
            date = "Today";
        }
        else {
            DateFormat dateFormat = new SimpleDateFormat("MMM dd");
            date = dateFormat.format(new Date(Long.parseLong(date)));
        }
        holder.date.setText(date);

        if(routerActivity != null && !sms.routingUrls.isEmpty()) {
            holder.routingURLText.setVisibility(View.VISIBLE);
            holder.routingUrl.setVisibility(View.VISIBLE);

            StringBuilder routingUrl = new StringBuilder();
            for(int i=0;i<sms.routingUrls.size(); ++i) {
                if (routingUrl.length() > 0)
                    routingUrl.append(", ");
                routingUrl.append(sms.routingUrls.get(i));
            }
            holder.routingUrl.setText(routingUrl);
        }
        else {
            holder.routingURLText.setVisibility(View.GONE);
        }

        // TODO: change color of unread messages in thread
        if(SMSHandler.hasUnreadMessages(context, sms.getThreadId())) {
            // Make bold
            holder.address.setTypeface(null, Typeface.BOLD);
            holder.snippet.setTypeface(null, Typeface.BOLD);

            holder.address.setTextColor(context.getResources().getColor(R.color.read_text));
            holder.snippet.setTextColor(context.getResources().getColor(R.color.read_text));
            holder.date.setTextColor(context.getResources().getColor(R.color.read_text));
        }

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent singleMessageThreadIntent = new Intent(context, SMSSendActivity.class);
                singleMessageThreadIntent.putExtra(SMSSendActivity.ADDRESS, sms.getAddress());
                singleMessageThreadIntent.putExtra(SMSSendActivity.THREAD_ID, sms.getThreadId());

                if (isSearch)
                    singleMessageThreadIntent.putExtra(SMSSendActivity.ID, sms.getId());
                if (searchString != null && !searchString.isEmpty()) {
                    singleMessageThreadIntent.putExtra(SMSSendActivity.SEARCH_STRING, searchString);
                }

                context.startActivity(singleMessageThreadIntent);
            }
        };


        if(sms.getRouterStatus().equals(WorkInfo.State.ENQUEUED.name())) {
            holder.snippet.setOnClickListener(onClickListener);
            holder.state.setText( holder.state.getText().toString());

            holder.state.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: restart the work

//                    ListenableFuture<List<WorkInfo>> workInfos = workManager.getWorkInfos(workQuery);
//
//                    try {
//                        List<WorkInfo> workInfoList = workInfos.get();
//
//                        for(WorkInfo workInfo: workInfoList) {
//                            // TODO: unless bug, failure cannot happen - task requeues if conditions are not met.
//                            // TODO: not totally sure to proceed.
//                            String[] tags = Helpers.convertSetToStringArray(workInfo.getTags());
//                            String messageId = new String();
//                            for(int i = 0; i< tags.length; ++i) {
//                                if (tags[i].contains("swob.work.id")) {
//                                    tags = tags[i].split("\\.");
//                                    messageId = tags[tags.length - 1];
//                                    break;
//                                }
//                            }
//                            if(sms.getId().equals(messageId)) {
//                                // workManager.
//                                // TODO: cancel the work and start a new one.
//                            }
//                        }
//                    } catch(Exception e ) {
//                        e.printStackTrace();
//                    }

                }
            });
        }
        else holder.layout.setOnClickListener(onClickListener);
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    public void submitList(List<SMS> list) {
        if(routerActivity != null) {
//            List<String> smsList = new ArrayList<>();
//
//            for (SMS sms : list)
//                smsList.add(sms.id);

//            workManagerFactories(smsList);
            workManagerFactories();
        }

        mDiffer.submitList(list);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        String id;
        TextView snippet;
        TextView address;
        TextView date;
        TextView state;
        TextView routingUrl;
        TextView routingURLText;
        ImageView contactPhoto;

        ConstraintLayout layout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            snippet = itemView.findViewById(R.id.messages_thread_text);
            address = itemView.findViewById(R.id.messages_thread_address_text);
            date = itemView.findViewById(R.id.messages_thread_date);
            layout = itemView.findViewById(R.id.messages_threads_layout);
            state = itemView.findViewById(R.id.messages_route_state);
            routingUrl = itemView.findViewById(R.id.message_route_url);
            routingURLText = itemView.findViewById(R.id.message_route_status);
            contactPhoto = itemView.findViewById(R.id.messages_threads_contact_photo);
        }
    }

    // TODO:
    public static final DiffUtil.ItemCallback<SMS> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SMS>() {
                @Override
                public boolean areItemsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull SMS oldItem, @NonNull SMS newItem) {
                    return oldItem.equals(newItem);
                }
            };
}
