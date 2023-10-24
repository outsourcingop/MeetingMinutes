package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecognizedPhrase {
   @SerializedName("recognitionStatus")
   @Expose
   public String recognitionStatus;

   @SerializedName("channel")
   @Expose
   public Integer channel;

   @SerializedName("speaker")
   @Expose
   public Integer speaker;

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

   @SerializedName("nBest")
   @Expose
   public List<NBest> nBest = null;
}
