package com.optoma.meeting.model.azureopenai;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompletionRequest {

//   @SerializedName("model")
//   @Expose
//   private String model;

   @SerializedName("prompt")
   @Expose
   private String prompt;

   @SerializedName("max_tokens")
   @Expose
   private int maxTokens;

//   @SerializedName("temperature")
//   @Expose
//   private float temperature;

   @SerializedName("stop")
   @Expose
   private String stop;

   public CompletionRequest(String prompt) {
      this.prompt = prompt;
      this.stop = "";
      this.maxTokens = 400;
   }

   public static class Message {

      @SerializedName("role")
      @Expose
      private String role;

      @SerializedName("content")
      @Expose
      private String content;

      public Message(String role, String content) {
         this.role = role;
         this.content = content;
      }

   }
}
