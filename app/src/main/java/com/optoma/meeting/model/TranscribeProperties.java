package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TranscribeProperties {
   @SerializedName("diarizationEnabled")
   @Expose
   public Boolean diarizationEnabled;

   @SerializedName("wordLevelTimestampsEnabled")
   @Expose
   public Boolean wordLevelTimestampsEnabled;

   @SerializedName("channels")
   @Expose
   public List<Integer> channels = new ArrayList<Integer>();

   @SerializedName("punctuationMode")
   @Expose
   public String punctuationMode;

   @SerializedName("profanityFilterMode")
   @Expose
   public String profanityFilterMode;

   @SerializedName("timeToLive")
   @Expose
   public String timeToLive;
}
