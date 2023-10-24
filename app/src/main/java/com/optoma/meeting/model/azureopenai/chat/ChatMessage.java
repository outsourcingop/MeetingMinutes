package com.optoma.meeting.model.azureopenai.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatMessage {
   @SerializedName("role")
   @Expose
   public String role;

   @SerializedName("content")
   @Expose
   public String content;

   public ChatMessage(String role, String content) {
      this.role = role;
      this.content = content;
   }
}
