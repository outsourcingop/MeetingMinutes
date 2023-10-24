package com.optoma.meeting.model.azureopenai;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompletionUsage {
   @SerializedName("completion_tokens")
   @Expose
   public int completionTokens;

   @SerializedName("prompt_tokens")
   @Expose
   public int promptTokens;

   @SerializedName("total_tokens")
   @Expose
   public int totalTokens;
}
