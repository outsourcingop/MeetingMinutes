package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Word {
   @SerializedName("word")
   @Expose
   public String word;

   @SerializedName("offset")
   @Expose
   public String offset;

   @SerializedName("duration")
   @Expose
   public String duration;

   @SerializedName("offsetInTicks")
   @Expose
   public Double offsetInTicks;

   @SerializedName("durationInTicks")
   @Expose
   public Double durationInTicks;

   @SerializedName("confidence")
   @Expose
   public Double confidence;
}
