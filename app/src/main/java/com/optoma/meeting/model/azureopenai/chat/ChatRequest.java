package com.optoma.meeting.model.azureopenai.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChatRequest {

   @SerializedName("messages")
   @Expose
   public List<ChatMessage> messages;

   @SerializedName("temperature")
   @Expose
   public float temperature;

   @SerializedName("max_tokens")
   @Expose
   public int max_tokens;

   public ChatRequest(List<ChatMessage> messages) {
      this.messages = messages;
      this.temperature = 0.0f;
      this.max_tokens = 400;
   }
}
