package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;

import java.util.ArrayList;
import java.util.List;

public class SingleMessageViewModel extends ViewModel {
    private MutableLiveData<List<SMS>> messagesList;

    String threadId;

    public LiveData<List<SMS>> getMessages(Context context, String threadId){
        if(messagesList == null) {
            this.threadId = threadId;
            messagesList = new MutableLiveData<>();
            loadSMSThreads(context);
        }
        return messagesList;
    }

    public void informChanges(Context context) {
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) {
        Cursor cursor = SMSHandler.fetchSMSForThread(context, this.threadId);
        if(cursor.moveToFirst()) {
            List<SMS> smsList = new ArrayList<>();
            do {
                SMS sms = new SMS(cursor);
                smsList.add(sms);
            } while(cursor.moveToNext());
            messagesList.setValue(smsList);
        }
    }
}
