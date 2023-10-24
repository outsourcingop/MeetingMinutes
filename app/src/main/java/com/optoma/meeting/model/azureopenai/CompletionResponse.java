package com.optoma.meeting.model.azureopenai;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CompletionResponse {

   @SerializedName("id")
   @Expose
   public String id;

   @SerializedName("choices")
   @Expose
   public List<CompletionChoices> choices;

   @SerializedName("object")
   @Expose
   public String object;

   @SerializedName("created")
   @Expose
   public long created;    // 1682998112

   @SerializedName("model")
   @Expose
   public String model;

   @SerializedName("usage")
   @Expose
   public CompletionUsage usage;
}
