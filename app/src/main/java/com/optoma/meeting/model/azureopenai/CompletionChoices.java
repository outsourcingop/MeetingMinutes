package com.optoma.meeting.model.azureopenai;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompletionChoices {
   @SerializedName("text")
   @Expose
   public String text;

   @SerializedName("index")
   @Expose
   public int index;

   @SerializedName("finish_reason")
   @Expose
   public String finishReason;
}
