package com.optoma.meeting.model.azureopenai;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompletionMessage {
   @SerializedName("role")
   @Expose
   public String role;

   @SerializedName("content")
   @Expose
   public String content;

   // Add a constructor that accepts two String arguments
   public CompletionMessage(String role, String content) {
      this.role = role;
      this.content = content;
   }
}
