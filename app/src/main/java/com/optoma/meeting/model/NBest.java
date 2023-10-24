package com.optoma.meeting.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class NBest {
   @SerializedName("confidence")
   @Expose
   public Double confidence;

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

   @SerializedName("words")
   @Expose
   public List<Word> words = null;
}
