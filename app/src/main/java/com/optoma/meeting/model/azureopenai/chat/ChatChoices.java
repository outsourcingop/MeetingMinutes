package com.optoma.meeting.model.azureopenai.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatChoices {

   @SerializedName("message")
   @Expose
   public ChatMessage message;
}
