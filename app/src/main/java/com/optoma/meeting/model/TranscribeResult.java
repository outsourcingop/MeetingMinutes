package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TranscribeResult {
   @SerializedName("source")
   @Expose
   public String source;

   @SerializedName("timestamp")
   @Expose
   public String timestamp;

   @SerializedName("durationInTicks")
   @Expose
   public Double durationInTicks;

   @SerializedName("duration")
   @Expose
   public String duration;

   @SerializedName("combinedRecognizedPhrases")
   @Expose
   public List<CombinedRecognizedPhrase> combinedRecognizedPhrases = null;

   @SerializedName("recognizedPhrases")
   @Expose
   public List<RecognizedPhrase> recognizedPhrases = null;
}
