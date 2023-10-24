package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CombinedRecognizedPhrase {
   @SerializedName("channel")
   @Expose
   public Integer channel;

   @SerializedName("lexical")
   @Expose
   public String lexical;

   @SerializedName("itn")
   @Expose
   public String itn;

   @SerializedName("maskedITN")
   @Expose
   public String maskedITN;

   @SerializedName("display")
   @Expose
   public String display;
}
